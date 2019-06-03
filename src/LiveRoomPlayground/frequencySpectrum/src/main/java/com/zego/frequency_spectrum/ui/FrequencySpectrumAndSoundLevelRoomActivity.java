package com.zego.frequency_spectrum.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.zego.common.ZGBaseHelper;

import com.zego.common.ZGPublishHelper;

import com.zego.zegoavkit2.frequencyspectrum.IZegoFrequencySpectrumCallback;
import com.zego.zegoavkit2.frequencyspectrum.ZegoFrequencySpectrumInfo;
import com.zego.zegoavkit2.frequencyspectrum.ZegoFrequencySpectrumMonitor;

import com.zego.zegoavkit2.soundlevel.ZegoSoundLevelMonitor;
import com.zego.frequency_spectrum.base.FrequencySpectrumAndSoundLevelSDKAPIRelatedActivity;


/**
 * Created by zego on 2019/5/6.
 */

public class FrequencySpectrumAndSoundLevelRoomActivity extends FrequencySpectrumAndSoundLevelSDKAPIRelatedActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        // 与业务逻辑相关和UI相关的设置都在父类FrequencySpectrumAndSoundLevelBaseActivity的onCreate方法中完成，
        // SDK的非音频频谱和声浪相关的接口设置可以在FrequencySpectrumAndSoundLevelSDKAPIRelatedActivity看到
        super.onCreate(savedInstanceState);

        // 推拉流之前需要登录房间，业务开发时需要注意，登录房间前需要初始化SDK
        loginRoom();


        // 设置频谱相关回调，由于SDK内部会抛出己方推流和拉到的流的音频频谱的数据，所以需要设置相关回调来监听抛出的数据并处理
        ZegoFrequencySpectrumMonitor.getInstance().setCallback(new IZegoFrequencySpectrumCallback() {

            /**
             * 拉流频率功率谱数据回调，停止拉流之后不会再抛出
             *
             * @param zegoFrequencySpectrumInfos
             */
            @Override
            public void onFrequencySpectrumUpdate(ZegoFrequencySpectrumInfo[] zegoFrequencySpectrumInfos) {

                // 这是本示例专题对于拉流频谱渲染到控件上的处理逻辑，业务方可根据原理使用业务所需的控件处理
                dealwithPlayFrequencySpectrumUpdate(zegoFrequencySpectrumInfos);

            }

            /**
             * 本地音量采集回调，由于是全局有效，即使停止推流还会回调
             *
             * @param zegoFrequencySpectrumInfo
             */
            @Override
            public void onCaptureFrequencySpectrumUpdate(ZegoFrequencySpectrumInfo zegoFrequencySpectrumInfo) {

                // 这是本示例专题对于己方推流频谱渲染到控件上的处理逻辑，业务方可根据原理使用业务所需的控件处理
                dealwithPublishFrequencySpectrumUpdate(zegoFrequencySpectrumInfo);
            }
        });

        // 设置音频频谱监听的周期，即回调抛出数据的频率，最小值为10ms,不设默认为500ms;
        ZegoFrequencySpectrumMonitor.getInstance().setCycle(last_frequency_spectrum_monitor_circle);

        // 以下跟本专题的业务策略相关，当开关状态标识为真时，启动音频频谱监听，业务可以根据自己的策略设计开启或关闭方式
        if(last_frequency_spectrum_monitor_state){
            ZegoFrequencySpectrumMonitor.getInstance().start();
        }else {
            ZegoFrequencySpectrumMonitor.getInstance().stop();

        }


        // 设置声浪相关回调，由于SDK内部会抛出己方推流和拉到的流的声浪数据，所以需要设置相关回调来监听抛出的数据并处理，
        // 这里在 DealWithSoundLevelCallback 类里实现了简单的声浪值变化的效果，业务可以根据自己的需求展现在控件上
        ZegoSoundLevelMonitor.getInstance().setCallback(new DealWithSoundLevelCallback());

        // 设置音频频谱监听的周期，即回调抛出数据的频率，取值范围 [100, 3000]，不设默认 200 ms
        ZegoSoundLevelMonitor.getInstance().setCycle(last_sound_level_monitor_circle);

        // 以下跟本专题的业务策略相关，当开关状态标识为真时，启动声浪监听，业务可以根据自己的策略设计开启或关闭方式
        if(last_sound_level_monitor_state){
            ZegoSoundLevelMonitor.getInstance().start();
        }else {
            ZegoSoundLevelMonitor.getInstance().stop();
        }


    }


    /**
     * 其他Activity跳转到本Activity的接口
     *
     * @param activity 其他Activity的实例
     * @param roomID 需要房间id在本Activity的ActionBar显示
     */
    public static void actionStart(Activity activity, String roomID) {
        Intent intent = new Intent(activity, FrequencySpectrumAndSoundLevelRoomActivity.class);
        intent.putExtra("roomID", roomID);
        activity.startActivity(intent);
    }


    /**
     * SDK销毁时应该释放资源，防止内存泄漏
     *
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 停止监听音频频谱
        ZegoFrequencySpectrumMonitor.getInstance().stop();
        // 音频频谱回调置null
        ZegoFrequencySpectrumMonitor.getInstance().setCallback(null);
        // 停止监听声浪
        ZegoSoundLevelMonitor.getInstance().stop();
        // 声浪回调设置为null
        ZegoSoundLevelMonitor.getInstance().setCallback(null);

        // 停止所有的推流和拉流后，才能执行 logoutRoom
        ZGPublishHelper.sharedInstance().stopPreviewView();
        ZGPublishHelper.sharedInstance().stopPublishing();

        // 当退出界面时退出登陆房间
        ZGBaseHelper.sharedInstance().loginOutRoom();
    }
}
