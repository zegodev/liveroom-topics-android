package com.zego.sound.processing.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.zego.sound.processing.R;
import com.zego.sound.processing.ZGSoundProcessingHelper;
import com.zego.sound.processing.adapter.SoundEffectViewAdapter;
import com.zego.sound.processing.base.SoundProcessPublishBaseUI;
import com.zego.zegoavkit2.audioprocessing.ZegoAudioReverbMode;

/**
 * Created by zego on 2019/4/22.
 *
 */

public class SoundProcessPublishUI extends SoundProcessPublishBaseUI {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化视图回调代理
        initViewCallback();
    }


    private void initViewCallback() {
        // 在推流前必须先打开SDK双声道才能使用虚拟立体声功能
        ZGSoundProcessingHelper.sharedInstance().setAudioChannelCount(2);

        getSoundEffectDialog().setOnSoundEffectAuditionCheckedListener(new SoundEffectViewAdapter.OnSoundEffectAuditionCheckedListener() {

            /**
             * 勾选了界面上的音效试听会触发此回调
             *
             * @param isChecked
             */
            @Override
            public void onSoundEffectAuditionChecked(boolean isChecked) {
                // 开启SDK耳返，此时可以听到自己的声音
                ZGSoundProcessingHelper.sharedInstance().enableLoopback(isChecked);
                if (!isChecked) {
                    Toast.makeText(SoundProcessPublishUI.this, R.string.sound_effect_audition_close_tip, Toast.LENGTH_LONG).show();
                }
            }

        });

        // 设置变声控件变化监听器
        getSoundEffectDialog().setOnVoiceChangeListener(new SoundEffectViewAdapter.OnVoiceChangeListener() {
            @Override
            public void onVoiceChangeParam(float param) {

                /**
                 * 设置 SDK 变声参数
                 */
                ZGSoundProcessingHelper.sharedInstance().setVoiceChangerParam(param);
            }
        });

        // 设置混响控件变化监听器
        getSoundEffectDialog().setOnReverberationChangeListener(new SoundEffectViewAdapter.OnReverberationChangeListener() {
            @Override
            public void onAudioReverbModeChange(boolean enable, ZegoAudioReverbMode mode) {

                /**
                 * 设置 SDK 启用 SDK 混响
                 */
                ZGSoundProcessingHelper.sharedInstance().enableReverb(enable, mode);
            }

            @Override
            public void onRoomSizeChange(float param) {

                /**
                 * 设置 SDK 混响房间大小
                 */
                ZGSoundProcessingHelper.sharedInstance().setReverbRoomSize(param);
            }

            @Override
            public void onDryWetRationChange(float param) {

                /**
                 * 设置 SDK 干湿比
                 */
                ZGSoundProcessingHelper.sharedInstance().setDryWetRation(param);
            }

            @Override
            public void onDamping(float param) {

                /**
                 * 设置 SDK 混响阻尼
                 */
                ZGSoundProcessingHelper.sharedInstance().setReverbDamping(param);
            }

            @Override
            public void onReverberance(float param) {

                /**
                 * 设置 SDK 余响
                 */
                ZGSoundProcessingHelper.sharedInstance().setReverberance(param);
            }
        });

        // 设置立体声角度变化监听器
        getSoundEffectDialog().setOnStereoChangeListener(new SoundEffectViewAdapter.OnStereoChangeListener() {
            @Override
            public void onStereoChangeParam(int param) {

                /**
                 * 设置 SDK 虚拟立体声
                 */
                ZGSoundProcessingHelper.sharedInstance().enableVirtualStereo(true, param);
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 恢复音效混响等默认设置, 避免在其他模块还会出现变声的效果
        // 恢复变声
        ZGSoundProcessingHelper.sharedInstance().setVoiceChangerParam(0.0f);
        // 关闭混响
        ZGSoundProcessingHelper.sharedInstance().enableReverb(false, ZegoAudioReverbMode.CONCERT_HALL);
        // 单声道推流
        ZGSoundProcessingHelper.sharedInstance().setAudioChannelCount(1);
        // 关闭虚拟立体声
        ZGSoundProcessingHelper.sharedInstance().enableVirtualStereo(false, 0);
        // 关闭耳返
        ZGSoundProcessingHelper.sharedInstance().enableLoopback(false);
    }



    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, SoundProcessPublishUI.class);
        activity.startActivity(intent);
    }

    /**
     * Button 点击事件
     * 音效处理
     *
     * @param view
     */
    public void onSoundProcess(View view) {
        getSoundEffectDialog().show();
    }

}
