package com.zego.mediaplayer.ui;

import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.zego.common.ZGManager;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.DeviceInfoManager;
import com.zego.mediaplayer.R;
import com.zego.mediaplayer.ZGMediaPlayerDemoHelper;
import com.zego.mediaplayer.ZGMultiPlayerDemo;
import com.zego.zegoavkit2.ZegoMediaPlayer;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;

//import com.zego.zegoliveroom.entity.ZegoStreamQuality;


public class ZGMultiPlayerDemoUI extends BaseActivity implements ZGMultiPlayerDemo.ZGMultiPlayerDemoCallback, IZegoLivePublisherCallback {

    private Button mPlayer1Btn;
    private Button mPlayer2Btn;
    private Button mPlayer3Btn;
    private Button mPublishBtn;
    private TextureView mPreview;
    private CheckBox mPlayer1CheckBox;
    private CheckBox mPlayer2CheckBox;
    private CheckBox mPlayer3CheckBox;
    private TextView mErrorTxt;

    private String mChannelID = "ZEGO_TOPIC_MULTIPLAYER";

    private ZegoMediaPlayer mMediaPlayer1 = null;
    private ZegoMediaPlayer mMediaPlayer2 = null;
    private ZegoMediaPlayer mMediaPlayer3 = null;

    private boolean isLoginRoomSuccess = false;

