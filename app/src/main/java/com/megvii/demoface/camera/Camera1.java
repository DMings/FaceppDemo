package com.megvii.demoface.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Camera1 implements Camera.PreviewCallback {
    private final String TAG = "CameraWrapperImpl";

    public static int mTargetWidth = 640;
    public static int mTargetHeight = 480;

    // event for Camera
    private final static int EVENT_OPEN_CAMERA = 1;
    private final static int EVENT_START_PREVIEW = 2;


    // worker thread for opening camera and starting preview
    private HandlerThread mHandlerThread;
    private Handler mHandler;


    // camera info
    private final static int INVALID_CAMERA_ID = -1;
    private Camera mCamera;
    private int mCameraId = INVALID_CAMERA_ID;
    private boolean mIsBackCamera;
    private CameraOpenCallback mCameraOpenCallback;

    // preview info
    private SurfaceTexture mSurfaceTexture;
    private ICameraCallback mCameraCallback;

    private byte[] mDataCache;
    private Lock lock = new ReentrantLock();  // 主要保护mDataCache

    private int mWidth = mTargetWidth;
    private int mHeight = mTargetHeight;
    private int mAngle = 90;

    public Camera1() {
        // init worker thread
        initEventLooper();
    }

    public Handler getHandler() {
        return mHandler;
    }

    /**
     * callback for frame data of preview
     *
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
//        Log.d(TAG, "onPreviewFrame");

        lock.lock();
        if (mDataCache == null || mDataCache.length != data.length) {
            mDataCache = new byte[data.length];
        }
        System.arraycopy(data, 0, mDataCache, 0, data.length);
        camera.addCallbackBuffer(data);
        if (checkNotNull(mCameraCallback)) {
            mCameraCallback.onPreviewFrame(mDataCache, camera);
        }
        lock.unlock();
    }

    /**
     * Open Camera asynchronously
     */
    public void openCamera(boolean isBackCamera, Context context, CameraOpenCallback callback) {
        Log.d(TAG, "openCamera()...back camera:" + isBackCamera);

        // init worker thread
        initEventLooper();
        // save params
        mIsBackCamera = isBackCamera;
        mCameraOpenCallback = callback;

        // obtain message
        if (mHandler != null) {
            Message msg = Message.obtain(mHandler, EVENT_OPEN_CAMERA);
            msg.sendToTarget();
        }
    }

    /**
     * Start Preview
     *
     * @param surfaceTexturer
     */
    public void startPreview(SurfaceTexture surfaceTexturer) {
        Log.d(TAG, "startPreview()...");

        // save holder
        mSurfaceTexture = surfaceTexturer;

        // obtain message
        if (mHandler != null) {
            Message msg = Message.obtain(mHandler, EVENT_START_PREVIEW);
            msg.sendToTarget();
        }
    }

    /**
     * Begin to detect and receive frame data of preview
     *
     * @param callback receive frame data of preview
     */
    public void startDetect(ICameraCallback callback) {
        mCameraCallback = callback;
    }

    /**
     * Stop detecting，
     */
    public void stopDetect() {
        mCameraCallback = null;
    }

    /**
     * Stop preview
     */
    public void stopPreview() {
        Log.d(TAG, "stopPreview()...");
        if (checkNotNull(mCamera)) {
            mCamera.stopPreview();
        }
    }

    /**
     * 关闭camera
     */
    public void closeCamera() {
        Log.d(TAG, "closeCamera()...");
        if (checkNotNull(mCamera)) {
            mCameraCallback = null;
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        // reset camera info
        resetPreviewInfo();

        // exit event looper
        exitEventLooper();
    }


    /**
     * Set Orientation of Display
     *
     * @param angle 0~360
     */
    public void setDisplayOrientation(int angle) {
        if (checkNotNull(mCamera)) {
            mCamera.setDisplayOrientation(angle);
        }
    }


    // init event thread
    private void initEventLooper() {
        Log.d(TAG, "initEventLooper()...");
        if (mHandlerThread != null) {
            return;
        }
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.d(TAG, "handleMessage()..." + msg);
                switch (msg.what) {
                    case EVENT_OPEN_CAMERA:
                        handleOpenCamera();
                        break;
                    case EVENT_START_PREVIEW:
                        handleStartPreview();
                        break;
                    default:
                        Log.d(TAG, "Unsupport event type");
                        break;
                }
                return false;
            }
        });

    }

    private void exitEventLooper() {
        Log.d(TAG, "exitEventLooper()...");
        if (checkNotNull(mHandler)) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (checkNotNull(mHandlerThread)) {
            mHandlerThread.quit();
        }
        mHandlerThread = null;
        mHandler = null;
    }

    /**
     * Open Camera
     */
    // handle for opening Camera
    private void handleOpenCamera() {
        Log.d(TAG, "handleOpenCamera()...");

        // select back or front camera
        mCameraId = selectBackOrFrontCamera();
        if (INVALID_CAMERA_ID == mCameraId) {
            Log.e(TAG, "camera id is invalid, return!");
            if (mCameraOpenCallback != null) {
                mCameraOpenCallback.onOpenFailed();
            }
            return;
        }

        // open camera
        try {
            mCamera = Camera.open(mCameraId);

            // config camera info
            configCameraParams();
            mAngle = getCameraAngle(0);
            // open successfully
            if (mCameraOpenCallback != null) {
                mCameraOpenCallback.onOpenSuccess(mWidth, mHeight, mAngle);
            }
        } catch (Exception e) {
            Log.e(TAG, "failed to open camera!");
            if (mCameraOpenCallback != null) {
                mCameraOpenCallback.onOpenFailed();
            }
            e.printStackTrace();
        }
    }

    /**
     * Start Preview
     */
    private void handleStartPreview() {
        Log.d(TAG, "handleStartPreview()...");
        if (checkNotNull(mCamera)) {
            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
                mCamera.startPreview();
                onPreviewStarted(mCamera);
                // set preview call
                setPreviewCallback(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void onPreviewStarted(Camera camera) {
        Camera.Size s = camera.getParameters().getPreviewSize();
        int wishedBufferSize = s.height * s.width * 3 / 2;
        camera.addCallbackBuffer(new byte[wishedBufferSize]);
//        camera.addCallbackBuffer(new byte[wishedBufferSize]);
//        camera.addCallbackBuffer(new byte[wishedBufferSize]);
        //camera.addCallbackBuffer(new byte[wishedBufferSize]);
    }

    // select back or front camera
    private int selectBackOrFrontCamera() {
        Log.d(TAG, "selectBackOrFrontCamera()...");

        // get Camera numbers
        int cameraNumber = Camera.getNumberOfCameras();

        // select camera based on isBackCamera
        int cameraId = INVALID_CAMERA_ID;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int index = 0; index < cameraNumber; index++) {
            Camera.getCameraInfo(index, cameraInfo);
            if (mIsBackCamera && // back camera
                    cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = index;
                break;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) { // front camera
                cameraId = index;
                break;
            }
        }

        return cameraId;
    }

    // config params of camera
    private void configCameraParams() {
        try {
            if (checkNotNull(mCamera)) {
                Camera.Parameters params = mCamera.getParameters();
                Camera.Size bestPreviewSize = calBestPreviewSize(
                        params, mWidth, mHeight);
                this.mWidth = bestPreviewSize.width;
                this.mHeight = bestPreviewSize.height;
                // set preview size
                params.setPreviewSize(this.mWidth, this.mHeight);
                // set data format
                params.setPreviewFormat(ImageFormat.NV21);

                List<String> focusModes = params.getSupportedFocusModes();
                if (focusModes
                        .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }

                Log.w(TAG, "Angle==" + mAngle + "|" + this.mWidth + "|" + this.mHeight);
                mCamera.setParameters(params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // set callback of preview
    private void setPreviewCallback(Camera.PreviewCallback callBack) {
        if (null != mCamera) {
            mCamera.setPreviewCallback(callBack);
        }
    }

    // check if Camera is null
    private boolean checkNotNull(Object object) {
        return (null != object);
    }

    // reset preview info
    private void resetPreviewInfo() {
        mCameraId = INVALID_CAMERA_ID;
        mSurfaceTexture = null;
        mCameraOpenCallback = null;

        mCameraCallback = null;
        mIsBackCamera = false;
    }

    /**
     * 通过传入的宽高算出最接近于宽高值的相机大小
     */
    private Camera.Size calBestPreviewSize(Camera.Parameters camPara, final int width, final int
            height) {
        List<Camera.Size> allSupportedSize = camPara.getSupportedPreviewSizes();
        ArrayList<Camera.Size> widthLargerSize = new ArrayList<Camera.Size>();
        for (Camera.Size tmpSize : allSupportedSize) {
            Log.w(TAG, "tmpSize.width===" + tmpSize.width
                    + ", tmpSize.height===" + tmpSize.height);
            if (tmpSize.width > tmpSize.height) {
                widthLargerSize.add(tmpSize);
            }
        }

        Collections.sort(widthLargerSize, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                int off_one = Math.abs(lhs.width * lhs.height - width * height);
                int off_two = Math.abs(rhs.width * rhs.height - width * height);
                return off_one - off_two;
            }
        });

        Log.w(TAG, "best matched size:" + widthLargerSize.get(0).width + ":" + widthLargerSize.get(0).height);
        return widthLargerSize.get(0);
    }

    // 获取照相机旋转角度
    private int getCameraAngle(int windowDefaultRotation) {
        int rotateAngle = 90;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int rotation = windowDefaultRotation;
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotateAngle = (info.orientation + degrees) % 360;
            rotateAngle = (360 - rotateAngle) % 360; // compensate the mirror
        } else { // back-facing
            rotateAngle = (info.orientation - degrees + 360) % 360;
        }
        return rotateAngle;
    }

    /**
     * callback for camera preview or capture picture
     */
    public interface ICameraCallback {
        /**
         * callback for frame data of preview
         */
        void onPreviewFrame(byte[] bytes, final Camera camera);
    }

    /**
     * callback result for opening camera
     */
    public interface CameraOpenCallback {
        /**
         * open successfully
         */
        void onOpenSuccess(int width, int height, int angle);

        /**
         * Camera is disconnected
         */
        void onDisconnected();

        /**
         * Failed to open Camera
         */
        void onOpenFailed();
    }
}
