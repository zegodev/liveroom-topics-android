package com.zego.mediaplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

import com.zego.common.ZGManager;
import com.zego.common.timer.ZGTimer;
import com.zego.zegoavkit2.IZegoMediaPlayerVideoPlayWithIndexCallback;
import com.zego.zegoavkit2.IZegoMediaPlayerVideoPlayWithIndexCallback2;
import com.zego.zegoavkit2.IZegoMediaPlayerWithIndexCallback;
import com.zego.zegoavkit2.ZegoMediaPlayer;
import com.zego.zegoavkit2.ZegoVideoCaptureFactory;
import com.zego.zegoavkit2.ZegoVideoDataFormat;
import com.zego.zegoliveroom.ZegoLiveRoom;

import java.util.Locale;

/**
 *
 * ZGMediaPlayerDemo
 *
 * 你可以通过 {@link #startPlay} 来进行播放音视频资源。
 * 支持播放格式  MP3、MP4。
 * 并且可以把播放的内容通过zegoSDK外部采集的方式进行推流。
 *
 *
 * 一般是k歌房场景需要使用到该功能。把MV视频作为推流数据推给观众。
 * k歌房还能通过 {@link #setPlayerType} 来指定类型，
 * 传入 {@link ZegoMediaPlayer#PlayerTypeAux} 类型让播放器的声音
 * 也一起混音到推流中。
 *
 * 基于zegoSDK {@link ZegoMediaPlayer} 播放器 与 zegoSDK {@link ZegoVideoCaptureFactory} 外部采集
 * 工厂实现。
 * <a href="https://doc.zego.im/CN/271.html"> zego外部采集 </a>
 * <a href="https://doc.zego.im/CN/283.html"> zego媒体播放器 </a>
 *   实现步骤如下
 *
 *                 1.开启zegoSDK外部采集功能
 *                 2.设置外部采集工厂 !!!注意，设置工厂需要在
 *                   { @link ZegoLiveRoom#initSDK(long, byte[]) }
 *                   之前。否则设置的工厂不生效
 *
 *                 3.创建初始化ZegoMediaPlayer播放器
 *                 4.设置ZegoMediaPlayer播放器回调
 *                 5.把ZegoMediaPlayer播放器回调的视频帧塞给外部采集工厂
 *
 *
 *     具体使用方法可以参考以下示例代码
 *     public class MainActivity extends Activity {
 *
 *          @Override
 *          protected void onCreate(@Nullable Bundle savedInstanceState) {
 *
 *               // 视图设置 view
 *               ZGMediaPlayerDemo.sharedInstance(this).setView(videoView);
 *
 *               // 音量设置 1-100
 *               ZGMediaPlayerDemo.sharedInstance(this).setVolume(100);
 *
 *               // 是开启aux混音
 *               ZGMediaPlayerDemo.sharedInstance(MediaPlayerDemoUI.this).setPlayerType(ZegoMediaPlayer.PlayerTypeAux);
 *
 *               // 播放视频资源
 *               ZGMediaPlayerDemo.sharedInstance(this).startPlay(path, repeat);
 *
 *          }
 *     }
 *
 *     <strong>警告:</strong> 如果不用播放器，
 *     请使用 {@link ZGMediaPlayerDemo#unInit()} 释放掉播放器，避免浪费内存
 *
 */
public class ZGMediaPlayerDemo implements IZegoMediaPlayerVideoPlayWithIndexCallback {

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
        // 播放停止中
        ZGPlayerState_Stopped,

        // 播放停止
        ZGPlayerState_Stopping,

