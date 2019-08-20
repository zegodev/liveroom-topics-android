package com.zego.sound.processing.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v4.widget.SwipeRefreshLayout;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.Toast;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGManager;
import com.zego.common.ui.BaseActivity;
import com.zego.common.util.AppLogger;
import com.zego.common.util.ZegoUtil;
import com.zego.sound.processing.R;
import com.zego.sound.processing.adapter.RoomListAdapter;
import com.zego.sound.processing.databinding.ActivityMainSoundProcessBinding;
import com.zego.support.RoomInfo;
import com.zego.support.RoomListUpdateListener;
import com.zego.support.api.ZGAppSupportHelper;
import com.zego.zegoliveroom.callback.IZegoInitSDKCompletionCallback;

import java.util.ArrayList;

/**
 * 变声/混响/立体声房间列表页。
 */
public class SoundProcessMainActivityUI extends BaseActivity {


    private RoomListAdapter roomListAdapter = new RoomListAdapter();

    /**
     * 即构demo常用的一个库，用于简单的请求房间列表。
     */
    private ZGAppSupportHelper zgAppSupportHelper;

    private ActivityMainSoundProcessBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_sound_process);

        binding.roomList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        // 设置adapter
        binding.roomList.setAdapter(roomListAdapter);
        // 设置Item添加和移除的动画
        binding.roomList.setItemAnimator(new DefaultItemAnimator());

        // 设置下拉刷新事件监听
        binding.refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                zgAppSupportHelper.api().updateRoomList(ZegoUtil.getAppID());
            }
        });

        // 初始化SDK
        initSDK();

        roomListAdapter.setOnItemClickListener(new RoomListAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(View view, int position, RoomInfo roomInfo) {
                if (roomInfo.getStreamInfo().size() > 0) {
                    // 跳转到拉流页面进行拉流
                    SoundProcessPlayUI.actionStart(SoundProcessMainActivityUI.this,
                            roomInfo.getRoomId(), roomInfo.getStreamInfo().get(0).getStreamId());
                } else {
                    AppLogger.getInstance().i(SoundProcessMainActivityUI.class, getString(R.string.room_no_publish));
                    Toast.makeText(SoundProcessMainActivityUI.this, R.string.room_no_publish, Toast.LENGTH_LONG).show();
                }
            }

        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放SDK
        ZGBaseHelper.sharedInstance().unInitZegoSDK();
    }

    /**
     * 初始化SDK逻辑
     */
    private void initSDK() {
        AppLogger.getInstance().i(ZGBaseHelper.class, "初始化zegoSDK");

        // 初始化SDK
        ZGBaseHelper.sharedInstance().initZegoSDK(ZegoUtil.getAppID(), ZegoUtil.getAppSign(), ZegoUtil.getIsTestEnv(), new IZegoInitSDKCompletionCallback() {
            @Override
            public void onInitSDK(int errorCode) {
                // 初始化完成后, 请求房间列表
                if (errorCode == 0) {
                    // 初始化完成后需要刷新房间列表
                    zgAppSupportHelper.api().updateRoomList(ZegoUtil.getAppID());

                    AppLogger.getInstance().i(SoundProcessMainActivityUI.class, "初始化zegoSDK成功");
                } else {
                    AppLogger.getInstance().i(SoundProcessMainActivityUI.class, "初始化zegoSDK失败 errorCode : %d", errorCode);
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

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, SoundProcessMainActivityUI.class);
        activity.startActivity(intent);
    }

    /**
     * 发起推流的 Button 点击事件
     *
     * @param view
     */
    public void startPublish(View view) {

        // 必须初始化SDK完成才能进行以下操作
        if (ZGBaseHelper.sharedInstance().getZGBaseState() == ZGBaseHelper.ZGBaseState.InitSuccessState) {
            SoundProcessPublishUI.actionStart(this);
        } else {
            AppLogger.getInstance().i(SoundProcessMainActivityUI.class, "请先初始化 SDK 再发起推流");
            Toast.makeText(this, "请先初始化 SDK 再发起推流", Toast.LENGTH_LONG).show();
        }
    }
}
