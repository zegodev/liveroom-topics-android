package com.zego.mixstream.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zego.common.ZGManager;
import com.zego.common.ui.BaseActivity;
import com.zego.mixstream.R;
import com.zego.mixstream.ZGMixStreamDemo;
import com.zego.mixstream.ZGMixStreamDemoHelper;
import com.zego.mixstream.ZGMixStreamPublisher;
import com.zego.zegoavkit2.mixstream.ZegoMixStreamInfo;
import com.zego.zegoliveroom.callback.IZegoEndJoinLiveCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

public class ZGMixAnchorUI extends BaseActivity implements ZGMixStreamPublisher.MixStreamPublisherCallback, ZGMixStreamDemo.MixStreamCallback/*, IZegoMixStreamExCallback*/ {
    private Button mExitBtn;
    private Button mMixBtn;
    private ToggleButton mCameraToggle;

    private TextureView mPreview;
    private TextureView mPlayView;

    private TextView mNetQualityTxt;
    private TextView mErrorTxt;

    private String mRoomID;
    private String joinLiveUserID;
    private int respondJoinLiveResult = -1;
    private boolean alreadyJoinLive = false;

    private String audienceStreamID = "";
    private boolean bePlayingStream = false;
    private String mixStreamID = "";

    private ZegoAvConfig zegoAvConfig = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("");
        setContentView(R.layout.activity_mix_anchor);

        mCameraToggle = (ToggleButton)findViewById(R.id.tb_enable_front_cam);
        mExitBtn = (Button)findViewById(R.id.quit_btn);
        mExitBtn.setEnabled(false);
        mMixBtn = (Button)findViewById(R.id.mix_btn);
        mPreview = (TextureView)findViewById(R.id.preview_view);
        mPlayView = (TextureView)findViewById(R.id.play_view);

        mNetQualityTxt = (TextView)findViewById(R.id.netQuality_txt);
        mErrorTxt = (TextView)findViewById(R.id.error_txt);

        mRoomID = ZGMixStreamDemoHelper.sharedInstance().generateRoomID(this);
        mixStreamID = ZGMixStreamDemo.mixStreamPrefix +mRoomID;

