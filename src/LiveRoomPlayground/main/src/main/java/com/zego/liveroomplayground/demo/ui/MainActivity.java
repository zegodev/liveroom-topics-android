package com.zego.liveroomplayground.demo.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.TextView;

import com.zego.frequency_spectrum.ui.FrequencySpectrumAndSoundLevelMainActivity;
import com.zego.liveroomplayground.R;
import com.zego.liveroomplayground.databinding.ActivityMainBinding;
import com.zego.liveroomplayground.demo.adapter.MainAdapter;
import com.zego.liveroomplayground.demo.entity.ModuleInfo;
import com.zego.play.ui.InitSDKPlayActivityUI;
import com.zego.common.ui.BaseActivity;
import com.zego.common.ui.WebActivity;
import com.zego.layeredcoding.ui.ZGRoomListUI;
import com.zego.mediaplayer.ui.ZGPlayerTypeUI;
import com.zego.mediarecorder.ZGMediaRecorderSettingUI;
import com.zego.mixing.ui.ZGMixingDemoUI;
import com.zego.mixstream.ui.ZGMixStreamRoomListUI;
import com.zego.sound.processing.ui.SoundProcessMainActivityUI;
import com.zego.mediasideinfo.ui.MediaSideInfoDemoUI;
import com.zego.publish.ui.InitSDKPublishActivityUI;
import com.zego.videocapture.ui.ZGVideoCaptureOriginUI;
import com.zego.videoexternalrender.ui.ZGVideoRenderTypeUI;


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
                    case "音频频谱":
                        FrequencySpectrumAndSoundLevelMainActivity.actionStart(MainActivity.this);
                        break;
                    case "mediaPlayer":
                        intent = new Intent(MainActivity.this, ZGPlayerTypeUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "mediaSideInfo":
                        intent = new Intent(MainActivity.this, MediaSideInfoDemoUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "layeredCoding":
                        intent = new Intent(MainActivity.this, ZGRoomListUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "mediaRecorder":
                        intent = new Intent(MainActivity.this, ZGMediaRecorderSettingUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "mixing":
                        intent = new Intent(MainActivity.this, ZGMixingDemoUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "mixstream":
                        intent = new Intent(MainActivity.this, ZGMixStreamRoomListUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "videoRender":
                        intent = new Intent(MainActivity.this, ZGVideoRenderTypeUI.class);
                        MainActivity.this.startActivity(intent);
                        break;
                    case "videoCapture":
                        intent = new Intent(MainActivity.this, ZGVideoCaptureOriginUI.class);
                        MainActivity.this.startActivity(intent);
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
                .moduleName("拉流").titleName("基础模块"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("推流"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("变声/混响/立体声").titleName("进阶模块"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("音频频谱"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("mediaPlayer"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("mediaSideInfo"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("layeredCoding"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("mediaRecorder"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("mixing"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("mixstream"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("videoRender"));
        mainAdapter.addModuleInfo(new ModuleInfo()
                .moduleName("videoCapture"));

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
}
