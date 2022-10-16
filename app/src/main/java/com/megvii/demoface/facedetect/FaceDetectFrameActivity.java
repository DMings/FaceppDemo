package com.megvii.demoface.facedetect;

import static com.megvii.facepp.multi.sdk.FaceDetectApi.FaceppConfig.DETECTION_MODE_TRACKING;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.megvii.demoface.BaseActivity;
import com.megvii.demoface.R;
import com.megvii.demoface.camera.Camera1;
import com.megvii.demoface.camera.CameraManager;
import com.megvii.demoface.filter.GLFramebuffer;
import com.megvii.demoface.filter.GLImageFaceReshapeFilter;
import com.megvii.demoface.filter.GLImageFilter2;
import com.megvii.demoface.filter.bean.BeautyParam;
import com.megvii.demoface.filter.landmark.LandmarkEngine;
import com.megvii.demoface.filter.landmark.OneFace;
import com.megvii.demoface.filter.sticker.DynamicStickerNormalFilter;
import com.megvii.demoface.filter.sticker.bean.DynamicSticker;
import com.megvii.demoface.filter.sticker.resource.ResourceHelper;
import com.megvii.demoface.filter.sticker.resource.ResourceJsonCodec;
import com.megvii.demoface.filter.sticker.resource.bean.ResourceData;
import com.megvii.demoface.filter.sticker.resource.bean.ResourceType;
import com.megvii.demoface.filter.utils.ByteBufferUtil;
import com.megvii.demoface.filter.utils.GLConstant;
import com.megvii.demoface.filter.utils.OpenGLUtils;
import com.megvii.demoface.filter.utils.TextureRotationUtils;
import com.megvii.demoface.opengl.CameraMatrix;
import com.megvii.demoface.opengl.ICameraMatrix;
import com.megvii.demoface.opengl.PointsMatrix;
import com.megvii.demoface.utils.Screen;
import com.megvii.facepp.multi.sdk.FaceDetectApi;
import com.megvii.facepp.multi.sdk.FacePPImage;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class FaceDetectFrameActivity extends BaseActivity implements Camera1.CameraOpenCallback, SurfaceTexture.OnFrameAvailableListener, Camera1.ICameraCallback, SurfaceHolder.Callback {
    TextView tvTitleBar;
    LinearLayout llGoBack;
    SurfaceView mGlSurfaceView;
    private int mWidth;
    private int mHeight;
    private int mAngle;

    private CameraManager mCameraManager;

    private EglCore mEglCore = new EglCore();
    private EGLSurface mEGLSurface;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_face_frame;
    }

    @Override
    protected void initView() {
        tvTitleBar = findViewById(R.id.tv_title_bar);
        llGoBack = findViewById(R.id.ll_go_back);
        mGlSurfaceView = findViewById(R.id.opengl_layout_surfaceview);
        mGlSurfaceView.getHolder().addCallback(this);
        llGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void initData() {
        tvTitleBar.setText("人脸检测高阶版");
        mCameraManager = new CameraManager(this);
        mCameraManager.setCameraOpenCallback(this);
        ResourceHelper.initAssetsResource(FaceDetectFrameActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraManager.closeCamera();
    }

    private void adjustTextureViewSize(int width, int height) {
        float scale = Math.min(Screen.mHeight * 1.0f / height, Screen.mWidth * 1.0f / width);
        int layout_width = (int) (width * scale);
        int layout_height = (int) (height * scale);
        LinearLayout.LayoutParams surfaceParams = (LinearLayout.LayoutParams) mGlSurfaceView.getLayoutParams();
        int topMargin = Math.min((Screen.mHeight - layout_height) / 2, 0);
        int leftMargin = Math.min((Screen.mWidth - layout_width) / 2, 0);
        surfaceParams.width = layout_width;
        surfaceParams.height = layout_height;
        surfaceParams.topMargin = topMargin;
        surfaceParams.leftMargin = leftMargin;
        mGlSurfaceView.setLayoutParams(surfaceParams);
    }

    private FacePPImage.Builder imageBuilder;

    @Override
    public void onPreviewFrame(final byte[] bytes, Camera camera) {
        Log.e("c_tid", "thread id " + Thread.currentThread().getId());
        int width = mWidth;
        int height = mHeight;
        final FaceDetectApi.Face[] faces = FaceDetectApi.getInstance().detectFace(imageBuilder.setData(bytes).build());
        final StringBuilder sb = new StringBuilder();
        if (faces != null && faces.length > 0) {
            ArrayList<ArrayList> pointsOpengl = new ArrayList<ArrayList>();
            FaceDetectApi.Face face = faces[0];
            FaceDetectApi.getInstance().getLandmark(face, FaceDetectApi.LMK_106, true);
            FaceDetectApi.getInstance().getRect(face, true);

            synchronized (LandmarkEngine.getInstance()) {
                //0.4.7之前（包括）jni把所有角度的点算到竖直的坐标，所以外面画点需要再调整回来，才能与其他角度适配
                //目前getLandmarkOrigin会获得原始的坐标，所以只需要横屏适配好其他的角度就不用适配了，因为texture和preview的角度关系是固定的
                ArrayList<FloatBuffer> triangleVBList = new ArrayList<FloatBuffer>();

                for (int i = 0; i < face.points.length; i++) {
                    float x = (face.points[i].x / width) * 2 - 1;
                    if (!mCameraManager.isFrontCam())
                        x = -x;
                    float y = (face.points[i].y / height) * 2 - 1;
                    float[] pointf = new float[]{y, x, 0.0f};

                    FloatBuffer fb = mCameraMatrix.floatBufferUtil(pointf);
                    triangleVBList.add(fb);

                }
                pointsOpengl.add(triangleVBList);

                OneFace oneFace = LandmarkEngine.getInstance().getOneFace(0);
                // 姿态角和置信度
                oneFace.pitch = face.pitch;
                oneFace.yaw = face.yaw;
                oneFace.roll = face.roll;
                oneFace.confidence = face.confidence;

                // 获取一个人的关键点坐标
                if (oneFace.vertexPoints == null || oneFace.vertexPoints.length != face.points.length * 2) {
                    oneFace.vertexPoints = new float[face.points.length * 2];
                }
                for (int i = 0; i < face.points.length; i++) {
                    float x = (face.points[i].x / width) * 2 - 1;
                    float y = (face.points[i].y / height) * 2 - 1;
//                        y = -y;
//                    if (!mCameraManager.isFrontCam())
//                        x = -x;
                    float[] point = new float[]{x, -y};
                    point[0] = y;
                    point[1] = x;
                    // 顶点坐标
                    oneFace.vertexPoints[2 * i] = point[0];
                    oneFace.vertexPoints[2 * i + 1] = point[1];
                }
                // 插入人脸对象
                LandmarkEngine.getInstance().putOneFace(0, oneFace);
            }

            FaceDetectApi.getInstance().getPose3D(face);

            synchronized (mPointsMatrix) {
                mPointsMatrix.points = pointsOpengl;
            }
        }
        drawFrame();
    }

    private void drawFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);// 清除屏幕和深度缓存
        float[] mtx = new float[16];
        mSurface.getTransformMatrix(mtx);
        mGLFramebuffer.bind(false);
        GLES20.glViewport(0, 0, mGLFramebuffer.getWidth(), mGLFramebuffer.getHeight());
        mCameraMatrix.draw(mtx);
        mGLFramebuffer.unbind();

        int textureId = mGLFramebuffer.getTextureId();

        mBeautyParam.eyeEnlargeIntensity = 2.0f;
        mGLImageFaceReshapeFilter.onBeauty(mBeautyParam);
        mGLImageFaceReshapeFilter.drawFrame(textureId, null, null);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1f, 0f);
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);
//        mPointsMatrix.draw(mMVPMatrix);

        mGLImageFaceReshapeFilter2.drawFrame(textureId, mMVPMatrix);

        FloatBuffer mVertexBuffer;
        FloatBuffer mTextureBuffer;
        mVertexBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.CubeVertices);
        mTextureBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.TextureVertices_flipx);

        mStickerFilter.drawFrame(textureId, mVertexBuffer, mTextureBuffer);

        mEglCore.swapBuffers(mEGLSurface);
    }

    private int faceRotation = FacePPImage.FACE_LEFT;

    @Override
    public void onOpenSuccess(int width, int height, int angle) {
        mWidth = width;
        mHeight = height;
        mAngle = angle;
        FaceDetectApi.FaceppConfig config = FaceDetectApi.getInstance().getFaceppConfig();
        config.face_confidence_filter = 0.6f;
        config.detectionMode = DETECTION_MODE_TRACKING;
        config.is_bind_big_cpu = true;
        int minface = 40; // 640*480;
        float scale = (width * height) * 1.0f / (640 * 480);
        config.minFaceSize = (int) (scale * minface);
        FaceDetectApi.getInstance().setFaceppConfig(config);

        if (mCameraManager.isFrontCam()) {
            faceRotation = FacePPImage.FACE_RIGHT;
        } else {
            faceRotation = FacePPImage.FACE_LEFT;
        }
        imageBuilder = new FacePPImage.Builder().setWidth(width).setHeight(height).setMode(FacePPImage.IMAGE_MODE_NV21).setRotation(faceRotation);

        int drawWidth;
        int drawHeight;
        if (angle == 0 || angle == 180) {
            drawWidth = width;
            drawHeight = height;
        } else {
            drawHeight = width;
            drawWidth = height;
        }
        if (mGLFramebuffer == null || mGLFramebuffer.isNeedInitAgain(drawWidth, drawHeight)) {
            mGLFramebuffer = new GLFramebuffer(1, drawWidth, drawHeight, false, false, true);
            mGLFramebuffer.setDoClearMask(true);
        }

        mGLImageFaceReshapeFilter.onInputSizeChanged(drawWidth, drawHeight);
        mGLImageFaceReshapeFilter2.onInputSizeChanged(drawWidth, drawHeight);
        mStickerFilter.initFrameBuffer(drawWidth, drawHeight);
        mStickerFilter.onInputSizeChanged(drawWidth, drawHeight);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adjustTextureViewSize(drawWidth, drawHeight);
            }
        });
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onOpenFailed() {

    }

    private int mTextureID = -1;
    private CameraMatrix mCameraMatrix;
    private PointsMatrix mPointsMatrix;

    private final float[] mProjMatrix = new float[16];
    private final float[] mVMatrix = new float[16];
    private final float[] mMVPMatrix = new float[16];

    private GLFramebuffer mGLFramebuffer;

    private GLImageFaceReshapeFilter mGLImageFaceReshapeFilter;
    private GLImageFilter2 mGLImageFaceReshapeFilter2;
    private DynamicStickerNormalFilter mStickerFilter;

    private final BeautyParam mBeautyParam = new BeautyParam();
    private SurfaceTexture mSurface;


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        mCameraManager.getHandler().post(new Runnable() {
            @Override
            public void run() {
                mEGLSurface = mEglCore.createWindowSurface(holder.getSurface());
                mEglCore.makeCurrent(mEGLSurface);

                mGLImageFaceReshapeFilter = new GLImageFaceReshapeFilter(FaceDetectFrameActivity.this);
                mGLImageFaceReshapeFilter2 = new GLImageFilter2(FaceDetectFrameActivity.this);

                ResourceData resourceData = new ResourceData("cat", "assets://resource/cat.zip", ResourceType.STICKER, "cat", "assets://cat.png");
                String unzipFolder = resourceData.unzipFolder;
                String folderPath = ResourceHelper.getResourceDirectory(FaceDetectFrameActivity.this) + File.separator + unzipFolder;
                DynamicSticker sticker = null;
                try {
                    sticker = ResourceJsonCodec.decodeStickerData(folderPath);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mStickerFilter = new DynamicStickerNormalFilter(FaceDetectFrameActivity.this, sticker);

                mCameraManager.openCamera();
                mTextureID = ICameraMatrix.getOESTexture();
                mSurface = new SurfaceTexture(mTextureID);
                mSurface.setOnFrameAvailableListener(FaceDetectFrameActivity.this);// 设置照相机有数据时进入
                mCameraManager.startPreview(mSurface);
                mCameraManager.actionDetect(FaceDetectFrameActivity.this);

                mCameraMatrix = new CameraMatrix(mTextureID);
                mPointsMatrix = new PointsMatrix(false);

            }
        });
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        mCameraManager.getHandler().post(new Runnable() {
            @Override
            public void run() {
                // 设置画面的大小
                GLES20.glViewport(0, 0, width, height);

                float ratio = (float) width / height;
                ratio = 1; // 这样OpenGL就可以按照屏幕框来画了，不是一个正方形了

                // this projection matrix is applied to object coordinates
                // in the onDrawFrame() method
                Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

                mGLImageFaceReshapeFilter.onDisplaySizeChanged(width, height);
                mGLImageFaceReshapeFilter2.onDisplaySizeChanged(width, height);
                mStickerFilter.onDisplaySizeChanged(width, height);
            }
        });
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mCameraManager.getHandler().post(new Runnable() {
            @Override
            public void run() {
                mCameraManager.closeCamera();
                mEglCore.release();
            }
        });
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        surfaceTexture.updateTexImage();
    }
}
