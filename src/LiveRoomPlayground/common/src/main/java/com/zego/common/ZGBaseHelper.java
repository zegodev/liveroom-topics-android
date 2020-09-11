package com.zego.common;


import android.app.Application;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.zego.common.util.AppLogger;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.IZegoInitSDKCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLogHookCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoRoomCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

/**
 * ZGBaseHelper
 * <p>
 * 此类将SDK 初始化与房间登录等基本接口进行封装
 * 开发者可参考该类的代码, 理解 SDK 接口
 */
public class ZGBaseHelper {

    private ZegoLiveRoom zegoLiveRoom = null;

    public static ZGBaseHelper zgBaseHelper;

    private ZGBaseState zgBaseState = ZGBaseState.WaitInitState;

    public String userID = "";
    public String userName = "";

    public enum ZGBaseState {
        WaitInitState, // 等待初始化状态
        InitSuccessState, // 初始化完成状态
        InitFailureState
    }

    public void setZGBaseState(ZGBaseState zgBaseState) {
        this.zgBaseState = zgBaseState;
    }

    public ZGBaseState getZGBaseState() {

        return zgBaseState;
    }

    public static ZGBaseHelper sharedInstance() {
        if (zgBaseHelper == null) {
            synchronized (ZGPublishHelper.class) {
                if (zgBaseHelper == null) {
                    zgBaseHelper = new ZGBaseHelper();
                }
            }
        }
        return zgBaseHelper;
    }

    /**
     * 获取 ZegoLiveRoom 实例
     *
     * @return zegoLiveRoom 实例，如果为 null 说明没有初始化 sdk
     */
    public ZegoLiveRoom getZegoLiveRoom() {
        return zegoLiveRoom;
    }


    /**
     * 注意!!! 一定要在初始化 ZegoSDK {@link #initZegoSDK} 之前先设置好 sdk 上下文
     * <p>
     * 建议在app中的 {@link Application#onCreate()} 中调用。
     *
     * @param userID      注意!!! 必须保证 userID 的唯一性。可与app业务后台账号进行关联，
     *                    userID 还能便于 zego 技术支持帮忙查找定位线上问题，请定义成一个有意义的 userID.
     * @param userName    用户昵称, 与 userID 关联的用户昵称。
     * @param logPath     sdk 日志存储路径，设置 null 则将日志文件存储到 sdcard/Android/data/ 包名目录下.
     *                    注意!!! 应用必须具备存取该目录的权限
     * @param soFullPath  null 表示使用默认方式加载 libzegoliveroom.so,
     *                    可通过外部加载 .so 文件时使用，如将 .so 存放到服务器上，等 app 运行时再加载，以降低 apk 尺寸
     *                    注意!!!  1. SDK 会优先使用 APK 自带的 libzegoliveroom.so；
     *                    2. 目前只支持本地文件路径，不支持网络路径；
     *                    3. 请确保文件有可读权限。
     * @param logFileSize 日志文件大小，必须在 [5M, 100M] 之间。设置 0 则不存储日志。
     * @param application android上下文.
     */
    public void setSDKContextEx(String userID, String userName, final String logPath, final String soFullPath, final long logFileSize, final Application application) {

        this.userID = userID;
        this.userName = userName;
        // 初始化之前必须先 setContext
        // 用于设置 SDK 上下文，如日志路径，Application Context 等，同时检查 so 库是否成功加载。
        ZegoLiveRoom.setSDKContext(new ZegoLiveRoom.SDKContextEx() {

            @Override
            public long getLogFileSize() {
                return logFileSize;  // 单个日志文件的大小，必须在 [5M, 100M] 之间。当返回 0 时，取默认大小 5M
            }

            @Override
            public String getSoFullPath() {
                return soFullPath; // return null 表示使用默认方式加载 libzegoliveroom.so
                // 此处可以返回 so 的绝对路径，用来指定从这个位置加载 libzegoliveroom.so，确保应用具备存取此路径的权限
            }

            @Override
            public String getLogPath() {
                return logPath; // 表示日志文件会存储到默认位置，如果返回非空，则将日志文件存储到该路径下，注意应用必须具备存取该目录的权限
            }


            @Override
            public Application getAppContext() {
                return application;
            }

            @Override
            @Nullable
            public String getSubLogFolder(){
                return null;
            }

            @Override
            public IZegoLogHookCallback getLogHookCallback() {
                return null;
            }

            ;
        });


        AppLogger.getInstance().i(ZGBaseHelper.class, "setSDKContext");

        // 需要提前设置用户信息, 便于后续使用 zego 房间服务
        // userID 还能便于 Zego 技术支持帮忙查找定位问题, 所以一定要设置成一个有意义的值
        ZegoLiveRoom.setUser(userID, userName);

        AppLogger.getInstance().i(ZGBaseHelper.class, "设置用户信息 userID : %s, userName : %s", userID, userName);
    }

