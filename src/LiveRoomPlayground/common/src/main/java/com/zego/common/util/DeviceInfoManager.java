package com.zego.common.util;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;


/**
 * DeviceInfoManager 设备信息管理
 * <p>
 * 主要用于获取系统cpu 内存 唯一标示等一些系统信息
 */
public class DeviceInfoManager {


    private static final String TAG = "DeviceInfoManager";
    private static ActivityManager mActivityManager;

    public synchronized static ActivityManager getActivityManager(Context context) {
        if (mActivityManager == null) {
            mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        }
        return mActivityManager;
    }


    static final private String INVALID_SERIAL_NUMBER = "12345678900";

    /**
     * 通过wifi-mac地址与设备硬件标识符等拼接成唯一设备ID
     *
     * @param context android上下文，用于获取设备信息
     * @return 返回生成的设备唯一ID值
     */
    @SuppressLint("HardwareIds")
    static final public String generateDeviceId(Context context) {
        String deviceId = getEthernetMac();
        if (!TextUtils.isEmpty(deviceId) && !Build.UNKNOWN.equals(deviceId)) {
            return deviceId;
        }

        deviceId = Build.SERIAL;

        if (!Build.UNKNOWN.equals(deviceId) && !INVALID_SERIAL_NUMBER.equals(deviceId)) {
            return deviceId;
        }


        if (!TextUtils.isEmpty(deviceId) && !Build.UNKNOWN.equals(deviceId) && !INVALID_SERIAL_NUMBER.equals(deviceId)) {
            return deviceId;
        }

        deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (!"9774d56d682e549c".equals(deviceId) && !INVALID_SERIAL_NUMBER.equals(deviceId)
                && !TextUtils.isEmpty(deviceId) && deviceId.length() > 6) {
            return deviceId;
        }

        // wifi mac地址
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        String wifiMac = info.getMacAddress();
        if (!TextUtils.isEmpty(wifiMac)) {
            return String.format("w%s", wifiMac.replace(":", ""));
        }

        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取产品名称
     *
     * @return
     */
    static public String getProductName() {
        String productName = Build.MODEL;
        if (productName != null) {
            productName = productName.replace(" ", "");
        }
        return productName;
    }

    /**
     * 获取有线网卡的 MAC 地址
     *
     * @return
     */
    static private String getEthernetMac() {
        String macSerial = null;
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/eth0/address ");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (IOException ex) {
            // 赋予默认值
            ex.printStackTrace();

            return Build.UNKNOWN;
        }

        if (macSerial != null && macSerial.length() > 0)
            macSerial = macSerial.replaceAll(":", "");
        else {
            return Build.UNKNOWN;
        }

        return macSerial;
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getScreenWidth(Context context) {
        int screenWith = -1;
        try {
            screenWith = context.getResources().getDisplayMetrics().widthPixels;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return screenWith;
    }

    public static int getScreenHeight(Context context) {
        int screenHeight = -1;
        try {
            screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return screenHeight;
    }

    /**
     * 计算已使用内存的百分比，并返回。
     *
     * @param context 可传入应用程序上下文。
     * @return 已使用内存的百分比，以字符串形式返回。
     */
    public static String getUsedPercentValue(Context context) {
        long totalMemorySize = getTotalMemory();
        long availableSize = getAvailableMemory(context) / 1024;
        int percent = (int) ((totalMemorySize - availableSize) / (float) totalMemorySize * 100);
        return percent + "%";
    }

    /**
     * 获取当前可用内存，返回数据以字节为单位。
     *
     * @param context 可传入应用程序上下文。
     * @return 当前可用内存。
     */
    public static long getAvailableMemory(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        getActivityManager(context).getMemoryInfo(mi);
        return mi.availMem;
    }

    /**
     * 获取系统总内存,返回字节单位为KB
     *
     * @return 系统总内存
     */
    public static long getTotalMemory() {
        long totalMemorySize = 0;
        String dir = "/proc/meminfo";
        try {
            FileReader fr = new FileReader(dir);
            BufferedReader br = new BufferedReader(fr, 2048);
            String memoryLine = br.readLine();
            String subMemoryLine = memoryLine.substring(memoryLine.indexOf("MemTotal:"));
            br.close();
            //将非数字的字符替换为空
            totalMemorySize = Integer.parseInt(subMemoryLine.replaceAll("\\D+", ""));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalMemorySize;
    }

    /**
     * 获取顶层activity的包名
     *
     * @param context
     * @return activity的包名
     */
    public static String getTopActivityPackageName(Context context) {
        ActivityManager activityManager = getActivityManager(context);
        List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1);
        return runningTasks.get(0).topActivity.getPackageName();
    }

    /**
     * 获取当前进程的CPU使用率
     *
     * @return CPU的使用率
     */
    public static float getCurProcessCpuRate() {

        float cpuRate = 0;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            try {
                float totalCpuTime1 = getTotalCpuTime();
                float processCpuTime1 = getAppCpuTime();

                Thread.sleep(360);

                float totalCpuTime2 = getTotalCpuTime();
                float processCpuTime2 = getAppCpuTime();
                cpuRate = 100 * (processCpuTime2 - processCpuTime1)
                        / (totalCpuTime2 - totalCpuTime1);

            } catch (Exception e) {

            }
        } else {
            cpuRate = getAppCpuTop();
        }


//        float processCpuTime2 = getAppCpuTime();
//
//
//        float upTime = bootTime();
//
//        String[] cpuInfo = getAppSZ();
//
//        float utTime = Long.parseLong(cpuInfo[13]);
//        float sTime = Long.parseLong(cpuInfo[14]);
//        float cuTime = Long.parseLong(cpuInfo[15]);
//        float csTime = Long.parseLong(cpuInfo[16]);
//        float startTime = Long.parseLong(cpuInfo[21]);
//
//        float totalTime = utTime + sTime;
//
//        totalTime = totalTime + cuTime + csTime;
//
//        float Hertz = CpuUtil.sayHello();
//
//        float seconds = upTime - (startTime / Hertz);
//
//        cpuRate = 100 * ((totalTime / Hertz) / seconds);

        return cpuRate;
    }

    /**
     * 通过top 命令获取 cpu 当前使用率
     *
     * @return 当前进程的CPU使用时间
     */

    public static float getAppCpuTop() {

        int pid = android.os.Process.myPid();
        float cpuUsage = 0;
        try {
            @SuppressLint("DefaultLocale") Process pp = Runtime.getRuntime().exec(String.format("top -p %d -n 1", pid));
            pp.waitFor();
            InputStream fis = pp.getInputStream();

            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String cpuUsageStr = "0";
            String cpuNumberStr = "0";
            String line = null;
            while ((line = br.readLine()) != null) {
                int cpuIndex = line.indexOf("%cpu");
                int cpuUsageIndex = line.indexOf(String.valueOf(pid));
                if (cpuIndex != -1) {
                    cpuNumberStr = line.substring(0, cpuIndex);
                }

                if (cpuUsageIndex != -1) {
                    String[] cpuInfo = line.split("\\s");
                    cpuUsageStr = checkCpuInfo(cpuInfo);

                }
            }

            cpuUsage = Float.valueOf(cpuUsageStr) / (Float.valueOf(cpuNumberStr) / 100);

        } catch (Exception ex) {
            // 赋予默认值
            ex.printStackTrace();
        }


        return cpuUsage;
    }

    private static String checkCpuInfo(String[] cpuInfo) {
        int index = 0;
        for (String info : cpuInfo) {
            if (!" ".equals(info) && !"".equals(info)) {
                if (index == 8) {
                    return info;
                }
                index++;
            }
        }
        return "0";
    }


    static long bootTime() {
        return SystemClock.elapsedRealtime() / 1000;
    }

    /**
     * 获取总的CPU使用率
     *
     * @return CPU使用率
     */
    public static float getTotalCpuRate() {
        float cpuRate = 0;
        try {
            float totalCpuTime1 = getTotalCpuTime();
            float totalUsedCpuTime1 = totalCpuTime1 - sStatus.idletime;

            Thread.sleep(360);

            float totalCpuTime2 = getTotalCpuTime();
            float totalUsedCpuTime2 = totalCpuTime2 - sStatus.idletime;
            cpuRate = 100 * (totalUsedCpuTime2 - totalUsedCpuTime1)
                    / (totalCpuTime2 - totalCpuTime1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return cpuRate;
    }

    /**
     * 获取系统总CPU使用时间
     *
     * @return 系统CPU总的使用时间
     */
    public static long getTotalCpuTime() {
        String[] cpuInfos = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        } catch (IOException ex) {

        }
        sStatus.usertime = Long.parseLong(cpuInfos[2]);
        sStatus.nicetime = Long.parseLong(cpuInfos[3]);
        sStatus.systemtime = Long.parseLong(cpuInfos[4]);
        sStatus.idletime = Long.parseLong(cpuInfos[5]);
        sStatus.iowaittime = Long.parseLong(cpuInfos[6]);
        sStatus.irqtime = Long.parseLong(cpuInfos[7]);
        sStatus.softirqtime = Long.parseLong(cpuInfos[8]);
        return sStatus.getTotalTime();
    }

    /**
     * 获取当前进程的CPU使用时间
     *
     * @return 当前进程的CPU使用时间
     */
    public static long getAppCpuTime() {
        // 获取应用占用的CPU时间
        String[] cpuInfos = null;
        try {
            int pid = android.os.Process.myPid();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/" + pid + "/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        long appCpuTime = Long.parseLong(cpuInfos[13])
                + Long.parseLong(cpuInfos[14]) + Long.parseLong(cpuInfos[15])
                + Long.parseLong(cpuInfos[16]);
        return appCpuTime;
    }

    /**
     * 获取当前进程的CPU使用数据
     *
     * @return
     */
    public static String[] getAppSZ() {
        // 获取应用占用的CPU时间
        String[] cpuInfos = null;
        try {
            int pid = android.os.Process.myPid();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/" + pid + "/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return cpuInfos;
    }

    static DeviceInfoManager.Status sStatus = new DeviceInfoManager.Status();

    static class Status {
        public long usertime;
        public long nicetime;
        public long systemtime;
        public long idletime;
        public long iowaittime;
        public long irqtime;
        public long softirqtime;

        public long getTotalTime() {
            return (usertime + nicetime + systemtime + idletime + iowaittime
                    + irqtime + softirqtime);
        }
    }


}
