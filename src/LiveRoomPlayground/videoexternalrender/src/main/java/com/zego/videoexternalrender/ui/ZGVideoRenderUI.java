package com.zego.videoexternalrender.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zego.common.ZGManager;
import com.zego.common.util.DeviceInfoManager;
import com.zego.videoexternalrender.R;
import com.zego.videoexternalrender.videorender.VideoRenderer;
import com.zego.zegoavkit2.videorender.ZegoExternalVideoRender;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;

public class ZGVideoRenderUI extends AppCompatActivity implements IZegoLivePublisherCallback, IZegoLivePlayerCallback {
    private TextureView mPreView;
    private TextureView mPlayView;
    private TextView mErrorTxt;
    private Button mDealBtn;
    private Button mDealPlayBtn;

    private String mRoomID = "zgver_";
    private String mRoomName = "VideoExternalRenderDemo";
    private String mPlayStreamID = "";

    // 渲染类
    private VideoRenderer videoRenderer;
    private int chooseRenderType;

    private boolean isSetDecodeCallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zgvideo_render);

        mPreView = (TextureView)findViewById(R.id.pre_view);
        mPlayView = (TextureView)findViewById(R.id.play_view);
        mErrorTxt = (TextView)findViewById(R.id.error_txt);
        mDealBtn = (Button)findViewById(R.id.publish_btn);
        mDealPlayBtn = (Button)findViewById(R.id.play_btn);

        // 获取已选的渲染类型
        chooseRenderType = getIntent().getIntExtra("RenderType", 0);
        isSetDecodeCallback = getIntent().getBooleanExtra("IsUseNotDecode", false);
        Log.e("test","****** chooseRenderType: " + chooseRenderType + ", isSetDecodeCallback: " + isSetDecodeCallback);

        // 获取设备唯一ID
        String deviceID = DeviceInfoManager.generateDeviceId(this);
        mRoomID += deviceID;

        videoRenderer = new VideoRenderer();

        videoRenderer.init();

        // 设置外部渲染回调监听
        ZegoExternalVideoRender.setVideoRenderCallback(videoRenderer);
        // 设置码流类型的回调监听
        if (isSetDecodeCallback) {
            ZegoExternalVideoRender.setVideoDecodeCallback(videoRenderer);
        }

        // 设置推流回调监听
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(this);
        // 设置拉流回调监听
        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(this);

        // 登录房间
        boolean ret = ZGManager.sharedInstance().api().loginRoom(mRoomID, mRoomName, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] zegoStreamInfos) {
                // 登录成功后设置预览视图，开启预览并推流
                if (errorcode == 0) {

                    // 添加外部渲染视图
                    videoRenderer.addView(com.zego.zegoavkit2.ZegoConstants.ZegoVideoDataMainPublishingStream, mPreView);

                    ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                    ZGManager.sharedInstance().api().enableCamera(true);
                    // 设置推流分辨率，540*960
                    ZGManager.sharedInstance().api().setAVConfig(new ZegoAvConfig(ZegoAvConfig.Level.High));
                    ZGManager.sharedInstance().api().startPreview();
                    ZGManager.sharedInstance().api().startPublishing(mRoomID, mRoomName, ZegoConstants.PublishFlag.JoinPublish);

                } else {
                    mErrorTxt.setText("login room fail, err:"+errorcode);
                }
            }
        });

        if (!ret){
            mErrorTxt.setText("login room fail(sync)");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ZegoExternalVideoRender.setVideoRenderCallback(null);
        if (isSetDecodeCallback) {
            ZegoExternalVideoRender.setVideoDecodeCallback(null);
        }

        // 释放渲染类
        videoRenderer.uninit();

        // 登出房间，去除推拉流回调监听，释放 ZEGO SDK
        ZGManager.sharedInstance().api().logoutRoom();
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(null);
        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(null);
        ZGManager.sharedInstance().unInitSDK();
    }

    // 处理推流相关操作
    public void DealPublishing(View view){
        // 界面button==停止推流
        if (mDealBtn.getText().toString().equals("StopPublish")){
            //停止预览，停止推流
            ZGManager.sharedInstance().api().stopPreview();
            ZGManager.sharedInstance().api().setPreviewView(null);
            ZGManager.sharedInstance().api().stopPublishing();
            //移除渲染视图
            videoRenderer.removeView(mRoomID);

            mDealBtn.setText("StartPublish");

        } else {
            // 界面button==开始推流
            // 开启预览再开始推流

            // 添加外部渲染视图
            videoRenderer.addView(com.zego.zegoavkit2.ZegoConstants.ZegoVideoDataMainPublishingStream, mPreView);

            ZGManager.sharedInstance().api().startPreview();
            ZGManager.sharedInstance().api().startPublishing(mRoomID, mRoomName, ZegoConstants.PublishFlag.JoinPublish);
        }
    }

    // 处理拉流相关操作
    public void dealPlay(View view){

        // 界面button==开始拉流
        if (mDealPlayBtn.getText().toString().equals("StartPlay") && !mPlayStreamID.equals("")){
            // 设置拉流视图
            if (isSetDecodeCallback) {
                // 若选择的外部渲染类型是未解码型，设置添加解码类渲染视图
                videoRenderer.addDecodView(mPlayView);
            } else {
                // 选择的外部渲染类型不是未解码型，根据拉流流名设置渲染视图
                videoRenderer.addView (mPlayStreamID, mPlayView);
            }

            // 开始拉流，不为 SDK 设置渲染视图，使用自渲染的视图
            boolean ret = ZGManager.sharedInstance().api().startPlayingStream(mPlayStreamID, null);

            mErrorTxt.setText("");
            if (!ret) {
                mErrorTxt.setText("拉流失败");
            }

        } else {
            // 界面button==停止拉流
            if (!mPlayStreamID.equals("")){
                //停止拉流
                ZGManager.sharedInstance().api().stopPlayingStream(mPlayStreamID);
                //移除外部渲染视图
                videoRenderer.removeView(mPlayStreamID);

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
    public void onVideoSizeChangedTo(String streamID, int width, int height) {
        Log.d("Zego","onVideoSizeChangedTo callback, streamID: "+streamID+", width:"+width+",height:"+height);

    }
}
