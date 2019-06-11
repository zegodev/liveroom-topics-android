package com.zego.mediasideinfo.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.zego.common.util.DeviceInfoManager;
import com.zego.common.ZGManager;
import com.zego.common.ui.BaseActivity;
import com.zego.mediasideinfo.ZGMediaSideInfoDemo;
import com.zego.mediasideinfo.ZGMediaSideInfoDemoHelper;
import com.zego.mediasideinfo.R;


/**
 * Created by winnie on 2018/12/04.
 */

public class MediaSideInfoDemoUI extends BaseActivity implements ZGMediaSideInfoDemo.RecvMediaSideInfoCallback, ZGMediaSideInfoDemoHelper.StatusChangedNotify {

    private TextView mStatusText;
    private TextView mCheckResultText;
    private TextView mShowBytesText;
    private Switch mOnlyAudioSw;
    private Switch mCutomPacketSw;
    private Button mPublishBtn;
    private Button mSendBtn;
    private Button mCheckBtn;
    private TextureView mPreview;
    private TextureView mPlayView;

    private TextView mSendContent;
    private TextView mRecvContent;

    private EditText mSendStr;

    private boolean isUseOnlyAudio = false;
    private boolean isUseCustomPacket = false;

    private String allSendContent = "";
    private String allRecvContent = "";

    private Handler handler=null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("MediaSideInfo");
        setContentView(R.layout.activity_sideinfo);

        mStatusText = (TextView)findViewById(R.id.txt_status);
        mCheckResultText = (TextView)findViewById(R.id.txt_checkResult);
        mShowBytesText = (TextView)findViewById(R.id.txt_bytes);

        mOnlyAudioSw = (Switch)findViewById(R.id.switch_onlyAudio);
        mOnlyAudioSw.setChecked(false);
        mCutomPacketSw = (Switch)findViewById(R.id.switch_custom);
        mCutomPacketSw.setChecked(false);

        mPublishBtn = (Button)findViewById(R.id.btn_publish);
        mSendBtn = (Button)findViewById(R.id.btn_send);
        mCheckBtn = (Button)findViewById(R.id.btn_check);

        mPreview = (TextureView)findViewById(R.id.view_preview);
        mPlayView = (TextureView)findViewById(R.id.view_playview);

        mSendContent = (TextView)findViewById(R.id.txt_sendContent);
        mRecvContent = (TextView)findViewById(R.id.txt_recvContent);

        mSendStr = (EditText)findViewById(R.id.send_data);

        ZGMediaSideInfoDemo.sharedInstance().setMediaSideInfoCallback(this);
        ZGMediaSideInfoDemoHelper.sharedInstance().setRecvStatusChangedNotify(this);

        //创建属于主线程的handler
        handler = new Handler();

        mOnlyAudioSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    isUseOnlyAudio = true;
                } else {
                    isUseOnlyAudio = false;
                }
                if (ZGMediaSideInfoDemoHelper.ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_Login_OK == ZGMediaSideInfoDemoHelper.sharedInstance().getTopicStatus()) {
                    ZGMediaSideInfoDemo.sharedInstance().activateMediaSideInfoForPublishChannel(isUseOnlyAudio, 0);
                }
            }
        });

        mCutomPacketSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    isUseCustomPacket = true;
                } else {
                    isUseCustomPacket = false;
                }

                // control senddata header 5bytes
                ZGMediaSideInfoDemo.sharedInstance().setUseCutomPacket(isUseCustomPacket);
            }
        });

        mSendContent.setMovementMethod(ScrollingMovementMethod.getInstance());
        mRecvContent.setMovementMethod(ScrollingMovementMethod.getInstance());

        // 进频道
        ZGMediaSideInfoDemoHelper.sharedInstance().loginRoom();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZGMediaSideInfoDemo.sharedInstance().unSetMediaSideInfoCallback();
        ZGMediaSideInfoDemoHelper.sharedInstance().unSetRecvStatusChangedNotify();
        ZGMediaSideInfoDemoHelper.sharedInstance().setTopicStatus(ZGMediaSideInfoDemoHelper.ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_None);
        ZGManager.sharedInstance().unInitSDK();
    }

    public void startPublish(View view) {

        ZGMediaSideInfoDemoHelper.sharedInstance().setPlayView(mPlayView);

        ZGMediaSideInfoDemoHelper.sharedInstance().publishAndPlayWithCongfig(this, isUseOnlyAudio, mPreview);
    }

    public void sendData(View view) {
        String content = mSendStr.getText().toString();

        if (content.length() > 0) {
            if (isUseCustomPacket) {
                ZGMediaSideInfoDemo.sharedInstance().sendMediaSideInfo(content,0);
            } else {
                ZGMediaSideInfoDemo.sharedInstance().sendMediaSideInfo(content);
            }

            allSendContent += content;
            allSendContent += "\n";
            mSendContent.setText(allSendContent);

            String bytesLen = String.valueOf(content.getBytes().length) + " bytes";

            mShowBytesText.setText(bytesLen);
        }

        mCheckResultText.setText("");
    }

    public void checkResult(View view) {

        String checkResult = ZGMediaSideInfoDemo.sharedInstance().checkSendRecvMsgs();

        mCheckResultText.setText(checkResult);
    }

    @Override
    public void onRecvMediaSideInfo(String streamID, String content) {
        allRecvContent += content;
        allRecvContent += "\n";

        runOnUiThread(()->{
            //更新界面
            mRecvContent.setText(allRecvContent);
        });
    }

    @Override
    public void onRecvMixStreamUserData(String streamID, String content) {

        allRecvContent += content;
        allRecvContent += "\n";
        runOnUiThread(()->{
            //更新界面
            mRecvContent.setText(allRecvContent);
        });
    }

    @Override
    public void onStatusChanged(ZGMediaSideInfoDemoHelper.ZGMediaSideTopicStatus status) {
        mStatusText.setText(ZGMediaSideInfoDemoHelper.sharedInstance().stringOfTopicStatus(status));

        switch (status) {
            case ZGMediaSideTopicStatus_None:
            case ZGMediaSideTopicStatus_Logining_Room:
            case ZGMediaSideTopicStatus_Login_OK:
                mPublishBtn.setEnabled(true);
                mSendBtn.setEnabled(false);
                break;
            case ZGMediaSideTopicStatus_Start_Publishing:
            case ZGMediaSideTopicStatus_Start_Playing:
                mPublishBtn.setEnabled(false);
                mSendBtn.setEnabled(false);
                break;
            case ZGMediaSideTopicStatus_Ready_For_Messaging:
                mPublishBtn.setEnabled(false);
                mSendBtn.setEnabled(true);
        }

        if (ZGMediaSideInfoDemoHelper.ZGMediaSideTopicStatus.ZGMediaSideTopicStatus_Login_OK == status) {
            ZGMediaSideInfoDemo.sharedInstance().activateMediaSideInfoForPublishChannel(isUseOnlyAudio, 0);
        }
    }
}
