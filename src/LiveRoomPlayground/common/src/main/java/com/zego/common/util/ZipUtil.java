package com.zego.common.util;

import android.media.MediaMetadataRetriever;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * <p>Copyright © 2017 Zego. All rights reserved.</p>
 *
 * @author realuei on 04/09/2017.
 */

public class ZipUtil {
    static final private int BUFF_SIZE = 1024 * 1024; // 1M Byte

    /**
     * 压缩文件或者文件夹
     *
     * @param files 待压缩文件列表
     * @param outputZipFile 待生成的 zip 文件路径
     * @param comment 文件注解
     * @throws IOException
     */
    static final public void zipFiles(File[] files, File outputZipFile, String comment) throws IOException {
        ZipOutputStream zipout = null;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                zipout = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(outputZipFile), new CRC32()), StandardCharsets.UTF_8);
            } else {
                zipout = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(outputZipFile), new CRC32()));
            }

            for (File inputFile : files) {
                zipFile(inputFile, zipout, "");
            }
            zipout.setComment(comment);
        } finally {
            if (zipout != null) {
                zipout.close();
            }
        }
    }

    /**
     * 压缩文件
     *
     * @param resFile 需要压缩的文件（夹）
     * @param zipOut 压缩的目的文件
     * @param rootPath 压缩的文件路径
     * @throws IOException 当压缩过程出错时抛出
     */
    static final private void zipFile(File resFile, ZipOutputStream zipOut, String rootPath) throws IOException {
        rootPath = rootPath + (rootPath.trim().length() == 0 ? "" : File.separator) + resFile.getName();
//        rootPath = new String(rootPath.getBytes("8859_1"), "UTF-8");
        if (resFile.isDirectory()) {
            File[] fileList = resFile.listFiles();
            for (File file : fileList) {
                zipFile(file, zipOut, rootPath);
            }
        } else {
            byte buffer[] = new byte[BUFF_SIZE];
            BufferedInputStream in = null;
            try {
                in = new BufferedInputStream(new FileInputStream(resFile), BUFF_SIZE);
                ZipEntry zipEntry = new ZipEntry(rootPath);
                zipOut.putNextEntry(zipEntry);
                int realLength;
                while ((realLength = in.read(buffer)) != -1) {
                    zipOut.write(buffer, 0, realLength);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
            zipOut.flush();
            zipOut.closeEntry();
        }
    }

    /**
     * 根据文件后缀名获得对应的MIME类型。
     *
     * @param filePath
     */
    static final private String getMimeType(String filePath) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        String mime = "text/plain";
        if (filePath != null) {
            try {
                mmr.setDataSource(filePath);
                mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            } catch (IllegalStateException e) {
                return mime;
            } catch (IllegalArgumentException e) {
                return mime;
            } catch (RuntimeException e) {
                return mime;
            }
        }
        return mime;
    }
}
