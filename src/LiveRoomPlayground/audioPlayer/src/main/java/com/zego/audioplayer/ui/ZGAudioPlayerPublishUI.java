package com.zego.audioplayer.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.zego.audioplayer.R;
import com.zego.audioplayer.ZGAudioPlayerHelper;
import com.zego.audioplayer.adapter.EffectListAdapter;
import com.zego.audioplayer.databinding.ActivityZgaudioPlayerPublishBinding;
import com.zego.audioplayer.entity.EffectInfo;
import com.zego.common.ZGBaseHelper;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.zegoavkit2.audioplayer.IZegoAudioPlayerCallback;
import com.zego.zegoavkit2.audioplayer.ZegoAudioPlayer;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ZGAudioPlayerPublishUI extends BaseActivity {

    private ActivityZgaudioPlayerPublishBinding binding;

    private EffectListAdapter effectListAdapter = new EffectListAdapter();

    private List<EffectInfo> mEffectInfos = new ArrayList<>(8);

    private String[] effectNameArr;
    private String[] effectFilePathArr = new String[8];

    private String mRoomID;
    private String mStreamID;

    private ZegoAudioPlayer mZegoAudioPlayer = null;

    // 是否混音音效
    private boolean useAux = true;
    private boolean isPublishing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_zgaudio_player_publish);

        binding.effectList.setLayoutManager(new GridLayoutManager(this, 4));
        binding.effectList.setAdapter(effectListAdapter);
        // 设置Item添加和移除的动画
        binding.effectList.setItemAnimator(new DefaultItemAnimator());

        // 返回键处理
        binding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 暂停、恢复、停止事件监听
        binding.resumeAllEffect.setOnClickListener(clickListener);
        binding.pauseAllEffect.setOnClickListener(clickListener);
        binding.stopAllEffect.setOnClickListener(clickListener);

        // 音量进度条监听
        binding.soundlevelStep.setMax(100);
        binding.soundlevelStep.setOnSeekBarChangeListener(seekBarChangeListener);

        mRoomID = getIntent().getStringExtra("roomID");
        mStreamID = getIntent().getStringExtra("streamID");

        binding.roomIDTx.setText(getString(R.string.tx_show_roomID, mRoomID));
        binding.streamIDTx.setText(getString(R.string.tx_show_streamID, mStreamID));

        // 登录房间并推流
        loginAndPublish();

        // 获取音效播放器实例
        mZegoAudioPlayer = ZGAudioPlayerHelper.sharedInstance().getZegoAudioPlayer();
        // 设置音效播放器回调监听
        mZegoAudioPlayer.setCallback(audioPlayerCallback);

        // 获取音效名称
        effectNameArr = getResources().getStringArray(R.array.effect_describe);
        for (int i=0; i<effectNameArr.length; i++) {
            // 获取音效文件路径
            effectFilePathArr[i] = ZGAudioPlayerHelper.sharedInstance().getPath(this, effectNameArr[i]+".mp3");
            // 预加载音效，多次播放下可提升性能
            if (i > 0) {
                mZegoAudioPlayer.preloadEffect(effectFilePathArr[i], i);
            }
        }

        // 混音按钮控制
        binding.auxSw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                useAux = !useAux;
                if (useAux) {
                    binding.auxSw.setImageResource(R.mipmap.ic_sw_open);
                } else {
                    binding.auxSw.setImageResource(R.mipmap.ic_sw_close);
                }
            }
        });

        // 添加音效
        effectListAdapter.addEffectInfos(getEffectInfos());

        effectListAdapter.setOnItemClickListener(new EffectListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, EffectInfo effectInfo) {
                if (position == 0) {
                    // 该音效时长超过30s，不能使用预加载
                    mZegoAudioPlayer.playEffect(effectFilePathArr[0], position, 0, useAux);
                } else {
                    // 播放音效，因为是播放预加载的音效，此处的音效文件路径填空
                    mZegoAudioPlayer.playEffect(null, position, 0, useAux);
                }

            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        mEffectInfos.clear();
        mZegoAudioPlayer.stopAll();
        mZegoAudioPlayer.setCallback(null);
        // 删除欲加载音效
        for (int i=0; i<effectFilePathArr.length; i++) {
            mZegoAudioPlayer.unloadEffect(i);
        }

        // 释放音效播放器
        ZGAudioPlayerHelper.sharedInstance().destoryAudioPlayer();

        ZGBaseHelper.sharedInstance().getZegoLiveRoom().setZegoLivePublisherCallback(null);
        if (isPublishing) {
            ZGBaseHelper.sharedInstance().getZegoLiveRoom().stopPublishing();
        }
        // 登出房间
        ZGBaseHelper.sharedInstance().loginOutRoom();
    }

    // 获取所有音效
    public List<EffectInfo> getEffectInfos() {

        if (mEffectInfos.size() <= 0) {
            for (int i=0; i<effectNameArr.length; i++) {
                EffectInfo effectInfo = new EffectInfo(effectNameArr[i], effectFilePathArr[i]);
                mEffectInfos.add(effectInfo);
            }
        }

        return mEffectInfos;
    }

    public static void actionStart(Activity activity, String roomID, String streamID) {
        Intent intent = new Intent(activity, ZGAudioPlayerPublishUI.class);
        intent.putExtra("roomID", roomID);
        intent.putExtra("streamID", streamID);
        activity.startActivity(intent);
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == binding.resumeAllEffect.getId()) {
                // 恢复所有暂停的音效
                mZegoAudioPlayer.resumeAll();
            } else if (v.getId() == binding.pauseAllEffect.getId()) {
                // 暂停所有正在播放的音效
                mZegoAudioPlayer.pauseAll();
            } else {
                // 停止播放所有的音效
                mZegoAudioPlayer.stopAll();
            }
        }
    };

    // 音量进度条响应
    final SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // 设置音量
            mZegoAudioPlayer.setVolumeAll(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void loginAndPublish() {
        // 设置推流回调监听
        ZGBaseHelper.sharedInstance().getZegoLiveRoom().setZegoLivePublisherCallback(publisherCallback);
        ZGBaseHelper.sharedInstance().getZegoLiveRoom().loginRoom(mRoomID, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int err, ZegoStreamInfo[] zegoStreamInfos) {

                if (err == 0) {
                    // 推流，只推音频流
                    ZGBaseHelper.sharedInstance().getZegoLiveRoom().enableCamera(false);
                    ZGBaseHelper.sharedInstance().getZegoLiveRoom().startPublishing(mStreamID, "", ZegoConstants.PublishFlag.JoinPublish);
                } else {
                    Toast.makeText(ZGAudioPlayerPublishUI.this, getString(R.string.tx_login_fail_hint, mRoomID, err)
                    , Toast.LENGTH_SHORT).show();
                    AppLogger.getInstance().e(ZGAudioPlayerPublishUI.class, getString(R.string.tx_login_fail_hint), mRoomID, err);
                }
            }
        });
    }

    // 音效播放器回调
    private IZegoAudioPlayerCallback audioPlayerCallback = new IZegoAudioPlayerCallback() {
        @Override
        public void onPlayEffect(int soundID, int error) {
            // 开始播放音效结果回调
            if (error != 0) {
                AppLogger.getInstance().e(ZGAudioPlayerPublishUI.class, String.format("onPlayEffect err:%d,soundID:%d", error, soundID));
            }
        }

        @Override
        public void onPlayEnd(int soundID) {
            // 播放音效完成回调

        }

        @Override
        public void onPreloadEffect(int soundID, int error) {
            // 预加载音效结果回调

        }

        @Override
        public void onPreloadComplete(int soundID) {
            // 预加载音效结果回调

        }
    };

    // 推流回调监听
    private IZegoLivePublisherCallback publisherCallback = new IZegoLivePublisherCallback() {
        @Override
        public void onPublishStateUpdate(int err, String streamID, HashMap<String, Object> hashMap) {
            if (err != 0) {
                Toast.makeText(ZGAudioPlayerPublishUI.this, getString(R.string.tx_publish_fail_hint, streamID, err)
                        , Toast.LENGTH_SHORT).show();

                AppLogger.getInstance().e(ZGAudioPlayerPublishUI.class, getString(R.string.tx_publish_fail_hint, streamID, err));

            } else {
                isPublishing = true;
            }

        }

        @Override
        public void onJoinLiveRequest(int i, String s, String s1, String s2) {

        }

        @Override
        public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {

        }

        @Override
        public void onCaptureVideoSizeChangedTo(int i, int i1) {

        }

        @Override
        public void onCaptureVideoFirstFrame() {

        }

        @Override
        public void onCaptureAudioFirstFrame() {

        }
    };
}
