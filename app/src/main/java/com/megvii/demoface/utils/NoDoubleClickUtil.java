package com.megvii.demoface.utils;

public class NoDoubleClickUtil {
    private static long lastClickTime;
    private static long lastFastClickTime;
    private static long lastClickTime2;
    private static long lastClickTime3;
    private final static int SPACE_TIME = 1000;

    private static long lastChangedTime;

    public static void initLastClickTime() {
        lastClickTime = 0;
    }

    public synchronized static boolean isDoubleClick() {
        long currentTime = System.currentTimeMillis();
        boolean isClick2;
        if (currentTime - lastClickTime > SPACE_TIME) {
            isClick2 = false;
        } else {
            isClick2 = true;
        }
        lastClickTime = currentTime;
        return isClick2;
    }

    public synchronized static boolean isDoubleChanged() {
        long currentTime = System.currentTimeMillis();
        boolean changed;
        if (currentTime - lastChangedTime > SPACE_TIME) {
            changed = false;
        } else {
            changed = true;
        }
        lastChangedTime = currentTime;
        return changed;
    }

    public static boolean isFastTochClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastFastClickTime;
        if ( 0 < timeD && timeD < 200) {
            return true;
        }
        lastFastClickTime = time;
        return false;
    }

    public static boolean isFastDoubleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime2;
        if ( 0 < timeD && timeD < 800) {
            return true;
        }
        lastClickTime2 = time;
        return false;
    }


}