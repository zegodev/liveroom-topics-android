package com.zego.mediaplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

import com.zego.common.ZGManager;
import com.zego.common.timer.ZGTimer;
import com.zego.zegoavkit2.IZegoMediaPlayerCallback;
import com.zego.zegoavkit2.IZegoMediaPlayerVideoPlayCallback;
import com.zego.zegoavkit2.ZegoMediaPlayer;
import com.zego.zegoavkit2.ZegoVideoDataFormat;

import java.util.Locale;


/**
 * Created by zego on 2018/10/16.
 */

public class ZGMediaPlayerDemo implements IZegoMediaPlayerVideoPlayCallback {

    static private ZGMediaPlayerDemo zgMediaPlayerDemo;

    public static ZGMediaPlayerDemo sharedInstance(Context context) {
        synchronized (ZGMediaPlayerDemo.class) {
            if (zgMediaPlayerDemo == null) {
                zgMediaPlayerDemo = new ZGMediaPlayerDemo(context);
            }
        }
        return zgMediaPlayerDemo;
    }

    public enum ZGPlayerState {
        ZGPlayerState_Stopped,
        ZGPlayerState_Stopping,
        ZGPlayerState_Playing
    }

    public enum ZGPlayingSubState {
        ZGPlayingSubState_Requesting,
        ZGPlayingSubState_PlayBegin,
        ZGPlayingSubState_Paused,
        ZGPlayingSubState_Buffering
    }

    private ZGVideoCaptureForMediaPlayer.ZGMediaPlayerVideoCapture zgMediaPlayerVideoCapture = null;
    private ZGVideoCaptureForMediaPlayer zgVideoCaptureForMediaPlayer;
    private ZGMediaPlayerPublishingHelper zgMediaPlayerPublishingHelper;
    private ZGMediaPlayerDemoDelegate zgMediaPlayerDemoDelegate;
    private ZGPlayerState zgPlayerState = ZGPlayerState.ZGPlayerState_Stopped;
    private ZGPlayingSubState zgPlayingSubState = ZGPlayingSubState.ZGPlayingSubState_Requesting;
    private int currentProgress;
    private int duration;


    public ZGMediaPlayerDemoHelper getZgMediaPlayerDemoHelper() {
        return zgMediaPlayerDemoHelper;
    }

    public void setZgMediaPlayerDemoHelper(ZGMediaPlayerDemoHelper zgMediaPlayerDemoHelper) {
        this.zgMediaPlayerDemoHelper = zgMediaPlayerDemoHelper;
    }

    public String TAG = "MediaPlayerDemo";

    private ZGMediaPlayerDemoHelper zgMediaPlayerDemoHelper;

    public ZGMediaPlayerDemo(Context context) {
        zgMediaPlayerVideoCapture = new ZGVideoCaptureForMediaPlayer.ZGMediaPlayerVideoCapture();
        zgVideoCaptureForMediaPlayer = new ZGVideoCaptureForMediaPlayer(zgMediaPlayerVideoCapture);
        zgMediaPlayerPublishingHelper = new ZGMediaPlayerPublishingHelper();
        ZGManager.sharedInstance().enableExternalVideoCapture(zgVideoCaptureForMediaPlayer);
        // 创建播放器对象
        zegoMediaPlayer = new ZegoMediaPlayer();
        // 初始化播放器
        zegoMediaPlayer.init(ZegoMediaPlayer.PlayerTypePlayer);
        // 设置播放器回调
        zegoMediaPlayer.setCallback(zgMediaPlayerCallback);
        zgMediaPlayerPublishingHelper.startPublishing(context, msg -> {
            zgMediaPlayerDemoDelegate.onPublishState(msg);
            Log.v(TAG, msg);
        });
        zegoMediaPlayer.setVideoPlayCallback(this, ZegoVideoDataFormat.PIXEL_FORMAT_RGBA32);

    }

    ZGTimer zgTimer;

    private void setPlayingSubState(ZGPlayingSubState zgPlayingSubState) {
        this.zgPlayingSubState = zgPlayingSubState;
        Log.v(TAG, String.format("setPlayingSubState zgPlayingSubState: %s", zgPlayingSubState.name()));
        updateCurrentState();
    }

    private void setPlayerState(ZGPlayerState zgPlayerState) {
        Log.v(TAG, String.format("setPlayerState zgPlayerState: %s", zgPlayerState.name()));
        this.zgPlayerState = zgPlayerState;
        updateCurrentState();

        if (zgPlayerState == ZGPlayerState.ZGPlayerState_Playing) {
            // 定时器存在则销毁定时器
            if (zgTimer != null) {
                zgTimer.cancel();
                zgTimer = null;
            }
            // 创建定时器 单位 秒
            zgTimer = new ZGTimer(1000) {
                @Override
                public void onFinish() {
                    currentProgress = (int) zegoMediaPlayer.getCurrentDuration();
                    updateProgressDesc();
                }
            };
            zgTimer.start();

        } else if (zgTimer != null) {
            // 定时器有效情况下销毁
            zgTimer.cancel();
            zgTimer = null;
            if (zgPlayerState == ZGPlayerState.ZGPlayerState_Stopped) {
                zgMediaPlayerDemoDelegate.onPlayerStop();
            }
        }
    }

