package com.zego.mixing;

import android.util.Log;
import com.zego.zegoliveroom.entity.AuxData;

import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

public class ZGMixingDemo {

    private final int DURATION = 15000;
    private int mSampleRate;
    private int mChannels;
    private int mBitRate;

    private static ZGMixingDemo zgMixingDemo = null;

    private ZGMixingDemo() {

    }

    public static ZGMixingDemo sharedInstance(){
        if (zgMixingDemo == null){
            synchronized (ZGMixingDemo.class){
                if (zgMixingDemo == null){
                    zgMixingDemo = new ZGMixingDemo();
                }
            }
        }
        return zgMixingDemo;
    }

    // 处理混音传递pcm数据给SDK
    protected InputStream mBackgroundMusic = null;

    public AuxData handleAuxCallback(String pcmFilePath, int exceptDataLength) {

        AuxData auxData = new AuxData();
        auxData.dataBuf = new byte[exceptDataLength];

        try {

            if (mBackgroundMusic == null) {
                if (!pcmFilePath.equals("")) {

                    mBackgroundMusic = new FileInputStream(pcmFilePath);
                }
            }

            int len = mBackgroundMusic.read(auxData.dataBuf);

            if (len <= 0) {
                // 歌曲播放完毕
                mBackgroundMusic.close();
                mBackgroundMusic = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        auxData.channelCount = mChannels;
        auxData.sampleRate = mSampleRate;

        return auxData;
    }

    // mp3格式转pcm
    public void MP3ToPCM(String mp3FilePath, String pcmFilePath) {
        FileOutputStream fos = null;

        try {
            byte[] pcmData = decodeToPCM(mp3FilePath, 0,DURATION);

            if (pcmData != null) {

                File outfile = new File(pcmFilePath);
                if (!outfile.exists()) {
                    fos = new FileOutputStream(outfile);
                    fos.write(pcmData);
                    fos.close();
                }
            }

        } catch (IOException e){
            Log.e("Zego", "io exception happened to mp3 to pcm");
        }
    }

    // mp3解码为pcm数据
    private static byte[] decodeToPCM(String mp3FilePath, int startMs, int maxMs)
            throws IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);

        float totalMs = 0;
        boolean seeking = true;

        File file = new File(mp3FilePath);
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file), 8 * 1024);

        try {

            Bitstream bitstream = new Bitstream(inputStream);
            Decoder decoder = new Decoder();

            boolean done = false;
            while (!done) {
                Header frameHeader = bitstream.readFrame();
                if (frameHeader == null) {
                    done = true;
                } else {
                    totalMs += frameHeader.ms_per_frame();

                    if (totalMs >= startMs) {
                        seeking = false;
                    }

                    if (!seeking) {
                        SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);

                        if (output.getSampleFrequency() != 44100
                                || output.getChannelCount() != 2) {
                            Log.e("Zego", "mono or non-44100 MP3 not supported");
                        }

                        short[] pcm = output.getBuffer();
                        for (short s : pcm) {
                            outStream.write(s & 0xff);
                            outStream.write((s >> 8) & 0xff);
                        }
                    }

                    if (totalMs >= (startMs + maxMs)) {
                        done = true;
                    }
                }
                bitstream.closeFrame();
            }
            
            return outStream.toByteArray();
        } catch (BitstreamException e) {
            Log.e("Zego", "Bitstream error: "+ e.getMessage());
            throw new IOException("Bitstream error: " + e);

        } catch (DecoderException e) {
            Log.e("Zego", "Decoder error " + e.getMessage());
        } finally {
            inputStream.close();
        }
        return null;
    }

    // 获取mp3文件采样率等信息
    public void getMP3FileInfo(String mp3FilePath) {

        try {
            MP3File mp3File = new MP3File(mp3FilePath);
            MP3AudioHeader header = mp3File.getMP3AudioHeader();
            int timeLen = header.getTrackLength();
            String bitrate = header.getBitRate();
            mBitRate = Integer.valueOf(bitrate);

            String format = header.getFormat();
            String channels = header.getChannels();
            if (channels.equals("Joint Stereo")) {
                mChannels = 2;
            } else {
                mChannels = 1;
            }

            String samplerate = header.getSampleRate();
            mSampleRate = Integer.valueOf(samplerate);

        } catch (Exception e) {
            Log.e("Zego","get mp3file info exception");
            e.printStackTrace();
        }
    }
}
