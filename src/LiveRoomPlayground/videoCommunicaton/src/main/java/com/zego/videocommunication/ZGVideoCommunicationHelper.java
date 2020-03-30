package com.zego.videocommunication;

import android.app.Application;
import android.support.annotation.NonNull;
import android.view.TextureView;
import android.view.View;

import com.zego.common.util.AppLogger;
import com.zego.common.util.ZegoUtil;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.IZegoInitSDKCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.ArrayList;
import java.util.HashMap;

import static com.zego.videocommunication.ZGVideoCommunicationHelper.ZGVideoCommunicationHelperCallback.NUMBER_OF_PEOPLE_EXCEED_LIMIT;


/**
 * 本类为 VideoCommunication 专题的帮助类，视频通话场景需要关注的SDK的API接口, 集成型开发者可以考虑直接复制本类到自己项目中
 *
 */
public class ZGVideoCommunicationHelper {

    public boolean getZgMicState() {
        return mZgMicState;
    }

    private void setZgMicState(boolean zgMicState) {
        this.mZgMicState = zgMicState;
    }

    public boolean getZgCameraState() {
        return mZgCameraState;
    }

    private void setZgCameraState(boolean zgCameraState) {
        mZgCameraState = zgCameraState;
    }

    private boolean mZgMicState = true;
    private boolean mZgCameraState = true;

    private ZGVideoCommunicationHelperCallback mCallback;

    // 记录SDK的初始化状态
    private ZGSDKInitState zgsdkInitState = ZGSDKInitState.WaitInitState;

    private enum ZGSDKInitState {
        WaitInitState, // 等待初始化状态
        InitSuccessState, // 初始化完成状态
        InitFailureState
    }

    private ZGVideoCommunicationHelper(){
    }

    public void initZGVideoCommunicationHelper(){
        // 在进入当前Activity之后马上初始化SDK
        ZGVideoCommunicationHelper.sharedInstance().initZegoSDK(ZegoUtil.getAppID(), ZegoUtil.getAppSign(), ZegoUtil.getIsTestEnv());

    }

    private ZGSDKInitState getZgsdkInitState() {
        return zgsdkInitState;
    }

    private void setZgsdkInitState(ZGSDKInitState zgsdkInitState) {
        this.zgsdkInitState = zgsdkInitState;
    }

    private static ZGVideoCommunicationHelper zgVideoCommunicationHelper;

    /**
      当前示例专题的 VideoCommunication 的 Helper 单例

      @return ZGVideoCommunicationHelper 单例对象
     */
    public static ZGVideoCommunicationHelper sharedInstance() {
        if (zgVideoCommunicationHelper == null) {
            synchronized (ZGVideoCommunicationHelper.class) {
                zgVideoCommunicationHelper = new ZGVideoCommunicationHelper();
            }
        }
        return zgVideoCommunicationHelper;
    }

    private ZegoLiveRoom zegoLiveRoom = null;

    /**
     * 获取 ZegoLiveRoom 实例
     *
     * @return zegoLiveRoom 实例，如果为 null 说明没有初始化 sdk
     */
    private ZegoLiveRoom getZegoLiveRoom() {
        return zegoLiveRoom;
    }

    /**
     * 初始化 Zego sdk
     * 帮助简化 Zego  sdk初始化流程。
     * 建议在 App 中的 {@link Application#onCreate()} 中去初始化sdk。
     *
     * @param appID    Zego appID, 可通过 <a>https://console.zego.im/acount/login</a> 申请 appID
     * @param appSign  Zego 分配的签名, 用来校验对应appID的合法性。 可通过 <a>https://console.zego.im/acount/login</a> 申请 appID 与 appSign
     * @param testEnv  注意!!! 如果没有向 Zego 申请正式环境的 appID, 则需设置成测试环境, 否则SDK会初始化失败
     * @return true 为调用成功，false 为调用失败
     */
    private boolean initZegoSDK(final long appID, byte[] appSign, boolean testEnv) {

        if (zegoLiveRoom == null) {
            zegoLiveRoom = new ZegoLiveRoom();
        }

        if (getZgsdkInitState() == ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState) {
            AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "sdk已初始化, 无需重复初始化");
            return false;
        }

