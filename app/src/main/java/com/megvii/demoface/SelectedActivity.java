package com.megvii.demoface;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.megvii.demoface.facedetect.FaceDetectFrameActivity;
import com.megvii.demoface.utils.ConUtil;
import com.megvii.demoface.utils.Util;
import com.megvii.facepp.multi.sdk.BodySegmentApi;
import com.megvii.facepp.multi.sdk.DLmkDetectApi;
import com.megvii.facepp.multi.sdk.FaceDetectApi;
import com.megvii.facepp.multi.sdk.FacePPMultiAuthManager;
import com.megvii.facepp.multi.sdk.FaceppApi;
import com.megvii.facepp.multi.sdk.HandDetectApi;
import com.megvii.facepp.multi.sdk.SceneDetectApi;
import com.megvii.facepp.multi.sdk.SkeletonDetectApi;
import com.megvii.licensemanager.sdk.LicenseManager;


public class SelectedActivity extends BaseActivity implements View.OnClickListener {
    public static final int CAMERA_CODE = 100;
    public static final int REQ_GALLERY_CODE = 101;
    public static final int GALLERY_CODE = 101;
    LinearLayout llGoBack;
    TextView tvTitle;
    TextView tvVersion;
    LinearLayout llImageDetect;
    LinearLayout llFrameDetect;
    TextView tvTitleBar;
    LinearLayout llVideoDetect;
    LinearLayout rlLoadingView;