    /**
     * 初始化 Zego sdk
     * 帮助简化 Zego  sdk初始化流程。
     * 建议在 App 中的 {@link Application#onCreate()} 中去初始化sdk。
     *
     * @param appID    Zego appID, 可通过 <a>https://console.zego.im/acount/login</a> 申请 appID
     * @param appSign  Zego 分配的签名, 用来校验对应appID的合法性。 可通过 <a>https://console.zego.im/acount/login</a> 申请 appID 与 appSign
     * @param testEnv  注意!!! 如果没有向 Zego 申请正式环境的 appID, 则需设置成测试环境, 否则SDK会初始化失败
     * @param callback 初始化sdk代理, 初始化结果会在{@link IZegoInitSDKCompletionCallback#onInitSDK(int)} 中回调。
     *                 返回 非0 代表初始化sdk失败, 具体错误码说明请查看<a> https://doc.zego.im/CN/308.html </a>
     * @return true 为调用成功，false 为调用失败
     */
    public boolean initZegoSDK(long appID, byte[] appSign, boolean testEnv, final IZegoInitSDKCompletionCallback callback) {

        if (zegoLiveRoom == null) {
            zegoLiveRoom = new ZegoLiveRoom();
        }

        if (getZGBaseState() == ZGBaseState.InitSuccessState) {
            AppLogger.getInstance().i(ZGBaseHelper.class, "sdk已初始化, 无需重复初始化");
            return false;
        }

        // 需要在initSDK之前设置sdk环境。
        ZegoLiveRoom.setTestEnv(testEnv);

        AppLogger.getInstance().i(ZGBaseHelper.class, "设置sdk测试环境 testEnv : %b", testEnv);

        // 初始化sdk
        boolean initSDKResults = zegoLiveRoom.initSDK(appID, appSign, new IZegoInitSDKCompletionCallback() {
            @Override
            public void onInitSDK(int i) {

                // 初始化完成
                if (i == 0) {
                    setZGBaseState(ZGBaseState.InitSuccessState);
                } else {
                    setZGBaseState(ZGBaseState.InitFailureState);

                    // 当初始化失败时释放SDK, 避免下次再次初始化SDK会收不到回调
                    unInitZegoSDK();
                }

                if (callback != null) {
                    callback.onInitSDK(i);
                }
            }
        });

        AppLogger.getInstance().i(ZGBaseHelper.class, "初始化zegoSDK");
        return initSDKResults;
    }

    /**
     * 释放 zegoSDK
     * 当开发者不再需要使用到 sdk 时, 可以释放 sdk。
     * 注意!!! 请根据业务需求来释放 sdk。
     * <p>
     *
     * @return true 为调用成功，false 为调用失败
     */
    public boolean unInitZegoSDK() {
        boolean isUnitSDK = false;
        if (zegoLiveRoom != null) {
            setZGBaseState(ZGBaseState.WaitInitState);
            isUnitSDK = zegoLiveRoom.unInitSDK();
            AppLogger.getInstance().i(ZGBaseHelper.class, "释放zego SDK!");
            zegoLiveRoom = null;
        }
        return isUnitSDK;
    }


