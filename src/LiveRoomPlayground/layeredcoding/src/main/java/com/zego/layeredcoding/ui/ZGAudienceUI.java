package com.zego.layeredcoding.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zego.common.ZGManager;
import com.zego.common.util.DeviceInfoManager;
import com.zego.layeredcoding.R;
import com.zego.layeredcoding.ZGLayeredCodingDemoHelper;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoResponseCallback;
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

public class ZGAudienceUI extends AppCompatActivity implements IZegoLivePlayerCallback, IZegoLivePublisherCallback, IZegoRoomCallback {

    private ToggleButton mCameraTog;
    private TextView mCameraTxt;
    private Button mRequestBtn;
    private Button mExitBtn;

    private TextureView mPreview;
    private TextureView mPlayview;

    private Spinner mLayerSpin;
    private TextView mNetQualityTxt;
    private TextView mBitrateTxt;
    private TextView mFpsTxt;
    private TextView mErrorTxt;

    private String mRoomName;
    private String mRoomID;

    private String anchorID;
    private String anchorRoomID;
    private String anchorRoomName;

    private boolean useFrontCamera = true;
    private boolean bePlayingStream = false;
    private String playStreamID = "";
    private static final String[] mLayeredChoices = {"Auto", "BaseLayer", "ExtendLayer"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("");
        setContentView(R.layout.activity_audience);

        mCameraTog = (ToggleButton) findViewById(R.id.tb_enable_front_cam);
        mCameraTxt = (TextView)findViewById(R.id.front_camera_txt);
        mRequestBtn = (Button)findViewById(R.id.conference_btn);
        mRequestBtn.setEnabled(false);
        mExitBtn = (Button)findViewById(R.id.quit_btn);
        mExitBtn.setEnabled(false);
        mPlayview = (TextureView)findViewById(R.id.play_view);
        mPreview = (TextureView)findViewById(R.id.preview_view);
        mNetQualityTxt = (TextView)findViewById(R.id.netQuality_txt);
        mBitrateTxt = (TextView)findViewById(R.id.bitrate_txt);
        mFpsTxt = (TextView)findViewById(R.id.fps_txt);
        mErrorTxt = (TextView)findViewById(R.id.error_txt);

        mLayerSpin = (Spinner)findViewById(R.id.sp_layers);

        List<String> allLayers = new ArrayList<String>();
        for (int i = 0; i < mLayeredChoices.length; i++) {
            allLayers.add(mLayeredChoices[i]);
        }

        ArrayAdapter<String> aspnLayers = new ArrayAdapter<String>(this, R.layout.spinner_item, allLayers);
        mLayerSpin.setAdapter(aspnLayers);

        mCameraTog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mCameraTog.setChecked(true);
                    useFrontCamera = true;
                } else {
                    mCameraTog.setChecked(false);
                    useFrontCamera = false;
                }
            }
        });

        mRoomID = DeviceInfoManager.generateDeviceId(this);
        mRoomName = "zglc_layercoding";
        anchorRoomID = getIntent().getStringExtra("AnchorRoomID");
        anchorRoomName = getIntent().getStringExtra("AnchorRoomName");
        anchorID = getIntent().getStringExtra("AnchorID");

        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(this);
        ZGManager.sharedInstance().api().setZegoRoomCallback(this);
        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(this);

        //加入房间
        boolean ret = ZGManager.sharedInstance().api().loginRoom(anchorRoomID, anchorRoomName, ZegoConstants.RoomRole.Audience, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] zegoStreamInfos) {
                if (0 == errorcode) {
                    //拉流
                    if (zegoStreamInfos.length > 0) {
                        for (int i = 0; i < zegoStreamInfos.length;i++){
                            if (zegoStreamInfos[i].userID.equals(anchorID)) {
                                playStreamID = zegoStreamInfos[i].streamID;
                                break;
                            }
                        }

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

        mLayerSpin.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String layerChoice = mLayeredChoices[i];

                // 设置视频分层
                int videoLayer = ZGLayeredCodingDemoHelper.sharedInstance().getVideoLayer(layerChoice);
                if (bePlayingStream) {
                    ZGManager.sharedInstance().api().activateVideoPlayStream(playStreamID, true, videoLayer);
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

        if (bePlayingStream) {
            ZGManager.sharedInstance().api().stopPlayingStream(playStreamID);
        }

        ZGManager.sharedInstance().api().setZegoLivePlayerCallback(null);
        ZGManager.sharedInstance().api().setZegoRoomCallback(null);
        ZGManager.sharedInstance().api().setZegoLivePublisherCallback(null);

        ZGManager.sharedInstance().api().logoutRoom();
    }

    public void RequestBeAnchor(View view) {
        //申请连麦
         ZGManager.sharedInstance().api().requestJoinLive(new IZegoResponseCallback() {
             @Override
             public void onResponse(int result, String fromUserID, String fromUserName) {

                 if (0 == result){
                     mLayerSpin.setVisibility(View.VISIBLE);
                     mPreview.setVisibility(View.VISIBLE);

                     // 开始推流
                     ZGManager.sharedInstance().api().setFrontCam(useFrontCamera);
                     ZGManager.sharedInstance().api().setPreviewView(mPreview);
                     ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                     ZGManager.sharedInstance().api().enableCamera(true);
                     ZGManager.sharedInstance().api().startPreview();
                     // 设置延迟模式
                     ZGManager.sharedInstance().api().setLatencyMode(ZegoConstants.LatencyMode.Low3);
                     // 设置流量控制
                     ZGManager.sharedInstance().api().enableTrafficControl(ZegoConstants.ZegoTrafficControlProperty.ZEGOAPI_TRAFFIC_FPS | ZegoConstants.ZegoTrafficControlProperty.ZEGOAPI_TRAFFIC_RESOLUTION, true);
                     // 设置分层编码
                     ZGManager.sharedInstance().api().setVideoCodecId(ZegoConstants.ZegoVideoCodecAvc.VIDEO_CODEC_MULTILAYER, ZegoConstants.PublishChannelIndex.MAIN);

                     // 推流
                     ZGManager.sharedInstance().api().startPublishing(mRoomID, mRoomName, ZegoConstants.PublishFlag.JoinPublish);

                 }
             }
         });
    }

    public void QuitLiveRoom(View view) {
        // 停止推流
        ZGManager.sharedInstance().api().stopPreview();
        ZGManager.sharedInstance().api().setPreviewView(null);
        ZGManager.sharedInstance().api().stopPublishing();

        mRequestBtn.setEnabled(true);
        mPreview.setVisibility(View.INVISIBLE);
        mCameraTog.setVisibility(View.VISIBLE);
        mCameraTxt.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPlayStateUpdate(int stateCode, String streamID) {
        if (0 == stateCode) {
            mRequestBtn.setEnabled(true);
            bePlayingStream = true;
        } else {
            runOnUiThread(()->{
                mErrorTxt.setText("play fail,err: "+stateCode);
            });
        }
    }


    @Override
//    public void onPlayQualityUpdate(String streamID, ZegoStreamQuality zegoStreamQuality) {
    public void onPlayQualityUpdate(String s, ZegoPlayStreamQuality zegoPlayStreamQuality) {
        String qualityStr = "";
        switch (zegoPlayStreamQuality.quality) {
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
        String netQuality = "(play)当前网络质量："+qualityStr;
//        String bitrate = "码率：" + zegoStreamQuality.videoBitrate+"kb/s";
//        String fps = "帧率："+zegoStreamQuality.videoFPS;

        String bitrate = "码率：" + zegoPlayStreamQuality.vkbps+"kb/s";
        String fps = "帧率："+zegoPlayStreamQuality.vnetFps;

        runOnUiThread(()->{
            mNetQualityTxt.setText(netQuality);
            mBitrateTxt.setText(bitrate);
            mFpsTxt.setText(fps);
        });
    }

    @Override
    public void onInviteJoinLiveRequest(int i, String s, String s1, String s2) {

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
            mPreview.setVisibility(View.INVISIBLE);

            mCameraTog.setVisibility(View.VISIBLE);
            mCameraTxt.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onVideoSizeChangedTo(String s, int i, int i1) {

    }

    @Override
    public void onPublishStateUpdate(int stateCode, String streamID, HashMap<String, Object> hashMap) {

        if (0 == stateCode) {
            mRequestBtn.setEnabled(false);
            mExitBtn.setEnabled(true);
            mCameraTog.setVisibility(View.INVISIBLE);
            mCameraTxt.setVisibility(View.INVISIBLE);
        } else {
            runOnUiThread(()->{
                mErrorTxt.setText("publish fail,err: " + stateCode);
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

    @Override
    public void onKickOut(int reason, String roomID, String customReason) {

    }

    @Override
    public void onDisconnect(int i, String s) {

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

        } else if (ZegoConstants.StreamUpdateType.Deleted == type) {
            // 处理流删除

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
    public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String s) {

    }

    @Override
    public void onRecvCustomCommand(String s, String s1, String s2, String s3) {

    }
}
