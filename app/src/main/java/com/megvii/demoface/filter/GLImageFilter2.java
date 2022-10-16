package com.megvii.demoface.filter;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.text.TextUtils;

import com.megvii.demoface.filter.landmark.LandmarkEngine;
import com.megvii.demoface.filter.utils.OpenGLUtils;
import com.megvii.demoface.filter.utils.TextureRotationUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;

/**
 * 基类滤镜
 * Created by cain on 2017/7/9.
 */

public class GLImageFilter2 {

    protected static final String VERTEX_SHADER = "" +
            "attribute vec4 aPosition;                                  \n" +
            "uniform mat4 uMVPMatrix;                            \n" +
            "void main() {                                              \n" +
            "    gl_Position = uMVPMatrix * aPosition;                               \n" +
            "}                                                          \n";

    protected static final String FRAGMENT_SHADER = "" +
            "precision mediump float;                                   \n" +
            "void main() {                                              \n" +
            "if (length(gl_PointCoord - vec2(0.5)) > 0.5) {" +
            "        discard;" +
            "    }else {\n" +
            "    gl_FragColor = vec4(1.0,0.0,0.0,1.0); \n" +
            "}                                                          \n" +
            "}                                                          \n";


    protected String TAG = getClass().getSimpleName();

    protected Context mContext;

    private final LinkedList<Runnable> mRunOnDraw;

    // 纹理字符串
    protected String mVertexShader;
    protected String mFragmentShader;

    // 是否初始化成功
    protected boolean mIsInitialized;
    // 滤镜是否可用，默认可用
    protected boolean mFilterEnable = true;

    // 每个顶点坐标有几个参数
    protected int mCoordsPerVertex = TextureRotationUtils.CoordsPerVertex;
    // 顶点坐标数量
    protected int mVertexCount = TextureRotationUtils.CubeVertices.length / mCoordsPerVertex;

    // 句柄
    protected int mProgramHandle;
    protected int mPositionHandle;

    // 渲染的Image的宽高
    protected int mImageWidth;
    protected int mImageHeight;

    // 显示输出的宽高
    protected int mDisplayWidth;
    protected int mDisplayHeight;

    // FBO的宽高，可能跟输入的纹理大小不一致
    protected int mFrameWidth = -1;
    protected int mFrameHeight = -1;

    // FBO
    protected int[] mFrameBuffers;
    protected int[] mFrameBufferTextures;

    // 顶点坐标
    private float[] mVertices = new float[122 * 2];
    // 纹理坐标
    private float[] mTextureVertices = new float[122 * 2];
    // 122个关键点
    private final int IndicesLength = 122 * 2;
    // 106 个关键点
    private final int FacePoints = 106;
    // 顶点坐标缓冲
    private FloatBuffer mVertexBuffer;

    protected ShortBuffer mIndexBuffer;

    protected int uVerMatrixHandle;

    public GLImageFilter2(Context context) {
        this(context, VERTEX_SHADER, FRAGMENT_SHADER);
    }

    public GLImageFilter2(Context context, String vertexShader, String fragmentShader) {
        mContext = context;
        mRunOnDraw = new LinkedList<>();
        // 记录shader数据
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
        // 初始化程序句柄
        initProgramHandle();

        mVertexBuffer = ByteBuffer.allocateDirect(IndicesLength * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        mIndexBuffer = OpenGLUtils.createShortBuffer(GLImageFaceReshapeFilter.FaceImageIndices);
    }

    /**
     * 初始化程序句柄
     */
    public void initProgramHandle() {
        // 只有在shader都不为空的情况下才初始化程序句柄
        if (!TextUtils.isEmpty(mVertexShader) && !TextUtils.isEmpty(mFragmentShader)) {
            mProgramHandle = OpenGLUtils.createProgram(mVertexShader, mFragmentShader);
            mPositionHandle = GLES30.glGetAttribLocation(mProgramHandle, "aPosition");
            uVerMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
            mIsInitialized = true;
        } else {
            mPositionHandle = OpenGLUtils.GL_NOT_INIT;
            mIsInitialized = false;
        }
    }

    /**
     * Surface发生变化时调用
     *
     * @param width
     * @param height
     */
    public void onInputSizeChanged(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
    }

    /**
     * 显示视图发生变化时调用
     *
     * @param width
     * @param height
     */
    public void onDisplaySizeChanged(int width, int height) {
        mDisplayWidth = width;
        mDisplayHeight = height;
    }

    /**
     * 绘制Frame
     *
     * @param textureId
     */
    public boolean drawFrame(int textureId,float[] mMvpMatrix) {
        // 没有初始化、输入纹理不合法、滤镜不可用时直接返回
        if (!mIsInitialized || textureId == OpenGLUtils.GL_NOT_INIT || !mFilterEnable) {
            return false;
        }

        // 设置视口大小
        GLES30.glViewport(0, 0, mDisplayWidth, mDisplayHeight);
//        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        // 使用当前的program
        GLES30.glUseProgram(mProgramHandle);
        // 运行延时任务
        runPendingOnDrawTasks();
        GLES20.glUniformMatrix4fv(uVerMatrixHandle, 1, false, mMvpMatrix, 0);

        LandmarkEngine.getInstance().updateFaceAdjustPoints(mVertices, mTextureVertices, 0);
        mVertexBuffer.clear();
        mVertexBuffer.put(mVertices);
        mVertexBuffer.position(0);
        // 绘制纹理
        onDrawTexture(textureId, mVertexBuffer, null);

        return true;
    }


    public void bindFrameBuffer() {
        // 绑定FBO
        GLES30.glViewport(0, 0, mFrameWidth, mFrameHeight);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBuffers[0]);
        // 使用当前的program
        GLES30.glUseProgram(mProgramHandle);
        // 运行延时任务，这个要放在glUseProgram之后，要不然某些设置项会不生效
        runPendingOnDrawTasks();
    }

