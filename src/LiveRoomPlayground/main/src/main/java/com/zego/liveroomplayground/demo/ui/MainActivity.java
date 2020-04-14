package com.zego.liveroomplayground.demo.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.TextView;

import com.zego.audioplayer.ui.AudioPlayerMainUI;
import com.zego.frequency_spectrum.ui.FrequencySpectrumAndSoundLevelMainActivity;
import com.zego.joinlive.ui.JoinLiveMainActivityUI;
import com.zego.liveroomplayground.R;
import com.zego.liveroomplayground.databinding.ActivityMainBinding;
import com.zego.liveroomplayground.demo.adapter.MainAdapter;
import com.zego.liveroomplayground.demo.entity.ModuleInfo;
import com.zego.mediarecorder.ZGMediaRecorderRecordUI;
import com.zego.play.ui.InitSDKPlayActivityUI;
import com.zego.common.ui.BaseActivity;
import com.zego.common.ui.WebActivity;
import com.zego.layeredcoding.ui.ZGRoomListUI;
import com.zego.mediaplayer.ui.ZGPlayerTypeUI;
import com.zego.mixing.ui.ZGMixingDemoUI;
import com.zego.mixstream.ui.ZGMixStreamRoomListUI;
import com.zego.sound.processing.ui.SoundProcessMainActivityUI;
import com.zego.mediasideinfo.ui.MediaSideInfoDemoUI;
import com.zego.publish.ui.InitSDKPublishActivityUI;
import com.zego.videocapture.ui.ZGVideoCaptureOriginUI;
import com.zego.videocommunication.ui.VideoCommunicationMainUI;
import com.zego.videoexternalrender.ui.ZGVideoRenderTypeUI;
import com.zego.videofilter.ui.VideoFilterMainUI;


public class MainActivity extends BaseActivity {


    private MainAdapter mainAdapter = new MainAdapter();
    private static final int REQUEST_PERMISSION_CODE = 101;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 判断当前页面是否处于任务栈的根Activity，如果不是根Activity 则无需打开
        // 避免在浏览器中打开应用时会启动2个首页
        if (!isTaskRoot()) {
            /* If this is not the root activity */
            Intent intent = getIntent();
            String action = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                finish();
                return;
            }
        }

        setTitle("ZegoDemo");
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.setting.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                SettingActivity.actionStart(MainActivity.this);
            }
        });


        mainAdapter.setOnItemClickListener((view, position) -> {
            boolean orRequestPermission = checkOrRequestPermission(REQUEST_PERMISSION_CODE);
            ModuleInfo moduleInfo = (ModuleInfo) view.getTag();
            if (orRequestPermission) {
                Intent intent;
                switch (moduleInfo.getModule()) {
                    case "推流":
                        InitSDKPublishActivityUI.actionStart(MainActivity.this);
                        break;
                    case "拉流":
                        InitSDKPlayActivityUI.actionStart(MainActivity.this);
                        break;
                    case "变声/混响/立体声":
                        SoundProcessMainActivityUI.actionStart(MainActivity.this);
                        break;
                    case "声浪/音频频谱":
                        FrequencySpectrumAndSoundLevelMainActivity.actionStart(MainActivity.this);
                        break;
                    case "媒体播放器":
                        intent = new Intent(MainActivity.this, ZGPlayerTypeUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "音效播放器":
                        AudioPlayerMainUI.actionStart(MainActivity.this);
                        break;
                    case "媒体次要信息":
                        intent = new Intent(MainActivity.this, MediaSideInfoDemoUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "分层编码":
                        intent = new Intent(MainActivity.this, ZGRoomListUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "本地媒体录制":
                        intent = new Intent(MainActivity.this, ZGMediaRecorderRecordUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "混音":
                        intent = new Intent(MainActivity.this, ZGMixingDemoUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "多路混流":
                        intent = new Intent(MainActivity.this, ZGMixStreamRoomListUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "视频外部渲染":
                        intent = new Intent(MainActivity.this, ZGVideoRenderTypeUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "自定义采集":
                        intent = new Intent(MainActivity.this, ZGVideoCaptureOriginUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "多人视频通话":
                        intent = new Intent(MainActivity.this, VideoCommunicationMainUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "直播连麦":
                        JoinLiveMainActivityUI.actionStart(MainActivity.this);
                        break;
                    case "自定义前处理-Face Unity":
                        VideoFilterMainUI.actionStart(MainActivity.this);
                        break;
                    case "webRTC":
                        jumpWebRtc();
                        break;

                }
            }
        });

        binding.moduleList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        // 设置adapter
        binding.moduleList.setAdapter(mainAdapter);
        // 设置Item添加和移除的动画
        binding.moduleList.setItemAnimator(new DefaultItemAnimator());

        // 添加模块
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("推流").titleName("快速开始"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("拉流"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("多人视频通话").titleName("常用功能"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("直播连麦"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("多路混流"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("媒体次要信息").titleName("进阶功能"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("媒体播放器"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("音效播放器"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("本地媒体录制"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("分层编码"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("自定义前处理-Face Unity"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("自定义采集"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("混音"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("变声/混响/立体声"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("声浪/音频频谱"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("视频外部渲染"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("webRTC"));

    }


    public void jumpSourceCodeDownload(View view) {
        WebActivity.actionStart(this, "https://github.com/zegodev/liveroom-topics-android", ((TextView) view).getText().toString());
    }

    public void jumpCommonProblems(View view) {
        WebActivity.actionStart(this, "https://doc.zego.im/CN/496.html", ((TextView) view).getText().toString());
    }

    public void jumpDoc(View view) {
        WebActivity.actionStart(this, " https://doc.zego.im/CN/303.html", ((TextView) view).getText().toString());
    }

    public void jumpWebRtc() {
        WebActivity.actionStart(this, "https://bansheehannibal.github.io/webrtcDemo/", "WebRtc");
    }
}
