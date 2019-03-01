package com.zego.videocapture.ui;


import android.annotation.TargetApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zego.common.ZGHelper;
import com.zego.common.ZGManager;
import com.zego.videocapture.R;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;

@TargetApi(21)
public class ZGVideoCaptureDemoUI extends AppCompatActivity implements IZegoLivePublisherCallback, IZegoLivePlayerCallback {

    private TextureView mPreView;
    private TextureView mPlayView;
    private TextView mErrorTxt;
    private TextView mNumTxt;
    private Button mDealBtn;
    private Button mDealPlayBtn;

    private String mRoomID = "zgvc_";
    private String mRoomName = "VideoExternalCaptureDemo";
    private String mPlayStreamID = "";

    private boolean isLoginSuccess = false;
    private boolean isScreen = false;

    private int nCur = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zgvideo_capture_demo);

        mPreView = (TextureView)findViewById(R.id.pre_view);
        mPlayView = (TextureView)findViewById(R.id.play_view);
        mErrorTxt = (TextView)findViewById(R.id.error_txt);
        mNumTxt = (TextView)findViewById(R.id.num_txt);
        mDealBtn = (Button)findViewById(R.id.publish_btn);
        mDealPlayBtn = (Button)findViewById(R.id.play_btn);

        String deviceID = ZGHelper.generateDeviceId(this);
        mRoomID += deviceID;
        ZGManager.setLoginUser(deviceID, deviceID);

        isScreen = getIntent().getBooleanExtra("IsScreenCapture", false);


        if (isScreen) {
            new Thread(new ThreadShow()).start();
        }

        // 登录房间
        loginLiveRoom();

        // 开始推流
        doPublish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        logoutLiveRoom();
    }

    public void loginLiveRoom(){
        //设置推流回调监听
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(ZGVideoCaptureDemoUI.this);
        //设置拉流回调监听
        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(ZGVideoCaptureDemoUI.this);

        //加入房间
        boolean ret = ZGManager.sharedInstance().api().loginRoom(mRoomID, mRoomName, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] zegoStreamInfos) {
                if (errorcode == 0) {
                    isLoginSuccess = true;

                } else {
                    mErrorTxt.setText("login room fail, err:" + errorcode);
                }

            }
        });
    }

    public void doPublish(){
        ZGManager.sharedInstance().api().setPreviewView(mPreView);
        ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleToFill);
        // 设置推流分辨率
        ZGManager.sharedInstance().api().setAVConfig(new ZegoAvConfig(ZegoAvConfig.Level.High));
        ZGManager.sharedInstance().api().startPreview();
        boolean ret = ZGManager.sharedInstance().api().startPublishing(mRoomID, mRoomName, ZegoConstants.PublishFlag.JoinPublish);

    }

    public void logoutLiveRoom(){
        ZGManager.sharedInstance().api().logoutRoom();
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(null);
        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(null);
        ZGManager.sharedInstance().unInitSDK();
    }


    public void DealPublishing(View view){
        if (mDealBtn.getText().toString().equals("StopPublish")){

            ZGManager.sharedInstance().api().stopPreview();
            ZGManager.sharedInstance().api().setPreviewView(null);
            ZGManager.sharedInstance().api().stopPublishing();

            runOnUiThread(()->{
                mDealBtn.setText("StartPublish");
            });

        } else {
            doPublish();
        }
    }

    public void DealPlay(View view){
        if (mDealPlayBtn.getText().toString().equals("StartPlay") && !mPlayStreamID.equals("")) {

            // 开始拉流
            boolean ret = ZGManager.sharedInstance().api().startPlayingStream(mPlayStreamID, mPlayView);
            ZGManager.sharedInstance().api().setViewMode(ZegoVideoViewMode.ScaleToFill, mPlayStreamID);
            mErrorTxt.setText("");
            if (!ret){
                runOnUiThread(()->{
                    mErrorTxt.setText("拉流失败");
                });
            }
        } else {
            if (!mPlayStreamID.equals("")){
                //停止拉流
                ZGManager.sharedInstance().api().stopPlayingStream(mPlayStreamID);
                runOnUiThread(()->{
                    mDealPlayBtn.setText("StartPlay");
                });
            }
        }
    }

    // 推流回调
    @Override
    public void onPublishStateUpdate(int stateCode, String streamID, HashMap<String, Object> hashMap) {
        if (stateCode != 0) {
            runOnUiThread(()->{
                mErrorTxt.setText("publish fail, err:"+stateCode);
                mDealBtn.setText("StartPublish");
            });
        } else {
            mDealBtn.setText("StopPublish");
            mPlayStreamID = streamID;
        }
    }

    @Override
    public void onJoinLiveRequest(int i, String s, String s1, String s2) {

    }

    @Override
    public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {

    }

    @Override
    public AuxData onAuxCallback(int i) {
        return null;
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

    // 拉流回调
    @Override
    public void onPlayStateUpdate(int stateCode, String streamID) {
        if (stateCode != 0){
            runOnUiThread(()->{
                mErrorTxt.setText("拉流失败，err："+stateCode);
            });
        } else {
            runOnUiThread(() -> {
                mDealPlayBtn.setText("StopPlay");
            });
        }
    }

    @Override
    public void onPlayQualityUpdate(String s, ZegoPlayStreamQuality zegoPlayStreamQuality) {

    }

    @Override
    public void onInviteJoinLiveRequest(int i, String s, String s1, String s2) {

    }

    @Override
    public void onRecvEndJoinLiveCommand(String s, String s1, String s2) {

    }

    @Override
    public void onVideoSizeChangedTo(String s, int i, int i1) {

    }

    // 界面动画数字线程类
    class ThreadShow implements Runnable {

        @Override
        public void run() {
            // TODO Auto-generated method stub

            while (true) {
                try {
                    Thread.sleep(1000);

                    runOnUiThread(()-> {
                        mNumTxt.setText(String.valueOf(nCur));
                    });

                    nCur++;
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    System.out.println("thread error...");
                }
            }
        }
    }
}
