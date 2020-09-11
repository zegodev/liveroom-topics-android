package com.zego.mediarecorder;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.mediarecorder.databinding.ActivityReplayBinding;
import com.zego.zegoavkit2.IZegoMediaPlayerAudioPlayCallback;
import com.zego.zegoavkit2.IZegoMediaPlayerWithIndexCallback;
import com.zego.zegoavkit2.ZegoMediaPlayer;
import com.zego.zegoavkit2.ZegoVideoDataFormat;

public class ZGMediaRecorderReplayUI extends BaseActivity {

    private ActivityReplayBinding binding;


    /* 媒体播放器 */
    private ZegoMediaPlayer zegoMediaPlayer = null;

    private String mFilePath = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_replay);

        // 创建播放器对象
        zegoMediaPlayer = new ZegoMediaPlayer();
        // 初始化播放器
        zegoMediaPlayer.init(ZegoMediaPlayer.PlayerTypePlayer);
        // 设置播放器回调监听
        setPlayerCallback();

        // 录制文件存储路径
        mFilePath = getIntent().getStringExtra("filePath");

        binding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void finish() {
        super.finish();

        // 去除播放器回调监听
        zegoMediaPlayer.setVideoPlayWithIndexCallback(null, ZegoVideoDataFormat.PIXEL_FORMAT_NV21);
        // 释放播放器
        zegoMediaPlayer.uninit();
        zegoMediaPlayer = null;
    }

    public void dealReplay(View view) {
        if (binding.replayBtn.getText().toString().equals(getString(R.string.tx_begin_play))) {
            if (zegoMediaPlayer != null) {
                zegoMediaPlayer.setView(binding.playView);
            }

            if (!mFilePath.equals("")) {
                zegoMediaPlayer.setVolume(100);
                zegoMediaPlayer.start(mFilePath, false);
            }
        } else {
            if (zegoMediaPlayer != null) {
                zegoMediaPlayer.setView(null);
                zegoMediaPlayer.stop();
            }
        }
    }

    /**
     * 供其他Activity调用，进入本专题的方法
     *
     * @param activity
     * @param filePath 录制文件路径
     */
    public static void actionStart(Activity activity, String filePath) {
        Intent intent = new Intent(activity, ZGMediaRecorderReplayUI.class);
        intent.putExtra("filePath", filePath);
        activity.startActivity(intent);
    }

    // 设置播放器回调监听
    public void setPlayerCallback() {
        zegoMediaPlayer.setEventWithIndexCallback(new IZegoMediaPlayerWithIndexCallback() {
            @Override
            public void onPlayStart(int i) {
                binding.replayBtn.setText(getString(R.string.tx_end_play));
            }

            @Override
            public void onPlayPause(int i) {
                binding.replayBtn.setText(getString(R.string.tx_begin_play));
            }

            @Override
            public void onPlayStop(int i) {
                binding.replayBtn.setText(getString(R.string.tx_begin_play));
            }

            @Override
            public void onPlayResume(int i) {
                binding.replayBtn.setText(getString(R.string.tx_end_play));
            }

            @Override
            public void onPlayError(int error,int i) {
                AppLogger.getInstance().e(ZGMediaRecorderReplayUI.class, "回放出错，err: %d", error);

            }

            @Override
            public void onVideoBegin(int i) {

            }

            @Override
            public void onAudioBegin(int i) {

            }

            @Override
            public void onPlayEnd(int i) {
                binding.replayBtn.setText(getString(R.string.tx_begin_play));
            }

            @Override
            public void onBufferBegin(int i) {

            }

            @Override
            public void onBufferEnd(int i) {

            }

            @Override
            public void onSeekComplete(int i, long l,int index) {

            }

            @Override
            public void onSnapshot(Bitmap bitmap,int index) {

            }

            @Override
            public void onLoadComplete(int index) {

            }

            @Override
            public void onProcessInterval(long l,int index) {

            }

            @Override
            public void onReadEOF(int i) {

            }
        });
    }
}
