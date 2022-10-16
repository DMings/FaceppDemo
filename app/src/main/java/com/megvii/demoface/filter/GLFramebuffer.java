package com.megvii.demoface.filter;

import android.opengl.GLES20;

import java.nio.Buffer;

/**
 * Created by zwq on 2016/07/26 17:33.<br/><br/>
 * v2: 20180606，在GLFramebufferV1版基础上优化
 * 一个Framebuffer 对应一个纹理
 */
public class GLFramebuffer {

    private int mNum;
    private int mWidth, mHeight;
    private int[] mFrameBuffers;
    private int[] mFrameBuffersTextures;
    private int[] mColorBuffers;
    private int[] mDepthBuffers;
    private int[] mStencilBuffers;

    private int mReleaseBufferFlag;
    private boolean mDoClearMask;
    private float[] mClearColor = new float[4];//rgba
    private int mClearMask;

    private int mCurrentTextureIndex = -1;
    private int mPreviousTextureIndex = -1;
    private boolean mHasBindFramebuffer;
    private boolean mHasBind;

    public GLFramebuffer(int width, int height) {
        this(1, width, height);
    }

    public GLFramebuffer(int num, int width, int height) {
        this(num, width, height, false, false, false);
    }

    public GLFramebuffer(int num, int width, int height, boolean color, boolean depth, boolean stencil) {
        this(num, width, height, color, depth, stencil, GLES20.GL_RGBA);
    }

    public GLFramebuffer(int num, int width, int height, int format) {
        this(num, width, height, false, false, false, format);
    }

    /**
     * @param num    数量
     * @param width
     * @param height
     */
    public GLFramebuffer(int num, int width, int height, boolean color, boolean depth, boolean stencil, int format) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        mNum = num;
        if (mNum < 1) {
            mNum = 1;
        }
        mWidth = width;
        mHeight = height;

