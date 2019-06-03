package com.zego.frequency_spectrum.base;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.zego.common.ui.BaseActivity;
import com.zego.common.util.DeviceInfoManager;
import com.zego.common.widgets.TitleLayout;
import com.zego.frequency_spectrum.R;
import com.zego.frequency_spectrum.ui.FrequencySpectrumAndSoundLevelSettingsActivity;
import com.zego.frequency_spectrum.widget.BeatLoadView;
import com.zego.frequency_spectrum.widget.FrequencySpectrumAndSoundLevelItem;
import com.zego.zegoavkit2.frequencyspectrum.ZegoFrequencySpectrumInfo;
import com.zego.zegoavkit2.soundlevel.IZegoSoundLevelCallback;
import com.zego.zegoavkit2.soundlevel.ZegoSoundLevelInfo;

import java.util.ArrayList;


/**
 * 本类为 FrequencySpectrumAndSoundLevelRoomActivity 的业务方面逻辑和UI逻辑部分的提取，以帮助客户能直观的关注 FrequencySpectrumAndSoundLevelRoomActivity 里与声浪和频谱设置的方式
 * 即在 FrequencySpectrumAndSoundLevelRoomActivity 类里只包含了音频频谱和声浪设置相关逻辑
 *
 */
public class FrequencySpectrumAndSoundLevelBaseActivity extends BaseActivity {

    // 在 activity_frequency_spectrum_sound_level_room.xml 布局文件展现roomid
    public String roomID ;
    // 推流的流id，这里为避免流id冲突，会获取随机值
    String publishStreamID;

    // 拉流的频谱展现视图使用动态添加的方式
    //BeatLoadView play_beat_load_view;
    // 推流的频谱展现视图这里可以使用写死的方式
    public BeatLoadView publish_beat_load_view;

    // 用于在 frequency_spectrum_sound_level_item.xml 布局控件的右上角展现所拉的流的对应用户名，在本系列的专题里，用户名为设备信息
    public TextView tv_username;
    // 使用线性布局作为容器，以动态添加所拉的流频谱和声浪展现
    public LinearLayout ll_container;
    // 本地推流的声浪的展现，需要获取该控件来设置进度值
    public ProgressBar pb_publish_sound_level;
    // 作为 activity_frequency_spectrum_sound_level_room.xml 和 FrequencySpectrumAndSoundLevelRoomActivity 的 ActionBar
    public TitleLayout tl_activity_frequency_spectrum_room_title;

    // 这里使用 SharedPreferences 来保存音频频谱和声浪的相关设置状态，以实现在UI设置界面进行相关设置之后起效果的功能
    public SharedPreferences sp ;
    // 音频频谱监控的上一次开关状态
    public Boolean last_frequency_spectrum_monitor_state;
    // 声浪监控的上一次开关状态
    public Boolean last_sound_level_monitor_state;
    // 上一次音频频谱的监控周期
    public Integer last_frequency_spectrum_monitor_circle;
    // 上一次声浪的监控周期
    public Integer last_sound_level_monitor_circle;

    // 拉多条流的时候，使用list来保存展现的频谱和声浪的视图
    public ArrayList<FrequencySpectrumAndSoundLevelItem> arrayList_FrequencySpectrumAndSoundLevelItem = new ArrayList<>();



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 加载布局，初始化UI
        setContentView(R.layout.activity_frequency_spectrum_sound_level_room);

        //*******************获取相关控件*******************//

        publish_beat_load_view = findViewById(R.id.publish_beat_load_view);

        tv_username = findViewById(R.id.tv_username);
        // 对于自己推流的一端，指定自己的设备型号
        String deviceName =DeviceInfoManager.getProductName();
        tv_username.setText(deviceName+"(我)");

        ll_container = findViewById(R.id.ll_container);

        pb_publish_sound_level = findViewById(R.id.pb_publish_sound_level);

        tl_activity_frequency_spectrum_room_title = findViewById(R.id.tl_activity_frequency_spectrum_room_title);

        //*******************获取相关控件*******************//


        //*******************获取音频频谱和声浪的设置值*******************//

        sp = getSharedPreferences("FrequencySpectrumAndSoundLevel",Context.MODE_PRIVATE);
        last_frequency_spectrum_monitor_state = sp.getBoolean("last_frequency_spectrum_state", true);
        last_sound_level_monitor_state = sp.getBoolean("last_sound_level_monitor_state", true);
        last_frequency_spectrum_monitor_circle = sp.getInt("last_frequency_spectrum_monitor_circle", 500);
        last_sound_level_monitor_circle = sp.getInt("last_sound_level_monitor_circle", 200);

        //*******************获取音频频谱和声浪的设置值*******************//


