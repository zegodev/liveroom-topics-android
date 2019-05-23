package com.zego.interrupthandler.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.Toast;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGManager;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.interrupthandler.AudioInterruptHandler;
import com.zego.interrupthandler.CameraInterruptHandler;
import com.zego.interrupthandler.ProcessManager;
import com.zego.interrupthandler.R;
import com.zego.interrupthandler.adapter.RoomListAdapter;
import com.zego.interrupthandler.databinding.ActivityMainInterruptHandlerBinding;
import com.zego.support.RoomInfo;
import com.zego.support.RoomListUpdateListener;
import com.zego.support.api.ZGAppSupportHelper;
import com.zego.zegoliveroom.callback.IZegoInitSDKCompletionCallback;

import java.util.ArrayList;

/**
 * 房间列表页。
 */
public class InterruptHandlerMainActivityUI extends BaseActivity {


    private RoomListAdapter roomListAdapter = new RoomListAdapter();

    /**
     * 即构demo常用的一个库，用于简单的请求房间列表。
     */
    private ZGAppSupportHelper zgAppSupportHelper;

    private ActivityMainInterruptHandlerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_interrupt_handler);

        binding.roomList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        // 设置adapter
        binding.roomList.setAdapter(roomListAdapter);
        // 设置Item添加和移除的动画
        binding.roomList.setItemAnimator(new DefaultItemAnimator());

        // 设置下拉刷新事件监听
        binding.refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                zgAppSupportHelper.api().updateRoomList(ZGManager.appId);
            }
        });

        // 初始化SDK
        initSDK();

        initCameraInterruptHandler();
        initAudioInterruptHandler();

        roomListAdapter.setOnItemClickListener(new RoomListAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(View view, int position, RoomInfo roomInfo) {
                if (roomInfo.getStreamInfo().size() > 0) {
                    // 跳转到拉流页面进行拉流
                    InterruptHandlerPlayUI.actionStart(InterruptHandlerMainActivityUI.this,
                            roomInfo.getRoomId(), roomInfo.getStreamInfo().get(0).getStreamId());
                } else {
                    AppLogger.getInstance().i(InterruptHandlerMainActivityUI.class, getString(R.string.room_no_publish));
                    Toast.makeText(InterruptHandlerMainActivityUI.this, R.string.room_no_publish, Toast.LENGTH_LONG).show();
                }
            }

        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        releaseCameraInterruptHandler();
        releaseAudioInterruptHandler();
        // 释放SDK
        ZGBaseHelper.sharedInstance().unInitZegoSDK();
    }

    /**
     * 初始化SDK逻辑
     */
    private void initSDK() {
        AppLogger.getInstance().i(ZGBaseHelper.class, "初始化zegoSDK");

        // 初始化SDK
        ZGBaseHelper.sharedInstance().initZegoSDK(ZGManager.appId, ZGManager.appSign, true, new IZegoInitSDKCompletionCallback() {
            @Override
            public void onInitSDK(int errorCode) {
                // 初始化完成后, 请求房间列表
                if (errorCode == 0) {
                    // 初始化完成后需要刷新房间列表
                    zgAppSupportHelper.api().updateRoomList(ZGManager.appId);

                    AppLogger.getInstance().i(InterruptHandlerMainActivityUI.class, "初始化zegoSDK成功");
                } else {
                    AppLogger.getInstance().i(InterruptHandlerMainActivityUI.class, "初始化zegoSDK失败 errorCode : %d", errorCode);
                }
            }
        });

        zgAppSupportHelper = ZGAppSupportHelper.create(this);

        // 监听房间列表更新
        zgAppSupportHelper.api().setRoomListUpdateListener(new RoomListUpdateListener() {
            @Override
            public void onUpdateRoomList(ArrayList<RoomInfo> arrayList) {
                roomListAdapter.addRoomInfo(arrayList);
                binding.refreshLayout.setRefreshing(false);
                if (arrayList.size() > 0) {
                    binding.queryRoomState.setVisibility(View.GONE);
                } else {
                    binding.queryRoomState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onUpdateRoomListError() {
                binding.refreshLayout.setRefreshing(false);
            }
        });
    }

    /**
     * 初始化摄像头打断事件处理器
     */
    private void initCameraInterruptHandler() {
        ProcessManager.shared().setApplicationContext(getApplication());
        CameraInterruptHandler.shared().setApplicationContext(getApplication());
        CameraInterruptHandler.shared().setLiveRoom(ZGBaseHelper.sharedInstance().getZegoLiveRoom());
        CameraInterruptHandler.shared().setCameraEnable(true);
    }

    /**
     * 初始化音频打断事件处理器
     */
    private void initAudioInterruptHandler() {
        AudioInterruptHandler.shared().setApplicationContext(getApplication());
        AudioInterruptHandler.shared().setLiveRoom(ZGBaseHelper.sharedInstance().getZegoLiveRoom());
        AudioInterruptHandler.shared().setAudioModuleEnable(true);
    }

    /**
     * 释放摄像头打断事件处理器
     */
    private void releaseCameraInterruptHandler() {
        ProcessManager.shared().release(getApplication());
        CameraInterruptHandler.shared().release(getApplication());
    }

    /**
     * 释放音频打断事件处理器
     */
    private void releaseAudioInterruptHandler() {
        AudioInterruptHandler.shared().release(getApplication());
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, InterruptHandlerMainActivityUI.class);
        activity.startActivity(intent);
    }

    /**
     * 发起推流的 Button 点击事件
     */
    public void startPublish(View view) {

        // 必须初始化SDK完成才能进行以下操作
        if (ZGBaseHelper.sharedInstance().getZGBaseState() == ZGBaseHelper.ZGBaseState.InitSuccessState) {
            InterruptHandlerPublishUI.actionStart(this);
        } else {
            AppLogger.getInstance().i(InterruptHandlerMainActivityUI.class, "请先初始化 SDK 再发起推流");
            Toast.makeText(this, "请先初始化 SDK 再发起推流", Toast.LENGTH_LONG).show();
        }
    }
}
