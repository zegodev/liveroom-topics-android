package com.zego.joinlive;

import android.view.View;

import com.zego.joinlive.constants.JoinLiveUserInfo;
import com.zego.joinlive.constants.JoinLiveView;
import com.zego.zegoliveroom.ZegoLiveRoom;

import java.util.ArrayList;

public class ZGJoinLiveHelper {

    public static final String PREFIX = "JoinLiveRoom-";

    // 最大连麦数
    public static final int MaxJoinLiveNum = 3;
    // 已连麦列表
    private ArrayList<JoinLiveUserInfo> mHasJoinedUsersList = new ArrayList<>();
    // 连麦展示视图列表
    private ArrayList<JoinLiveView> mJoinLiveViewList = null;

    private ZegoLiveRoom zegoLiveRoom = null;

    private static ZGJoinLiveHelper zgJoinLiveHelper = null;

    public static ZGJoinLiveHelper sharedInstance() {
        synchronized (ZGJoinLiveHelper.class) {
            if (zgJoinLiveHelper == null) {
                zgJoinLiveHelper = new ZGJoinLiveHelper();
            }
        }

        return zgJoinLiveHelper;
    }

    // 获取 ZegoLiveRoom 实例
    public ZegoLiveRoom getZegoLiveRoom(){
        if (null == zegoLiveRoom) {
            zegoLiveRoom = new ZegoLiveRoom();
        }
        return zegoLiveRoom;
    }

    // 销毁 ZegoLiveRoom 实例
    public void releaseZegoLiveRoom(){
        if (null != zegoLiveRoom){
            // 释放 SDK 资源
            zegoLiveRoom.unInitSDK();
            zegoLiveRoom = null;
        }
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
                modifyTextureViewInfo(joinLiveView);
            }
        }
    }

    // 获取连麦视图列表
    public ArrayList<JoinLiveView> getJoinLiveViewList(){
        return mJoinLiveViewList;
    }
}
