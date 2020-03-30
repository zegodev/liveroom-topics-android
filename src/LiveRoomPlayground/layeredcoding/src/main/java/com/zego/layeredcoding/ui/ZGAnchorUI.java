package com.zego.layeredcoding.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.zego.common.ZGManager;
import com.zego.layeredcoding.R;
import com.zego.layeredcoding.ZGLayeredCodingDemoHelper;
import com.zego.zegoliveroom.callback.IZegoEndJoinLiveCallback;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//import com.zego.zegoliveroom.entity.ZegoStreamQuality;


public class ZGAnchorUI extends AppCompatActivity implements IZegoLivePublisherCallback, IZegoLivePlayerCallback, IZegoRoomCallback {
    private Button mExitBtn;

    private TextureView mPreview;
    private TextureView mPlayView;

    private Spinner mLayerSpin;
    private TextView mNetQualityTxt;
    private TextView mBitrateTxt;
    private TextView mFpsTxt;
    private TextView mErrorTxt;

    private String mRoomName;
    private String mRoomID;

    private String joinLiveUserID;
    private int respondJoinLiveResult = -1;
    private boolean alreadyJoinLive = false;

    private String audienceStreamID = "";
    private boolean bePlayingStream = false;

    private boolean useFrontCamera = true;
    private boolean useOptimisedNet = true;

