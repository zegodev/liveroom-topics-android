package com.zego.play.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.zego.common.ZGConfigHelper;
import com.zego.common.application.ZegoApplication;
import com.zego.common.util.AppLogger;
import com.zego.common.widgets.log.FloatingView;
import com.zego.play.R;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;

/**
 * Created by zego on 2019/3/21.
 */
public class PlaySettingActivityUI extends FragmentActivity {


    public static String SHARE_PREFERENCE_NAME = "playSetting";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play_setting);

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

    public static class PrefFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private ListPreference viewModeListPreference;
        private String streamID;
        private String[] stringArray;

        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName(SHARE_PREFERENCE_NAME);
            getPreferenceManager().setSharedPreferencesMode(AppCompatActivity.MODE_PRIVATE);
            //从xml文件加载选项
            addPreferencesFromResource(R.xml.play_setting_preference);

            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
            viewModeListPreference = (ListPreference) findPreference("play_view_mode");

            streamID = getActivity().getIntent().getStringExtra("streamID");

            stringArray = getResources().getStringArray(R.array.view_setting_describe);

            String mode = sharedPreferences.getString("play_view_mode", "1");
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

            if ("play_view_mode".equals(key)) {
                if (streamID != null && !"".equals(streamID)) {
                    String viewModeStr = sharedPreferences.getString(key, "1");
                    int viewMode = Integer.valueOf(viewModeStr);
                    setPlayViewMode(viewMode);
                    // 动态修改当前描述
                    String[] stringArray = getResources().getStringArray(R.array.view_setting_describe);
                    viewModeListPreference.setSummary(stringArray[viewMode]);
                } else {
                    Toast.makeText(PrefFragment.this.getActivity(), R.string.tx_set_play_view_mode_failure, Toast.LENGTH_LONG).show();
                }
            } else if ("play_hardware_decode".equals(key)) {
                boolean enable = sharedPreferences.getBoolean(key, false);
                enableHardwareDecode(enable);
            } else if ("play_volume".equals(key)) {
                int volume = sharedPreferences.getInt(key, 100);
                setPlayVolume(volume);
            }

        }

        /**
         * 设置SDK拉流视图
         */
        private void setPlayViewMode(int viewMode){
            ZGConfigHelper.sharedInstance().setPlayViewMode(viewMode, streamID);
        }

        /**
         * 启用SDK硬编
         */
        private void enableHardwareDecode(boolean enable){
            ZGConfigHelper.sharedInstance().enableHardwareDecode(enable);
        }

        /**
         * 设置拉流音量
         * @param volume
         */
        private void setPlayVolume(int volume){
            ZGConfigHelper.sharedInstance().setPlayVolume(volume);
        }

    }

    /**
     * 清空推流配置
     */
    public static void clearPlayConfig() {
        AppLogger.getInstance().i(ZGConfigHelper.class, "推流常用功能恢复sdk默认配置");
        SharedPreferences sharedPreferences = ZegoApplication.zegoApplication.getSharedPreferences(SHARE_PREFERENCE_NAME, AppCompatActivity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // 恢复sdk默认设置
        ZGConfigHelper.sharedInstance().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
        ZGConfigHelper.sharedInstance().enableHardwareEncode(false);
        ZGConfigHelper.sharedInstance().enableSpeaker(true);
        ZGConfigHelper.sharedInstance().setFrontCam(true);
    }

    public static void actionStart(Activity activity, String mStreamID) {
        Intent intent = new Intent(activity, PlaySettingActivityUI.class);
        intent.putExtra("streamID", mStreamID);
        activity.startActivity(intent);
    }
}