    public int unBindFrameBuffer() {
        GLES30.glUseProgram(0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        return mFrameBufferTextures[0];
    }


    /**
     * 绘制
     *
     * @param textureId
     * @param vertexBuffer
     * @param textureBuffer
     */
    public void onDrawTexture(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        // 绑定顶点坐标缓冲
        vertexBuffer.position(0);
        GLES30.glVertexAttribPointer(mPositionHandle, mCoordsPerVertex,
                GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glEnableVertexAttribArray(mPositionHandle);
        // 绑定纹理坐标缓冲
        if (textureBuffer != null)
            textureBuffer.position(0);
        // 绑定纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(getTextureType(), textureId);
        onDrawFrameBegin();
        onDrawFrame();
        onDrawFrameAfter();
        // 解绑
        GLES30.glDisableVertexAttribArray(mPositionHandle);
        GLES30.glBindTexture(getTextureType(), 0);

    }

    /**
     * 调用glDrawArrays/glDrawElements之前，方便添加其他属性
     */
    public void onDrawFrameBegin() {

    }

    /**
     * 绘制图像
     */
    protected void onDrawFrame() {
        GLES30.glLineWidth(3f);
        GLES30.glDrawElements(GLES30.GL_LINE_STRIP, mIndexBuffer.capacity(), GLES30.GL_UNSIGNED_SHORT, mIndexBuffer);
    }

    /**
     * glDrawArrays/glDrawElements调用之后，方便销毁其他属性
     */
    public void onDrawFrameAfter() {

    }

    protected void onUnbindTextureValue() {

    }

    /**
     * 获取Texture类型
     * GLES30.TEXTURE_2D / GLES11Ext.GL_TEXTURE_EXTERNAL_OES等
     */
    public int getTextureType() {
        return GLES30.GL_TEXTURE_2D;
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mIsInitialized) {
            GLES30.glDeleteProgram(mProgramHandle);
            mProgramHandle = OpenGLUtils.GL_NOT_INIT;
        }
        destroyFrameBuffer();
    }

    /**
     * 创建FBO
     *
     * @param width
     * @param height
     */
    public void initFrameBuffer(int width, int height) {
        if (!isInitialized()) {
            return;
        }
        if (mFrameBuffers != null && (mFrameWidth != width || mFrameHeight != height)) {
            destroyFrameBuffer();
        }
        if (mFrameBuffers == null) {
            mFrameWidth = width;
            mFrameHeight = height;
            mFrameBuffers = new int[1];
            mFrameBufferTextures = new int[1];
            OpenGLUtils.createFrameBuffer(mFrameBuffers, mFrameBufferTextures, width, height);
        }
    }

    /**
     * 销毁纹理
     */
    public void destroyFrameBuffer() {
        if (!mIsInitialized) {
            return;
        }
        if (mFrameBufferTextures != null) {
            GLES30.glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }

        if (mFrameBuffers != null) {
            GLES30.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
        mFrameWidth = -1;
        mFrameWidth = -1;
    }

    /**
     * 判断是否初始化
     *
     * @return
     */
    public boolean isInitialized() {
        return mIsInitialized;
    }

    /**
     * 设置滤镜是否可用
     *
     * @param enable
     */
    public void setFilterEnable(boolean enable) {
        mFilterEnable = enable;
    }

    /**
     * 获取输出宽度
     *
     * @return
     */
    public int getDisplayWidth() {
        return mDisplayWidth;
    }

    /**
     * 获取输出高度
     *
     * @return
     */
    public int getDisplayHeight() {
        return mDisplayHeight;
    }

    ///------------------ 统一变量(uniform)设置 ------------------------///
    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES30.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES30.glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES30.glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES30.glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }

    /**
     * 添加延时任务
     *
     * @param runnable
     */
    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    /**
     * 运行延时任务
     */
    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    protected static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }
}