    /**
     * 登陆zego房间
     * <p>
     * 注意!!! sdk的推拉流以及信令服务等常用功能都需要登录房间后才能使用,
     * 在退出房间时必须要调用{@link #loginOutRoom()}退出房间。
     *
     * @param roomID 房间号 App 需保证房间 ID 信息的全局唯一，只支持长度不超过 128 byte 的数字，下划线，字母。
     *               每个房间 ID 代表着一个房间。
     * @param role   用户角色，参考{@link ZegoConstants.RoomRole} 分主播和观众。请
     *               根据场景选择对应角色
     * @return true 为调用成功，false 为调用失败
     */
    public boolean loginRoom(String roomID, int role, final IZegoLoginCompletionCallback callback) {
        if (getZGBaseState() != ZGBaseState.InitSuccessState) {
            AppLogger.getInstance().i(ZGBaseHelper.class, "登陆失败: 请先InitSdk");
            return false;
        }

        AppLogger.getInstance().i(ZGBaseHelper.class, "开始登陆房间!");

        return zegoLiveRoom.loginRoom(roomID, role, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int i, ZegoStreamInfo[] zegoStreamInfos) {
                // zegoStreamInfos，内部封装了 userID、userName、streamID 和 extraInfo。
                // 登录房间成功后, 开发者可通过 zegoStreamInfos 获取到当前房间推流信息。便于后续的拉流操作
                // 当 zegoStreamInfos 为 null 时说明当前房间没有人推流
                callback.onLoginCompletion(i, zegoStreamInfos);
            }
        });
    }


    /**
     * 房间代理很重要, 开发者可以按自己的需求在回调里实现自己的业务
     * app相关业务。回调介绍请参考文档<a>https://doc.zego.im/CN/625.html</>
     * <p>
     * 调用时机：建议在登陆房间之前设置。
     *
     * @param roomCallback 房间代理
     */
    public void setZegoRoomCallback(final IZegoRoomCallback roomCallback) {

        if (getZGBaseState() != ZGBaseState.InitSuccessState) {
            AppLogger.getInstance().w(ZGBaseHelper.class, "设置房间代理失败! SDK未初始化, 请先初始化SDK");
            return;
        }

        /**
         * 通过设置sdk房间代理，可以收到房间内的一些信息回调。
         * 开发者可以按自己的需求在回调里实现自己的业务
         */
        zegoLiveRoom.setZegoRoomCallback(new IZegoRoomCallback() {
            @Override
            public void onKickOut(int reason, String roomID, String customReason) {
                AppLogger.getInstance().i(ZGBaseHelper.class, "您已被踢出房间 reason : %d, roomID : %s", reason, roomID);
                // 原因，16777219 表示该账户多点登录被踢出，16777220 表示该账户是被手动踢出，16777221 表示房间会话错误被踢出
                // 注意!!! 业务侧确保分配的userID保持唯一，不然会造成互相抢占。

                if (roomCallback != null) {
                    roomCallback.onKickOut(reason, roomID, customReason);
                }
            }

            @Override
            public void onDisconnect(int errorCode, String roomID) {
                AppLogger.getInstance().i(ZGBaseHelper.class, "房间与server断开连接 errorCode : %d, roomID : %s", errorCode, roomID);
                // 原因，16777219 网络断开。 断网90秒仍没有恢复后会回调这个错误，onDisconnect后会停止推流和拉流

                if (roomCallback != null) {
                    roomCallback.onDisconnect(errorCode, roomID);
                }
            }

            @Override
            public void onReconnect(int errorCode, String roomID) {
                AppLogger.getInstance().i(ZGBaseHelper.class, "房间与server重新连接. roomID : %s", roomID);

                if (roomCallback != null) {
                    roomCallback.onReconnect(errorCode, roomID);
                }
            }

            @Override
            public void onTempBroken(int errorCode, String roomID) {
                AppLogger.getInstance().i(ZGBaseHelper.class, "房间与server中断，SDK会尝试自动重连. roomID : %s", roomID);

                if (roomCallback != null) {
                    roomCallback.onTempBroken(errorCode, roomID);
                }
            }

            @Override
            public void onStreamUpdated(int type, ZegoStreamInfo[] listStream, String roomID) {
                // 当登陆房间成功后，如果房间内中途有人推流或停止推流。房间内其他人就能通过该回调收到流更新通知。
                for (ZegoStreamInfo streamInfo : listStream) {
                    if (type == ZegoConstants.StreamUpdateType.Added) {

                        AppLogger.getInstance().i(ZGBaseHelper.class, "房间内收到流新增通知. streamID : %s, userName : %s, extraInfo : %s", streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);

                    } else if (type == ZegoConstants.StreamUpdateType.Deleted) {

                        AppLogger.getInstance().i(ZGBaseHelper.class, "房间内收到流删除通知. streamID : %s, userName : %s, extraInfo : %s", streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);

                    }

                }

                if (roomCallback != null) {
                    roomCallback.onStreamUpdated(type, listStream, roomID);
                }
            }

            @Override
            public void onStreamExtraInfoUpdated(ZegoStreamInfo[] zegoStreamInfos, String roomID) {
                // 开发者可以通过流额外信息回调，来实现主播设备状态同步的功能，
                // 比如主播关闭麦克风，这个时候主播可以通过更新流额外信息发送主播当前的设备状态
                // 观众则可以通过此回调拿到流额外信息，更新主播设备状态。
                for (ZegoStreamInfo streamInfo : zegoStreamInfos) {
                    AppLogger.getInstance().i(ZGBaseHelper.class, "房间内收到流额外信息更新. streamID : %s, userName : %s, extraInfo : %s", streamInfo.streamID, streamInfo.userName, streamInfo.extraInfo);
                }

                if (roomCallback != null) {
                    roomCallback.onStreamExtraInfoUpdated(zegoStreamInfos, roomID);
                }
            }

            @Override
            public void onRecvCustomCommand(String userID, String userName, String content, String roomID) {
                AppLogger.getInstance().i(ZGBaseHelper.class, "收到自定义消息. userID : %s, userID : %s, content : %s, roomID : %s", userID, userName, content, roomID);

                if (roomCallback != null) {
                    roomCallback.onRecvCustomCommand(userID, userName, content, roomID);
                }
            }
        });

    }

    /**
     * 释放房间的代理
     * 当不再使用ZegoSDK时，可以释放房间的代理
     * <p>
     * 调用时机：建议在unInitSDK之前设置。
     *
     */
    public void releaseZegoRoomCallback() {
        zegoLiveRoom.setZegoRoomCallback(null);
    }


    /**
     * 登出房间
     * 注意!!! 停止所有的推流和拉流后，才能执行 logoutRoom
     * 否则会影响房间业务.
     *
     * @return true 为调用成功，false 为调用失败
     */
    public boolean loginOutRoom() {
        if (zegoLiveRoom != null) {
            AppLogger.getInstance().i(ZGBaseHelper.class, "退出房间");
            zegoLiveRoom.setZegoRoomCallback(null);
           return zegoLiveRoom.logoutRoom();
        }
        return false;
    }

}