    private int sdkType;
    private boolean isClickable = false;
    private long lastClickTime = 0;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_skeleton_home;
    }

    @Override
    protected void initView() {
        llGoBack = findViewById(R.id.ll_go_back);
        tvTitle = findViewById(R.id.tv_title);
        tvVersion = findViewById(R.id.tv_version);
        llImageDetect = findViewById(R.id.ll_image_detect);
        llFrameDetect = findViewById(R.id.ll_frame_detect);
        tvTitleBar = findViewById(R.id.tv_title_bar);
        llVideoDetect = findViewById(R.id.ll_video_detect);
        rlLoadingView = findViewById(R.id.rl_loading_view);
        llGoBack.setOnClickListener(this);
        llImageDetect.setOnClickListener(this);
        llFrameDetect.setOnClickListener(this);
        llVideoDetect.setOnClickListener(this);

        tvVersion.setText(FaceppApi.getInstance().getApiVersion());
    }

    @Override
    protected void initData() {
        setResult(100);
        sdkType = getIntent().getIntExtra("sdkType", 0);
        llVideoDetect.setVisibility(View.GONE);
        if (sdkType == SKELETON_DETECT) {
            tvTitleBar.setText("人体骨骼关键点");
        } else if (sdkType == SEGMENT) {
            tvTitleBar.setText("人体抠像");
            llVideoDetect.setVisibility(View.VISIBLE);
        } else if (sdkType == FACE_DETECT) {
            tvTitleBar.setText("人脸检测");
        } else if (sdkType == FACEPLUS_DETECT) {
            tvTitleBar.setText("人脸检测高阶版");
        } else if (sdkType == FACE_COMPARE) {
            tvTitleBar.setText("人脸比对");
            llFrameDetect.setVisibility(View.GONE);
        } else if (sdkType == DENSE_LMK) {
            tvTitleBar.setText("人脸稠密关键点");
        } else if (sdkType == HAND_DETECT) {
            tvTitleBar.setText("手部检测");
        } else if (sdkType == SCENE) {
            tvTitleBar.setText("场景检测");
        }
        network();
    }

    private void loadModel() {
        rlLoadingView.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int retCode;
                if (sdkType == SKELETON_DETECT) {
                    retCode = SkeletonDetectApi.getInstance().initSkeletonDetect();
                } else if (sdkType == SEGMENT) {
                    int segMode = getIntent().getIntExtra("segMode", BodySegmentApi.SEGMENT_MODE_ROBUST);
                    retCode = BodySegmentApi.getInstance().initBodySegment(ConUtil.SEGMENT_THREAD_COUNT, segMode, BodySegmentApi.PRIMARY_CL_SECONDARY_GL, ConUtil.SEGMENT_IS_SMOOTH);
                } else if (sdkType == FACE_DETECT) {
                    retCode = FaceDetectApi.getInstance().initFaceDetect();
                } else if (sdkType == FACEPLUS_DETECT) {
                    retCode = FaceDetectApi.getInstance().initFaceDetect();
                } else if (sdkType == FACE_COMPARE) {
                    retCode = FaceDetectApi.getInstance().initFaceDetect();
                } else if (sdkType == DENSE_LMK) {
                    retCode = FaceDetectApi.getInstance().initFaceDetect();
                    if (retCode == FaceppApi.MG_RETCODE_OK) {
                        retCode = DLmkDetectApi.getInstance().initDLmkDetect();
                    }
                } else if (sdkType == HAND_DETECT) {
                    retCode = HandDetectApi.getInstance().initHandDetect();
                } else if (sdkType == SCENE) {
                    retCode = SceneDetectApi.getInstance().initSceneDetect();
                } else {
                    retCode = -1;
                }

                final int finalRetCode = retCode;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        rlLoadingView.setVisibility(View.GONE);
                        if (finalRetCode != FaceppApi.MG_RETCODE_OK) {
                            setResult(101);
                            finish();
                        } else {
                            isClickable = true;
                        }
                    }
                });

            }
        }).start();
    }


    private void network() {
        long ability = FaceppApi.getInstance().getModelAbility(ConUtil.readAssetsData(SelectedActivity.this, "megviifacepp_model"));
        FacePPMultiAuthManager authManager = new FacePPMultiAuthManager(ability);
        final LicenseManager licenseManager = new LicenseManager(this);
        licenseManager.registerLicenseManager(authManager);
        String uuid = Util.getUUIDString(this);

        rlLoadingView.setVisibility(View.VISIBLE);
        licenseManager.takeLicenseFromNetwork(Util.TEST_LICENSE_URL, uuid, Util.API_KEY, Util.API_SECRET,
                "1", new LicenseManager.TakeLicenseCallback() {
                    @Override
                    public void onSuccess() {
                        Log.e("access123", "success");
                        loadModel();
                    }

                    @Override
                    public void onFailed(int i, byte[] bytes) {
                        rlLoadingView.setVisibility(View.GONE);
                        String msg = "";
                        if (bytes != null && bytes.length > 0) {
                            msg = new String(bytes);
                            Log.e("access123", "failed:" + msg);
                            Toast.makeText(SelectedActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                        setResult(101);
                        finish();
                    }
                });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sdkType == SKELETON_DETECT) {
            SkeletonDetectApi.getInstance().releaseSkeletonDetect();
        } else if (sdkType == SEGMENT) {
            BodySegmentApi.getInstance().releaseBodySegment();
        } else if (sdkType == FACE_DETECT || sdkType == FACEPLUS_DETECT || sdkType == FACE_COMPARE) {
            FaceDetectApi.getInstance().releaseFaceDetect();
        } else if (sdkType == DENSE_LMK) {
            FaceDetectApi.getInstance().releaseFaceDetect();
            DLmkDetectApi.getInstance().releaseDlmDetect();
        } else if (sdkType == HAND_DETECT) {
            HandDetectApi.getInstance().releaseHandDetect();
        } else if (sdkType == SCENE) {
            SceneDetectApi.getInstance().releaseSceneDetect();
        }
    }

    private void requestGalleryPerm() {
        if (sdkType == FACE_COMPARE) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //进行权限请求
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, GALLERY_CODE);
        } else {
            openGalleryActivity();
        }

    }

    private void requestCameraPerm() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //进行权限请求
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_CODE);
        } else {
            goToFrameDetect();
        }

    }

    private void openGalleryActivity() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(type == 1 ? "image/*" : "video/*");
        startActivityForResult(intent, REQ_GALLERY_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == GALLERY_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {// Permission Granted
                showSettingDialog("读取存储卡");
            } else {
                openGalleryActivity();
            }
        }
        if (requestCode == CAMERA_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {// Permission Granted
                showSettingDialog("相机");
            } else {
                goToFrameDetect();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_GALLERY_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    if (type == 1) {
                    } else if (type == 2) {
                    }
                }
                break;
        }
    }

    public void showSettingDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("无" + msg + "权限，去设置里打开");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ConUtil.getAppDetailSettingIntent(SelectedActivity.this);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private int type = 1; // 1 image 2 video

    @Override
    public void onClick(View view) {
        if (System.currentTimeMillis() - lastClickTime < 500) {
            return;
        }
        lastClickTime = System.currentTimeMillis();
        switch (view.getId()) {
            case R.id.ll_go_back:
                if (isClickable) {
                    finish();
                }
                break;
            case R.id.ll_image_detect:
                if (isClickable) {
                    type = 1;
                    requestGalleryPerm();
                }
                break;
            case R.id.ll_frame_detect:
                if (isClickable) {
                    requestCameraPerm();
                }
                break;
            case R.id.ll_video_detect:
                if (isClickable) {
                    type = 2;
                    requestGalleryPerm();
                }
                break;
        }
    }

    private void goToFrameDetect() {
        Intent intent = null;
        if (sdkType == FACE_DETECT) {
            intent = new Intent(this, FaceDetectFrameActivity.class);
            intent.putExtra("type", "face");
        } else if (sdkType == FACEPLUS_DETECT) {
            intent = new Intent(this, FaceDetectFrameActivity.class);
            intent.putExtra("type", "faceplus");
        }
        if (intent != null) {
            startActivity(intent);
        }
    }

}
