package com.zego.mediaplayer.ui;

import android.app.AlertDialog;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.zego.common.ZGManager;
import com.zego.common.ui.BaseActivity;
import com.zego.mediaplayer.ZGMediaPlayerDemo;
import com.zego.mediaplayer.ZGMediaPlayerDemoHelper;
import com.zego.mediaplayer.entity.ZGResourcesInfo;

import java.util.ArrayList;
import java.util.List;

import com.zego.mediaplayer.R;
import com.zego.mediaplayer.databinding.ActivityMediaPlayerBinding;

import static com.zego.zegoavkit2.ZegoMediaPlayer.PlayerTypeAux;
import static com.zego.zegoavkit2.ZegoMediaPlayer.PlayerTypePlayer;

/**
 * Created by zego on 2018/10/16.
 */

public class MediaPlayerDemoUI extends BaseActivity implements ZGMediaPlayerDemo.ZGMediaPlayerDemoDelegate {


    private ActivityMediaPlayerBinding binding;
    private volatile boolean repeat = true;
    private ZGResourcesInfo zgResourcesInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_media_player);
        setTitle("MediaPlayer");

        // 设置播放器view
        ZGMediaPlayerDemo.sharedInstance(this).setView(binding.videoView);
        // 获取视频数据
        zgResourcesInfo = (ZGResourcesInfo) getIntent().getSerializableExtra("value");

        // 设置 MediaPlayer 代理
        ZGMediaPlayerDemo.sharedInstance(this).setZGMediaPLayerDelegate(this);
        binding.micButton.setChecked(true);
        // 麦克风监听
        binding.micButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 开启或关闭麦克风
            ZGManager.sharedInstance().api().enableMic(isChecked);
        });

        binding.repeatButton.setChecked(repeat);
        // 麦克风监听
        binding.repeatButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            repeat = isChecked;
        });
        arr_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, data_list);
        //设置样式
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.audioStream.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 切换音轨
                ZGMediaPlayerDemo.sharedInstance(MediaPlayerDemoUI.this).setAudioStream(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        binding.audioVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ZGMediaPlayerDemo.sharedInstance(MediaPlayerDemoUI.this).setVolume(seekBar.getProgress());
            }
        });

        binding.videoProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ZGMediaPlayerDemo.sharedInstance(MediaPlayerDemoUI.this).seekTo(seekBar.getProgress());
            }
        });

        binding.aux.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 开启或关闭aux混音
            ZGMediaPlayerDemo.sharedInstance(MediaPlayerDemoUI.this).setPlayerType(isChecked ? PlayerTypeAux : PlayerTypePlayer);
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZGMediaPlayerDemo .sharedInstance(this).setView(null);
        ZGMediaPlayerDemo.sharedInstance(this).setZGMediaPLayerDelegate(null);
        ZGMediaPlayerDemo.sharedInstance(this).setZgMediaPlayerDemoHelper(null);
        ZGMediaPlayerDemo.sharedInstance(this).unInit();
        binding.aux.setOnCheckedChangeListener(null);
        binding.videoProgress.setOnSeekBarChangeListener(null);
        binding.audioVolume.setOnSeekBarChangeListener(null);
        binding.audioStream.setOnItemSelectedListener(null);
        binding.micButton.setOnCheckedChangeListener(null);
    }

    private String path;

    /**
     * 播放视频点击事件
     *
     * @param view view
     */
    public void playVideo(View view) {
        if (zgResourcesInfo != null) {
            if ("online".equals(zgResourcesInfo.getMediaSourceTypeKey())) {
                path = zgResourcesInfo.getMediaUrlKey();
            } else {
                path = ZGMediaPlayerDemoHelper.sharedInstance().getPath(MediaPlayerDemoUI.this, zgResourcesInfo.getMediaUrlKey() + "." + zgResourcesInfo.getMediaFileTypeKey());
            }
            startPlay(path);
        }
    }

    public void stopVideo(View view) {
        ZGMediaPlayerDemo.sharedInstance(this).stopPlay();
    }

    public void pausePlay(View view) {
        ZGMediaPlayerDemo.sharedInstance(this).pausePlay();
    }

    public void resume(View view) {
        ZGMediaPlayerDemo.sharedInstance(this).resume();
    }

    public void startPlay(String path) {
        // 播放选中的资源
        ZGMediaPlayerDemo.sharedInstance(this).startPlay(path, repeat);
    }


    @Override
    public void onPlayerState(String state) {
        runOnUiThread(() -> binding.mediaPlayerState.setText(state));
    }

    @Override
    public void onPlayerProgress(long current, long max, final String desc) {
        runOnUiThread(() -> {
            binding.videoProgress.setProgress((int) current);
            binding.videoProgress.setMax((int) max);
            binding.progressTxt.setText(desc);
        });
    }

    @Override
    public void onPlayerStop() { }

    @Override
    public void onPublishState(String state) {
        runOnUiThread(() -> binding.url.setText(state));
    }

    @Override
    public void onGetAudioStreamCount(int count) {
        runOnUiThread(() -> {
            if (count > 1) {
                //数据
                arr_adapter.clear();
                for (int i = 0; i < count; i++) {
                    arr_adapter.add(String.valueOf(i));
                }
                arr_adapter.notifyDataSetChanged();
                //加载适配器
                binding.audioStream.setAdapter(arr_adapter);
                binding.audioStreamLayout.setVisibility(View.VISIBLE);
            } else {
                binding.audioStreamLayout.setVisibility(View.GONE);
            }
        });
    }

    private List<String> data_list = new ArrayList<>();
    //适配器
    private ArrayAdapter<String> arr_adapter;

}
