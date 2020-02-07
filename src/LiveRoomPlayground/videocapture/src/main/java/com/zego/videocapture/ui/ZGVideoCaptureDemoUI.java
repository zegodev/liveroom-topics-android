package com.zego.videocapture.ui;


import android.annotation.TargetApi;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zego.common.ZGManager;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.DeviceInfoManager;
import com.zego.videocapture.R;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;

/**
 * ZGVideoCaptureDemoUI
 * 主要是处理推拉流并展示渲染视图
 */
@TargetApi(21)
public class ZGVideoCaptureDemoUI extends BaseActivity implements IZegoLivePublisherCallback, IZegoLivePlayerCallback {

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
    // 采集源是否是录屏
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

        // 获取设备唯一ID
        String deviceID = DeviceInfoManager.generateDeviceId(this);
        mRoomID += deviceID;

        // 采集源是否是录屏
        isScreen = getIntent().getBooleanExtra("IsScreenCapture", false);

        if (isScreen) {
            // 采集源为录屏时，启动一个线程在界面上显示动态数字
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

        // 登出房间并释放ZEGO SDK
        logoutLiveRoom();
    }

    // 登录房间并设置推拉流回调监听
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

    // 推流
    public void doPublish(){
        // 设置预览视图及视图展示模式
        ZGManager.sharedInstance().api().setPreviewView(mPreView);
        ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
        // 设置推流分辨率
        ZGManager.sharedInstance().setZegoAvConfig(480,640);
        // 启动预览
        ZGManager.sharedInstance().api().startPreview();
        // 开始推流
        boolean ret = ZGManager.sharedInstance().api().startPublishing(mRoomID, mRoomName, ZegoConstants.PublishFlag.JoinPublish);

    }

    // 登出房间，去除推拉流回调监听并释放ZEGO SDK
    public void logoutLiveRoom(){
        ZGManager.sharedInstance().api().logoutRoom();
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(null);
        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(null);
        ZGManager.sharedInstance().unInitSDK();
    }


    // 处理推流操作
    public void dealPublishing(View view){
        // 界面button==停止推流
        if (mDealBtn.getText().toString().equals("StopPublish")){

            // 停止预览和推流
            ZGManager.sharedInstance().api().stopPreview();
            ZGManager.sharedInstance().api().setPreviewView(null);
            ZGManager.sharedInstance().api().stopPublishing();

            mDealBtn.setText("StartPublish");

        } else {
            // 界面button==开始推流
            doPublish();
        }
    }

    // 处理拉流操作
    public void dealPlay(View view){
        // 界面button==开始拉流
        if (mDealPlayBtn.getText().toString().equals("StartPlay") && !mPlayStreamID.equals("")) {

            // 开始拉流
            boolean ret = ZGManager.sharedInstance().api().startPlayingStream(mPlayStreamID, mPlayView);
            // 设置拉流视图模式，填充整个view
            ZGManager.sharedInstance().api().setViewMode(ZegoVideoViewMode.ScaleAspectFill, mPlayStreamID);
            mErrorTxt.setText("");
            if (!ret) {
                mErrorTxt.setText("拉流失败");
            }
        } else {
            // 界面button==停止拉流
            if (!mPlayStreamID.equals("")){
                //停止拉流
                ZGManager.sharedInstance().api().stopPlayingStream(mPlayStreamID);
                mDealPlayBtn.setText("StartPlay");
            }
        }
    }

    // 推流状态回调
    @Override
    public void onPublishStateUpdate(int stateCode, String streamID, HashMap<String, Object> hashMap) {
        if (stateCode != 0) {
            mErrorTxt.setText("publish fail, err:" + stateCode);
            mDealBtn.setText("StartPublish");
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
    public void onCaptureVideoSizeChangedTo(int i, int i1) {

    }

    @Override
    public void onCaptureVideoFirstFrame() {

    }

    @Override
    public void onCaptureAudioFirstFrame() {
        // 当SDK音频采集设备捕获到第一帧时会回调该方法
    }

    // 拉流状态回调
    @Override
    public void onPlayStateUpdate(int stateCode, String streamID) {
        if (stateCode != 0) {
            mErrorTxt.setText("拉流失败，err：" + stateCode);
        } else {
            mDealPlayBtn.setText("StopPlay");
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

                    // 实现界面上的数字递增
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
