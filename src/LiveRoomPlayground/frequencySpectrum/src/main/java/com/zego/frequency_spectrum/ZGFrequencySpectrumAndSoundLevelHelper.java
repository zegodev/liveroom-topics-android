package com.zego.frequency_spectrum;

import com.zego.zegoavkit2.frequencyspectrum.ZegoFrequencySpectrumMonitor;
import com.zego.zegoavkit2.soundlevel.ZegoSoundLevelMonitor;


/**
 * 本专题的帮助类，用于抽取一些与音频频谱设置和音浪设置相关的逻辑
 *
 */
public class ZGFrequencySpectrumAndSoundLevelHelper {

    public static void modifySoundLevelMonitorCycle(int cycle){
        ZegoSoundLevelMonitor.getInstance().stop();
        ZegoSoundLevelMonitor.getInstance().setCycle(cycle);
        ZegoSoundLevelMonitor.getInstance().start();

    }

    public static void modifyFrequencySpectrumMonitorCycle(int cycle){

        ZegoFrequencySpectrumMonitor.getInstance().stop();
        ZegoFrequencySpectrumMonitor.getInstance().setCycle(cycle);
        ZegoFrequencySpectrumMonitor.getInstance().start();
    }

}
