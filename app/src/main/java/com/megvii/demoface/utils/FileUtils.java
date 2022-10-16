package com.megvii.demoface.utils;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

    public static String saveBitmap(Context mContext, Bitmap bitmaptosave, String fileName) {
        if (bitmaptosave == null)
            return null;

//        File mediaStorageDir = mContext
//                .getExternalFilesDir("megLive");
        File mediaStorageDir = new File("/sdcard/DCIM/facePP/");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        fileName = System.currentTimeMillis() + fileName + ".png";
//		String bitmapFileName = System.currentTimeMillis() + "";
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mediaStorageDir + "/" + fileName);
            boolean successful = bitmaptosave.compress(
                    Bitmap.CompressFormat.PNG, 100, fos);

            if (successful)
                return mediaStorageDir.getAbsolutePath() + "/" + fileName;
            else
                return null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