    /* 媒体播放器 */
    private ZegoMediaPlayer zegoMediaPlayer;

    // --------------------------------对外接口-------------------------------- //

    /* 设置view */
    public void setView(View view) {
        if (zegoMediaPlayer != null) {
            zegoMediaPlayer.setView(view);
        }
    }

    /* 释放MediaPlayer 和一些相关操作 */
    public void unInit() {
        if (zegoMediaPlayer != null) {
            if (zgTimer != null) {
                zgTimer.cancel();
                zgTimer = null;
            }
            ZGManager.sharedInstance().api().logoutRoom();
            stopPlay();
            zegoMediaPlayer.setCallback(null);
            zegoMediaPlayer.setVideoPlayCallback(null, 0);
            zegoMediaPlayer.uninit();
            zegoMediaPlayer = null;
            zgMediaPlayerDemo = null;
            ZGManager.sharedInstance().enableExternalVideoCapture(null);

        }
    }

    /* 停止播放 */
    public void stopPlay() {
        Log.e(TAG, "stopPlay");
        if (zegoMediaPlayer != null) {
            zegoMediaPlayer.stop();
            setPlayerState(ZGPlayerState.ZGPlayerState_Stopping);
        }
    }

    /**
     * 开始播放
     *
     * @param filePath file路径
     * @param repeat   是否重复播放
     */
    public void startPlay(String filePath, boolean repeat) {
        Log.e(TAG, String.format("startPlay path: %s", filePath));
        if (zegoMediaPlayer != null) {
            if (zgPlayerState == ZGPlayerState.ZGPlayerState_Playing) {
                zegoMediaPlayer.stop();
            }
            zegoMediaPlayer.start(filePath, repeat);
            setPlayerState(ZGPlayerState.ZGPlayerState_Playing);
            setPlayingSubState(ZGPlayingSubState.ZGPlayingSubState_Requesting);
        }
    }

    /**
     * 暂停播放
     */
    public void pausePlay() {
        if (zegoMediaPlayer != null) {
            zegoMediaPlayer.pause();
        }
    }

    /**
     * 恢复播放
     */
    public void resume() {
        if (zegoMediaPlayer != null) {
            zegoMediaPlayer.resume();
        }
    }

    /**
     * 快进到指定进度
     *
     * @param millisecond 进度单位 毫秒
     */
    public void seekTo(long millisecond) {
        if (zegoMediaPlayer != null) {
            zegoMediaPlayer.seekTo(millisecond);
        }
    }

    /**
     * 获取该文件的播放时长
     *
     * @return
     */
    public long getDuration() {
        if (zegoMediaPlayer != null) {
            return zegoMediaPlayer.getDuration();
        }
        return -1;
    }

    /**
     * 设置音量
     *
     * @param volume
     */
    public void setVolume(int volume) {
        if (zegoMediaPlayer != null) {
            zegoMediaPlayer.setVolume(volume);
        }
    }

    /**
     * 设置音轨
     *
     * @param audioStream
     */
    public void setAudioStream(int audioStream) {
        if (zegoMediaPlayer != null) {
            zegoMediaPlayer.setAudioStream(audioStream);
        }
    }

    /**
     * 设置播放类型
     *
     * @param type
     */
    public void setPlayerType(int type) {
        if (zegoMediaPlayer != null) {
            zegoMediaPlayer.setPlayerType(type);
        }
    }

    /**
     * 获取音轨数量
     */
    public long getAudioStreamCount() {
        if (zegoMediaPlayer != null) {
            return zegoMediaPlayer.getAudioStreamCount();
        }
        return -1;
    }


    // 设置播放器回调代理
    public void setZGMediaPLayerDelegate(ZGMediaPlayerDemoDelegate zgMediaPLayerDelegate) {
        this.zgMediaPlayerDemoDelegate = zgMediaPLayerDelegate;
    }

    // --------------------------------对外接口-------------------------------- //


    public interface MediaPlayerVideoDataCallback {
        void onPlayVideoData(byte[] bytes, int i, ZegoVideoDataFormat f);
    }

    @Override
    public void onPlayVideoData(byte[] bytes, int i, ZegoVideoDataFormat f) {
        if (zgMediaPlayerVideoCapture != null) {
            zgMediaPlayerVideoCapture.onPlayVideoData(bytes, i, f);
        }
    }