        // 需要在initSDK之前设置sdk环境。
        ZegoLiveRoom.setTestEnv(testEnv);

        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "设置sdk测试环境 testEnv : %b", testEnv);

        // 初始化sdk
        boolean initSDKResults = zegoLiveRoom.initSDK(appID, appSign, new IZegoInitSDKCompletionCallback() {
            @Override
            public void onInitSDK(int i) {

                // 初始化完成
                if (i == 0) {
                    setZgsdkInitState(ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState);
                    AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "初始化SDK "+ appID + " 成功");

                    // 多人实时视频通话由于人多的时候性能原因，这里设置较低的分辨率
                    setVideoQuality(90, 160);

                } else {
                    setZgsdkInitState(ZGVideoCommunicationHelper.ZGSDKInitState.InitFailureState);
                    AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "初始化SDK "+ appID + " 失败");
                    // 当初始化失败时释放SDK, 避免下次再次初始化SDK会收不到回调
                    unInitZegoSDK();
                }

            }
        });

        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "初始化zegoSDK");
        return initSDKResults;
    }

    private void setVideoQuality(int width, int height){
        ZegoAvConfig mZegoAvConfig = new ZegoAvConfig(ZegoAvConfig.Level.VeryLow);
        mZegoAvConfig.setVideoEncodeResolution(width, height);
        mZegoAvConfig.setVideoCaptureResolution(width, height);
        mZegoAvConfig.setVideoFPS(15);
        zegoLiveRoom.setAVConfig(mZegoAvConfig);
    }


    /**
     * 开始实时视频通话
     * <p>
     * 注意!!! sdk的推拉流以及信令服务等常用功能都需要登录房间后才能使用,
     * 在退出房间时必须要调用{@link #logoutRoom()}退出房间。
     *
     * @param roomID 房间号 App 需保证房间 ID 信息的全局唯一，只支持长度不超过 128 byte 的数字，下划线，字母。
     *               每个房间 ID 代表着一个房间。
     * @return true 为调用成功，false 为调用失败
     */
    public void startVideoCommunication(String roomID, TextureView localPreviewView, String publishStreamid){

        if (getZgsdkInitState() != ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState) {
            AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "登陆失败: 请先InitSdk");
            return;
        }
        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "开始登陆房间!");
        zegoLiveRoom.loginRoom(roomID, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int i, ZegoStreamInfo[] zegoStreamInfos) {
                // zegoStreamInfos，内部封装了 userID、userName、streamID 和 extraInfo。
                // 登录房间成功后, 开发者可通过 zegoStreamInfos 获取到当前房间推流信息。便于后续的拉流操作
                // 当 zegoStreamInfos 为 null 时说明当前房间没有人推流

                if(0 == i){

                    if(zegoStreamInfos.length < 12){
                        for(ZegoStreamInfo zegoStreamInfo : zegoStreamInfos){
                            AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "房间内收到流新增通知. streamID : %s, userName : %s, extraInfo : %s", zegoStreamInfo.streamID, zegoStreamInfo.userName, zegoStreamInfo.extraInfo);

                            TextureView playRenderView =  mCallback.addRenderViewByStreamAdd(zegoStreamInfo);
                            ZGVideoCommunicationHelper.sharedInstance().startPlaying(zegoStreamInfo.streamID, playRenderView);
                            ZGVideoCommunicationHelper.sharedInstance().setPlayViewMode(ZegoVideoViewMode.ScaleAspectFill, zegoStreamInfo.streamID);
                        }
                    }else {
                        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "房间已满人，目前demo只展示12人通讯");
                        mCallback.onLoginRoomFailed(NUMBER_OF_PEOPLE_EXCEED_LIMIT);
                    }


                }else {

                    mCallback.onLoginRoomFailed(i);

                }
            }
        });

        ZGVideoCommunicationHelper.sharedInstance().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
        ZGVideoCommunicationHelper.sharedInstance().startPreview(localPreviewView);
        ZGVideoCommunicationHelper.sharedInstance().startPublishing(publishStreamid, publishStreamid + "-title", ZegoConstants.PublishFlag.JoinPublish);


    }

    /**
     * 释放 zegoSDK
     * 当开发者不再需要使用到 sdk 时, 可以释放 sdk。
     * 注意!!! 请根据业务需求来释放 sdk。
     * <p>
     *
     * @return true 为调用成功，false 为调用失败
     */
    private boolean unInitZegoSDK() {
        boolean isUnitSDK = false;
        if (zegoLiveRoom != null) {
            setZgsdkInitState(ZGSDKInitState.WaitInitState);
            isUnitSDK = zegoLiveRoom.unInitSDK();
            AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "释放zego SDK!");
            zegoLiveRoom = null;
        }
        return isUnitSDK;
    }

    /**
     * 登出房间
     * 注意!!! 停止所有的推流和拉流后，才能执行 logoutRoom
     * 否则会影响房间业务.
     *
     * @return true 为调用成功，false 为调用失败
     */
    private boolean logoutRoom() {
        if (zegoLiveRoom != null) {
            AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "退出房间");
            zegoLiveRoom.setZegoRoomCallback(null);
            return zegoLiveRoom.logoutRoom();
        }
        return false;
    }


    /**
     * SDK 的接口 setZegoRoomCallback 的参数 IZegoRoomCallback 的封装，由于本示例专题中只关注房间内流变化的情况，所以该封装的接口只关注 onStreamUpdated
     *
     */
    public interface ZGVideoCommunicationHelperCallback {

        int NUMBER_OF_PEOPLE_EXCEED_LIMIT = 12;

        TextureView addRenderViewByStreamAdd(ZegoStreamInfo listStream);
        void removeRenderViewByStreamDelete(ZegoStreamInfo listStream);
        void onLoginRoomFailed(int errorcode);
        void onPublishStreamFailed(int errorcode);
    }

    /**
     * 设置本专题的向UI层抛出的代理
     *
     * @param callback 本示例专题Helper的代理
     */
    public void setZGVideoCommunicationHelperCallback(final ZGVideoCommunicationHelperCallback callback) {

        mCallback = callback;

        if (getZgsdkInitState() != ZGSDKInitState.InitSuccessState) {
            AppLogger.getInstance().w(ZGVideoCommunicationHelper.class, "设置房间代理失败! SDK未初始化, 请先初始化SDK");
            return;
        }

        /**
         * 通过设置sdk房间代理，可以收到房间内的一些信息回调。
         * 开发者可以按自己的需求在回调里实现自己的业务
         */
        zegoLiveRoom.setZegoRoomCallback(new IZegoRoomCallback() {

            /**
             * 被踢出房间的回调，一般为重复的userid导致，本示例专题demo中不关注，业务可以根据自己的情况关注并做处理
             *
             * @param reason 被踢原因
             * @param roomID 房间id
             */
            @Override
            public void onKickOut(int reason, String roomID, String customReason) {
                AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "您已被踢出房间 reason : %d, roomID : %s", reason, roomID);
                // 原因，16777219 表示该账户多点登录被踢出，16777220 表示该账户是被手动踢出，16777221 表示房间会话错误被踢出
                // 注意!!! 业务侧确保分配的userID保持唯一，不然会造成互相抢占。

            }

            /**
             * 断网的回调，一般为客户端网络问题导致，本示例专题demo为演示用，并不关注，业务可以根据自己的情况关注并做处理
             *
             * @param errorCode 错误码
             * @param roomID 房间id
             */
            @Override
            public void onDisconnect(int errorCode, String roomID) {
                AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "房间与server断开连接 errorCode : %d, roomID : %s", errorCode, roomID);
                // 原因，16777219 网络断开。 断网90秒仍没有恢复后会回调这个错误，onDisconnect后会停止推流和拉流
                mCallback.onPublishStreamFailed(errorCode);
            }

            /**
             * SDK内部网络重连时的回调，一般为客户端网络问题断开之后会触发，本示例专题demo为演示用，并不关注，业务可以根据自己的情况关注并做处理
             *
             * @param errorCode 错误码
             * @param roomID 房间
             */
            @Override
            public void onReconnect(int errorCode, String roomID) {
                AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "房间与server重新连接. roomID : %s", roomID);
            }

            /**
             * SDK内部网络临时中断时的回调，一般为客户端网络问题断开之后会触发，本示例专题demo为演示用，并不关注，业务可以根据自己的情况关注并做处理
             *
             * @param errorCode 错误码
             * @param roomID 房间id
             */
            @Override
            public void onTempBroken(int errorCode, String roomID) {
                AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "房间与server中断，SDK会尝试自动重连. roomID : %s", roomID);
            }

            /**
             * 房间内推流变化的回调，开发者应监听此回调来在视频通话场景中拉别人的流或停拉别人的流
             *
             * @param type 流增加还是流减少的类型
             * @param listStream 变化的流列表
             * @param roomID 房间id
             */
            @Override
            public void onStreamUpdated(int type, ZegoStreamInfo[] listStream, String roomID) {
                // 当登陆房间成功后，如果房间内中途有人推流或停止推流。房间内其他人就能通过该回调收到流更新通知。
                for (ZegoStreamInfo streamInfo : listStream) {
                    if (type == ZegoConstants.StreamUpdateType.Added) {

                        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "房间内收到流新增通知. streamID : %s, userName : %s, extraInfo : %s", streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);
                        TextureView playRenderView =  callback.addRenderViewByStreamAdd(streamInfo);
                        ZGVideoCommunicationHelper.sharedInstance().startPlaying(streamInfo.streamID, playRenderView);
                        ZGVideoCommunicationHelper.sharedInstance().setPlayViewMode(ZegoVideoViewMode.ScaleAspectFill, streamInfo.streamID);



                    } else if (type == ZegoConstants.StreamUpdateType.Deleted) {
                        ZGVideoCommunicationHelper.sharedInstance().stopPlaying(streamInfo.streamID);

                        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "房间内收到流删除通知. streamID : %s, userName : %s, extraInfo : %s", streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);
                        callback.removeRenderViewByStreamDelete(streamInfo);

                    }

                }

            }

            /**
             * 流额外信息更新的回调，本示例专题demo为演示用，并不关注，业务可以根据自己的情况关注并做处理
             *
             * @param zegoStreamInfos 流额外信息的流列表
             * @param roomID 房间id
             */
            @Override
            public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String roomID) {
                // 开发者可以通过流额外信息回调，来实现主播设备状态同步的功能，
                // 比如主播关闭麦克风，这个时候主播可以通过更新流额外信息发送主播当前的设备状态
                // 观众则可以通过此回调拿到流额外信息，更新主播设备状态。
                for (ZegoStreamInfo streamInfo : zegoStreamInfos) {
                    AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "房间内收到流额外信息更新. streamID : %s, userName : %s, extraInfo : %s", streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);
                }

            }

            /**
             * 接收到自定义消息的回调，本示例专题demo为演示用，并不关注，业务可以根据自己的情况关注并做处理
             *
             * @param userID 发送自定义消息的用户id
             * @param userName 发送自定义消息的用户名
             * @param content 自定义消息的内容
             * @param roomID 房间id
             */
            @Override
            public void onRecvCustomCommand(String userID, String userName, String content, String roomID) {
                AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "收到自定义消息. userID : %s, userID : %s, content : %s, roomID : %s", userID, userName, content, roomID);
            }
        });

        /**
         * 通过设置sdk推流代理，可以收到推流相关的一些信息回调。
         * 开发者可以按自己的需求在回调里实现自己的业务
         */
        zegoLiveRoom.setZegoLivePublisherCallback(new IZegoLivePublisherCallback() {

            /**
             * 推流结果的回调
             * @param i 推流结果的状态码
             * @param s 推流的流id
             * @param hashMap 推流结果的一些信息
             */
            @Override
            public void onPublishStateUpdate(int i, String s, HashMap<String, Object> hashMap) {
                if(i == 0){

                }else{
                    AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "推流 "+ s +" 失败，请检查网络");
                    mCallback.onPublishStreamFailed(i);
                }

            }

            @Override
            public void onJoinLiveRequest(int i, String s, String s1, String s2) {

            }

            @Override
            public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {

            }

            @Override
            public void onCaptureVideoSizeChangedTo(int i, int i1) {

            }

            @Override
            public void onCaptureVideoFirstFrame() {

            }

            @Override
            public void onCaptureAudioFirstFrame() {

            }
        });

    }

    /**
     * 释放代理对象，防止内存泄露
     * 当不再使用ZegoSDK时，可以释放房间的代理
     * <p>
     * 调用时机：建议在unInitSDK之前设置。
     *
     */
    public void releaseZGVideoCommunicationHelperCallback() {
        zegoLiveRoom.setZegoRoomCallback(null);
        zegoLiveRoom.setZegoLivePublisherCallback(null);
    }

    /**
     * 开始预览
     * 可以调用{@link ZegoLiveRoom#setPreviewView(Object)} 更新渲染视图
     *
     * @param view 要渲染的视图，sdk会把采集到的数据渲染到view上,
     */
    private void startPreview(@NonNull View view) {

        if (getZgsdkInitState() != ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState) {
            AppLogger.getInstance().w(ZGVideoCommunicationHelper.class, "推流预览失败, 请先初始化sdk");
            return;
        }
        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "开始预览");
        ZegoLiveRoom zegoLiveRoom = ZGVideoCommunicationHelper.sharedInstance().getZegoLiveRoom();
        zegoLiveRoom.setPreviewView(view);
        zegoLiveRoom.startPreview();
    }

    /**
     * 开始推流
     * 注意!!! 登陆房间后才能使用推流接口，该接口要与 {@link #stopPublishing()} 成对使用。
     * <li><a href="https://doc.zego.im/CN/490.html"> 推流常见问题 </a>
     *
     * @param streamID streamID，不能为空，只支持长度不超过 256 byte 的数字，下划线，字母。
     *                 注意!!! 每个用户的流名必须保持唯一，也就是流名必须appID全局唯一，
     *                 也不能包含特殊字符。
     * @param title    标题，长度不可超过 255 byte
     * @param flag     推流标记, 详见 {@link com.zego.zegoliveroom.constants.ZegoConstants.PublishFlag}
     * @return true 为推流成功 false 为推流失败
     */
    private boolean startPublishing(@NonNull String streamID, @NonNull String title, int flag) {
        if (getZgsdkInitState() != ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState) {
            AppLogger.getInstance().w(ZGVideoCommunicationHelper.class, "推流失败, 请先初始化sdk再进行推流");
            return false;
        }
        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "开始推流, streamID : %s, title : %s, flag : %s", streamID, title, flag);
        ZegoLiveRoom zegoLiveRoom = ZGVideoCommunicationHelper.sharedInstance().getZegoLiveRoom();
        return zegoLiveRoom.startPublishing(streamID, title, flag);
    }

    /**
     * 停止推流
     */
    private void stopPublishing() {
        if (getZgsdkInitState() != ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState) {
            AppLogger.getInstance().w(ZGVideoCommunicationHelper.class, "停止推流失败, 请先初始化sdk");
            return;
        }
        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "停止推流");
        ZGVideoCommunicationHelper.sharedInstance().getZegoLiveRoom().stopPublishing();
    }

    /**
     * 停止预览
     *
     * 注意!!! 停止预览后并不会停止推流，需要停止推流请调用 {@link #stopPublishing()}
     */
    private void stopPreviewView() {
        if (getZgsdkInitState() != ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState) {
            AppLogger.getInstance().w(ZGVideoCommunicationHelper.class, "停止预览失败, 请先初始化sdk");
            return;
        }
        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "停止预览");
        ZGVideoCommunicationHelper.sharedInstance().getZegoLiveRoom().stopPreview();
    }

    /**
     * 停止拉流
     *
     * @param streamID 不能为null。
     */
    private void stopPlaying(@NonNull String streamID) {
        if (getZgsdkInitState() == ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState) {
            AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "停止拉流:"+streamID);

            ZGVideoCommunicationHelper.sharedInstance().getZegoLiveRoom().stopPlayingStream(streamID);
        }
    }


    /**
     * 开始拉流
     *
     * @param streamID 同一房间内对应推流端的streamID, sdk基于streamID进行拉流
     * @param playView 用于渲染视频的view, 推荐使用 TextureView, 支持 SurfaceView 或 Surface
     * @return true 为调用成功, false为调用失败
     */
    private boolean startPlaying(@NonNull String streamID, View playView) {
        if (getZgsdkInitState() == ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState) {
            AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "开始拉流, streamID : %s", streamID);
            ZegoLiveRoom zegoLiveRoom = ZGVideoCommunicationHelper.sharedInstance().getZegoLiveRoom();
            return zegoLiveRoom.startPlayingStream(streamID, playView);
        } else {
            AppLogger.getInstance().w(ZGVideoCommunicationHelper.class, "拉流失败! SDK未初始化, 请先初始化SDK");
        }
        return false;
    }


    /**
     * 设置拉流预览视图模式
     * sdk内置3种视图模式
     * 1: 等比缩放填充整View，可能有部分被裁减。 SDK 默认值。{@link com.zego.zegoliveroom.constants.ZegoVideoViewMode#ScaleAspectFill}
     * 2: 等比缩放，可能有黑边 {@link com.zego.zegoliveroom.constants.ZegoVideoViewMode#ScaleAspectFit}
     * 3: 填充整个View，视频可能会变形。{@link com.zego.zegoliveroom.constants.ZegoVideoViewMode#ScaleToFill}
     * <p>
     * 调用时机: 拉流之后
     *
     * @param viewMode 视图模式 {@link com.zego.zegoliveroom.constants.ZegoVideoViewMode}
     * @param streamID 开发者需要改变哪条streamID视图就传对应的streamID
     */
    private void setPlayViewMode(int viewMode, String streamID) {
        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "设置拉流视图模式 viewMode : %d, streamID : %s", viewMode, streamID);
        if (getZgsdkInitState() == ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState) {
            ZegoLiveRoom zegoLiveRoom = ZGVideoCommunicationHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.setViewMode(viewMode, streamID);
        }
    }

    /**
     * 设置推流预览视图模式
     * sdk内置3种视图模式
     * 1: 等比缩放填充整View，可能有部分被裁减。 SDK 默认值。{@link com.zego.zegoliveroom.constants.ZegoVideoViewMode#ScaleAspectFill}
     * 2: 等比缩放，可能有黑边 {@link com.zego.zegoliveroom.constants.ZegoVideoViewMode#ScaleAspectFit}
     * 3: 填充整个View，视频可能会变形。{@link com.zego.zegoliveroom.constants.ZegoVideoViewMode#ScaleToFill}
     * <p>
     * <p>
     * 调用时机: 无要求
     *
     * @param viewMode 视图模式 {@link com.zego.zegoliveroom.constants.ZegoVideoViewMode}
     */
    private void setPreviewViewMode(int viewMode) {
        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, "设置预览视图模式 viewMode : %d", viewMode);
        if (getZgsdkInitState() == ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState) {
            ZegoLiveRoom zegoLiveRoom = ZGVideoCommunicationHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.setPreviewViewMode(viewMode);
        }
    }

    /**
     * 启用摄像头
     * <p>
     * 调用时机: 初始化之后
     *
     * @param enable true 为开启, false 为关闭
     */
    public void enableCamera(boolean enable) {
        if (getZgsdkInitState() == ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState) {
            AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, enable ? "启用摄像头" : "关闭摄像头");
            ZegoLiveRoom zegoLiveRoom = ZGVideoCommunicationHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.enableCamera(enable);
            this.setZgCameraState(enable);
        }
    }

    /**
     * 启用麦克风
     * <p>
     * 调用时机: 初始化之后
     *
     * @param enable true 为启用麦克风
     */
    public void enableMic(boolean enable) {
        AppLogger.getInstance().i(ZGVideoCommunicationHelper.class, enable ? "启用麦克风" : "关闭麦克风");
        if (getZgsdkInitState() == ZGVideoCommunicationHelper.ZGSDKInitState.InitSuccessState) {
            ZegoLiveRoom zegoLiveRoom = ZGVideoCommunicationHelper.sharedInstance().getZegoLiveRoom();
            zegoLiveRoom.enableMic(enable);
            this.setZgMicState(enable);
        }
    }

    /**
     *
     *
     * @param playStreamids 退出时应传入正在拉流的流id来停止拉流
     */
    public void quitVideoCommunication(ArrayList<String> playStreamids){
        ZGVideoCommunicationHelper.sharedInstance().stopPublishing();
        ZGVideoCommunicationHelper.sharedInstance().stopPreviewView();
        for(String playStreamid : playStreamids){
            ZGVideoCommunicationHelper.sharedInstance().stopPlaying(playStreamid);
        }
        ZGVideoCommunicationHelper.sharedInstance().logoutRoom();
    }

    /**
     * 完全退出专题时需要做的释放动作
     *
     */
    public void releaseZGVideoCommunicationHelper(){
        stopPublishing();
        logoutRoom();
        releaseZGVideoCommunicationHelperCallback();
        unInitZegoSDK();

    }

}
