package com.zego.mixstream.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zego.common.ZGManager;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.DeviceInfoManager;
import com.zego.mixstream.R;
import com.zego.mixstream.ZGMixStreamDemo;
import com.zego.mixstream.ZGMixStreamPublisher;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoResponseCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;


public class ZGMixAudienceUI extends BaseActivity implements ZGMixStreamPublisher.MixStreamPublisherCallback,ZGMixStreamDemo.MixStreamCallback {

    private ToggleButton mCameraTog;
    private Button mRequestBtn;
    private Button mExitBtn;
    private Button mPlayAuxBtn;

    private TextureView mPreview;
    private TextureView mPlayview;

    private TextView mQualityTxt;
    private TextView mErrorTxt;
    private TextView mSoundInfoTxt;

    private String mRoomID;

    private String anchorID;
    private String anchorRoomID;
    private String anchorRoomName;

    private boolean bePlayingStream = false;
    private boolean bePlayingMixStream = false;
    private String playStreamID = "";
    private String mixStreamID = "";
    private boolean beLinked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("");
        setContentView(R.layout.activity_mix_audience);

        mCameraTog = (ToggleButton) findViewById(R.id.tb_enable_front_cam);
        mRequestBtn = (Button)findViewById(R.id.conference_btn);
        mRequestBtn.setEnabled(false);
        mExitBtn = (Button)findViewById(R.id.quit_btn);
        mExitBtn.setEnabled(false);
        mPlayAuxBtn = (Button)findViewById(R.id.playAux_btn);
        mPlayAuxBtn.setEnabled(false);
        mPlayview = (TextureView)findViewById(R.id.play_view);
        mPreview = (TextureView)findViewById(R.id.preview_view);
        mQualityTxt = (TextView)findViewById(R.id.quality_txt);
        mErrorTxt = (TextView)findViewById(R.id.error_txt);
        mSoundInfoTxt = (TextView)findViewById(R.id.soundlevel_txt);

