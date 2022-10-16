package com.megvii.demoface.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.megvii.demoface.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

/**
 * 绘制经过sdk处理过后的图片
 */
public class ImageMixMatrix {

    private final LinkedList<Runnable> mRunOnDraw;
    private final String mVertexShader;
    private final String mFragmentShader;
    protected int mGLProgId;
    protected int mGLAttribPosition;
    //  protected int mGLUniformTexture;
    protected int mGLAttribTextureCoordinate;
    protected int mGLAttribTextureCoordinatebg;
    protected boolean mIsInitialized;
    protected FloatBuffer mGLCubeBuffer;
    protected FloatBuffer mGLTextureBuffer;
    protected int mSurfaceWidth, mSurfaceHeight;
    protected int mTableTextureID = OpenGLUtils.NO_TEXTURE;

    protected int textureUniform;
    protected int textureUniform2;
    protected int textureUniform3;
    protected int maxLocation;
    protected int minLocation;

    protected int backTextureId;

    protected int scaleSizeLocation;

    protected int[] mFrameBuffers = null;
    protected int[] mFrameBufferTextures = null;

    private int mFrameWidth = -1;
    private int mFrameHeight = -1;

    public ImageMixMatrix(Context context) {
        mRunOnDraw = new LinkedList<Runnable>();
        mVertexShader = OpenGLUtils.loadFromRawFile(context, R.raw.image_vertex);
        mFragmentShader = OpenGLUtils.loadFromRawFile(context, R.raw.mix_fragment);

        mGLCubeBuffer = ByteBuffer.allocateDirect(OpenGLUtils.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(OpenGLUtils.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(OpenGLUtils.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(OpenGLUtils.TEXTURE_NO_ROTATION).position(0);
    }

    public void init() {
        onInit();
        mIsInitialized = true;
    }

    protected void onInit() {
        mGLProgId = OpenGLUtils.loadProgram(mVertexShader, mFragmentShader);
        Log.d("fenghx", "The program ID for image is " + mGLProgId);
        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId, "position");
        maxLocation = GLES20.glGetUniformLocation(mGLProgId, "scalarMax");
        minLocation = GLES20.glGetUniformLocation(mGLProgId, "scalarMin");
        scaleSizeLocation=GLES20.glGetUniformLocation(mGLProgId, "scaleSize");
        textureUniform = GLES20.glGetUniformLocation(mGLProgId, "Texture");
        textureUniform2 = GLES20.glGetUniformLocation(mGLProgId, "Texture2");
        textureUniform3 = GLES20.glGetUniformLocation(mGLProgId, "Texture3");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId, "inputTextureCoordinate");
        mGLAttribTextureCoordinatebg = GLES20.glGetAttribLocation(mGLProgId,"inputTextureCoordinatebg");
//        backTextureId=initTextureID(1,1)[0];
//        backTextureId=initTextureIdBg()[0];
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
    }

    public static int[] initTextureID(int width, int height) {
         byte[] greenB={(byte) 0xff, 0,0,0};
        ByteBuffer greenBuffer=ByteBuffer.wrap(greenB);

        int[] mTextureOutID = new int[1];
        GLES20.glGenTextures(1, mTextureOutID, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureOutID[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, greenBuffer);
        return mTextureOutID;
    }

    public int bgWidth,bgHeight;
//
//    public  int[] initTextureIdBg() {
//
//        Resources res = MainApp.getContext().getResources();
//        Bitmap bmp = BitmapFactory.decodeResource(res, R.drawable.bg_img);
//        byte rgba[]=  ConUtil.getPixelsRGBA(bmp);
//
//        ByteBuffer greenBuffer=ByteBuffer.wrap(rgba);
//
//        bgWidth=bmp.getWidth();
//        bgHeight=bmp.getHeight();
//
//        int[] mTextureOutID = new int[1];
//        GLES20.glGenTextures(1, mTextureOutID, 0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureOutID[0]);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bmp.getWidth(), bmp.getHeight(), 0, GLES20.GL_RGBA,
//                GLES20.GL_UNSIGNED_BYTE, greenBuffer);
//        return mTextureOutID;
//    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    protected void runPendingOnDrawTasks() {
        synchronized (mRunOnDraw) {
            while (!mRunOnDraw.isEmpty()) {
                mRunOnDraw.removeFirst().run();
            }
        }
    }

    public final void destroy() {
        mIsInitialized = false;
        destroyFramebuffers();
        GLES20.glDeleteProgram(mGLProgId);
    }

    public int onDrawFrame(final int textureId, final FloatBuffer cubeBuffer,
                           final FloatBuffer textureBuffer, final int alphaTextureId,float max, float min,float scaler,final int bgTextureId) {
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return OpenGLUtils.NOT_INIT;
        }

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinatebg,2,GLES20.GL_FLOAT,false,0,mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinatebg);

        GLES20.glUniform1f(maxLocation,max);
        GLES20.glUniform1f(minLocation,min);
        GLES20.glUniform1f(scaleSizeLocation,scaler);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bgTextureId);
        GLES20.glUniform1i(textureUniform, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(textureUniform2, 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, alphaTextureId);
        GLES20.glUniform1i(textureUniform3, 2);


        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return OpenGLUtils.ON_DRAWN;
    }

    public int onDrawToTexture(final int textureId, final FloatBuffer cubeBuffer,
                           final FloatBuffer textureBuffer, final int alphaTextureId,float max, float min,float scaler,final int bgTextureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);

        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return OpenGLUtils.NOT_INIT;
        }

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        GLES20.glUniform1f(maxLocation,max);
        GLES20.glUniform1f(minLocation,min);
        GLES20.glUniform1f(scaleSizeLocation,scaler);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bgTextureId);
        GLES20.glUniform1i(textureUniform, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(textureUniform2, 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, alphaTextureId);
        GLES20.glUniform1i(textureUniform3, 2);


        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        return mFrameBufferTextures[0];
    }

    public void initCameraFrameBuffer(int width, int height) {
        if(mFrameBuffers != null && (mFrameWidth != width || mFrameHeight != height))
            destroyFramebuffers();
        if (mFrameBuffers == null) {
            mFrameWidth = width;
            mFrameHeight = height;
            mFrameBuffers = new int[2];
            mFrameBufferTextures = new int[2];

            GLES20.glGenFramebuffers(2, mFrameBuffers, 0);
            GLES20.glGenTextures(2, mFrameBufferTextures, 0);

            bindFrameBuffer(mFrameBufferTextures[0], mFrameBuffers[0], width, height);
            bindFrameBuffer(mFrameBufferTextures[1], mFrameBuffers[1], height, width);
        }
    }

    private void bindFrameBuffer(int textureId, int frameBuffer, int width, int height) {

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void destroyFramebuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(2, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(2, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
        mFrameWidth = -1;
        mFrameHeight = -1;
    }
}
