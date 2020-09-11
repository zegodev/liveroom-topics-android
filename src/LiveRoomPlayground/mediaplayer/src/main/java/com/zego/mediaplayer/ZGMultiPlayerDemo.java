package com.zego.mediaplayer;

import android.graphics.Bitmap;

import com.zego.zegoavkit2.IZegoMediaPlayerWithIndexCallback;
import com.zego.zegoavkit2.ZegoMediaPlayer;
import com.zego.zegoavkit2.ZegoVideoDataFormat;

public class ZGMultiPlayerDemo implements IZegoMediaPlayerWithIndexCallback {

    static private ZGMultiPlayerDemo zgMultiPlayerDemo;

    public static ZGMultiPlayerDemo sharedInstance() {
        synchronized (ZGMultiPlayerDemo.class) {
            if (zgMultiPlayerDemo == null) {
                zgMultiPlayerDemo = new ZGMultiPlayerDemo();
            }
        }
        return zgMultiPlayerDemo;
    }

    public enum ZGPlayerStateType {
        ZGPlayerStateType_Start,
        ZGPlayerStateType_Stop,
        ZGPlayerStateType_End
    }

    public enum ZGPlayerIndex {
        ZGPlayerIndex_First,
        ZGPlayerIndex_Second,
        ZGPlayerIndex_Third
    }

    private ZGMultiPlayerDemoCallback zgMultiPlayerDemoCallback;

    private ZegoMediaPlayer zegoMediaPlayerFirst;
    private ZegoMediaPlayer zegoMediaPlayerSecond;
    private ZegoMediaPlayer zegoMediaPlayerThird;

    public ZegoMediaPlayer createZegoMediaPlayer(ZGPlayerIndex index) {

        if (ZGPlayerIndex.ZGPlayerIndex_First == index) {
            if (zegoMediaPlayerFirst == null) {
                zegoMediaPlayerFirst = new ZegoMediaPlayer();
                zegoMediaPlayerFirst.init(ZegoMediaPlayer.PlayerTypePlayer, ZegoMediaPlayer.PlayerIndex.First);
                zegoMediaPlayerFirst.setEventWithIndexCallback(this);
            }
            return zegoMediaPlayerFirst;

        } else if (ZGPlayerIndex.ZGPlayerIndex_Second == index) {
            if (zegoMediaPlayerSecond == null) {
                zegoMediaPlayerSecond = new ZegoMediaPlayer();
                zegoMediaPlayerSecond.init(ZegoMediaPlayer.PlayerTypePlayer, ZegoMediaPlayer.PlayerIndex.Second);
                zegoMediaPlayerSecond.setEventWithIndexCallback(this);
            }
            return zegoMediaPlayerSecond;

        } else if (ZGPlayerIndex.ZGPlayerIndex_Third == index) {
            if (zegoMediaPlayerThird == null) {
                zegoMediaPlayerThird = new ZegoMediaPlayer();
                zegoMediaPlayerThird.init(ZegoMediaPlayer.PlayerTypePlayer, ZegoMediaPlayer.PlayerIndex.Third);
                zegoMediaPlayerThird.setEventWithIndexCallback(this);
            }
            return zegoMediaPlayerThird;

        } else {
            return null;
        }
    }

    /* 释放MediaPlayer 和一些相关操作 */
    public void unInit() {

        if (zegoMediaPlayerFirst != null) {
            zegoMediaPlayerFirst.stop();
            zegoMediaPlayerFirst.setVideoPlayWithIndexCallback(null, ZegoVideoDataFormat.PIXEL_FORMAT_NV21);
            zegoMediaPlayerFirst.uninit();
            zegoMediaPlayerFirst = null;
        }
        if (zegoMediaPlayerSecond != null) {
            zegoMediaPlayerSecond.stop();
            zegoMediaPlayerSecond.setVideoPlayWithIndexCallback(null, ZegoVideoDataFormat.PIXEL_FORMAT_NV21);
            zegoMediaPlayerSecond.uninit();
            zegoMediaPlayerSecond = null;
        }
        if (zegoMediaPlayerThird != null) {
            zegoMediaPlayerThird.stop();
            zegoMediaPlayerThird.setVideoPlayWithIndexCallback(null, ZegoVideoDataFormat.PIXEL_FORMAT_NV21);
            zegoMediaPlayerThird.uninit();
            zegoMediaPlayerThird = null;
        }

        zgMultiPlayerDemo = null;
    }

    // 设置播放器回调代理
    public void setZGMultiPlayerDemoCallback(ZGMultiPlayerDemoCallback zgMediaPLayerCallback) {
        this.zgMultiPlayerDemoCallback = zgMediaPLayerCallback;
    }

    public void unSetZGMultiPlayerDemoCallback() {
        this.zgMultiPlayerDemoCallback = null;
    }

    public interface ZGMultiPlayerDemoCallback {
        void onPlayerState(ZGPlayerStateType type, int index);

        void onPlayerError(int errorcode, int index);
    }

    @Override
    public void onPlayStart(int index) {
        if (zgMultiPlayerDemoCallback != null) {
            zgMultiPlayerDemoCallback.onPlayerState(ZGPlayerStateType.ZGPlayerStateType_Start, index);
        }
    }

    @Override
    public void onPlayPause(int index) {

    }

    @Override
    public void onPlayStop(int index) {
        if (zgMultiPlayerDemoCallback != null) {
            zgMultiPlayerDemoCallback.onPlayerState(ZGPlayerStateType.ZGPlayerStateType_Stop, index);
        }
    }

    @Override
    public void onPlayResume(int i) {

    }

    @Override
    public void onPlayError(int errorCode, int index) {
        if (zgMultiPlayerDemoCallback != null) {
            zgMultiPlayerDemoCallback.onPlayerError(errorCode, index);
        }
    }

    @Override
    public void onVideoBegin(int i) {

    }

    @Override
    public void onAudioBegin(int i) {

    }

    @Override
    public void onPlayEnd(int index) {
        if (zgMultiPlayerDemoCallback != null) {
            zgMultiPlayerDemoCallback.onPlayerState(ZGPlayerStateType.ZGPlayerStateType_End, index);
        }
    }

    @Override
    public void onBufferBegin(int i) {

    }

    @Override
    public void onBufferEnd(int i) {

    }

    @Override
    public void onSeekComplete(int i, long l, int i1) {

    }

    @Override
    public void onSnapshot(Bitmap bitmap, int i) {

    }

    @Override
    public void onLoadComplete(int i) {

    }

    @Override
    public void onProcessInterval(long l, int i) {

    }

    @Override
    public void onReadEOF(int i) {

    }
}
