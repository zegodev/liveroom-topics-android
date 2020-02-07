package com.zego.mixing.ui;

import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.zego.common.ZGManager;
import com.zego.common.ui.BaseActivity;
import com.zego.mediaplayer.ZGMediaPlayerDemoHelper;
import com.zego.mixing.R;
import com.zego.mixing.ZGMixingDemo;
import com.zego.zegoavkit2.audioaux.IZegoAudioAuxCallbackEx;
import com.zego.zegoavkit2.entities.AuxDataEx;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.io.File;
import java.util.HashMap;

public class ZGMixingDemoUI extends BaseActivity implements IZegoLivePublisherCallback {

    private CheckBox mAuxCheckBox;
    private TextView mAuxTxt;
    private TextView mErrorTxt;
    private TextView mHintTxt;
    private Button mPublishBtn;
    private TextureView mPreview;

    private String mRoomID = "ZEGO_TOPIC_MIXING";
    private boolean isLoginRoomSuccess = false;

    private String mMP3FilePath = "";
    private String mPCMFilePath = "";
    private Thread convertThread = null;

    private String hintStr = "! 推流后请用另一个设备进入“拉流”功能，登录“ZEGO_TOPIC_MIXING”房间，填写“ZEGO_TOPIC_MIXING”流名来查看该流";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zgmixing);

        mAuxTxt = (TextView)findViewById(R.id.aux_txt);
        mAuxCheckBox = (CheckBox)findViewById(R.id.CheckboxAux);
        mPreview = (TextureView) findViewById(R.id.pre_view);
        mErrorTxt = (TextView) findViewById(R.id.error_txt);
        mHintTxt = (TextView)findViewById(R.id.hint);
        mPublishBtn = (Button)findViewById(R.id.publish_btn);

        String dirPath = this.getExternalCacheDir().getPath();
        mPCMFilePath = dirPath + "/mixdemo.pcm";
        mMP3FilePath = ZGMediaPlayerDemoHelper.sharedInstance().getPath(this, "road.mp3");

        // 设置如何查看混音效果的说明
        mHintTxt.setText(hintStr);
        // 获取mp3文件采样率，声道
        ZGMixingDemo.sharedInstance().getMP3FileInfo(mMP3FilePath);

        // 生成pcm数据文件
        File file = new File(mPCMFilePath);
        if (!file.exists()) {
            mAuxTxt.setVisibility(View.INVISIBLE);
            mAuxCheckBox.setVisibility(View.INVISIBLE);
            convertThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    ZGMixingDemo.sharedInstance().MP3ToPCM(mMP3FilePath, mPCMFilePath);
                    runOnUiThread(()->{
                        mAuxTxt.setVisibility(View.VISIBLE);
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
                    ZGMixingDemo.sharedInstance().getZegoAudioAux().enableAux(true);
                } else {
                    ZGMixingDemo.sharedInstance().getZegoAudioAux().enableAux(false);
                }
            }
        });

        // join room
        boolean ret = ZGManager.sharedInstance().api().loginRoom(mRoomID, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] zegoStreamInfos) {

                if (0 == errorcode) {
                    isLoginRoomSuccess = true;

                    // 设置推流回调监听
                    ZGManager.sharedInstance().api().setZegoLivePublisherCallback(ZGMixingDemoUI.this);

                    // 设置预览
                    ZGManager.sharedInstance().api().setPreviewView(mPreview);
                    ZGManager.sharedInstance().api().setAVConfig(new ZegoAvConfig(ZegoAvConfig.Level.High));
                    ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                    ZGManager.sharedInstance().api().startPreview();

                    // 设置混音回调监听
                    ZGMixingDemo.sharedInstance().getZegoAudioAux().setZegoAuxCallbackEx(auxCallbackEx);

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
            ZGMixingDemo.sharedInstance().getZegoAudioAux().setZegoAuxCallbackEx(null);
            ZGManager.sharedInstance().api().setZegoLivePublisherCallback(null);
            ZGManager.sharedInstance().api().logoutRoom();
        }

        ZGManager.sharedInstance().unInitSDK();
    }

    public void dealPublish(View view) {
        if (isLoginRoomSuccess) {
            if (mPublishBtn.getText().toString().equals(getString(R.string.tx_startpublish))) {

                // 设置预览
                ZGManager.sharedInstance().api().setPreviewView(mPreview);
                ZGManager.sharedInstance().api().setAVConfig(new ZegoAvConfig(ZegoAvConfig.Level.High));
                ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                ZGManager.sharedInstance().api().startPreview();

                // 推流
                boolean ret = ZGManager.sharedInstance().api().startPublishing(mRoomID, "ZegoMultiPlayer", ZegoConstants.PublishFlag.JoinPublish);

                if (!ret) {
                    mErrorTxt.setText("start publish fail(sync)");
                } else {
                    mErrorTxt.setText("");
                }
            } else {

                // 停止推流
                ZGManager.sharedInstance().api().stopPreview();
                ZGManager.sharedInstance().api().setPreviewView(null);
                boolean ret_pub = ZGManager.sharedInstance().api().stopPublishing();
                if (ret_pub) {
                    mPublishBtn.setText(getString(R.string.tx_startpublish));
                }
            }
        }
    }

    // 推流回调
    @Override
    public void onPublishStateUpdate(int stateCode, String streamID, HashMap<String, Object> hashMap) {
        if (stateCode == 0) {
            mPublishBtn.setText(getString(R.string.tx_stoppublish));
        } else {
            mErrorTxt.setText("publish fail err: " + stateCode);
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
        // 当SDK音频采集设备捕获到第一帧时会回调该方法
    }

    private IZegoAudioAuxCallbackEx auxCallbackEx = new IZegoAudioAuxCallbackEx() {
        @Override
        public AuxDataEx onAuxCallback(int dataLen) {
            return ZGMixingDemo.sharedInstance().handleAuxCallback(mPCMFilePath,dataLen);
        }
    };
}