        mCameraTog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mCameraTog.setChecked(true);
                    ZGManager.sharedInstance().api().setFrontCam(true);
                } else {
                    mCameraTog.setChecked(false);
                    ZGManager.sharedInstance().api().setFrontCam(false);
                }
            }
        });

        mRoomID = DeviceInfoManager.generateDeviceId(this);
        anchorRoomID = getIntent().getStringExtra("AnchorRoomID");
        anchorRoomName = getIntent().getStringExtra("AnchorRoomName");
        anchorID = getIntent().getStringExtra("AnchorID");

        // 设置混流相关回调
        ZGMixStreamDemo.sharedInstance().setMixStreamCallback(this);

        //加入房间
        boolean ret = ZGManager.sharedInstance().api().loginRoom(anchorRoomID, anchorRoomName, ZegoConstants.RoomRole.Audience, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] zegoStreamInfos) {
                if (0 == errorcode) {
                    //设置推拉流回调监听
                    ZGMixStreamPublisher.sharedInstance().setMixStreamPublisherCallback(ZGMixAudienceUI.this);

                    if (zegoStreamInfos.length > 0) {
                        for (ZegoStreamInfo info : zegoStreamInfos) {
                            if (info.userID.equals(anchorID)) {
                                playStreamID = info.streamID;
                                break;
                            }
                        }

                        String tmp = ZGMixStreamDemo.sharedInstance().getMixStreamID(zegoStreamInfos);
                        if (!tmp.equals("")) {
                            mixStreamID = tmp;
                            if (!beLinked) {
                                mPlayAuxBtn.setEnabled(true);
                            }
                        }

                        //拉流
                        if (!playStreamID.equals("")) {
                            ZGManager.sharedInstance().api().startPlayingStream(playStreamID, mPlayview);
                            ZGManager.sharedInstance().api().setViewMode(ZegoVideoViewMode.ScaleToFill, playStreamID);

                        }
                    } else {
                        mErrorTxt.setText("There are no anchor is publising.");
                    }
                } else {
                    mErrorTxt.setText("login room fail, err: " + errorcode);
                }

            }
        });
        if (!ret) {
            mErrorTxt.setText("login room fail(sync)");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bePlayingStream) {
            ZGManager.sharedInstance().api().stopPlayingStream(playStreamID);
        }

        if (bePlayingMixStream) {
            ZGManager.sharedInstance().api().stopPlayingStream(mixStreamID);
        }

        ZGMixStreamPublisher.sharedInstance().unInit();
    }

    public void RequestBeAnchor(View view) {
        //申请连麦
         ZGManager.sharedInstance().api().requestJoinLive(new IZegoResponseCallback() {
             @Override
             public void onResponse(int result, String fromUserID, String fromUserName) {

                 if (0 == result){
                     mPreview.setVisibility(View.VISIBLE);

                     ZGMixStreamPublisher.sharedInstance().startPublish(mRoomID, ZegoConstants.PublishFlag.JoinPublish, mPreview);
                 }
             }
         });
    }

    public void QuitLiveRoom(View view) {

        ZGMixStreamPublisher.sharedInstance().stopPublish();

        mRequestBtn.setEnabled(true);
        beLinked = false;
        if (!mixStreamID.equals("")){
            mPlayAuxBtn.setEnabled(true);
        }
        mPreview.setVisibility(View.INVISIBLE);
    }

    public void PlayAuxStream(View view) {

        //拉流
        if (mPlayAuxBtn.getText().toString().equals("播放混流")) {
            if (!mixStreamID.equals("")) {
                if (bePlayingStream) {
                    ZGManager.sharedInstance().api().stopPlayingStream(playStreamID);
                    bePlayingStream = false;
                }

                boolean ret = ZGManager.sharedInstance().api().startPlayingStream(mixStreamID, mPlayview);
                ZGManager.sharedInstance().api().setViewMode(ZegoVideoViewMode.ScaleToFill, mixStreamID);
            }

        } else {
            if (!playStreamID.equals("")){

                if (bePlayingMixStream) {
                    ZGManager.sharedInstance().api().stopPlayingStream(mixStreamID);
                    bePlayingMixStream = false;
                }
                ZGManager.sharedInstance().api().startPlayingStream(playStreamID, mPlayview);
                ZGManager.sharedInstance().api().setViewMode(ZegoVideoViewMode.ScaleToFill, playStreamID);
            }
        }
    }

    @Override
    public void onPlayStateUpdate(int stateCode, String streamID) {
        if (0 == stateCode) {

            if (!streamID.equals(playStreamID)) {
                mRequestBtn.setEnabled(false);
                bePlayingMixStream = true;
                runOnUiThread(()->{
                    mPlayAuxBtn.setText("播放主播流");
                });
            } else {
                mRequestBtn.setEnabled(true);
                bePlayingStream = true;
                runOnUiThread(()->{
                    mPlayAuxBtn.setText("播放混流");
                });
            }
        } else {
            runOnUiThread(()->{
                mErrorTxt.setText("play fail,err: "+stateCode);
            });
        }
    }

    @Override
    public void onPlayQualityUpdate(String quality) {

        runOnUiThread(()->{
            mQualityTxt.setText(quality);
        });
    }

    @Override
    public void onRecvEndJoinLiveCommand(String fromUserId, String fromUserName, String roomID) {

        if (roomID.equals(anchorRoomID)) {
            // 停止推流
            ZGManager.sharedInstance().api().stopPreview();
            ZGManager.sharedInstance().api().setPreviewView(null);
            ZGManager.sharedInstance().api().stopPublishing();

            mRequestBtn.setEnabled(true);
            mExitBtn.setEnabled(false);
            beLinked = false;
            if (!mixStreamID.equals("")){
                mPlayAuxBtn.setEnabled(true);
            }
            mPreview.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onPublishStateUpdate(int stateCode, String streamID) {
        if (0 == stateCode) {
            mRequestBtn.setEnabled(false);
            mExitBtn.setEnabled(true);
            beLinked = true;
        } else {
            runOnUiThread(()->{
                mErrorTxt.setText("publish fail,err: " + stateCode);
            });
        }
    }

    @Override
    public void onJoinLiveRequest(int seq, String fromUserID, String roomID) {

    }

    @Override
    public void onPublishQualityUpdate(String quality) {

    }

    @Override
    public void onDisconnect(int errorCode) {

    }

    @Override
    public void onStreamUpdated(int type, ZegoStreamInfo[] zegoStreamInfos, String roomID) {

        if (ZegoConstants.StreamUpdateType.Added == type) {
            // 处理流增加

        } else if (ZegoConstants.StreamUpdateType.Deleted == type) {

            // 对应于此demo中 只拉一条流，此处只停止拉一条流
            if (bePlayingStream) {
                for (int i = 0; i<zegoStreamInfos.length;i++) {
                    if (playStreamID.equals(zegoStreamInfos[i].streamID)) {
                        ZGManager.sharedInstance().api().stopPlayingStream(zegoStreamInfos[i].streamID);
                        bePlayingStream = false;

                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String roomID) {

        mixStreamID = ZGMixStreamDemo.sharedInstance().getMixStreamID(zegoStreamInfos);
        if (!mixStreamID.equals("") && !beLinked){
            mPlayAuxBtn.setEnabled(true);
        }
    }

    @Override
    public void onMixStreamCallback(int errorcode, String mixStreamID) {

    }

    // 音量回调
    @Override
    public void onSoundLevelInMixStream(long anchorSoundLevel, long audienceSoundLevel) {
        runOnUiThread(()->{
            mSoundInfoTxt.setText("主播音量："+String.valueOf(anchorSoundLevel)+", 副主播音量："+String.valueOf(audienceSoundLevel));
        });
    }
}
