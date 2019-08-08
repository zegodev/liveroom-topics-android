package com.zego.videofilter.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.zego.common.util.AppLogger;
import com.zego.common.widgets.CustomPopWindow;
import com.zego.videofilter.R;
import com.zego.videofilter.databinding.ActivityVideoFilterMainBinding;
import com.zego.videofilter.faceunity.FURenderer;
import com.zego.videofilter.faceunity.authpack;
import com.zego.videofilter.videoFilter.VideoFilterFactoryDemo;

public class VideoFilterMainUI extends AppCompatActivity implements View.OnClickListener {

    private ActivityVideoFilterMainBinding binding;

    private VideoFilterFactoryDemo.FilterType mFilterType = VideoFilterFactoryDemo.FilterType.FilterType_HybridMem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_filter_main);

        // 获取选定的前处理传递数据的类型
        setCheckedFilterTypeListener();

        // 前处理传递数据类型说明的点击事件监听
        binding.syncTexture2DDescribe.setOnClickListener(this);
        binding.memTexture2DDescribe.setOnClickListener(this);
        binding.rgba32MemDescribe.setOnClickListener(this);
        binding.i420MemDescribe.setOnClickListener(this);
        binding.surfaceTextureDescribe.setOnClickListener(this);

        binding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (null == authpack.A()) {
            binding.authpack.setText(R.string.tx_has_no_fu_authpack);
            binding.loginBtn.setVisibility(View.INVISIBLE);
        } else {
            // 初始化 FaceUnity
            FURenderer.initFURenderer(this);
        }
    }

    // 获取选定的前处理传递数据的类型
    public void setCheckedFilterTypeListener(){
        binding.RadioSyncTexture2D.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.RadioSurfaceTexture.setChecked(false);
                binding.RadioI420Mem.setChecked(false);
                binding.RadioRGBA32Mem.setChecked(false);
                binding.RadioMemTexture2D.setChecked(false);

                mFilterType = VideoFilterFactoryDemo.FilterType.FilterType_SyncTexture;
            }
        });

        binding.RadioMemTexture2D.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.RadioSurfaceTexture.setChecked(false);
                binding.RadioI420Mem.setChecked(false);
                binding.RadioRGBA32Mem.setChecked(false);
                binding.RadioSyncTexture2D.setChecked(false);

                mFilterType = VideoFilterFactoryDemo.FilterType.FilterType_HybridMem;
            }
        });

        binding.RadioRGBA32Mem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.RadioSurfaceTexture.setChecked(false);
                binding.RadioI420Mem.setChecked(false);
                binding.RadioMemTexture2D.setChecked(false);
                binding.RadioSyncTexture2D.setChecked(false);

                mFilterType = VideoFilterFactoryDemo.FilterType.FilterType_Mem;
            }
        });

        binding.RadioI420Mem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.RadioSurfaceTexture.setChecked(false);
                binding.RadioMemTexture2D.setChecked(false);
                binding.RadioRGBA32Mem.setChecked(false);
                binding.RadioSyncTexture2D.setChecked(false);

                mFilterType = VideoFilterFactoryDemo.FilterType.FilterType_ASYNCI420Mem;
            }
        });

        binding.RadioSurfaceTexture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.RadioMemTexture2D.setChecked(false);
                binding.RadioI420Mem.setChecked(false);
                binding.RadioRGBA32Mem.setChecked(false);
                binding.RadioSyncTexture2D.setChecked(false);

                mFilterType = VideoFilterFactoryDemo.FilterType.FilterType_SurfaceTexture;
            }
        });
    }

    public void onClickLoginRoomAndPublish(View view) {
        String roomID = binding.edRoomId.getText().toString();
        if (!"".equals(roomID)) {
            // 跳转到创建并登录房间的页面
            FUBeautyActivity.actionStart(VideoFilterMainUI.this, roomID, mFilterType);
        } else {
            Toast.makeText(VideoFilterMainUI.this, getString(com.zego.common.R.string.tx_room_id_is_no_null), Toast.LENGTH_SHORT).show();
            AppLogger.getInstance().i(VideoFilterMainUI.class, getString(com.zego.common.R.string.tx_room_id_is_no_null));
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 供其他Activity调用，进入本专题的方法
     *
     * @param activity
     */
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, VideoFilterMainUI.class);
        activity.startActivity(intent);
    }

    // 前处理传递数据类型的描述
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.syncTexture2D_describe) {
            showPopWindows(getString(R.string.syncTexture2D_describe), v);
        } else if (id == R.id.memTexture2D_describe) {
            showPopWindows(getString(R.string.memTexture2D_describe), v);
        } else if (id == R.id.rgba32Mem_describe) {
            showPopWindows(getString(R.string.rgba32Mem_describe), v);
        } else if (id == R.id.i420Mem_describe) {
            showPopWindows(getString(R.string.i420Mem_describe), v);
        } else if (id == R.id.surfaceTexture_describe) {
            showPopWindows(getString(R.string.surfaceTexture_describe), v);
        }
    }

    /**
     * 显示描述窗口
     *
     * @param msg  显示内容
     * @param view
     */
    private void showPopWindows(String msg, View view) {
        //创建并显示popWindow
        new CustomPopWindow.PopupWindowBuilder(this)
                .enableBackgroundDark(true) //弹出popWindow时，背景是否变暗
                .setBgDarkAlpha(0.7f) // 控制亮度
                .create()
                .setMsg(msg)
                .showAsDropDown(view, 0, 20);
    }
}
