package com.megvii.demoface.opengl;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class is used to render nv21 images to a texture.
 */
public class NV21Matrix {
    public static final int NO_TEXTURE = -1;
    private int mGLProgId;
    protected int[] mFrameBuffers = null;
    private boolean inited;
    //The Y and UV buffers that will pass our image channel data to the textures
    private ByteBuffer yBuffer;
    private ByteBuffer uvBuffer;

    private ByteBuffer vertexBuffer;
    private ByteBuffer texBuffer;

    private int yTexture = NO_TEXTURE; //Our Y texture
    private int uvTexture = NO_TEXTURE; //Our UV texture

    private int attr_pos;
    private int attr_tex;
    private int yloc;
    private int uvloc;

    //
    private int width;
    private int height;

    /**
     * Delete the openGL objects.S
     */
    public void destroy(){

        GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);

        unloadTexture(yTexture);
        unloadTexture(uvTexture);
    }

    private final float vertexPoint[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    /**
     * 对于前置摄像头，用上下反转的纹理坐标。
     */
    private final float texturePoint[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };

    private String mFragmentShader =
            "precision mediump float;\n" +
            "varying vec2 v_texCoord;\n" +
            "uniform sampler2D y_texture;\n" +
            "uniform sampler2D uv_texture;\n" +

            "void main (void){\n" +
            "   float r, g, b, y, u, v;\n" +
            "   y = texture2D(y_texture, v_texCoord).r;\n" +
            "   u = texture2D(uv_texture, v_texCoord).a - 0.5;\n" +
            "   v = texture2D(uv_texture, v_texCoord).r - 0.5;\n" +
            "   r = y + 1.370705*v;\n" +
            "   g = y - 0.337633*u - 0.698001*v;\n" +
            "   b = y + 1.732446*u;\n" +
            "   gl_FragColor = vec4(r, g, b, 1.0);\n" +
            "}\n";


    String mVertexShader =
            "attribute vec2 a_position;                         \n" +
            "attribute vec2 a_texCoord;                         \n" +
            "varying vec2 v_texCoord;                           \n" +

            "void main(){                                       \n" +
            "   gl_Position = vec4(a_position, 1, 1);                       \n" +
            "   v_texCoord = a_texCoord;                        \n" +
            "}                                                  \n";

    private int mOutputWidth;
    private int mOutputHeight;
    public void setOutputSize(int width, int height){
        mOutputWidth = width;
        mOutputHeight = height;
    }

    /**
     * Render the NV21 data to the texture.
     * @param textureId
     */
    public void renderNv21(byte[] nv21Data
            , int textureId
            ,  int width
            , int height
            , boolean isFrontCamera){

        if (width == 0 || height == 0) {
            return;
        }
        checkInit(width, height, isFrontCamera);

        GLES20.glViewport(0,0, mOutputWidth, mOutputHeight);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER
                , GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);

        GLES20.glUseProgram(mGLProgId);
        GLES20.glUniform1i(yloc, 0);
        GLES20.glUniform1i(uvloc, 1);

        GLES20.glEnableVertexAttribArray(attr_pos);
        GLES20.glEnableVertexAttribArray(attr_tex);

        GLES20.glVertexAttribPointer(attr_pos
                , 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(attr_tex
                , 2, GLES20.GL_FLOAT, false, 0, texBuffer);
        /*
         * Prepare the Y channel texture
         */
        //Set texture slot 0 as active and bind our texture object to it
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        yBuffer = ByteBuffer.wrap(nv21Data, 0, width*height);

        uvBuffer.put(nv21Data, width*height, width*height/2);
        uvBuffer.position(0);
        //final ByteBuffer data, int xoffset, int yOffset,  final int type, final Camera.Size size, int usedTexId)
        yTexture = loadTexture(yBuffer, width,  height, GLES20.GL_LUMINANCE, yTexture);
        yBuffer = null;

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        uvTexture = loadTexture(uvBuffer, width/2,  height/2, GLES20.GL_LUMINANCE_ALPHA, uvTexture);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(attr_pos);
        GLES20.glDisableVertexAttribArray(attr_tex);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLES20.glUseProgram(0);
    }

    private void checkError(){
        int error = GLES20.glGetError();
        if(error!= 0){
            Log.e("wangshuai", "error", new Exception());
        }
    }


    public static int unloadTexture(int texture){
        if (texture != NO_TEXTURE) {
            int textures[] = new int[] {
                texture
            };
            GLES20.glDeleteTextures(1, textures, 0);
        }
        return NO_TEXTURE;
    }

    public static int loadTexture(final ByteBuffer data, int width, int height, final int type, int usedTexId) {
        if (usedTexId == NO_TEXTURE) {
            int textures[] = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            usedTexId = textures[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, type, width, height,
                    0, type, GLES20.GL_UNSIGNED_BYTE, null);
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, width,
                height, type, GLES20.GL_UNSIGNED_BYTE, data);
        return usedTexId;
    }

    /**
     * 初始化
     */
    private void checkInit(int width, int height, boolean isFrontCamera) {
        if (!inited) {
            inited = true;
            this.width = width;
            this.height = height;
            mGLProgId = OpenGLUtils.loadProgram(mVertexShader, mFragmentShader);
            mFrameBuffers = new int[1];
            GLES20.glGenFramebuffers(1, mFrameBuffers, 0);

           //int[] textures = new int[2];
            //GLES20.glGenTextures(2, textures, 0);
            yTexture = NO_TEXTURE;
            uvTexture = NO_TEXTURE;

            attr_pos = GLES20.glGetAttribLocation(mGLProgId, "a_position");

            attr_tex = GLES20.glGetAttribLocation(mGLProgId, "a_texCoord");

            yloc = GLES20.glGetUniformLocation(mGLProgId, "y_texture");
            uvloc = GLES20.glGetUniformLocation(mGLProgId, "uv_texture");


            yBuffer = ByteBuffer.allocateDirect(width*height);
            uvBuffer = ByteBuffer.allocateDirect(width*height/2); //We have (width/2*height/2) pixels, each pixel is 2 bytes
            yBuffer.order(ByteOrder.nativeOrder());
            uvBuffer.order(ByteOrder.nativeOrder());

            vertexBuffer = ByteBuffer.allocateDirect(vertexPoint.length*4);
            vertexBuffer.order(ByteOrder.nativeOrder());
            vertexBuffer.asFloatBuffer().put(vertexPoint);
            vertexBuffer.position(0);
            texBuffer = ByteBuffer.allocateDirect(texturePoint.length * 4);
            texBuffer.order(ByteOrder.nativeOrder());
            texBuffer.asFloatBuffer().put(texturePoint);
            texBuffer.position(0);
        }else {

            if(this.width!= width
                    || this.height!= height) {
                this.width = width;
                this.height = height;
                yTexture = unloadTexture(yTexture);
                uvTexture = unloadTexture(uvTexture);
                yBuffer = ByteBuffer.allocateDirect(width * height);
                uvBuffer = ByteBuffer.allocateDirect(width * height / 2); //We have (width/2*height/2) pixels, each pixel is 2 bytes
                yBuffer.order(ByteOrder.nativeOrder());
                uvBuffer.order(ByteOrder.nativeOrder());
            }
        }
    }
}
