package com.zego.mediarecorder;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zego.common.ZGHelper;
import com.zego.common.ZGManager;
import com.zego.zegoavkit2.IZegoMediaPlayerCallback;
import com.zego.zegoavkit2.mediarecorder.IZegoMediaRecordCallback;
import com.zego.zegoavkit2.mediarecorder.ZegoMediaRecordChannelIndex;
import com.zego.zegoavkit2.mediarecorder.ZegoMediaRecordFormat;
import com.zego.zegoavkit2.mediarecorder.ZegoMediaRecordType;
import com.zego.zegoavkit2.ZegoMediaPlayer;
import com.zego.zegoavkit2.mediarecorder.ZegoMediaRecorder;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;
//import com.zego.zegoliveroom.entity.ZegoStreamQuality;

import java.util.HashMap;


public class ZGMediaRecorderDemoUI extends AppCompatActivity implements IZegoMediaRecordCallback, IZegoLivePublisherCallback, IZegoMediaPlayerCallback {

    private TextureView mPreView;
    private Button mReordBtn;
    private Button mPublishBtn;
    private Button mPlayBtn;
    private ToggleButton mCameraToggle;
    private TextView mFrontCameraTxt;
    private TextView mErrorTxt;
    private TextView mSavePathTxt;

    private boolean isLoginRoomSuccess = false;
    private String mRoomID;
    private boolean useFrontCamera = true;

    private int recordMode;
    private boolean useFlvFormat; // true - flv， false - mp4
    private String mRecordingPath = "";
    private String mSavePath = "";

