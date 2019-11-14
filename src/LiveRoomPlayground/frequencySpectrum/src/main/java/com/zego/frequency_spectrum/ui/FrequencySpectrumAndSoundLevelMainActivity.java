package com.zego.frequency_spectrum.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGManager;
import com.zego.common.ui.BaseActivity;
import com.zego.common.ui.WebActivity;
import com.zego.common.util.AppLogger;
import com.zego.common.util.ZegoUtil;
import com.zego.frequency_spectrum.R;
import com.zego.zegoliveroom.callback.IZegoInitSDKCompletionCallback;

/**
 * Created by zego on 2019/5/6.
 *
 * 本类为音频功率谱与声浪专题界面的入口，主要逻辑为加载进入此Activity时：
 *  初始化SDK
 *  触发登录房间的相关逻辑
 */
public class FrequencySpectrumAndSoundLevelMainActivity extends BaseActivity{


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 加载登录房间的布局
        setContentView(R.layout.activity_frequency_spectrum_sound_level_main);
        // 初始化SDK
        initSDK();
    }

    /**
     * 初始化SDK逻辑
     *
     */
    private void initSDK() {
        // 初始化SDK
        ZGBaseHelper.sharedInstance().initZegoSDK(ZegoUtil.getAppID(), ZegoUtil.getAppSign(), ZegoUtil.getIsTestEnv(), new IZegoInitSDKCompletionCallback() {
            @Override
            public void onInitSDK(int errorCode) {
                // 初始化完成后, 请求房间列表
                if (errorCode == 0) {
                    AppLogger.getInstance().i(FrequencySpectrumAndSoundLevelMainActivity.class, "初始化zegoSDK成功");
                } else {
                    AppLogger.getInstance().i(FrequencySpectrumAndSoundLevelMainActivity.class, "初始化zegoSDK失败 errorCode : %d", errorCode);
                }
            }
        });

    }

    /**
     * 登录房间触发方法
     *
     * @param view
     */
    public void joinRoom(View view) {

        final String roomId = ((EditText)findViewById(R.id.ed_room_id)).getText().toString();
        if (!"".equals(roomId)) {
            FrequencySpectrumAndSoundLevelRoomActivity.actionStart(this, roomId);

        } else {
            Toast.makeText(FrequencySpectrumAndSoundLevelMainActivity.this, getString(com.zego.common.R.string.tx_room_id_is_no_null), Toast.LENGTH_SHORT).show();
            AppLogger.getInstance().i(FrequencySpectrumAndSoundLevelMainActivity.class, getString(com.zego.common.R.string.tx_room_id_is_no_null));
        }

    }

    public void goToFrequencySpectrumAndSoundLevelDocs(View view){

        WebActivity.actionStart(this, "https://doc.zego.im/CN/709.html", "音频频谱与声浪");
    }

    /**
     * 退到主界面(可进入其他专题)的触发方法
     *
     * @param view
     */
    public void goBack(View view){

        finish();

    }

    /**
     * 供其他Activity调用，进入本专题的方法
     *
     * @param activity
     */
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, FrequencySpectrumAndSoundLevelMainActivity.class);
        activity.startActivity(intent);
    }

    /**
     * 进入此Activity时会初始化SDK，在退出本专题时可按需释放SDK
     *
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        ZGBaseHelper.sharedInstance().unInitZegoSDK();
    }
}