        //*******************activity_frequency_spectrum_sound_level_room.xml布局的tab的title的名称，标识roomid*******************//

        Intent it = getIntent();
        String roomid = it.getStringExtra("roomID");

        tl_activity_frequency_spectrum_room_title.setTitleName("roomID:" + roomid);
        roomID = getIntent().getStringExtra("roomID");

        //*******************activity_frequency_spectrum_sound_level_room.xml布局的tab的title的名称，标识roomid*******************//


    }


    /**
     * 点击活动条左边的返回，销毁当前Activity
     *
     * @param vewi
     */
    public void goBackToFrequencySpectrumMainActivity(View vewi){

        finish();

    }

    /**
     * 处理拉到的流频谱展现在控件上
     *
     * @param zegoFrequencySpectrumInfos SDK频谱回调抛出的频谱的信息
     */
    public void dealwithPlayFrequencySpectrumUpdate(ZegoFrequencySpectrumInfo[] zegoFrequencySpectrumInfos){

        for (ZegoFrequencySpectrumInfo zegoFrequencySpectrumInfo : zegoFrequencySpectrumInfos) {

            for(FrequencySpectrumAndSoundLevelItem frequencySpectrumAndSoundLevelItem:arrayList_FrequencySpectrumAndSoundLevelItem){

                if (zegoFrequencySpectrumInfo.streamID.equals(frequencySpectrumAndSoundLevelItem.getStream_id())) {

                    frequencySpectrumAndSoundLevelItem.getBeatLoadView().updateFrequencySpectrum(zegoFrequencySpectrumInfo.frequencySpectrumList);
                }
            }

        }

    }

    /**
     * 处理推流的频谱展现在控件上
     *
     * @param zegoFrequencySpectrumInfo SDK频谱回调抛出的频谱的信息
     */
    public void dealwithPublishFrequencySpectrumUpdate(ZegoFrequencySpectrumInfo zegoFrequencySpectrumInfo){

        publish_beat_load_view.updateFrequencySpectrum(zegoFrequencySpectrumInfo.frequencySpectrumList);

    }


    /**
     * 由于本专题声浪效果方面需要动画效果，在这里 DealWithSoundLevelCallback 里单独处理SDK抛出的回调并且实现过渡动画的效果
     *
     */
    public class DealWithSoundLevelCallback implements IZegoSoundLevelCallback{


        // 由于本专题中声浪需要做动画效果，这里使用两个实例变量来保存上一次SDK声浪回调中抛出的值，以实现过度动画的效果
        // 上一次推流的进度值
        private int last_progress_publish = 0;
        // 默认情况SDK默认支持最多拉12路流，这里使用一个12长度的int数值来保存所拉的流监控周期
        private int[] last_progress_play = new int[12];

        @Override
        public void onSoundLevelUpdate(ZegoSoundLevelInfo[] zegoSoundLevelInfos) {

            for(int i = 0; i < zegoSoundLevelInfos.length; i++ ){

                for(FrequencySpectrumAndSoundLevelItem frequencySpectrumAndSoundLevelItem:arrayList_FrequencySpectrumAndSoundLevelItem){

                    if (zegoSoundLevelInfos[i].streamID.equals(frequencySpectrumAndSoundLevelItem.getStream_id())) {

                        ValueAnimator animator = ValueAnimator.ofInt(last_progress_play[i], (int)zegoSoundLevelInfos[i].soundLevel).setDuration(last_sound_level_monitor_circle);

                        animator.addUpdateListener( new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                frequencySpectrumAndSoundLevelItem.getPb_play_sound_level().setProgress((int)valueAnimator.getAnimatedValue());

                            }
                        });
                        animator.start();
                        last_progress_play[i] = (int)zegoSoundLevelInfos[i].soundLevel;

                    }
                }
            }

        }

        /**
         * 本地音量采集回调，由于是全局有效，即使停止推流还会回调
         *
         * @param zegoSoundLevelInfo
         */
        @Override
        public void onCaptureSoundLevelUpdate(ZegoSoundLevelInfo zegoSoundLevelInfo) {

            ValueAnimator animator = ValueAnimator.ofInt(last_progress_publish, (int)zegoSoundLevelInfo.soundLevel).setDuration(last_sound_level_monitor_circle);

            animator.addUpdateListener( new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    pb_publish_sound_level.setProgress((int)valueAnimator.getAnimatedValue());
                }
            });
            animator.start();
            last_progress_publish = (int)zegoSoundLevelInfo.soundLevel;

        }


    }

    /**
     * 跳转到设置的 Activity 中
     *
     * @param view
     */
    public void goSetting(View view){

        FrequencySpectrumAndSoundLevelSettingsActivity.actionStart(this);

    }

}
