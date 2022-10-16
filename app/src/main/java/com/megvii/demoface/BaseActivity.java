package com.megvii.demoface;

import android.app.Activity;
import android.os.Bundle;

import com.megvii.demoface.utils.Screen;
import com.megvii.demoface.utils.StatusBarUtil;


public abstract class BaseActivity extends Activity {
    protected static final int FACE_DETECT = 1;
    protected static final int FACEPLUS_DETECT = 2;
    protected static final int FACE_COMPARE = 3;
    protected static final int DENSE_LMK = 4;
    protected static final int SKELETON_DETECT = 5;
    protected static final int SEGMENT = 6;
    protected static final int HAND_DETECT = 7;
    protected static final int SCENE = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Screen.initialize(this);
        setContentView(getLayoutResId());
        StatusBarUtil.setStatusBarForGrayBg(this);
        initView();
        initData();
    }
    //子类Activity实现
    protected abstract int getLayoutResId();
    //子类Activity实现
    protected abstract void initView();
    //子类Activity实现
    protected abstract void initData();
}
