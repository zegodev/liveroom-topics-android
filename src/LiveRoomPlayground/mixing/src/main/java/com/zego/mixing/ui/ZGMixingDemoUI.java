package com.zego.mixing.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.zego.common.ZGHelper;
import com.zego.common.ZGManager;
import com.zego.mediaplayer.ZGMediaPlayerDemoHelper;
import com.zego.mixing.R;
import com.zego.mixing.ZGMixingDemo;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;
//import com.zego.zegoliveroom.entity.ZegoStreamQuality;

import java.io.File;
import java.util.HashMap;

public class ZGMixingDemoUI extends AppCompatActivity implements IZegoLivePublisherCallback {

    private CheckBox mAuxCheckBox;
    private TextView mErrorTxt;
    private Button mPublishBtn;
    private TextureView mPreview;

    private String mChannelID = "ZEGO_TOPIC_MIXING";
    private boolean isLoginRoomSuccess = false;

    private String mMP3FilePath = "";
    private String mPCMFilePath = "";
    private Thread convertThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zgmixing);

        mAuxCheckBox = (CheckBox)findViewById(R.id.CheckboxAux);
        mPreview = (TextureView) findViewById(R.id.pre_view);
        mErrorTxt = (TextView) findViewById(R.id.error_txt);
        mPublishBtn = (Button)findViewById(R.id.publish_btn);

        String dirPath = this.getExternalCacheDir().getPath();
        mPCMFilePath = dirPath + "/mixdemo.pcm";
        mMP3FilePath = ZGMediaPlayerDemoHelper.sharedInstance().getPath(this, "road.mp3");

        // 获取mp3文件采样率，声道
        ZGMixingDemo.sharedInstance().getMP3FileInfo(mMP3FilePath);

        // 生成pcm数据文件
        File file = new File(mPCMFilePath);
        if (!file.exists()) {
            mAuxCheckBox.setVisibility(View.INVISIBLE);
            convertThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    ZGMixingDemo.sharedInstance().MP3ToPCM(mMP3FilePath, mPCMFilePath);
                    runOnUiThread(()->{
                        mAuxCheckBox.setVisibility(View.VISIBLE);
                    });
                }
            });
            convertThread.start();
        }

        mAuxCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                // 是否启用混音
                if (checked) {
                    ZGManager.sharedInstance().api().enableAux(true);
                } else {
                    ZGManager.sharedInstance().api().enableAux(false);
                }
            }
        });

        String deviceID = ZGHelper.generateDeviceId(this);
        ZGManager.setLoginUser(deviceID, deviceID);

        // join room
        boolean ret = ZGManager.sharedInstance().api().loginRoom(mChannelID, "ZegoMixing", ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] zegoStreamInfos) {

                if (0 == errorcode) {
                    isLoginRoomSuccess = true;

                    // 设置推流回调监听
                    ZGManager.sharedInstance().api().setZegoLivePublisherCallback(ZGMixingDemoUI.this);

                    // 设置预览
                    ZGManager.sharedInstance().api().setPreviewView(mPreview);
                    ZGManager.sharedInstance().api().setAVConfig(new ZegoAvConfig(ZegoAvConfig.Level.High));
                    ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleToFill);
                    ZGManager.sharedInstance().api().startPreview();

                } else {
                    mErrorTxt.setText("login room fail, err: " + errorcode);
                }
            }
        });
        if (!ret) {
            mErrorTxt.setText("login room fail(sync) ");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        convertThread = null;

        if (isLoginRoomSuccess) {
            ZGManager.sharedInstance().api().logoutRoom();
            ZGManager.sharedInstance().api().setZegoLivePublisherCallback(null);
        }

        ZGManager.sharedInstance().unInitSDK();
    }

    public void DealPublish(View view) {
        if (isLoginRoomSuccess) {
            if (mPublishBtn.getText().toString().equals("StartPublish")) {

                // 设置预览
                ZGManager.sharedInstance().api().setPreviewView(mPreview);
                ZGManager.sharedInstance().api().setAVConfig(new ZegoAvConfig(ZegoAvConfig.Level.High));
                ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleToFill);
                ZGManager.sharedInstance().api().startPreview();

                // 推流
                boolean ret = ZGManager.sharedInstance().api().startPublishing(mChannelID, "ZegoMultiPlayer", ZegoConstants.PublishFlag.JoinPublish);

                if (!ret) {
                    mErrorTxt.setText("start publish fail(sync)");
                }
            } else {

                // 停止推流
                ZGManager.sharedInstance().api().stopPreview();
                ZGManager.sharedInstance().api().setPreviewView(null);
                boolean ret_pub = ZGManager.sharedInstance().api().stopPublishing();
                if (ret_pub) {
                    mPublishBtn.setText("StartPublish");
                }
            }
        }
    }

    // 推流回调
    @Override
    public void onPublishStateUpdate(int stateCode, String streamID, HashMap<String, Object> hashMap) {
        if (stateCode == 0) {
            runOnUiThread(()->{
                mPublishBtn.setText("StopPublish");
            });
        } else {
            runOnUiThread(()->{
                mErrorTxt.setText("publish fail err: " + stateCode);
            });
        }
    }

    @Override
    public void onJoinLiveRequest(int i, String s, String s1, String s2) {

    }

    @Override
    public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {

    }

//    @Override
//    public void onPublishQualityUpdate(String s, ZegoStreamQuality zegoStreamQuality) {
//
//    }

    // 混音回调
    @Override
    public AuxData onAuxCallback(int dataLen) {
        return ZGMixingDemo.sharedInstance().handleAuxCallback(mPCMFilePath,dataLen);
    }

    @Override
    public void onCaptureVideoSizeChangedTo(int i, int i1) {

    }

    @Override
    public void onMixStreamConfigUpdate(int i, String s, HashMap<String, Object> hashMap) {

    }

    @Override
    public void onCaptureVideoFirstFrame() {

    }
}
