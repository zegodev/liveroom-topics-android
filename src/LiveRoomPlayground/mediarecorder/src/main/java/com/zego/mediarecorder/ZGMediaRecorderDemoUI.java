package com.zego.mediarecorder;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zego.common.util.DeviceInfoManager;
import com.zego.common.ZGManager;
import com.zego.common.ui.BaseActivity;
import com.zego.zegoavkit2.IZegoMediaPlayerCallback;
import com.zego.zegoavkit2.mediarecorder.IZegoMediaRecordCallback;
import com.zego.zegoavkit2.mediarecorder.ZegoMediaRecordChannelIndex;
import com.zego.zegoavkit2.mediarecorder.ZegoMediaRecordFormat;
import com.zego.zegoavkit2.mediarecorder.ZegoMediaRecordType;
import com.zego.zegoavkit2.ZegoMediaPlayer;
import com.zego.zegoavkit2.mediarecorder.ZegoMediaRecorder;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

public class ZGMediaRecorderDemoUI extends BaseActivity implements IZegoMediaRecordCallback, IZegoMediaPlayerCallback {

    private TextureView mPreView;
    private Button mReordBtn;
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
        mPlayBtn = (Button) findViewById(R.id.play_btn);
        mPlayBtn.setEnabled(false);
        mCameraToggle = (ToggleButton) findViewById(R.id.tb_enable_front_cam);
        mFrontCameraTxt = (TextView) findViewById(R.id.front_camera_txt);
        mErrorTxt = (TextView) findViewById(R.id.error_txt);
        mSavePathTxt = (TextView) findViewById(R.id.savePath_txt);

        mRoomID = DeviceInfoManager.generateDeviceId(this);

        recordMode = getIntent().getIntExtra("RecordMode", 2);
        useFlvFormat = getIntent().getBooleanExtra("RecordFormat", true);

        String dirPath = this.getExternalCacheDir().getPath();
        mRecordingPath = dirPath + "/" + generateAVFileName();

        zegoMediaRecorder = new ZegoMediaRecorder();

        // 创建播放器对象
        zegoMediaPlayer = new ZegoMediaPlayer();
        // 初始化播放器
        zegoMediaPlayer.init(ZegoMediaPlayer.PlayerTypePlayer);

        // join room
        ZGManager.sharedInstance().api().loginRoom(DeviceInfoManager.generateDeviceId(this), ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorcode, ZegoStreamInfo[] zegoStreamInfos) {

                if (0 == errorcode) {

                    isLoginRoomSuccess = true;

                    // 设置媒体录制回调监听
                    zegoMediaRecorder.setZegoMediaRecordCallback(ZGMediaRecorderDemoUI.this);
                    // 设置播放回调监听
                    zegoMediaPlayer.setCallback(ZGMediaRecorderDemoUI.this);

                    ZGManager.sharedInstance().api().setPreviewView(mPreView);
                    ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
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
            ZGManager.sharedInstance().api().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
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


    public void dealRecording(View view) {

        if (mReordBtn.getText().toString().equals(getString(R.string.tx_start_record))) {

            // 开始录制
            recording();
        } else {

            // 停止录制
            ZGManager.sharedInstance().api().stopPreview();
            ZGManager.sharedInstance().api().setPreviewView(null);

            boolean ret = zegoMediaRecorder.stopRecord(ZegoMediaRecordChannelIndex.MAIN);
            if (ret) {
                mReordBtn.setText(R.string.tx_start_record);
                mPlayBtn.setEnabled(true);

                mSavePathTxt.setText("storagePath: " + mSavePath);
            } else {
                mErrorTxt.setText("stop record fail!");
            }
        }
    }

    public void dealPlay(View view) {
        if (mPlayBtn.getText().toString().equals(getString(R.string.tx_start_play))) {
            if (zegoMediaPlayer != null) {
                zegoMediaPlayer.setView(mPreView);
            }

            if (!mSavePath.equals("")) {
                zegoMediaPlayer.setVolume(100);
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

        // 删除媒体录制回调监听
        zegoMediaRecorder.setZegoMediaRecordCallback(null);
        zegoMediaRecorder = null;
        zegoMediaPlayer.setCallback(null);
        zegoMediaPlayer.uninit();
        zegoMediaPlayer = null;
        ZGManager.sharedInstance().api().logoutRoom();
        ZGManager.sharedInstance().unInitSDK();
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
            //更新界面按钮标识
            mReordBtn.setText(getString(R.string.tx_stop_record));
        } else {
            mErrorTxt.setText("onMediaRecord err:" + errCode);
        }
    }

    @Override
    public void onRecordStatusUpdate(ZegoMediaRecordChannelIndex zegoMediaRecordChannelIndex, String storagePath, long duration, long fileSize) {

    }

    // 媒体播放器回调
    @Override
    public void onPlayStart() {
        //更新界面
        mPlayBtn.setText(getString(R.string.tx_stop_play));
        mErrorTxt.setText("");
    }

    @Override
    public void onPlayStop() {

        zegoMediaPlayer.setView(null);
        //更新界面
        mPlayBtn.setText(getString(R.string.tx_start_play));
    }

    @Override
    public void onPlayEnd() {

        zegoMediaPlayer.setView(null);
        //更新界面
        mPlayBtn.setText(getString(R.string.tx_start_play));
    }

    @Override
    public void onPlayError(int errorcode) {
        //更新界面
        mErrorTxt.setText("play err: " + errorcode);
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

    @Override
    public void onProcessInterval(long l) {

    }
}

