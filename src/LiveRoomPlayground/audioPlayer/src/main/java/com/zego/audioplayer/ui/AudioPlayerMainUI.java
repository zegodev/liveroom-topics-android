package com.zego.audioplayer.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.zego.audioplayer.R;
import com.zego.audioplayer.databinding.ActivityAudioPlayerMainBinding;
import com.zego.common.ZGBaseHelper;
import com.zego.common.ui.BaseActivity;
import com.zego.common.ui.WebActivity;
import com.zego.common.util.AppLogger;
import com.zego.common.util.ZegoUtil;
import com.zego.zegoliveroom.callback.IZegoInitSDKCompletionCallback;

public class AudioPlayerMainUI extends BaseActivity {

    private ActivityAudioPlayerMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_audio_player_main);
        binding.publishBtn.setClickable(false);
        binding.playBtn.setClickable(false);

        // 返回键处理
        binding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //初始化 SDK
        ZGBaseHelper.sharedInstance().initZegoSDK(ZegoUtil.getAppID(), ZegoUtil.getAppSign(), ZegoUtil.getIsTestEnv(), new IZegoInitSDKCompletionCallback() {
            @Override
            public void onInitSDK(int err) {
                if (err == 0) {
                    binding.publishBtn.setClickable(true);
                    binding.playBtn.setClickable(true);
                } else {
                    Toast.makeText(AudioPlayerMainUI.this, getString(R.string.tx_init_failure), Toast.LENGTH_SHORT).show();
                    AppLogger.getInstance().e(AudioPlayerMainUI.class, String.format(getString(R.string.tx_init_failure)+",err:%d",err));
                }
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        // 释放SDK
        ZGBaseHelper.sharedInstance().unInitZegoSDK();
    }

    public void onStartPublish(View view) {
        String roomID = binding.roomIDed.getText().toString();
        String streamID = binding.streamIDed.getText().toString();

        if (roomID.length() > 0 && streamID.length() > 0) {
            ZGAudioPlayerPublishUI.actionStart(this, roomID, streamID);
        } else {
            Toast.makeText(this, getString(R.string.tx_id_not_null), Toast.LENGTH_SHORT).show();
        }
    }

    public void onStartPlay(View view) {
        String roomID = binding.roomIDed.getText().toString();
        String streamID = binding.streamIDed.getText().toString();

        if (roomID.length() > 0 && streamID.length() > 0) {
            ZGAudioPlayerPlayUI.actionStart(this, roomID, streamID);
        } else {
            Toast.makeText(this, getString(R.string.tx_id_not_null), Toast.LENGTH_SHORT).show();
        }
    }

    public void browseDoc(View view) {
        WebActivity.actionStart(this, "https://doc.zego.im/CN/1233.html", getString(R.string.tx_audioplayer_guide));
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, AudioPlayerMainUI.class);
        activity.startActivity(intent);
    }
}
