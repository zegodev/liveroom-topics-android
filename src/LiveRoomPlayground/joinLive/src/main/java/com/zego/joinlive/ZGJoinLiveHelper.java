package com.zego.joinlive;

import android.view.View;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGPublishHelper;
import com.zego.common.util.AppLogger;
import com.zego.joinlive.constants.JoinLiveUserInfo;
import com.zego.joinlive.constants.JoinLiveView;
import com.zego.zegoliveroom.callback.IZegoEndJoinLiveCallback;
import com.zego.zegoliveroom.callback.IZegoResponseCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;

import java.util.ArrayList;

public class ZGJoinLiveHelper {

    // 最大连麦数
    public static final int MaxJoinLiveNum = 3;
    // 观众列表
    private ArrayList<String> mAudienceList = new ArrayList<>();
    // 已连麦列表
    private ArrayList<JoinLiveUserInfo> mHasJoinedUsersList = new ArrayList<>();
    // 连麦展示视图列表
    private ArrayList<JoinLiveView> mJoinLiveViewList = null;

    // 连麦回调
    private JoinLiveCallback joinLiveCallback = null;

    private static ZGJoinLiveHelper zgJoinLiveHelper = null;

    public static ZGJoinLiveHelper sharedInstance() {
        synchronized (ZGJoinLiveHelper.class) {
            if (zgJoinLiveHelper == null) {
                zgJoinLiveHelper = new ZGJoinLiveHelper();
            }
        }

        return zgJoinLiveHelper;
    }

    /**
     * 是否已经成功 initSDK
     *
     * @return true 代表initSDK完成, false 代表initSDK失败
     */
    private boolean isInitSDKSuccess() {
        if (ZGBaseHelper.sharedInstance().getZGBaseState() != ZGBaseHelper.ZGBaseState.InitSuccessState) {
            AppLogger.getInstance().w(ZGJoinLiveHelper.class, "请求失败! SDK未初始化, 请先初始化SDK");
            return false;
        }
        return true;
    }

    /**
     * 观众请求与主播连麦
     */
    public void requestJoinLive() {

        if (isInitSDKSuccess()) {
            // 已经成功初始化 SDK 的情况下，请求连麦
            ZGBaseHelper.sharedInstance().getZegoLiveRoom().requestJoinLive(new IZegoResponseCallback() {
                @Override
                public void onResponse(int result, String fromUserID, String fromUserName) {
                    if (joinLiveCallback != null) {
                        joinLiveCallback.requestJoinLiveResult((result == ZegoConstants.ResultCode.YES) ? true : false, fromUserID);
                    }
                }
            });
        }
    }

    /**
     * 主播响应观众的连麦请求
     *
     * @param requestReq 请求序号
     * @param isAgree    是否同意连麦
     */
    public void respondJoinLiveRequest(int requestReq, boolean isAgree) {
        if (isInitSDKSuccess()) {
            ZGBaseHelper.sharedInstance().getZegoLiveRoom().respondJoinLiveReq(requestReq, isAgree ? 0 : 1);
        }
    }


    /**
     * 主播邀请观众连麦
     *
     * @param audienceID 观众ID
     */
    public void inviteJoinLive(String audienceID) {
        if (isInitSDKSuccess()) {
            ZGBaseHelper.sharedInstance().getZegoLiveRoom().inviteJoinLive(audienceID, new IZegoResponseCallback() {
                @Override
                public void onResponse(int result, String fromUserID, String fromUserName) {
                    if (joinLiveCallback != null) {
                        joinLiveCallback.inviteJoinLiveResult((result == ZegoConstants.ResultCode.YES) ? true : false, fromUserID);
                    }
                }
            });
        }
    }

    /**
     * 观众响应主播的连麦邀请
     *
     * @param requestReq
     * @param isAgree
     */
    public void respondInvitedJoinLiveRequest(int requestReq, boolean isAgree) {
        if (isInitSDKSuccess()) {
            ZGBaseHelper.sharedInstance().getZegoLiveRoom().respondInviteJoinLiveReq(requestReq, isAgree ? 0 : 1);
        }
    }

    /**
     * 主播结束连麦
     */
    public void endJoinLive(String userID) {
        if (isInitSDKSuccess()) {
            for (final JoinLiveUserInfo userInfo : mHasJoinedUsersList) {
                if (userInfo.userID.equals(userID)) {
                    ZGBaseHelper.sharedInstance().getZegoLiveRoom().endJoinLive(userInfo.userID, new IZegoEndJoinLiveCallback() {
                        @Override
                        public void onEndJoinLive(int result, String roomID) {
                            if (joinLiveCallback != null) {
                                joinLiveCallback.endJoinLiveResult((0 == result) ? true : false, userInfo.userID, roomID);
                            }
                        }
                    });

                    break;
                }
            }
        }
    }

    /**
     * 观众响应结束连麦信令
     */
    public void handleEndJoinLiveCommand() {
        // 停止推流
        ZGPublishHelper.sharedInstance().stopPreviewView();
        ZGPublishHelper.sharedInstance().stopPublishing();
    }

    /**
     * 设置连麦相关的回调监听
     *
     * @param callback 连麦相关的回调
     */
    public void setJoinLiveCallback(JoinLiveCallback callback) {
        this.joinLiveCallback = callback;
    }

