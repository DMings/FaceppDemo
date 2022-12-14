package com.megvii.demoface.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class Screen {

	public static float LEFTMENU_UI_PERCENT = 0.15f;
	public static int mNotificationBarHeight;

	public static int mScreenWidth;
	public static int mScreenHeight;
	public static int mRealWidth;
	public static int mRealHeight;
	public static int mWidth;
	public static int mHeight;
	public static float densityDpi;
	public static float density;

	public static float drawWidth;
	public static float drawHeight;

	private static final int PADDING_L = 30;
	private static final int PADDING_R = 30;
	private static final int PADDING_T = 50;
	private static final int PADDING_B = 40;

	public static float drawPaddingLeft;
	public static float drawPaddingRight;
	public static float drawPaddingTop;
	public static float drawPaddingBottom;

	public static int drawRows;
	public static float lineHeight;
	public static float line_space = 0;
	public static float charHeight;

	public static void initialize(Context context) {
		if (drawWidth == 0 || drawHeight == 0 || mWidth == 0 || mHeight == 0
				|| density == 0) {
			Resources res = context.getResources();
			DisplayMetrics metrics = res.getDisplayMetrics();
			WindowManager wm = (WindowManager) (context.getSystemService(Context.WINDOW_SERVICE));
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
				wm.getDefaultDisplay().getRealMetrics(metrics);
			}
			// TODO // - 50
			density = metrics.density;
			mNotificationBarHeight = (int) (35 * density);
			mWidth = metrics.widthPixels;// - (int)(50 * density)
			mHeight = metrics.heightPixels/* - mNotificationBarHeight */;// -
			// (int)(50
			// *
			// density)
			mScreenWidth = metrics.widthPixels;
			mScreenHeight = metrics.heightPixels;

			densityDpi = metrics.densityDpi;

			drawPaddingLeft = density * PADDING_L;
			drawPaddingRight = density * PADDING_R;
			drawPaddingTop = density * PADDING_T;
			drawPaddingBottom = density * PADDING_B;

			drawWidth = mWidth - drawPaddingLeft - drawPaddingRight;
			// TODO ????????????????????????????????????????????????????
			drawHeight = mHeight - drawPaddingTop - drawPaddingBottom;


			Point point = new Point();
			wm.getDefaultDisplay().getRealSize(point);
			mRealWidth = point.x;
			mRealHeight = point.y;
		}
	}

	public static String clipImageUrl(String url, String add) {
		String temp = null;
		if (url != null) {
			if (add != null) {
				if (url.endsWith(".jpg") || url.endsWith(".png")
						|| url.endsWith(".gif") || url.endsWith(".bmp")) {
					String end = url.substring(url.length() - 4, url.length());
					int point = url.lastIndexOf(".");
					int index = url.lastIndexOf("/");
					if (index != -1) {
						String sub = url.substring(index + 1, point);
						if (sub.endsWith("_m") || sub.endsWith("_b")
								|| sub.endsWith("_s")) {
							String clip = sub.substring(0, sub.length() - 2);
							temp = url.substring(0, index + 1) + clip + add
									+ end;
						} else {
							temp = url.substring(0, index + 1) + sub + add
									+ end;
						}
					}
				}
			} else {
				temp = url;
			}
		}
		return temp;
	}
}