    private String mp3FilePath = "";
    private String m4aFilePath = "";
    private String aacFilePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);

        mPlayer1CheckBox = (CheckBox) findViewById(R.id.CheckboxPlayer1);
        mPlayer2CheckBox = (CheckBox) findViewById(R.id.CheckboxPlayer2);
        mPlayer3CheckBox = (CheckBox) findViewById(R.id.CheckboxPlayer3);
        mPreview = (TextureView) findViewById(R.id.pre_view);
        mErrorTxt = (TextView) findViewById(R.id.error_txt);

        String deviceID = DeviceInfoManager.generateDeviceId(this);

        mChannelID += "_" + deviceID;

        mPlayer1Btn = (Button) findViewById(R.id.player1_btn);
        mPlayer2Btn = (Button) findViewById(R.id.player2_btn);
        mPlayer3Btn = (Button) findViewById(R.id.player3_btn);
        mPublishBtn = (Button) findViewById(R.id.publish_btn);

        mMediaPlayer1 = ZGMultiPlayerDemo.sharedInstance().createZegoMediaPlayer(ZGMultiPlayerDemo.ZGPlayerIndex.ZGPlayerIndex_First);
        mMediaPlayer2 = ZGMultiPlayerDemo.sharedInstance().createZegoMediaPlayer(ZGMultiPlayerDemo.ZGPlayerIndex.ZGPlayerIndex_Second);
        mMediaPlayer3 = ZGMultiPlayerDemo.sharedInstance().createZegoMediaPlayer(ZGMultiPlayerDemo.ZGPlayerIndex.ZGPlayerIndex_Third);

        // 设置媒体播放器回调监听
        ZGMultiPlayerDemo.sharedInstance().setZGMultiPlayerDemoCallback(ZGMultiPlayerDemoUI.this);

        // join room
        boolean ret = ZGManager.sharedInstance().api().loginRoom(mChannelID, "ZegoMultiPlayer", ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] zegoStreamInfos) {

                if (0 == errorcode) {
                    isLoginRoomSuccess = true;

                    // 设置推流回调监听
                    ZGManager.sharedInstance().api().setZegoLivePublisherCallback(ZGMultiPlayerDemoUI.this);

                    // 设置预览
                    ZGManager.sharedInstance().api().setPreviewView(mPreview);
                    ZGManager.sharedInstance().api().setAVConfig(new ZegoAvConfig(ZegoAvConfig.Level.High));
                    ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                    ZGManager.sharedInstance().api().startPreview();

                } else {
                    mErrorTxt.setText("login room fail, err: " + errorcode);
                }
            }
        });
        if (!ret) {
            mErrorTxt.setText("login room fail(sync) ");
        }

        // 待播放音频文件路径
        mp3FilePath = ZGMediaPlayerDemoHelper.sharedInstance().getPath(this, "first.mp3");
        m4aFilePath = ZGMediaPlayerDemoHelper.sharedInstance().getPath(this, "second.m4a");
        aacFilePath = ZGMediaPlayerDemoHelper.sharedInstance().getPath(this, "third.aac");

        // 控制是否使用混音
        mPlayer1CheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                // 设置是否混音，在需要混音的任一时间设置
                if (checked) {
                    mMediaPlayer1.setPlayerType(ZegoMediaPlayer.PlayerTypeAux);
                } else {
                    mMediaPlayer1.setPlayerType(ZegoMediaPlayer.PlayerTypePlayer);
                }
            }
        });

        mPlayer2CheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                // 设置是否混音
                if (checked) {
                    mMediaPlayer2.setPlayerType(ZegoMediaPlayer.PlayerTypeAux);
                } else {
                    mMediaPlayer2.setPlayerType(ZegoMediaPlayer.PlayerTypePlayer);
                }
            }
        });

        mPlayer3CheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                // 设置是否混音
                if (checked) {
                    mMediaPlayer3.setPlayerType(ZegoMediaPlayer.PlayerTypeAux);
                } else {
                    mMediaPlayer3.setPlayerType(ZegoMediaPlayer.PlayerTypePlayer);
                }
            }
        });
    }

    public void DealPlay1(View view) {
        if (mPlayer1Btn.getText().toString().equals("StartPlay1")) {

            // 开始播放
            if (mMediaPlayer1 != null && !mp3FilePath.equals("")) {
                mMediaPlayer1.start(mp3FilePath, false);
            } else {
                mErrorTxt.setText("audio file path is invalid");
            }
        } else {
            // 停止播放
            mMediaPlayer1.stop();
        }
    }

    public void DealPlay2(View view) {
        if (mPlayer2Btn.getText().toString().equals("StartPlay2")) {

            // 开始播放
            if (mMediaPlayer2 != null && !m4aFilePath.equals("")) {
                mMediaPlayer2.start(m4aFilePath, false);
            } else {
                mErrorTxt.setText("audio file path is invalid");
            }
        } else {
            // 停止播放
            mMediaPlayer2.stop();
        }
    }

    public void DealPlay3(View view) {
        if (mPlayer3Btn.getText().toString().equals("StartPlay3")) {

            // 开始播放
            if (mMediaPlayer3 != null && !aacFilePath.equals("")) {
                mMediaPlayer3.start(aacFilePath, false);
            } else {
                mErrorTxt.setText("audio file path is invalid");
            }
        } else {
            // 停止播放
            mMediaPlayer3.stop();
        }
    }

    public void DealPublish(View view) {
        if (isLoginRoomSuccess) {
            if (mPublishBtn.getText().toString().equals("StartPublish")) {
                // 推流
                ZGManager.sharedInstance().api().setPreviewView(mPreview);
                ZGManager.sharedInstance().api().startPreview();
                boolean ret = ZGManager.sharedInstance().api().startPublishing(mChannelID, "ZegoMultiPlayer", ZegoConstants.PublishFlag.JoinPublish);

                if (!ret) {
                    mErrorTxt.setText("start publish fail(sync)");
                }
            } else {

                ZGManager.sharedInstance().api().stopPreview();
                ZGManager.sharedInstance().api().setPreviewView(null);
                boolean ret_pub = ZGManager.sharedInstance().api().stopPublishing();
                if (ret_pub) {
                    mPublishBtn.setText("StartPublish");
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZGManager.sharedInstance().api().logoutRoom();

        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(null);
        ZGMultiPlayerDemo.sharedInstance().unSetZGMultiPlayerDemoCallback();
        ZGMultiPlayerDemo.sharedInstance().unInit();
        ZGManager.sharedInstance().unInitSDK();
    }

    // 媒体播放器回调
    @Override
    public void onPlayerState(ZGMultiPlayerDemo.ZGPlayerStateType type, int index) {

        if (type == ZGMultiPlayerDemo.ZGPlayerStateType.ZGPlayerStateType_Start) {
            switch (index) {
                case ZegoMediaPlayer.PlayerIndex.First:
                    runOnUiThread(() -> {
                        mPlayer1Btn.setText("StopPlay1");
                    });

                    break;
                case ZegoMediaPlayer.PlayerIndex.Second:
                    runOnUiThread(() -> {
                        mPlayer2Btn.setText("StopPlay2");
                    });

                    break;
                case ZegoMediaPlayer.PlayerIndex.Third:
                    runOnUiThread(() -> {
                        mPlayer3Btn.setText("StopPlay3");
                    });

                    break;
                default:
                    break;
            }

        } else if (type == ZGMultiPlayerDemo.ZGPlayerStateType.ZGPlayerStateType_Stop) {
            switch (index) {
                case ZegoMediaPlayer.PlayerIndex.First:

                    runOnUiThread(() -> {
                        mPlayer1Btn.setText("StartPlay1");
                    });
                    break;
                case ZegoMediaPlayer.PlayerIndex.Second:

                    runOnUiThread(() -> {
                        mPlayer2Btn.setText("StartPlay2");
                    });
                    break;
                case ZegoMediaPlayer.PlayerIndex.Third:

                    runOnUiThread(() -> {
                        mPlayer3Btn.setText("StartPlay3");
                    });
                    break;
                default:
                    break;
            }

        } else if (type == ZGMultiPlayerDemo.ZGPlayerStateType.ZGPlayerStateType_End){
            switch (index) {
                case ZegoMediaPlayer.PlayerIndex.First:

                    runOnUiThread(() -> {
                        mPlayer1Btn.setText("StartPlay1");
                    });
                    break;
                case ZegoMediaPlayer.PlayerIndex.Second:

                    runOnUiThread(() -> {
                        mPlayer2Btn.setText("StartPlay2");
                    });
                    break;
                case ZegoMediaPlayer.PlayerIndex.Third:

                    runOnUiThread(() -> {
                        mPlayer3Btn.setText("StartPlay3");
                    });
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onPlayerError(int errorcode, int index) {
        runOnUiThread(()->{
            mErrorTxt.setText("player "+index+" play err: " + errorcode);
        });
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
}