        mFrameBuffers = new int[mNum];
        mFrameBuffersTextures = new int[mNum];
        //mDoClearMask = true;
        mClearMask = GLES20.GL_COLOR_BUFFER_BIT;
        if (color) {
            mColorBuffers = new int[mNum];
        }
        if (depth) {
            mDepthBuffers = new int[mNum];
            mClearMask |= GLES20.GL_DEPTH_BUFFER_BIT;
        }
        if (stencil) {
            mStencilBuffers = new int[mNum];
            mClearMask |= GLES20.GL_STENCIL_BUFFER_BIT;
        }
        generateBufferAndTexture(mNum, format, format, GLES20.GL_UNSIGNED_BYTE, null);
    }

    public GLFramebuffer(int width, int height, int outsideBufferId, boolean canRelease) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        mNum = 1;
        mWidth = width;
        mHeight = height;
        mFrameBuffers = new int[mNum];
        mFrameBuffers[0] = outsideBufferId;
        mReleaseBufferFlag = canRelease ? 1 : -1;
        mClearMask = GLES20.GL_COLOR_BUFFER_BIT;
    }

    private void generateBufferAndTexture(int num, int internalFormat, int format, int type, Buffer pixels) {
        //framebuffer
        GLES20.glGenFramebuffers(num, mFrameBuffers, 0);
        GLES20.glGenTextures(num, mFrameBuffersTextures, 0);
        for (int index = 0; index < num; index++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBuffersTextures[index]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, internalFormat, mWidth, mHeight, 0, format, type, pixels);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[index]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mFrameBuffersTextures[index], 0);

            if (mColorBuffers != null) {
                GLES20.glGenRenderbuffers(1, mColorBuffers, index);
                GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mColorBuffers[index]);
                GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_RGBA4, mWidth, mHeight);
            }
            if (mDepthBuffers != null) {
                GLES20.glGenRenderbuffers(1, mDepthBuffers, index);
                GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBuffers[index]);
                GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, mWidth, mHeight);
            }
            if (mStencilBuffers != null) {
                GLES20.glGenRenderbuffers(1, mStencilBuffers, index);
                GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mStencilBuffers[index]);
                GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_STENCIL_INDEX8, mWidth, mHeight);
            }

            // bind RenderBuffers to FrameBuffer object
            if (mColorBuffers != null) {
                GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_RENDERBUFFER, mColorBuffers[index]);
            }
            if (mDepthBuffers != null) {
                GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mDepthBuffers[index]);
            }
            if (mStencilBuffers != null) {
                GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_STENCIL_ATTACHMENT, GLES20.GL_RENDERBUFFER, mStencilBuffers[index]);
            }
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
    }

    public int getNum() {
        return mNum;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    /**
     * 比较尺寸是否一致
     *
     * @param width
     * @param height
     * @return true:尺寸不同
     */
    public boolean isDifferentSize(int width, int height) {
        if (width != mWidth || height != mHeight) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否需要重新初始化，如果需要则会调用 destroy 并返回 true
     *
     * @param width
     * @param height
     * @return
     */
    public boolean isNeedInitAgain(int width, int height) {
        if (width != mWidth || height != mHeight) {
            destroy();
            return true;
        }
        return false;
    }

    public void reset() {
        mCurrentTextureIndex = -1;
        mPreviousTextureIndex = -1;
        mHasBindFramebuffer = false;
        mHasBind = true;
    }

    public void updateOutsideBufferId(int bufferId) {
        if (mNum == 1
                && mWidth > 0
                && mHeight > 0
                && mFrameBuffers != null
                && mFrameBuffers.length == 1) {
            mFrameBuffers[0] = bufferId;
        }
    }

    public void setDoClearMask(boolean doClearMask) {
        mDoClearMask = doClearMask;
    }

    public void setClearColor(float r, float g, float b, float a) {
        mClearColor[0] = checkBound(r);
        mClearColor[1] = checkBound(g);
        mClearColor[2] = checkBound(b);
        mClearColor[3] = checkBound(a);
    }

    private float checkBound(float c) {
        if (c < 0.0F) {
            return 0.0F;
        } else if (c > 1.0F) {
            return 1.0F;
        }
        return c;
    }

    private int checkIndex(int index) {
        if (index < 0) {
            return 0;
        } else if (index >= mNum) {
            return mNum - 1;
        }
        return index;
    }

    public int getCurrentTextureIndex() {
        return mCurrentTextureIndex;
    }

    public boolean bindByIndex(int index, boolean clear) {
        return bindByIndex(index, clear, true);
    }

    public boolean bindByIndex(int index, boolean clear, boolean reset) {
        index = checkIndex(index);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[index]);
        if (mDoClearMask && clear) {
            GLES20.glClearColor(mClearColor[0], mClearColor[1], mClearColor[2], mClearColor[3]);//此函数仅仅设定颜色，并不执行清除工作
            GLES20.glClear(mClearMask);//用设定的颜色值清除缓存区
        }

        if (reset) {
            mPreviousTextureIndex = mCurrentTextureIndex;
            mCurrentTextureIndex = index;
            mHasBindFramebuffer = true;
            mHasBind = true;
        }
        return true;
    }

    public boolean bindNext(boolean clear) {
        return bindByIndex((mCurrentTextureIndex + 1) % mNum, clear);
    }

    public boolean bind(boolean clear) {
        return bindByIndex(0, clear);
    }

    public void unbind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public boolean rebind(boolean clear) {
        return bindByIndex(mCurrentTextureIndex, clear, false);
    }

    public void clear() {
        GLES20.glClearColor(mClearColor[0], mClearColor[1], mClearColor[2], mClearColor[3]);
        GLES20.glClear(mClearMask);
    }

    public boolean hasBindFramebuffer() {
        return mHasBindFramebuffer;
    }

    public void setHasBind(boolean bind) {
        mHasBind = bind;
    }

    public int getBufferIdByIndex(int index) {
        if (mFrameBuffers != null) {
            index = checkIndex(index);
            return mFrameBuffers[index];
        }
        return -1;
    }

    public int getCurrentBufferId() {
        return getBufferIdByIndex(mCurrentTextureIndex);
    }

    public int getTextureIdByIndex(int index) {
        if (mFrameBuffersTextures != null) {
            index = checkIndex(index);
            return mFrameBuffersTextures[index];
        }
        return -1;
    }

    public int getTextureId() {
        return getTextureIdByIndex(0);
    }

    public int getCurrentTextureId() {
        return getTextureIdByIndex(mCurrentTextureIndex);
    }

    public int getPreviousTextureId() {
        if (mPreviousTextureIndex < 0) {
            if (mHasBind) {
                mHasBind = false;
                mPreviousTextureIndex = mCurrentTextureIndex;
            }
            return -1;
        } else {
            if (!mHasBind) {
                mPreviousTextureIndex = mCurrentTextureIndex;
            }
        }
        mHasBind = false;
        return getTextureIdByIndex(mPreviousTextureIndex);
    }

    public int getPreviousTextureIdByIndex(int index) {
        if (index <= 0) {
            return getPreviousTextureId();
        }
        return getTextureIdByIndex((mCurrentTextureIndex - index + mNum) % mNum);
    }

    public int[] getBufferIds() {
        return mFrameBuffers;
    }

    public int[] getTextureIds() {
        return mFrameBuffersTextures;
    }

    public void destroy() {
        mHasBindFramebuffer = false;
        mHasBind = false;

        if (mColorBuffers != null) {
            GLES20.glDeleteRenderbuffers(mColorBuffers.length, mColorBuffers, 0);
            mColorBuffers = null;
        }
        if (mDepthBuffers != null) {
            GLES20.glDeleteRenderbuffers(mDepthBuffers.length, mDepthBuffers, 0);
            mDepthBuffers = null;
        }
        if (mStencilBuffers != null) {
            GLES20.glDeleteRenderbuffers(mStencilBuffers.length, mStencilBuffers, 0);
            mStencilBuffers = null;
        }
        if (mFrameBuffers != null) {
            if (mReleaseBufferFlag >= 0) {
                GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
            }
            mFrameBuffers = null;
        }
        if (mFrameBuffersTextures != null) {
            GLES20.glDeleteTextures(mFrameBuffersTextures.length, mFrameBuffersTextures, 0);
            mFrameBuffersTextures = null;
        }
        mReleaseBufferFlag = 0;
        mClearColor = null;
        mClearMask = 0;
    }
}
