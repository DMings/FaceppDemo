package com.megvii.demoface;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.megvii.demoface.utils.ConUtil;
import com.megvii.demoface.utils.StatusBarUtil;
import com.megvii.facepp.multi.sdk.FaceppApi;

import java.util.List;

public class HomeActivity extends BaseActivity implements AdapterView.OnItemClickListener {
    GridView gvAbilityView;
    private List<FaceppApi.Ability> abilities;
    private ProgressDialog mDialog;

    //    private String[] ability =
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtil.setStatusBarForBlueBg(this, null);

    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_home;
    }

    @Override
    protected void initView() {
        gvAbilityView = findViewById(R.id.gv_ability_view);
        mDialog = new ProgressDialog(this);
        mDialog.setMessage("模型加载中....");
        mDialog.setCancelable(false);

    }

    @Override
    protected void initData() {
        FaceppApi.getInstance().setLogLevel(4);
        int result = FaceppApi.getInstance().initHandle(ConUtil.readAssetsData(HomeActivity.this, "megviifacepp_model"));
        if (result == FaceppApi.MG_RETCODE_OK) {
            gvAbilityView.setVisibility(View.VISIBLE);
            abilities = FaceppApi.getInstance().getAbilityList(ConUtil.readAssetsData(HomeActivity.this, "megviifacepp_model")); //界面需要 可以不调
            gvAbilityView.setAdapter(new MyAdapter());
            gvAbilityView.setOnItemClickListener(this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FaceppApi.Ability ability = (FaceppApi.Ability) parent.getAdapter().getItem(position);
        switch (ability) {
            case FACE_DETECT:
                requestPermission(FACE_DETECT);
                break;
            case FACE_DETECT_ATTRIBUTE:
                requestPermission(FACEPLUS_DETECT);
                break;
            case FACE_COMPARE:
                requestPermission(FACE_COMPARE);
                break;
            case DENSE_LMK:
                requestPermission(DENSE_LMK);
                break;
            case SKELETON:
                requestPermission(SKELETON_DETECT);
                break;
            case SEGMENT:
                requestPermission(SEGMENT);
                break;
            case HAND:
                requestPermission(HAND_DETECT);
                break;
            case SCENE:
                requestPermission(SCENE);
                break;
        }
    }


    private void requestPermission(int requestCode) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    requestCode);
        } else {
            // Permission has already been granted
            gotoNext(requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.w("PermissionsResult", "permissions length:" + permissions.length + ",grantResults length=" + grantResults.length);
        Log.w("PermissionsResult", "permissions 0:" + grantResults[0] + ",grantResults 1=" + grantResults[1]);
        if (grantResults.length != 2 || grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {

        } else {
            gotoNext(requestCode);
        }
    }

    private static final int REQUESTCODE = 1;

    private void gotoNext(int sdkType) {
        if (sdkType != SEGMENT) {
            Intent intent = new Intent(this, SelectedActivity.class);
            intent.putExtra("sdkType", sdkType);
            startActivityForResult(intent, REQUESTCODE);
        } else {
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.w("onActivityResult", "requestCode = " + requestCode + " ,resultCode = " + resultCode);
        if (requestCode == REQUESTCODE) {
            if (mDialog != null) {
                Log.w("onActivityResult", "dismiss");
                mDialog.dismiss();
            }
            if (resultCode != 100) {
                Toast.makeText(this, "模型加载失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FaceppApi.getInstance().ReleaseHandle();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private class MyAdapter extends BaseAdapter {

        public MyAdapter() {

        }

        @Override
        public int getCount() {
            return abilities.size();
        }

        @Override
        public Object getItem(int position) {
            return abilities.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHold viewHold;
            if (convertView == null) {
                viewHold = new ViewHold();
                LayoutInflater inflater = LayoutInflater.from(HomeActivity.this);
                convertView = inflater.inflate(R.layout.home_page_item, parent, false);
                viewHold.name_en = convertView.findViewById(R.id.tv_ability_name_en);
                viewHold.name_zh = convertView.findViewById(R.id.tv_ability_name_zh);
                convertView.setTag(viewHold);
            } else {
                viewHold = (ViewHold) convertView.getTag();
            }
            FaceppApi.Ability ability = abilities.get(position);
            switch (ability) {
                case FACE_DETECT:
                    viewHold.name_zh.setText("人脸检测");
                    viewHold.name_en.setText("Face Detection");
                    break;
                case FACE_DETECT_ATTRIBUTE:
                    viewHold.name_zh.setText("人脸检测高阶版");
                    viewHold.name_en.setText("Face Detection");
                    break;
                case FACE_COMPARE:
                    viewHold.name_zh.setText("人脸比对");
                    viewHold.name_en.setText("Face Comparing");
                    break;
                case DENSE_LMK:
                    viewHold.name_zh.setText("人脸稠密关键点");
                    viewHold.name_en.setText("Dense Facial Landmarks");
                    break;
                case SKELETON:
                    viewHold.name_zh.setText("人体骨骼点");
                    viewHold.name_en.setText("Skeleton Detection");
                    break;
                case SEGMENT:
                    viewHold.name_zh.setText("人体抠像");
                    viewHold.name_en.setText("Body Outlining");
                    break;
                case HAND:
                    viewHold.name_zh.setText("手部检测");
                    viewHold.name_en.setText("Gesture Recognition");
                    break;
                case SCENE:
                    viewHold.name_zh.setText("场景识别");
                    viewHold.name_en.setText("Scene recognition");
                    break;
            }

            return convertView;
        }

        class ViewHold {
            TextView name_zh;
            TextView name_en;
        }
    }
}
