package jaygoo.library.m3u8downloader.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import jaygoo.library.m3u8downloader.M3U8DownloaderConfig;
import jaygoo.library.m3u8downloader.bean.M3U8;
import jaygoo.library.m3u8downloader.bean.M3U8Ts;

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2017/11/18
 * 描    述: 工具类
 * ================================================
 */

public class MUtils {


    /**
     * 将Url转换为M3U8对象
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static M3U8 parseIndex(String url) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));

        String basepath = confirmBasePath(url);//url.substring(0, url.lastIndexOf("/") + 1);

        //修改 2019年10月28日18:21:14
        /*URL fullUrl = new URL(url);
        basepath = fullUrl.getProtocol() + "://" + fullUrl.getHost();
*/

        Log.d("MUtils", "basePath " + basepath);
        M3U8 ret = new M3U8();
        ret.setBasePath(basepath);

        String line;
        float seconds = 0;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#")) {
                if (line.startsWith("#EXTINF:")) {
                    line = line.substring(8);
                    if (line.endsWith(",")) {
                        line = line.substring(0, line.length() - 1);
                    }
                    seconds = Float.parseFloat(line);
                }
                continue;
            }
            if (line.endsWith("m3u8")) {
                return parseIndex(basepath + line);
            }
            ret.addTs(new M3U8Ts(line, seconds));
            seconds = 0;
        }
        reader.close();

        return ret;
    }


    /***
     * 确认basePath
     * zhouzhongqing
     * 2019年12月11日10:15:49
     * */
    private static String confirmBasePath(String url) {


        String basePath = null;//url.substring(0, url.lastIndexOf("/") + 1);
        BufferedReader reader = null;
        int i = 0;

        try {
            reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));

            URL fullUrl = new URL(url);
            basePath = fullUrl.getProtocol() + "://" + fullUrl.getHost() + "/";

            String line;
            float seconds = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    if (line.startsWith("#EXTINF:")) {
                        line = line.substring(8);
                        if (line.endsWith(",")) {
                            line = line.substring(0, line.length() - 1);
                        }

                    }
                    continue;
                }
                if (line.endsWith(".m3u8") || line.endsWith(".ts")) {
                    if (i < 1) {
                        try {
                            BufferedReader tempReader = new BufferedReader(new InputStreamReader(new URL(basePath + line).openStream()));
                            System.out.println(tempReader);
                        } catch (IOException e) {
                            //e.printStackTrace();
                            Log.d("MUtils", basePath + line + "不能访问");
                            basePath = url.substring(0, url.lastIndexOf("/") + 1);
                            try {
                                BufferedReader tempReader = new BufferedReader(new InputStreamReader(new URL(basePath + line).openStream()));
                                Log.d("MUtils", basePath + line + "可以访问");
                            } catch (IOException e2) {
                                e2.printStackTrace();
                                Log.d("MUtils", basePath + line + "不能处理的url");
                            }

                        }
                    }
                    i++;
                }
                System.out.println(line + "---" + seconds);

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return basePath;
    }

    /**
     * 清空文件夹
     */
    public static boolean clearDir(File dir) {
        if (dir.exists()) {// 判断文件是否存在
            if (dir.isFile()) {// 判断是否是文件
                return dir.delete();// 删除文件
            } else if (dir.isDirectory()) {// 否则如果它是一个目录
                File[] files = dir.listFiles();// 声明目录下所有的文件 files[];
                for (int i = 0; i < files.length; i++) {// 遍历目录下所有的文件
                    clearDir(files[i]);// 把每个文件用这个方法进行迭代
                }
                return dir.delete();// 删除文件夹
            }
        }
        return true;
    }


    private static float KB = 1024;
    private static float MB = 1024 * KB;
    private static float GB = 1024 * MB;

    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long size) {
        if (size >= GB) {
            return String.format("%.1f GB", size / GB);
        } else if (size >= MB) {
            float value = size / MB;
            return String.format(value > 100 ? "%.0f MB" : "%.1f MB", value);
        } else if (size >= KB) {
            float value = size / KB;
            return String.format(value > 100 ? "%.0f KB" : "%.1f KB", value);
        } else {
            return String.format("%d B", size);
        }
    }

    /**
     * 生成本地m3u8索引文件，ts切片和m3u8文件放在相同目录下即可
     *
     * @param m3u8Dir
     * @param m3U8
     */
    public static File createLocalM3U8(File m3u8Dir, String fileName, M3U8 m3U8) throws IOException {
        return createLocalM3U8(m3u8Dir, fileName, m3U8, null);
    }

    /**
     * 生成AES-128加密本地m3u8索引文件，ts切片和m3u8文件放在相同目录下即可
     *
     * @param m3u8Dir
     * @param m3U8
     */
    public static File createLocalM3U8(File m3u8Dir, String fileName, M3U8 m3U8, String keyPath) throws IOException {
        File m3u8File = new File(m3u8Dir, fileName);
        BufferedWriter bfw = new BufferedWriter(new FileWriter(m3u8File, false));
        bfw.write("#EXTM3U\n");
        bfw.write("#EXT-X-VERSION:3\n");
        bfw.write("#EXT-X-MEDIA-SEQUENCE:0\n");
        bfw.write("#EXT-X-TARGETDURATION:13\n");
        for (M3U8Ts m3U8Ts : m3U8.getTsList()) {
            if (keyPath != null) bfw.write("#EXT-X-KEY:METHOD=AES-128,URI=\"" + keyPath + "\"\n");
            bfw.write("#EXTINF:" + m3U8Ts.getSeconds() + ",\n");
            bfw.write(m3U8Ts.obtainEncodeTsFileName());
            bfw.newLine();
        }
        bfw.write("#EXT-X-ENDLIST");
        bfw.flush();
        bfw.close();
        return m3u8File;
    }

    public static byte[] readFile(String fileName) throws IOException {
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        int length = fis.available();
        byte[] buffer = new byte[length];
        fis.read(buffer);
        fis.close();
        return buffer;
    }

    public static void saveFile(byte[] bytes, String fileName) throws IOException {
        File file = new File(fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }

    public static String getSaveFileDir(String url) {
        return M3U8DownloaderConfig.getSaveDir() + File.separator + MD5Utils.encode(url);
    }

}
