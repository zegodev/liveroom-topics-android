package com.zego.frequency_spectrum.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGPlayHelper;
import com.zego.common.ZGPublishHelper;
import com.zego.common.util.AppLogger;
import com.zego.common.util.ZegoUtil;
import com.zego.common.widgets.CustomDialog;
import com.zego.frequency_spectrum.R;
import com.zego.frequency_spectrum.ui.FrequencySpectrumAndSoundLevelMainActivity;
import com.zego.frequency_spectrum.widget.FrequencySpectrumAndSoundLevelItem;
import com.zego.zegoliveroom.callback.IZegoLivePlayerCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoResponseCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.ZegoPlayStreamQuality;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;
import java.util.Iterator;

/**
 * FrequencySpectrumAndSoundLevelBaseActivity 存放的是与SDK无关的UI逻辑和业务逻辑
 * 将SDK接口调用相关而跟SDK的音频频谱和声浪无关的接口定义在此类中
 *
 */
public class FrequencySpectrumAndSoundLevelSDKAPIRelatedActivity extends FrequencySpectrumAndSoundLevelBaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        // UI相关和业务相关的逻辑可以在父类的onCreate方法中看到
        super.onCreate(savedInstanceState);

        // 设置房间相关的回调
        setZegoRoomCallback();

        // 设置推流相关的回调
        setPublishCallback();

        // 设置拉流相关的回调
        setPlayCallback();
    }

    /**
     * 拉流接口在本专题的封装
     *
     * @param streamID 拉流的流id
     */
    protected synchronized void startPlay(String streamID) {

        ZGPlayHelper.sharedInstance().startPlaying(streamID, null);

    }

    /**
     * 推流接口在本专题的封装
     *
     */
    protected void startPublishing() {
        publishStreamID = ZegoUtil.getPublishStreamID();

        // 开始推流 推流使用 JoinPublish 连麦模式，可降低延迟
        ZGPublishHelper.sharedInstance().startPublishing(publishStreamID, "", ZegoConstants.PublishFlag.JoinPublish);
    }

    /**
     * 登录房间的接口在本专题的封装
     *
     */
    protected void loginRoom(){

        CustomDialog.createDialog("登录房间中...", this).show();


        // 开始推流前需要先登录房间
        ZGBaseHelper.sharedInstance().loginRoom(roomID, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                CustomDialog.createDialog(FrequencySpectrumAndSoundLevelSDKAPIRelatedActivity.this).cancel();
                if (errorCode == 0) {
                    AppLogger.getInstance().i(FrequencySpectrumAndSoundLevelMainActivity.class, "登陆房间成功 roomId : %s", roomID);


                    for(ZegoStreamInfo ZegoStreamInfo_tmp: zegoStreamInfos){

                        if(ZegoStreamInfo_tmp.streamID != null){
                            // 拉流
                            startPlay(ZegoStreamInfo_tmp.streamID);
                            // 创建临时的FrequencySpectrumAndSoundLevelItem
                            FrequencySpectrumAndSoundLevelItem frequencySpectrumAndSoundLevelItem =
                                    new FrequencySpectrumAndSoundLevelItem(FrequencySpectrumAndSoundLevelSDKAPIRelatedActivity.this, null);
                            // 添加到列表里
                            arrayList_FrequencySpectrumAndSoundLevelItem.add(frequencySpectrumAndSoundLevelItem);
                            // 动态添加到布局容器中, 拉流的放推流的下边所以索引值
                            ll_container.addView(frequencySpectrumAndSoundLevelItem);

                            frequencySpectrumAndSoundLevelItem.setStream_id(ZegoStreamInfo_tmp.streamID);
                            frequencySpectrumAndSoundLevelItem.getTv_username().setText(ZegoStreamInfo_tmp.userName);

                        }
                    }

                    startPublishing();
                } else {
                    AppLogger.getInstance().i(FrequencySpectrumAndSoundLevelMainActivity.class, "登陆房间失败, errorCode : %d", errorCode);
                    Toast.makeText(FrequencySpectrumAndSoundLevelSDKAPIRelatedActivity.this, getString(com.zego.common.R.string.tx_login_room_failure), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 设置房间相关的回调在本专题的封装
     *
     */
    protected void setZegoRoomCallback(){

        // 设置房间相关回调
        ZGBaseHelper.sharedInstance().setZegoRoomCallback(new IZegoRoomCallback() {
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
            public void onStreamUpdated(int type, ZegoStreamInfo[] zegoStreamInfos, String s) {

                if (type == ZegoConstants.StreamUpdateType.Added) {
                    for (ZegoStreamInfo zegoStreamInfo : zegoStreamInfos) {
                        if (zegoStreamInfo.streamID != null) {
                            startPlay(zegoStreamInfo.streamID);

                            // 创建临时的FrequencySpectrumAndSoundLevelItem
                            FrequencySpectrumAndSoundLevelItem frequencySpectrumAndSoundLevelItem =
                                    new FrequencySpectrumAndSoundLevelItem(FrequencySpectrumAndSoundLevelSDKAPIRelatedActivity.this, null);
                            // 添加到列表里
                            arrayList_FrequencySpectrumAndSoundLevelItem.add(frequencySpectrumAndSoundLevelItem);
                            // 动态添加到布局容器中, 拉流的放推流的下边所以索引值
                            ll_container.addView(frequencySpectrumAndSoundLevelItem);

                            frequencySpectrumAndSoundLevelItem.setStream_id(zegoStreamInfo.streamID);
                            frequencySpectrumAndSoundLevelItem.getTv_username().setText(zegoStreamInfo.userName);
                        }
                    }
                } else {
                    for (ZegoStreamInfo zegoStreamInfo : zegoStreamInfos) {

                        ZGPlayHelper.sharedInstance().stopPlaying(zegoStreamInfo.streamID);

                        Iterator<FrequencySpectrumAndSoundLevelItem> it = arrayList_FrequencySpectrumAndSoundLevelItem.iterator();

                        while(it.hasNext()){

                            FrequencySpectrumAndSoundLevelItem frequencySpectrumAndSoundLevelItem_tmp = it.next();
                            if(frequencySpectrumAndSoundLevelItem_tmp.getStream_id().equals(zegoStreamInfo.streamID)){
                                it.remove();
                                ll_container.removeView(frequencySpectrumAndSoundLevelItem_tmp);
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
        });

    }

    /**
     * 设置推流相关的回调在本专题的封装
     *
     */
    protected void setPublishCallback(){

        // 设置推流回调
        ZGPublishHelper.sharedInstance().setPublisherCallback(new IZegoLivePublisherCallback() {

            // 推流回调文档说明: <a>https://doc.zego.im/API/ZegoLiveRoom/Android/html/index.html</a>

            @Override
            public void onPublishStateUpdate(int errorCode, String
                    streamID, HashMap<String, Object> hashMap) {
                // 推流状态更新，errorCode 非0 则说明推流失败
                // 推流常见错误码请看文档: <a>https://doc.zego.im/CN/308.html</a>

                if (errorCode == 0) {

                    AppLogger.getInstance().i(FrequencySpectrumAndSoundLevelSDKAPIRelatedActivity.class, "推流成功, publishStreamID : %s", streamID);
                    Toast.makeText(FrequencySpectrumAndSoundLevelSDKAPIRelatedActivity.this, getString(R.string.tx_publish_success), Toast.LENGTH_SHORT).show();
                } else {

                    AppLogger.getInstance().i(FrequencySpectrumAndSoundLevelSDKAPIRelatedActivity.class, "推流失败, publishStreamID : %s, errorCode : %d", streamID, errorCode);
                    Toast.makeText(FrequencySpectrumAndSoundLevelSDKAPIRelatedActivity.this, getString(R.string.tx_publish_fail), Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onJoinLiveRequest(int i, String s, String s1, String s2) {
                /**
                 * 房间内有人申请加入连麦时会回调该方法
                 * 观众端可通过 {@link com.zego.zegoliveroom.ZegoLiveRoom#requestJoinLive(IZegoResponseCallback)}
                 *  方法申请加入连麦
                 * **/
            }

            @Override
            public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality
                    zegoPublishStreamQuality) {
                /**
                 * 推流质量更新, 回调频率默认3秒一次
                 * 可通过 {@link com.zego.zegoliveroom.ZegoLiveRoom#setPublishQualityMonitorCycle(long)} 修改回调频率
                 */

            }

            @Override
            public void onCaptureVideoSizeChangedTo(int width, int height) {

            }

            @Override
            public void onCaptureVideoFirstFrame() {
                // 当SDK采集摄像头捕获到第一帧时会回调该方法

            }

            @Override
            public void onCaptureAudioFirstFrame() {
                // 当SDK音频采集设备捕获到第一帧时会回调该方法
            }
        });

    }


    /**
     * 设置拉流相关的回调在本专题的封装
     *
     */
    protected void setPlayCallback(){

        // 设置拉流回调
        ZGPlayHelper.sharedInstance().setPlayerCallback(new IZegoLivePlayerCallback() {
            @Override
            public void onPlayStateUpdate(int i, String s) {

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
        });

    }

}
