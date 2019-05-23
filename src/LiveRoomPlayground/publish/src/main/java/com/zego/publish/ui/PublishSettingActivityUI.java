package com.zego.publish.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;

import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGConfigHelper;
import com.zego.common.application.ZegoApplication;
import com.zego.common.util.AppLogger;
import com.zego.common.widgets.log.FloatingView;
import com.zego.publish.R;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;


/**
 * Created by zego on 2019/3/21.
 */
public class PublishSettingActivityUI extends FragmentActivity {

    public static String SHARE_PREFERENCE_NAME = "publishSetting";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_publish_setting);
    }


    public static class PrefFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private String[] stringArray;
        private ListPreference viewModeListPreference, resolutionListPreference, bitrateListPreference, fpsListPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName(SHARE_PREFERENCE_NAME);
            getPreferenceManager().setSharedPreferencesMode(AppCompatActivity.MODE_PRIVATE);
            //从xml文件加载选项
            addPreferencesFromResource(R.xml.publish_setting_preference);

            stringArray = getResources().getStringArray(R.array.view_setting_describe);

            // 注册配置变化事件
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
            viewModeListPreference = (ListPreference) findPreference("publish_view_mode");
            resolutionListPreference = (ListPreference) findPreference("publish_resolution");
            bitrateListPreference = (ListPreference) findPreference("publish_bitrate");
            fpsListPreference = (ListPreference) findPreference("publish_fps");

            fpsListPreference.setSummary(sharedPreferences.getString("publish_fps", "15"));
            bitrateListPreference.setSummary(sharedPreferences.getString("publish_bitrate", "1200000"));
            resolutionListPreference.setSummary(sharedPreferences.getString("publish_resolution", "540x960"));

            // 动态修改当前描述
            String mode = sharedPreferences.getString("publish_view_mode", "1");
            viewModeListPreference.setSummary(stringArray[Integer.parseInt(mode)]);
        }

        @Override
        public void onDestroy() {

            // 取消注册监听配置事件
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onDestroy();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // 当界面配置修改时则会回调该方法
            // 方法内需要调用sdk函数来修改sdk配置参数

            if ("publish_view_mode".equals(key)) {
                String viewModeStr = sharedPreferences.getString(key, "1");
                int viewMode = Integer.valueOf(viewModeStr);

                // 设置推流预览视图模式
                ZGConfigHelper.sharedInstance().setPreviewViewMode(viewMode);

                // 动态修改当前描述
                viewModeListPreference.setSummary(stringArray[viewMode]);

            } else if ("publish_hardware_encode".equals(key)) {

                boolean enable = sharedPreferences.getBoolean(key, false);

                // 启用硬编
                ZGConfigHelper.sharedInstance().enableHardwareEncode(enable);

            } else if ("publish_preview_mirror".equals(key)) {

                boolean enable = sharedPreferences.getBoolean(key, true);

                // 启用预览镜像
                ZGConfigHelper.sharedInstance().enablePreviewMirror(enable);

            } else if ("publish_front_facing_camera".equals(key)) {

                boolean enable = sharedPreferences.getBoolean(key, true);

                // 启用前置摄像头
                ZGConfigHelper.sharedInstance().setFrontCam(enable);

            } else if ("publish_resolution".equals(key)) {
                String resolution = sharedPreferences.getString(key, "540x960");
                // 动态修改当前描述
                resolutionListPreference.setSummary(resolution);
                String[] resolutions = resolution.split("x");

                // 设置推流分辨率
                ZGConfigHelper.sharedInstance().setPublishResolution(Integer.parseInt(resolutions[0]), Integer.parseInt(resolutions[1]));

            } else if ("publish_bitrate".equals(key)) {
                String bitrate = sharedPreferences.getString(key, "1200000");
                // 动态修改当前描述
                bitrateListPreference.setSummary(bitrate);

                // 设置视频码率
                ZGConfigHelper.sharedInstance().setVideoBitrate(Integer.parseInt(bitrate));

            } else if ("publish_fps".equals(key)) {
                String fps = sharedPreferences.getString(key, "15");
                // 动态修改当前描述
                fpsListPreference.setSummary(fps);

                // 设置视频推流fps
                ZGConfigHelper.sharedInstance().setPublishVideoFps(Integer.parseInt(fps));
            }

        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        // 在应用内实现悬浮窗，需要依附Activity生命周期
        FloatingView.get().attach(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 在应用内实现悬浮窗，需要依附Activity生命周期
        FloatingView.get().detach(this);
    }

    /**
     * 清空推流配置
     */
    public static void clearPublishConfig() {
        AppLogger.getInstance().i(ZGConfigHelper.class, "推流常用功能, 恢复sdk默认配置");
        SharedPreferences sharedPreferences = ZegoApplication.zegoApplication.getSharedPreferences(SHARE_PREFERENCE_NAME, AppCompatActivity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        ZGConfigHelper.sharedInstance().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
        ZGConfigHelper.sharedInstance().enableHardwareEncode(false);
        ZGConfigHelper.sharedInstance().setFrontCam(true);
        ZGConfigHelper.sharedInstance().enableMic(true);
        ZGConfigHelper.sharedInstance().enableCamera(true);
        ZGConfigHelper.sharedInstance().enablePreviewMirror(true);
        ZGConfigHelper.sharedInstance().zegoAvConfig = new ZegoAvConfig(ZegoAvConfig.Level.High);
        ZGBaseHelper.sharedInstance().getZegoLiveRoom().setAVConfig(ZGConfigHelper.sharedInstance().zegoAvConfig);

        ZGBaseHelper.sharedInstance().getZegoLiveRoom().enableTrafficControl(
                ZegoConstants.ZegoTrafficControlProperty.ZEGOAPI_TRAFFIC_CONTROL_ADAPTIVE_FPS|
                        ZegoConstants.ZegoTrafficControlProperty.ZEGOAPI_TRAFFIC_CONTROL_RESOLUTION,true);
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, PublishSettingActivityUI.class);
        activity.startActivity(intent);
    }

}