        //控制前后摄像头
        mCameraToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mCameraToggle.setChecked(true);
                    ZGManager.sharedInstance().api().setFrontCam(true);
                } else {
                    mCameraToggle.setChecked(false);
                    ZGManager.sharedInstance().api().setFrontCam(false);
                }
            }
        });

        // join room
        boolean ret = ZGManager.sharedInstance().api().loginRoom(mRoomID, ZGMixStreamPublisher.roomName, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] zegoStreamInfos) {

                if (0 == errorcode) {
                    // 设置推拉流回调监听
                    ZGMixStreamPublisher.sharedInstance().setMixStreamPublisherCallback(ZGMixAnchorUI.this);

                    ZGMixStreamPublisher.sharedInstance().startPublish(mRoomID, ZegoConstants.PublishFlag.MixStream, mPreview);

                    // 设置混流回调
                    ZGMixStreamDemo.sharedInstance().setMixStreamCallback(ZGMixAnchorUI.this);

                } else {
                    mErrorTxt.setText("login room fail, err: "+ errorcode);
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

        ZGMixStreamPublisher.sharedInstance().stopPublish();
        ZGMixStreamPublisher.sharedInstance().unInit();
        ZGMixStreamDemo.sharedInstance().unInit();
    }

    public void EndJoinLive(View view){

        //结束连麦
        ZGManager.sharedInstance().api().endJoinLive(joinLiveUserID, new IZegoEndJoinLiveCallback() {
            @Override
            public void onEndJoinLive(int result, String roomID) {

                if (0 == result) {

                    if (bePlayingStream) {
                        //停止拉流
                        ZGManager.sharedInstance().api().stopPlayingStream(audienceStreamID);
                    }

                    mExitBtn.setEnabled(false);
                    alreadyJoinLive = false;
                    bePlayingStream = false;
                    mPlayView.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public void StartMixStream(View view){

        if (mMixBtn.getText().toString().equals("开始混流")) {
            ZGMixStreamDemo.sharedInstance().startMixStream(mixStreamID);
            runOnUiThread(() -> {
                mMixBtn.setText("停止混流");
            });
        } else {
            ZGMixStreamDemo.sharedInstance().stopMixStream(mixStreamID);
            runOnUiThread(() -> {
                mMixBtn.setText("开始混流");
            });
        }
    }

    @Override
    public void onPublishStateUpdate(int stateCode, String streamID) {

        if (0 !=  stateCode) {
            runOnUiThread(()->{
                mErrorTxt.setText("pulish fail, err: " + stateCode);
            });
        } else {
            ZegoMixStreamInfo mixStreamInfo = new ZegoMixStreamInfo();
            mixStreamInfo.streamID = mRoomID;
//            mixStreamInfo.contentControl = 0; //默认值 音视频都要
            mixStreamInfo.soundLevelID = ZGMixStreamDemo.anchorSoundLevelID; //音量ID
            mixStreamInfo.left = 0;
            mixStreamInfo.top = 0;
            zegoAvConfig = ZGMixStreamPublisher.sharedInstance().getZegoAvConfig();
            if (zegoAvConfig != null){
                mixStreamInfo.right = zegoAvConfig.getVideoCaptureResolutionWidth();
                mixStreamInfo.bottom = zegoAvConfig.getVideoCaptureResolutionHeight();
            } else {
                mixStreamInfo.right = 270;
                mixStreamInfo.bottom = 480;
            }

            ZGMixStreamDemo.sharedInstance().prepareMixStreamInfo(mixStreamInfo);
        }
    }

    @Override
    public void onJoinLiveRequest(int seq, String fromUserID, String roomID) {

        // 此demo只演示与一个观众连麦
        if (!alreadyJoinLive) {
            // 响应观众连麦
            ZGManager.sharedInstance().api().respondJoinLiveReq(seq, 0); // 0-同意连麦
            joinLiveUserID = fromUserID;
            respondJoinLiveResult = 0;
            mExitBtn.setEnabled(true);
            alreadyJoinLive = true;
        }
    }

    @Override
    public void onPublishQualityUpdate(String quality) {

        runOnUiThread(() -> {
            mNetQualityTxt.setText(quality);
        });
    }

    @Override
    public void onDisconnect(int errorCode) {
        runOnUiThread(()->{
            mErrorTxt.setText("disconnect zego server, err:"+errorCode);
        });
    }

    @Override
    public void onStreamUpdated(int type, ZegoStreamInfo[] zegoStreamInfos, String roomID) {
        if (ZegoConstants.StreamUpdateType.Added == type) {

            // 处理流增加
            if (0 == respondJoinLiveResult){

                for (int i=0;i<zegoStreamInfos.length;i++) {
                    if ((zegoStreamInfos[i].userID.equals(joinLiveUserID)) && !zegoStreamInfos[i].streamID.equals("") ) {

                        mPlayView.setVisibility(View.VISIBLE);

                        //拉其中一条流
                        ZGManager.sharedInstance().api().startPlayingStream(zegoStreamInfos[i].streamID, mPlayView);
                        ZGManager.sharedInstance().api().setViewMode(ZegoVideoViewMode.ScaleToFill, zegoStreamInfos[i].streamID);

                        audienceStreamID = zegoStreamInfos[i].streamID;

                        // 输入流布局,此布局在屏幕的左下方
                        ZegoMixStreamInfo mixStreamInfo = new ZegoMixStreamInfo();
                        mixStreamInfo.streamID = audienceStreamID;
//                        mixStreamInfo.contentControl = 0; //音视频都要
                        mixStreamInfo.soundLevelID = ZGMixStreamDemo.audienceSoundLevelID; //音量ID
                        mixStreamInfo.left = 0;
                        mixStreamInfo.right = 270;
                        if (zegoAvConfig != null){
                            mixStreamInfo.top = zegoAvConfig.getVideoCaptureResolutionHeight()-480;
                            mixStreamInfo.bottom = zegoAvConfig.getVideoCaptureResolutionHeight();
                        } else {
                            mixStreamInfo.top = 1280-480;
                            mixStreamInfo.bottom = 1280;
                        }

                        ZGMixStreamDemo.sharedInstance().prepareMixStreamInfo(mixStreamInfo);

                        // 以下接口实现连麦成功就开始混流
//                        ZGMixStreamDemo.sharedInstance().handleMixStreamAdded(zegoStreamInfos, mixStreamID);
                        break;
                    }
                }
            }

        } else if (ZegoConstants.StreamUpdateType.Deleted == type) {

            // 处理流删除
            // 对应于此demo中 只拉一条流，此处只停止拉一条流
            if (bePlayingStream) {
                for (ZegoStreamInfo streamInfo: zegoStreamInfos ) {
                    if (streamInfo.streamID.equals(audienceStreamID)){
                        ZGManager.sharedInstance().api().stopPlayingStream(streamInfo.streamID);
                        alreadyJoinLive = false;
                        bePlayingStream = false;
                        mExitBtn.setEnabled(false);
                        mPlayView.setVisibility(View.INVISIBLE);
                        ZGMixStreamDemo.sharedInstance().handleMixStreamDeleted(zegoStreamInfos, mixStreamID);
                    }
                }
            }
        }
    }

    @Override
    public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String roomID) {

    }

    @Override
    public void onPlayStateUpdate(int stateCode, String streamID) {

        if (0 == stateCode) {
            bePlayingStream = true;

        } else {
            runOnUiThread(()->{
                mErrorTxt.setText("play fail, err: "+stateCode);
            });
        }
    }

    @Override
    public void onPlayQualityUpdate(String quality) {

    }

    @Override
    public void onRecvEndJoinLiveCommand(String fromUserId, String fromUserName, String roomID) {

    }

    @Override
    public void onMixStreamCallback(int errorcode, String mixStreamID) {
        if (errorcode != 0) {
            runOnUiThread(()->{
                mErrorTxt.setText("mix stream fail, err: "+errorcode+", mixStreamID: "+mixStreamID);
            });
        }
    }

    @Override
    public void onSoundLevelInMixStream(long anchorSoundLevel, long audienceSoundLevel) {

    }
}
