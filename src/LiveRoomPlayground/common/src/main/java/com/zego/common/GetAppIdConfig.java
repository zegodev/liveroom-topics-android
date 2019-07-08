package com.zego.common;

/**
 * Created by zego on 2018/11/15.
 */

public class GetAppIdConfig {

    /**
     * 请提前在即构管理控制台获取 appID 与 appSign
     *  AppID 填写样式示例：
     *      public static final long appId = 123456789L;
     *  appSign 填写样式示例：
     *      public static final byte[] appSign = {
     *          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     *          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     *          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     *          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     *          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     *          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     *          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     *          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
     *      }
     *
     */

    public static final long appId = 1739272706L;

    public static final byte[] appSign = {(byte) 0x1e, (byte) 0xc3, (byte) 0xf8, (byte) 0x5c, (byte) 0xb2, (byte) 0xf2, (byte) 0x13, (byte) 0x70, (byte) 0x26, (byte) 0x4e, (byte) 0xb3, (byte) 0x71, (byte) 0xc8, (byte) 0xc6, (byte) 0x5c, (byte) 0xa3, (byte) 0x7f, (byte) 0xa3, (byte) 0x3b, (byte) 0x9d, (byte) 0xef, (byte) 0xef, (byte) 0x2a, (byte) 0x85, (byte) 0xe0, (byte) 0xc8, (byte) 0x99, (byte) 0xae, (byte) 0x82, (byte) 0xc0, (byte) 0xf6, (byte) 0xf8};
}