    private ZegoMediaRecorder zegoMediaRecorder;
    /* 媒体播放器 */
    private ZegoMediaPlayer zegoMediaPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_avrecording);

        mPreView = (TextureView) findViewById(R.id.pre_view);
        mReordBtn = (Button) findViewById(R.id.record_btn);
        mPublishBtn = (Button) findViewById(R.id.publish_btn);
        mPlayBtn = (Button) findViewById(R.id.play_btn);
        mPlayBtn.setEnabled(false);
        mCameraToggle = (ToggleButton) findViewById(R.id.tb_enable_front_cam);
        mFrontCameraTxt = (TextView) findViewById(R.id.front_camera_txt);
        mErrorTxt = (TextView) findViewById(R.id.error_txt);
        mSavePathTxt = (TextView) findViewById(R.id.savePath_txt);

        mRoomID = ZGHelper.generateDeviceId(this);

        recordMode = getIntent().getIntExtra("RecordMode", 2);
        useFlvFormat = getIntent().getBooleanExtra("RecordFormat", true);

        String deviceID = ZGHelper.generateDeviceId(this);
        ZGManager.setLoginUser(deviceID, deviceID);
        String dirPath = this.getExternalCacheDir().getPath();
        mRecordingPath = dirPath + "/" + generateAVFileName();

        zegoMediaRecorder = new ZegoMediaRecorder();

        // 创建播放器对象
        zegoMediaPlayer = new ZegoMediaPlayer();
        // 初始化播放器
        zegoMediaPlayer.init(ZegoMediaPlayer.PlayerTypePlayer);

        // join room
        ZGManager.sharedInstance().api().loginRoom(ZGHelper.generateDeviceId(this), ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] zegoStreamInfos) {

                if (0 == errorcode) {

                    isLoginRoomSuccess = true;

                    // 设置推流回调监听
                    ZGManager.sharedInstance().api().setZegoLivePublisherCallback(ZGMediaRecorderDemoUI.this);

                    // 设置媒体录制回调监听
                    zegoMediaRecorder.setZegoMediaRecordCallback(ZGMediaRecorderDemoUI.this);

                    // 设置播放回调监听
                    zegoMediaPlayer.setCallback(ZGMediaRecorderDemoUI.this);

                    ZGManager.sharedInstance().api().setPreviewView(mPreView);
                    ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleToFill);
                    ZGManager.sharedInstance().api().enableCamera(true);
                    ZGManager.sharedInstance().api().startPreview();

                } else {
                    mErrorTxt.setText("login room fail, err: " + errorcode);
                }
            }
        });


        // 摄像头方向开关
        if (0 == recordMode) { // record audio
            mFrontCameraTxt.setVisibility(View.GONE);
            mCameraToggle.setVisibility(View.GONE);
        } else {
            mCameraToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    if (isChecked) {
                        mCameraToggle.setChecked(true);
                        useFrontCamera = true;
                        if (0 != recordMode) {
                            ZGManager.sharedInstance().api().setFrontCam(useFrontCamera);
                            ZGManager.sharedInstance().api().startPreview();
                        }
                    } else {
                        mCameraToggle.setChecked(false);
                        useFrontCamera = false;
                        if (0 != recordMode) {
                            ZGManager.sharedInstance().api().setFrontCam(useFrontCamera);
                            ZGManager.sharedInstance().api().startPreview();
                        }
                    }
                }
            });
        }
    }

    public void recording() {
        if (isLoginRoomSuccess) {

            // 启动麦克风、摄像头
            ZGManager.sharedInstance().api().setPreviewView(mPreView);
            ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleToFill);
            ZGManager.sharedInstance().api().enableCamera(true);
            ZGManager.sharedInstance().api().startPreview();

            boolean ret = false;

            // 开始录制，录制模式：0 - audio，1 - video，2 - audio and video
            switch (recordMode) {
                case 0:
                    if (useFlvFormat) {
                        ret = zegoMediaRecorder.startRecord(ZegoMediaRecordChannelIndex.MAIN, ZegoMediaRecordType.AUDIO, mRecordingPath, true, 3000, ZegoMediaRecordFormat.FLV);
                    } else {
                        ret = zegoMediaRecorder.startRecord(ZegoMediaRecordChannelIndex.MAIN, ZegoMediaRecordType.AUDIO, mRecordingPath, true, 3000, ZegoMediaRecordFormat.MP4);
                    }
                    break;
                case 1:
                    if (useFlvFormat) {
                        ret = zegoMediaRecorder.startRecord(ZegoMediaRecordChannelIndex.MAIN, ZegoMediaRecordType.VIDEO, mRecordingPath, true, 3000, ZegoMediaRecordFormat.FLV);
                    } else {
                        ret = zegoMediaRecorder.startRecord(ZegoMediaRecordChannelIndex.MAIN, ZegoMediaRecordType.VIDEO, mRecordingPath, true, 3000, ZegoMediaRecordFormat.MP4);
                    }
                    break;
                case 2:
                    if (useFlvFormat) {
                        ret = zegoMediaRecorder.startRecord(ZegoMediaRecordChannelIndex.MAIN, ZegoMediaRecordType.BOTH, mRecordingPath, true, 3000, ZegoMediaRecordFormat.FLV);
                    } else {
                        ret = zegoMediaRecorder.startRecord(ZegoMediaRecordChannelIndex.MAIN, ZegoMediaRecordType.BOTH, mRecordingPath, true, 3000, ZegoMediaRecordFormat.MP4);
                    }
                    break;
                default:
                    break;
            }

            if (ret) {
                mSavePathTxt.setText("");
            } else {
                mErrorTxt.setText("start record fail!");
            }
        } else {
            mErrorTxt.setText("login room fail! ");
        }
    }


    public void DealRecording(View view) {

        if (mReordBtn.getText().toString().equals("StartRecord")) {

            // 开始录制
            recording();
        } else {

            // 停止录制
            ZGManager.sharedInstance().api().stopPreview();
            ZGManager.sharedInstance().api().setPreviewView(null);

            boolean ret = zegoMediaRecorder.stopRecord(ZegoMediaRecordChannelIndex.MAIN);
            if (ret) {
                mReordBtn.setText("StartRecord");
                mPlayBtn.setEnabled(true);

                mSavePathTxt.setText("storagePath: " + mSavePath);
            } else {
                mErrorTxt.setText("stop record fail!");
            }
        }
    }

    public void DealPublishing(View view) {
        if (isLoginRoomSuccess) {
            if (mPublishBtn.getText().toString().equals("StartPublish")) {

                ZGManager.sharedInstance().api().setPreviewView(mPreView);
                ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleToFill);
                ZGManager.sharedInstance().api().enableCamera(true);
                ZGManager.sharedInstance().api().startPreview();
                // 推流
                boolean ret = ZGManager.sharedInstance().api().startPublishing(mRoomID, "ZegoAVRecording", ZegoConstants.PublishFlag.JoinPublish);

                if (!ret) {
                    mErrorTxt.setText("publish fail sync");
                }

            } else {

                ZGManager.sharedInstance().api().stopPreview();
                ZGManager.sharedInstance().api().setPreviewView(null);
                boolean ret_stop = ZGManager.sharedInstance().api().stopPublishing();
                if (ret_stop) {
                    mPublishBtn.setText("StartPublish");
                } else {
                    mErrorTxt.setText("stop publish fail sync");
                }
            }
        }
    }

    public void DealPlay(View view) {
        if (mPlayBtn.getText().toString().equals("StartPlay")) {
            if (zegoMediaPlayer != null) {
                zegoMediaPlayer.setView(mPreView);
            }

            if (!mSavePath.equals("")) {
                zegoMediaPlayer.start(mSavePath, false);
            }
        } else {
            if (zegoMediaPlayer != null) {
                zegoMediaPlayer.setView(null);
            }

            zegoMediaPlayer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ZGManager.sharedInstance().api().logoutRoom();
        zegoMediaRecorder = null;
        zegoMediaPlayer.uninit();
        zegoMediaPlayer = null;
        ZGManager.sharedInstance().unInitSDK();
    }

    // 推流回调
    @Override
    public void onPublishStateUpdate(int stateCode, String streamID, HashMap<String, Object> hashMap) {
        if (stateCode == 0) {
            runOnUiThread(() -> {
                mPublishBtn.setText("StopPublish");
            });
        } else {
            runOnUiThread(() -> {
                mErrorTxt.setText("publish fail, err: " + stateCode);
            });
        }
    }

    @Override
    public void onJoinLiveRequest(int i, String s, String s1, String s2) {

    }

    @Override
    public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {
        Log.e("Zego", "onPublishQualityUpdate ,streamID: " + s + ",fps: " + zegoPublishStreamQuality.vcapFps);
    }

//    @Override
//    public void onPublishQualityUpdate(String s, ZegoStreamQuality zegoStreamQuality) {
//        Log.e("Zego", "onPublishQualityUpdate ,streamID: " + s + ",fps: " + zegoStreamQuality.videoFPS);
//    }

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


    public String generateAVFileName() {
        String fileName;

        if (useFlvFormat) {
            fileName = "zgmedia_" + System.currentTimeMillis() + ".flv";
        } else {
            fileName = "zgmedia_" + System.currentTimeMillis() + ".mp4";
        }

        return fileName;
    }

    // 本地录制回调
    @Override
    public void onMediaRecord(int errCode, ZegoMediaRecordChannelIndex zegoMediaRecordChannelIndex, String storagePath) {

        if (errCode == 0) {
            mSavePath = storagePath;
            runOnUiThread(() -> {
                //更新界面
                mReordBtn.setText("StopRecord");
            });

        } else {
            runOnUiThread(() -> {
                mErrorTxt.setText("onMediaRecord err:" + errCode);
            });
        }
    }

    @Override
    public void onRecordStatusUpdate(ZegoMediaRecordChannelIndex zegoMediaRecordChannelIndex, String storagePath, long duration, long fileSize) {

    }

    // 媒体播放器回调
    @Override
    public void onPlayStart() {

        runOnUiThread(() -> {
            //更新界面
            mPlayBtn.setText("StopPlay");
        });
    }

    @Override
    public void onPlayStop() {

        zegoMediaPlayer.setView(null);
        runOnUiThread(() -> {
            //更新界面
            mPlayBtn.setText("StartPlay");
        });
    }

    @Override
    public void onPlayEnd() {

        zegoMediaPlayer.setView(null);
        runOnUiThread(() -> {
            //更新界面
            mPlayBtn.setText("StartPlay");
        });
    }

    @Override
    public void onPlayError(int errorcode) {
        runOnUiThread(() -> {
            //更新界面
            mErrorTxt.setText("play err: "+errorcode);
        });
    }

    @Override
    public void onPlayPause() {

    }

    @Override
    public void onPlayResume() {

    }

    @Override
    public void onVideoBegin() {

    }

    @Override
    public void onAudioBegin() {

    }

    @Override
    public void onBufferBegin() {

    }

    @Override
    public void onBufferEnd() {

    }

    @Override
    public void onSeekComplete(int i, long l) {

    }

    @Override
    public void onSnapshot(Bitmap bitmap) {

    }

    @Override
    public void onLoadComplete() {
    }
}

