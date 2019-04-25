package com.zego.play.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.zego.common.util.ZegoUtil;
import com.zego.common.widgets.CustomDialog;
import com.zego.play.R;
import com.zego.common.ZGBaseHelper;
import com.zego.common.ui.BaseActivity;
import com.zego.common.ui.WebActivity;
import com.zego.common.util.AppLogger;
import com.zego.common.widgets.CustomPopWindow;
import com.zego.play.databinding.ActivityPlayInitSdkBinding;
import com.zego.zegoliveroom.callback.IZegoInitSDKCompletionCallback;

import java.util.regex.Pattern;


public class InitSDKPlayActivityUI extends BaseActivity implements View.OnClickListener {

    ActivityPlayInitSdkBinding binding;
    String flag;

    @Override
    protected void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_play_init_sdk);

        binding.appidDescribe.setOnClickListener(this);
        binding.appSignDescribe.setOnClickListener(this);
        binding.useridDescribe.setOnClickListener(this);
        flag = getIntent().getStringExtra("flag");
    }

    /**
     * Button 点击事件触发
     * <p>
     * 进行Zego SDK的初始化。
     */
    public void onInitSDK(View view) {
        AppLogger.getInstance().i(InitSDKPlayActivityUI.class, "点击 初始化SDK按钮");
        String appSignStr = binding.edAppSign.getText().toString();
        String appIDStr = binding.edAppid.getText().toString();
        boolean testEnv = binding.testEnv.isChecked();

        // 必须校验appid与appSign
        if (!TextUtils.isEmpty(appIDStr) || !TextUtils.isEmpty(appSignStr)) {

            // appSign 是byte[]的类型, 所以需要解析字符串。
            byte[] appSign = ZegoUtil.parseSignKeyFromString(appSignStr);
            long appID = ZegoUtil.parseAppIDFromString(appIDStr);

            // 有可能签名会解析失败，
            if (appSign == null || appID == 0) {
                Toast.makeText(this, appID == 0 ? "AppID格式非法" : "AppSign格式非法", Toast.LENGTH_LONG).show();
                return;
            }

            // 防止用户点击，弹出加载对话框
            CustomDialog.createDialog("初始化SDK中...", InitSDKPlayActivityUI.this).show();

            // 调用sdk接口, 初始化sdk
            boolean results = ZGBaseHelper.sharedInstance().initZegoSDK(appID, appSign, testEnv, new IZegoInitSDKCompletionCallback() {
                @Override
                public void onInitSDK(int errorCode) {

                    // 关闭加载对话框
                    CustomDialog.createDialog(InitSDKPlayActivityUI.this).cancel();

                    // errorCode 非0 代表初始化sdk失败
                    // 具体错误码说明请查看<a> https://doc.zego.im/CN/308.html </a>
                    if (errorCode == 0) {
                        AppLogger.getInstance().i(InitSDKPlayActivityUI.class, "初始化zegoSDK成功");
                        Toast.makeText(InitSDKPlayActivityUI.this, getString(com.zego.common.R.string.tx_init_success), Toast.LENGTH_SHORT).show();
                        // 初始化成功，跳转到登陆房间页面。
                        jumpLoginRoom();
                    } else {
                        AppLogger.getInstance().i(InitSDKPlayActivityUI.class, "初始化sdk失败 错误码 : %d", errorCode);
                        Toast.makeText(InitSDKPlayActivityUI.this, getString(com.zego.common.R.string.tx_init_failure), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // 如果接口调用失败，也需要关闭对话框
            if (!results) {
                // 关闭加载对话框
                CustomDialog.createDialog(InitSDKPlayActivityUI.this).cancel();
            }

        } else {
            AppLogger.getInstance().i(InitSDKPlayActivityUI.class, "AppId 或者 appSign 格式非法");
            Toast.makeText(InitSDKPlayActivityUI.this, "AppId 或者 appSign 格式非法", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * 跳转登陆页面
     */
    private void jumpLoginRoom() {
        // flag, 需要传递给登陆房间页面。
        LoginRoomPlayActivityUI.actionStart(this, flag);

        finish();
    }

    /**
     * Button 点击事件触发
     * <p>
     * 跳转到zego开发者中心
     *
     * @param view
     */
    public void goGetAppID(View view) {
        WebActivity.actionStart(this, "https://doc.zego.im/CN/621.html", getString(com.zego.common.R.string.tx_get_appid_guide));
    }

    /**
     * Button 点击事件触发
     * <p>
     * 跳转到示例代码
     *
     * @param view
     */
    public void goCodeDemo(View view) {
        WebActivity.actionStart(this, "https://doc.zego.im/CN/215.html", ((TextView) view).getText().toString());
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.userid_describe) {
            showPopWindows(getString(com.zego.common.R.string.userid_describe), v);
        } else if (id == R.id.appid_describe) {
            showPopWindows(getString(com.zego.common.R.string.appid_describe), v);
        } else if (id == R.id.app_sign_describe) {
            showPopWindows(getString(com.zego.common.R.string.app_sign_describe), v);
        }
    }


    /**
     * 显示描述窗口
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

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, InitSDKPlayActivityUI.class);
        activity.startActivity(intent);

    }
}
