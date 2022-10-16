package com.megvii.demoface.utils;

import android.graphics.Bitmap;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * 抠像结果处理工具类
 */
public class SegResultHandleUtils {

    /**
     * 设置黑白(人像区域白色背景黑色)
     *
     * @param image
     * @param alphas
     * @return
     */
    public static Bitmap setBlackWhite(Bitmap image, float[] alphas) {
        int bytes = image.getByteCount();
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the data.
        byte[] pixels = new byte[temp.length]; // Allocate for RGBA
        // Copy pixels into place
        for (int i = 0; i < (temp.length / 4); i++) {
            pixels[i * 4 + 0] = (byte) (alphas[i] * 255);       //R
            pixels[i * 4 + 1] = (byte) (alphas[i] * 255);       //G
            pixels[i * 4 + 2] = (byte) (alphas[i] * 255);       //B
            pixels[i * 4 + 3] = (byte) 255;
        }
        ByteBuffer bf = ByteBuffer.wrap(pixels);
        Bitmap cropBitmap = bufferToBitmap(bf, width, height);
        return cropBitmap;
    }

    /**
     * 背景融合 （建议背景尺寸大于前景）
     *
     * @param fgBitmap 前景图（抠像图）
     * @param bgBitmap 背景图
     * @param alphas   透明度（抠像接口返回的结果）
     * @return
     */
    public static Bitmap backGroudBlend(Bitmap fgBitmap, Bitmap bgBitmap, float[] alphas) {
        int fgWidth = fgBitmap.getWidth();
        int fgHeight = fgBitmap.getHeight();

        int bgWidth = bgBitmap.getWidth();
        int bgHeight = bgBitmap.getHeight();

        if (fgWidth > bgWidth || fgHeight > bgHeight) {
            //前背景尺寸不匹配，不做融合
            return fgBitmap;
        }
        byte[] fgArray = getPixelsRGB(fgBitmap);
        byte[] bgArray = getPixelsRGB(bgBitmap);
        byte[] pixels = new byte[fgArray.length]; // Allocate for RGB


        for (int i = 0; i < fgHeight; i++) {
            for (int j = 0; j < fgWidth; j++) {
                int bgIndex = (i * bgWidth + j) * 3;
                int fgIndex = (i * fgWidth + j) * 3;
                int dstIndex = (int) fgWidth * i + j;
                float bgR = (float) (bgArray[bgIndex] & 0xFF);
                float bgG = (float) (bgArray[bgIndex + 1] & 0xFF);
                float bgB = (float) (bgArray[bgIndex + 2] & 0xFF);

                float fgR = (float) (fgArray[fgIndex] & 0xFF);
                float fgG = (float) (fgArray[fgIndex + 1] & 0xFF);
                float fgB = (float) (fgArray[fgIndex + 2] & 0xFF);

                float alpha = alphas[dstIndex];
                pixels[fgIndex + 0] = (byte) (fgR * (alpha) + (bgR * (1 - alpha)));       //R
                pixels[fgIndex + 1] = (byte) (fgG * (alpha) + (bgG * (1 - alpha)));       //G
                pixels[fgIndex + 2] = (byte) (fgB * (alpha) + (bgB * (1 - alpha)));       //B

            }
        }

        return rgb2Bitmap(pixels, fgWidth, fgHeight);
    }

    /**
     * 背景透明
     *
     * @param image  抠像输入图
     * @param alphas 抠像返回结果
     * @return
     */
    public static Bitmap setBitmapAlpha(Bitmap image, float[] alphas) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the data.
        byte[] pixels = new byte[temp.length]; // Allocate for RGBA
        // Copy pixels into place
        for (int i = 0; i < (temp.length / 4); i++) {
            pixels[i * 4 + 0] = temp[i * 4 + 0];       //R
            pixels[i * 4 + 1] = temp[i * 4 + 1];       //G
            pixels[i * 4 + 2] = temp[i * 4 + 2];       //B
            pixels[i * 4 + 3] = (byte) (alphas[i] * 255);
        }
        ByteBuffer bf = ByteBuffer.wrap(pixels);
        Bitmap cropBitmap = bufferToBitmap(bf, width, height);
        return cropBitmap;
    }


    private static Bitmap rgb2Bitmap(byte[] imageData, int width, int height) {
        int[] argb = new int[width * height];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                argb[i * width + j] = 255;
                argb[i * width + j] = (argb[i * width + j] << 8) + imageData[(i * width + j) * 3 + 0]; //+r
                argb[i * width + j] = (argb[i * width + j] << 8) + imageData[(i * width + j) * 3 + 1]; //+g
                argb[i * width + j] = (argb[i * width + j] << 8) + imageData[(i * width + j) * 3 + 2]; //+b
            }
        }
        Bitmap bit = Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);
        return bit;
    }

    /**
     * 宽高对应buffer的实际宽高
     *
     * @param buffer
     * @param width
     * @param height
     * @return
     */
    private static Bitmap bufferToBitmap(ByteBuffer buffer, int width, int height) {

        byte bitmapBuffer[] = buffer.array();
//   rgba   argb
        int bitmapSource[] = new int[width * height];
        for (int i = 0, j = 0; i < width * height * 4; i++, j++) {

            bitmapSource[j] = (int) (((bitmapBuffer[i++] & 0xFF) << 16)
                    | ((bitmapBuffer[i++] & 0xFF) << 8)
                    | ((bitmapBuffer[i++] & 0xFF))
                    | (bitmapBuffer[i] & 0xFF) << 24);

        }

        return Bitmap.createBitmap(bitmapSource, width, height, Bitmap.Config.ARGB_8888);
    }

    private static byte[] getPixelsRGB(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        Log.w("getPixelsRGB", "image width = " + image.getWidth() + ", height = " + image.getHeight() + "bytes = " + bytes);
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer

        byte[] temp = buffer.array(); // Get the underlying array containing the data.

        byte[] pixels = new byte[temp.length * 3 / 4]; // Allocate for RGBA

        // Copy pixels into place   rgba-bgr
        for (int i = 0; i < (temp.length / 4); i++) {

            pixels[i * 3 + 0] = temp[i * 4 + 0];       //R
            pixels[i * 3 + 1] = temp[i * 4 + 1];       //G
            pixels[i * 3 + 2] = temp[i * 4 + 2];       //B

        }
        return pixels;
    }

    public static byte[] getPixelsRGBA(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();

        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer

        byte[] temp = buffer.array(); // Get the underlying array containing the data.

        return temp;
    }
}
