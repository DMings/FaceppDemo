package com.megvii.demoface.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class OpenGLUtils {

	public static final int NO_TEXTURE = -1;
	public static final int NOT_INIT = -1;	
	public static final int ON_DRAWN = 1;

	/**
	 * 虽然OpenGL介绍贴图以左下角为原点。
	 * 但是实际操作中，贴图的坐标系以左上角为原点，主要是因为最终还是要输出到屏幕上。
	 */
	public static final float TEXTURE_NO_ROTATION[] = { 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f };

	/**
	 * 前摄像头，上下反转，逆时针旋转90度
	 */
	public static final float TEXTURE_ROTATED_FRONT[] = { 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f };
	/**
	 * 后摄像头，顺时针90度
	 */
	public static final float TEXTURE_ROTATED_BACK[] = { 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f };

	/**
	 * 正规化坐标系。
	 */
	public static final float CUBE[] = { -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, };

	public static float[] getRotation(final int rotation, final boolean flipHorizontal, final boolean flipVertical) {
		return TEXTURE_ROTATED_FRONT;
	}

	public static int[] initTextureID(int width, int height) {
		int[] mTextureOutID = new int[2];

		GLES20.glGenTextures(2, mTextureOutID, 0);
		for(int i = 0; i < mTextureOutID.length; ++ i) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureOutID[i]);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA,
					GLES20.GL_UNSIGNED_BYTE, null);
		}
		return mTextureOutID;
	}

	public static int loadProgram(final String strVSource, final String strFSource) {
		int iVShader;
		int iFShader;
		int iProgId;
		int[] link = new int[1];
		iVShader = loadShader(strVSource, GLES20.GL_VERTEX_SHADER);
		if (iVShader == 0) {
			Log.d("Load Program", "Vertex Shader Failed");
			return 0;
		}
		iFShader = loadShader(strFSource, GLES20.GL_FRAGMENT_SHADER);
		if (iFShader == 0) {
			Log.d("Load Program", "Fragment Shader Failed");
			return 0;
		}

		iProgId = GLES20.glCreateProgram();

		GLES20.glAttachShader(iProgId, iVShader);
		GLES20.glAttachShader(iProgId, iFShader);

		GLES20.glLinkProgram(iProgId);

		GLES20.glGetProgramiv(iProgId, GLES20.GL_LINK_STATUS, link, 0);
		if (link[0] <= 0) {
			Log.d("Load Program", "Linking Failed");
			return 0;
		}
		GLES20.glDeleteShader(iVShader);
		GLES20.glDeleteShader(iFShader);
		return iProgId;
	}

	private static int loadShader(final String strSource, final int iType) {
		int[] compiled = new int[1];
		int iShader = GLES20.glCreateShader(iType);
		GLES20.glShaderSource(iShader, strSource);
		GLES20.glCompileShader(iShader);
		GLES20.glGetShaderiv(iShader, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) {
			Log.e("Load Shader Failed", "Compilation\n" + GLES20.glGetShaderInfoLog(iShader));
			return 0;
		}
		return iShader;
	}

	// 从sh脚本中加载shader内容的方法
	public static String loadFromRawFile(Context context, int rawId) {
		String result = null;
		try {
			InputStream in = context.getResources().openRawResource(rawId);;
			int ch = 0;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while ((ch = in.read()) != -1) {
				baos.write(ch);
			}
			byte[] buff = baos.toByteArray();
			baos.close();
			in.close();
			result = new String(buff, "UTF-8");
			result = result.replaceAll("\\r\\n", "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
