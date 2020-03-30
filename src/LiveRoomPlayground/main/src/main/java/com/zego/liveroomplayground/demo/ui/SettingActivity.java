package com.zego.liveroomplayground.demo.ui;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.zego.common.GetAppIdConfig;
import com.zego.common.util.PreferenceUtil;
import com.zego.common.util.ZegoUtil;
import com.zego.common.widgets.CustomPopWindow;
import com.zego.liveroomplayground.R;
import com.zego.liveroomplayground.databinding.ActivitySettingBinding;
import com.zego.zegoliveroom.ZegoLiveRoom;

import static com.zego.common.util.PreferenceUtil.KEY_APP_ID;
import static com.zego.common.util.PreferenceUtil.KEY_APP_SIGN;
import static com.zego.common.util.PreferenceUtil.KEY_TEST_ENVIRONMENT;


public class SettingActivity extends AppCompatActivity {

    private ActivitySettingBinding binding;

    private String veVersion = "VE版本：";
    private String sdkVersion = "SDK版本：";
    private String demoVersion = "Demo版本：";

//    private int envSelection = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);

        // 设置界面参数，原始设置值
        initViewValue();

        binding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // 以下实现长按复制功能
        binding.txVeVersion.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager cmb = (ClipboardManager)SettingActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                cmb.setText(binding.txVeVersion.getText());
                showPopWindows(getString(R.string.tx_copyed), v);
                return false;
            }
        });

        binding.txSdkVersion.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager cmb = (ClipboardManager)SettingActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                cmb.setText(binding.txSdkVersion.getText());
                showPopWindows(getString(R.string.tx_copyed), v);
                return false;
            }
        });

        binding.txDemoVersion.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager cmb = (ClipboardManager)SettingActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                cmb.setText(binding.txDemoVersion.getText());
                showPopWindows(getString(R.string.tx_copyed), v);
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        // 保存页面设置
        saveSetting();
        finish();
    }

    // 还原原设置参数
    private void initViewValue() {
        binding.txVeVersion.setText(getVeVersion());
        binding.txSdkVersion.setText(getSdkVersion());
        binding.txDemoVersion.setText(demoVersion +getLocalVersionName(this));

        binding.edAppId.setText(PreferenceUtil.getInstance().getStringValue(KEY_APP_ID, ""));
        binding.edAppSign.setText(PreferenceUtil.getInstance().getStringValue(KEY_APP_SIGN, ""));
        binding.spEnv.setSelection(PreferenceUtil.getInstance().getBooleanValue(KEY_TEST_ENVIRONMENT, true) ? 0 : 1);
    }

    // 保存界面上的设置参数
    private void saveSetting() {
        // AppID
        PreferenceUtil.getInstance().setStringValue(KEY_APP_ID, binding.edAppId.getText().toString().trim());
        // AppSign
        PreferenceUtil.getInstance().setStringValue(KEY_APP_SIGN, binding.edAppSign.getText().toString().trim());
        // env
        PreferenceUtil.getInstance().setBooleanValue(KEY_TEST_ENVIRONMENT, (binding.spEnv.getSelectedItemPosition() == 0) ? true : false);
    }

    // 获取 VE 版本
    public String getVeVersion() {

        return veVersion+ZegoLiveRoom.version2();
    }

    // 获取 SDK 版本
    public String getSdkVersion() {
        return sdkVersion+ZegoLiveRoom.version();
    }

    /**
     * 供其他Activity调用，进入当前Activity查看版本
     *
     * @param activity
     */
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, SettingActivity.class);

        activity.startActivity(intent);
    }

    public String getLocalVersionName(Context ctx) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("liveroomplayground","not found package name" + e.getMessage());

        }

        return localVersion;
    }

    /**
     * 显示长按复制结构窗口
     *
     * @param msg  显示内容
     * @param view
     */
    private void showPopWindows(String msg, View view) {
        //创建并显示popWindow
        new CustomPopWindow.PopupWindowBuilder(this)
                .enableBackgroundDark(true) //弹出popWindow时，背景是否变暗
                .setBgDarkAlpha(0.7f) // 控制亮度
                .create()
                .setMsg(msg)
                .showAsDropDown(view, 0, 20);
    }
}