    private static final String[] mLayeredChoices = {"Auto", "BaseLayer", "ExtendLayer"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("");
        setContentView(R.layout.activity_anchor);

        mExitBtn = (Button)findViewById(R.id.quit_btn);
        mExitBtn.setEnabled(false);
        mPreview = (TextureView)findViewById(R.id.preview_view);
        mPlayView = (TextureView)findViewById(R.id.play_view);

        mLayerSpin = (Spinner)findViewById(R.id.sp_layers);
        mNetQualityTxt = (TextView)findViewById(R.id.netQuality_txt);
        mBitrateTxt = (TextView)findViewById(R.id.bitrate_txt);
        mFpsTxt = (TextView)findViewById(R.id.fps_txt);
        mErrorTxt = (TextView)findViewById(R.id.error_txt);

        List<String> allLayers = new ArrayList<String>();
        for (int i = 0; i < mLayeredChoices.length; i++) {
            allLayers.add(mLayeredChoices[i]);
        }

        ArrayAdapter<String> aspnLayers = new ArrayAdapter<String>(this, R.layout.spinner_item, allLayers);
        mLayerSpin.setAdapter(aspnLayers);


        getIntent().getBooleanExtra("UseFrontCamera", useFrontCamera);
        getIntent().getBooleanExtra("UseOptimisedNet",useOptimisedNet);
        mRoomName = getIntent().getStringExtra("RoomName");
        mRoomID = ZGLayeredCodingDemoHelper.sharedInstance().generateRoomID(this);

        // 设置ZegoRoomCallback监听
        ZGManager.sharedInstance().api().setZegoRoomCallback(this);
        // 设置推流回调监听
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(this);
        // 设置拉流回调监听
        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(this);

        // join room
        boolean ret = ZGManager.sharedInstance().api().loginRoom(mRoomID, mRoomName, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] zegoStreamInfos) {

                if (0 == errorcode) {

                    ZGManager.sharedInstance().api().setFrontCam(useFrontCamera);

                    ZGManager.sharedInstance().api().setPreviewView(mPreview);
                    ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                    ZGManager.sharedInstance().api().enableCamera(true);
                    ZGManager.sharedInstance().api().startPreview();

                    if (useOptimisedNet) {
                        // 设置延迟模式
                        ZGManager.sharedInstance().api().setLatencyMode(ZegoConstants.LatencyMode.Low3);
                        // 设置分层编码
                        ZGManager.sharedInstance().api().setVideoCodecId(ZegoConstants.ZegoVideoCodecAvc.VIDEO_CODEC_MULTILAYER, ZegoConstants.PublishChannelIndex.MAIN);
                        // 设置流量控制
                        ZGManager.sharedInstance().api().enableTrafficControl(ZegoConstants.ZegoTrafficControlProperty.ZEGOAPI_TRAFFIC_FPS | ZegoConstants.ZegoTrafficControlProperty.ZEGOAPI_TRAFFIC_RESOLUTION, true);
                    }

                    // 推流
                    ZGManager.sharedInstance().api().startPublishing(mRoomID, mRoomName, ZegoConstants.PublishFlag.JoinPublish);

                } else {
                    mErrorTxt.setText("login room fail, err: "+ errorcode);
                }

            }
        });
        if (!ret) {
            mErrorTxt.setText("login room fail(sync) ");
        }

        mLayerSpin.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String layerChoice = mLayeredChoices[i];

                // 设置视频分层
                int videoLayer = ZGLayeredCodingDemoHelper.sharedInstance().getVideoLayer(layerChoice);
                if (bePlayingStream) {
                    ZGManager.sharedInstance().api().activateVideoPlayStream(audienceStreamID, true, videoLayer);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //停止推流
        ZGManager.sharedInstance().api().stopPreview();
        ZGManager.sharedInstance().api().setPreviewView(null);
        ZGManager.sharedInstance().api().stopPublishing();

        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(null);
        ZGManager.sharedInstance().api().setZegoRoomCallback(null);
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(null);

        ZGManager.sharedInstance().api().logoutRoom();
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

    @Override
    public void onPublishStateUpdate(int stateCode, String streamID, HashMap<String, Object> hashMap) {

        if (0 !=  stateCode) {
            runOnUiThread(()->{
                mErrorTxt.setText("pulish fail, err: " + stateCode);
            });
        }
    }

    @Override
    public void onJoinLiveRequest(int seq, String fromUserID, String fromUserName, String roomID) {

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
//    public void onPublishQualityUpdate(String streamID, ZegoStreamQuality zegoStreamQuality) {
    public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {
        String qualityStr = "";
        switch (zegoPublishStreamQuality.quality) {
            case 0:
                qualityStr = "优";
                break;
            case 1:
                qualityStr = "良";
                break;
            case 2:
                qualityStr = "中";
                break;
            case 3:
                qualityStr = "差";
            default:
                break;
        }
        String netQuality = "(pulish)当前网络质量："+qualityStr;
        String bitrate = "码率：" + zegoPublishStreamQuality.vkbps+"kb/s";
        String fps = "帧率："+zegoPublishStreamQuality.vcapFps;

//        String bitrate = "码率：" + zegoStreamQuality.videoBitrate+"kb/s";
//        String fps = "帧率："+zegoStreamQuality.videoFPS;

        runOnUiThread(()->{
            mNetQualityTxt.setText(netQuality);
            mBitrateTxt.setText(bitrate);
            mFpsTxt.setText(fps);
        });
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

    @Override
    public void onKickOut(int reason, String roomID, String customReason) {

    }

    @Override
    public void onDisconnect(int errorCode, String roomID) {
        runOnUiThread(()->{
            mErrorTxt.setText("disconnect zego server, err:"+errorCode);
        });
    }

    @Override
    public void onReconnect(int i, String s) {

    }

    @Override
    public void onTempBroken(int i, String s) {

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

                        mLayerSpin.setVisibility(View.VISIBLE);

                        break;
                    }
                }
            }

        } else if (ZegoConstants.StreamUpdateType.Deleted == type) {

            // 处理流删除

            // 对应于此demo中 只拉一条流，此处只停止拉一条流
            if (bePlayingStream) {
                ZGManager.sharedInstance().api().stopPlayingStream(zegoStreamInfos[0].streamID);
                alreadyJoinLive = false;
                bePlayingStream = false;
                mExitBtn.setEnabled(false);
                mPlayView.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String s) {

    }

    @Override
    public void onRecvCustomCommand(String s, String s1, String s2, String s3) {

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

}