    // 连麦相关的回调接口
    public interface JoinLiveCallback {
        /**
         * 观众请求连麦的结果通知
         *
         * @param isSuccess  请求连麦是否成功，true表示主播同意连麦，false表示主播拒绝连麦
         * @param fromUserID 请求与之连麦的主播ID
         */
        public void requestJoinLiveResult(boolean isSuccess, String fromUserID);

        /**
         * 主播邀请观众连麦的结果通知
         *
         * @param isSuccess  邀请连麦是否成功，true表示观众同意连麦，false表示观众拒绝连麦
         * @param fromUserID 被邀请连麦的观众ID
         */
        public void inviteJoinLiveResult(boolean isSuccess, String fromUserID);

        /**
         * 主播结束连麦的结果通知
         *
         * @param isSuccess 结束连麦操作是否成功，true表示成功结束连麦，false表示结束连麦失败
         * @param userID    用户ID
         * @param roomID    连麦者所在的房间ID
         */
        public void endJoinLiveResult(boolean isSuccess, String userID, String roomID);

    }



    /**
     * 向观众列表增加指定观众
     *
     * @param audienceID 观众ID
     */
    public void addAudience(String audienceID) {
        if (mAudienceList.size() > 0) {
            for (String item : mAudienceList) {
                if (item.equals(audienceID)) {
                    return;
                }
            }
        }

        mAudienceList.add(audienceID);
    }

    /**
     * 从观众列表移除指定观众
     *
     * @param audienceID 观众ID
     */
    public void removeAudience(String audienceID) {
        mAudienceList.remove(audienceID);
    }

    /**
     * 获取观众列表
     *
     * @return 观众列表
     */
    public ArrayList<String> getAudienceList() {
        return mAudienceList;
    }

    /**
     * 获取已连麦用户数
     *
     * @return 已连麦用户数
     */
    public ArrayList<JoinLiveUserInfo> getHasJoinedUsers() {
        return mHasJoinedUsersList;
    }

    /**
     * 向已连麦列表增加成功连麦的观众
     *
     * @param userInfo 连麦者信息
     */
    public void addJoinLiveAudience(JoinLiveUserInfo userInfo) {
        mHasJoinedUsersList.add(userInfo);
    }

    /**
     * 修改已连麦列表中的单个连麦者的信息
     *
     * @param userInfo 连麦者信息
     */
    public void modifyJoinLiveUserInfo(JoinLiveUserInfo userInfo) {
        mHasJoinedUsersList.set(mHasJoinedUsersList.indexOf(userInfo), userInfo);
    }

    /**
     * 从已连麦列表移除指定连麦者
     *
     * @param userInfo 连麦者信息
     */
    public void removeJoinLiveAudience(JoinLiveUserInfo userInfo) {

        mHasJoinedUsersList.remove(userInfo);
    }

    /**
     * 清空已连麦列表
     */
    public void resetJoinLiveAudienceList() {
        mHasJoinedUsersList.clear();
    }


    /**
     * 添加所有可展示的视图
     *
     * @param joinLiveViews 视图信息
     */
    public void addTextureView(ArrayList<JoinLiveView> joinLiveViews) {
        mJoinLiveViewList = joinLiveViews;
    }

    /**
     * 修改视图列表
     *
     * @param joinLiveView 视图信息
     */
    public void modifyTextureViewInfo(JoinLiveView joinLiveView) {
        mJoinLiveViewList.set(mJoinLiveViewList.indexOf(joinLiveView), joinLiveView);
    }

    /**
     * 获取可用的视图
     *
     * @return 可用的视图
     */
    public JoinLiveView getFreeTextureView() {
        JoinLiveView textureView = null;
        for (JoinLiveView joinLiveView : mJoinLiveViewList) {
            if (joinLiveView.isFree()) {
                textureView = joinLiveView;
                textureView.textureView.setVisibility(View.VISIBLE);
                if (textureView.kickOutBtn != null){
                    textureView.kickOutBtn.setVisibility(View.VISIBLE);
                }

                break;
            }
        }

        return textureView;
    }

    /**
     * 停止播放时设置视图为可用
     *
     * @param streamID 流名
     */
    public void setJoinLiveViewFree(String streamID) {
        for (JoinLiveView joinLiveView : mJoinLiveViewList) {
            if (joinLiveView.streamID.equals(streamID)) {
                joinLiveView.setFree();
                joinLiveView.textureView.setVisibility(View.INVISIBLE);
                if (joinLiveView.kickOutBtn != null){
                    joinLiveView.kickOutBtn.setVisibility(View.INVISIBLE);
                }
                modifyTextureViewInfo(joinLiveView);

                break;
            }
        }
    }

    /**
     * 将所有视图设置为可用
     */
    public void freeAllJoinLiveView(){
        for (JoinLiveView joinLiveView : mJoinLiveViewList){
            if (!joinLiveView.streamID.equals("")){
                joinLiveView.setFree();
                joinLiveView.textureView.setVisibility(View.INVISIBLE);
                if (joinLiveView.kickOutBtn != null){
                    joinLiveView.kickOutBtn.setVisibility(View.INVISIBLE);
                }
                modifyTextureViewInfo(joinLiveView);
            }
        }
    }

    // 获取连麦视图列表
    public ArrayList<JoinLiveView> getJoinLiveViewList(){
        return mJoinLiveViewList;
    }
}