        // 播放状态
        ZGPlayerState_Playing
    }

    public enum ZGPlayingSubState {
        // 请求播放
        ZGPlayingSubState_Requesting,

        // 开始播放
        ZGPlayingSubState_PlayBegin,

        // 暂停了
        ZGPlayingSubState_Paused,

        // 缓冲中
        ZGPlayingSubState_Buffering
    }

    // 播放视频外部采集类，用于实现Zego SDK外部采集
    private ZGVideoCaptureForMediaPlayer.ZGMediaPlayerVideoCapture zgMediaPlayerVideoCapture = null;

    private ZGVideoCaptureForMediaPlayer zgVideoCaptureForMediaPlayer;

    private ZGMediaPlayerPublishingHelper zgMediaPlayerPublishingHelper;
    private ZGMediaPlayerDemoDelegate zgMediaPlayerDemoDelegate;
    private ZGPlayerState zgPlayerState = ZGPlayerState.ZGPlayerState_Stopped;
    private ZGPlayingSubState zgPlayingSubState = ZGPlayingSubState.ZGPlayingSubState_Requesting;
    private int duration;
    private int currentProgress;


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
        zegoMediaPlayer.setEventWithIndexCallback(zgMediaPlayerCallback);
        zegoMediaPlayer.setProcessInterval(1000);
        zgMediaPlayerPublishingHelper.startPublishing(context, msg -> {
            zgMediaPlayerDemoDelegate.onPublishState(msg);
            Log.v(TAG, msg);
        });
        zegoMediaPlayer.setVideoPlayWithIndexCallback(this, ZegoVideoDataFormat.PIXEL_FORMAT_RGBA32);
    }

    private void setPlayingSubState(ZGPlayingSubState zgPlayingSubState) {
        this.zgPlayingSubState = zgPlayingSubState;
        Log.v(TAG, String.format("setPlayingSubState zgPlayingSubState: %s", zgPlayingSubState.name()));
        updateCurrentState();
    }

    private void setPlayerState(ZGPlayerState zgPlayerState) {
        Log.v(TAG, String.format("setPlayerState zgPlayerState: %s", zgPlayerState.name()));
        this.zgPlayerState = zgPlayerState;
        updateCurrentState();
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
            ZGManager.sharedInstance().api().logoutRoom();
            stopPlay();
            zegoMediaPlayer.setEventWithIndexCallback(zgMediaPlayerCallback);
            zegoMediaPlayer.setVideoPlayWithIndexCallback(null,0);
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
    public void onPlayVideoData(byte[] bytes, int i, ZegoVideoDataFormat f,int index) {
        if (zgMediaPlayerVideoCapture != null) {
            zgMediaPlayerVideoCapture.onPlayVideoData(bytes, i, f);
        }
    }

    IZegoMediaPlayerWithIndexCallback zgMediaPlayerCallback = new IZegoMediaPlayerWithIndexCallback() {

        @Override
        public void onPlayStart(int i) {
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
        public void onPlayPause(int i) {
            Log.v(TAG, "onPlayPause");
            assert (zgPlayerState == ZGPlayerState.ZGPlayerState_Playing);
            setPlayingSubState(ZGPlayingSubState.ZGPlayingSubState_Paused);
        }

        @Override
        public void onPlayStop(int i) {
            Log.v(TAG, "onPlayStop");
            if (zgPlayerState == ZGPlayerState.ZGPlayerState_Stopping) {
                setPlayerState(ZGPlayerState.ZGPlayerState_Stopped);
            }
        }

        @Override
        public void onPlayResume(int i) {
            Log.v(TAG, "onPlayResume");
            assert (zgPlayerState == ZGPlayerState.ZGPlayerState_Playing);
            setPlayingSubState(ZGPlayingSubState.ZGPlayingSubState_PlayBegin);
        }

        @Override
        public void onPlayError(int errorCode,int i) {
            Log.e(TAG, String.format("onPlayError error: %d", errorCode));
            assert (zgPlayerState == ZGPlayerState.ZGPlayerState_Playing);
            setPlayingSubState(ZGPlayingSubState.ZGPlayingSubState_PlayBegin);
        }

        @Override
        public void onVideoBegin(int i) {
            Log.v(TAG, "onVideoBegin");
        }

        @Override
        public void onAudioBegin(int i) {
            Log.v(TAG, "onAudioBegin");
        }

        @Override
        public void onPlayEnd(int i) {
            setPlayerState(ZGPlayerState.ZGPlayerState_Stopped);
        }

        @Override
        public void onBufferBegin(int i) {
            Log.v(TAG, "onBufferBegin");
            if (zgPlayerState == ZGPlayerState.ZGPlayerState_Playing) {
                setPlayingSubState(ZGPlayingSubState.ZGPlayingSubState_Buffering);
            } else {
                assert (false);
            }
        }

        @Override
        public void onBufferEnd(int i) {
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
        public void onSeekComplete(int code, long millisecond,int i) {
            Log.v(TAG, "onSeekComplete");
        }

        @Override
        public void onSnapshot(Bitmap bitmap,int i) {
            
        }

        @Override
        public void onLoadComplete(int i) {

        }

        @Override
        public void onProcessInterval(long l,int i) {
            currentProgress = (int) l;
            updateProgressDesc();
        }

        @Override
        public void onReadEOF(int i) {

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