    IZegoMediaPlayerCallback zgMediaPlayerCallback = new IZegoMediaPlayerCallback() {

        @Override
        public void onPlayStart() {
            Log.v(TAG, "onPlayStart");

            assert (zgPlayerState == ZGPlayerState.ZGPlayerState_Playing);
            assert (zgPlayingSubState == ZGPlayingSubState.ZGPlayingSubState_Requesting);

            setPlayingSubState(ZGPlayingSubState.ZGPlayingSubState_PlayBegin);

            currentProgress = 0;
            duration = (int) zegoMediaPlayer.getDuration();

            updateProgressDesc();

            int audioStreamCount = (int) getAudioStreamCount();
            if (zgMediaPlayerDemoDelegate != null) {
                zgMediaPlayerDemoDelegate.onGetAudioStreamCount(audioStreamCount);
            }
        }

        @Override
        public void onPlayPause() {
            Log.v(TAG, "onPlayPause");
            assert (zgPlayerState == ZGPlayerState.ZGPlayerState_Playing);
            setPlayingSubState(ZGPlayingSubState.ZGPlayingSubState_Paused);
        }

        @Override
        public void onPlayStop() {
            Log.v(TAG, "onPlayStop");
            if (zgPlayerState == ZGPlayerState.ZGPlayerState_Stopping) {
                setPlayerState(ZGPlayerState.ZGPlayerState_Stopped);
            }
        }

        @Override
        public void onPlayResume() {
            Log.v(TAG, "onPlayResume");
            assert (zgPlayerState == ZGPlayerState.ZGPlayerState_Playing);
            setPlayingSubState(ZGPlayingSubState.ZGPlayingSubState_PlayBegin);
        }

        @Override
        public void onPlayError(int errorCode) {
            Log.e(TAG, String.format("onPlayError error: %d", errorCode));
            assert (zgPlayerState == ZGPlayerState.ZGPlayerState_Playing);
            setPlayingSubState(ZGPlayingSubState.ZGPlayingSubState_PlayBegin);
        }

        @Override
        public void onVideoBegin() {
            Log.v(TAG, "onVideoBegin");
        }

        @Override
        public void onAudioBegin() {
            Log.v(TAG, "onAudioBegin");
        }

        @Override
        public void onPlayEnd() {
            setPlayerState(ZGPlayerState.ZGPlayerState_Stopped);
        }

        @Override
        public void onBufferBegin() {
            Log.v(TAG, "onBufferBegin");
            if (zgPlayerState == ZGPlayerState.ZGPlayerState_Playing) {
                setPlayingSubState(ZGPlayingSubState.ZGPlayingSubState_Buffering);
            } else {
                assert (false);
            }
        }

        @Override
        public void onBufferEnd() {
            Log.v(TAG, "onBufferEnd");
            if (zgPlayerState == ZGPlayerState.ZGPlayerState_Playing) {
                if (zgPlayingSubState == ZGPlayingSubState.ZGPlayingSubState_Buffering) {
                    setPlayingSubState(ZGPlayingSubState.ZGPlayingSubState_PlayBegin);
                } else {
                    assert (false);
                }
            } else {
                assert (false);
            }
        }

        /**
         * 完成快进到指定时刻
         * @param code 大于等于0表示成功，其它表示失败
         * @param millisecond 实际快进的进度
         */

        @Override
        public void onSeekComplete(int code, long millisecond) {
            Log.v(TAG, "onSeekComplete");
        }

        @Override
        public void onSnapshot(Bitmap bitmap) {
            
        }
    };


    private void updateProgressDesc() {
        if (zgMediaPlayerDemoDelegate != null) {
            int v = (currentProgress / 1000);
            int v2 = (duration / 1000);
            String desc = String.format(Locale.getDefault(), "%d / %d", v, v2);
            zgMediaPlayerDemoDelegate.onPlayerProgress(currentProgress, duration,
                    desc);
        }
    }

    private void updateCurrentState() {
        String currentStateDesc = null;
        switch (zgPlayerState) {
            case ZGPlayerState_Stopped:
                currentStateDesc = "Stopped";
                break;
            case ZGPlayerState_Stopping:
                currentStateDesc = "Stopping";
                break;
            case ZGPlayerState_Playing: {
                String prefix = "Playing";
                String subState = null;
                switch (zgPlayingSubState) {
                    case ZGPlayingSubState_Requesting:
                        subState = "Requesting";
                        break;
                    case ZGPlayingSubState_PlayBegin:
                        subState = "Begin";
                        break;
                    case ZGPlayingSubState_Paused:
                        subState = "Paused";
                        break;
                    case ZGPlayingSubState_Buffering:
                        subState = "Buffering";
                        break;
                    default:
                        break;
                }
                currentStateDesc = String.format(Locale.getDefault(), "%s: %s", prefix, subState);
                break;
            }
        }
        if (zgMediaPlayerDemoDelegate != null) {
            zgMediaPlayerDemoDelegate.onPlayerState(currentStateDesc);
        }
    }


    public interface ZGMediaPlayerDemoDelegate {

        void onPlayerState(String state);

        void onPlayerProgress(long current, long max, String desc);

        void onPlayerStop();

        void onPublishState(String state);

        void onGetAudioStreamCount(int count);
    }
}
