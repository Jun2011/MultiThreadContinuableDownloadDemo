package jun.multithreadcontinuabledownloaddemo.tool;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jun.multithreadcontinuabledownloaddemo.model.DownloadInfo;

/**
 * Created by Jun on 2016/7/18.
 * <p/>
 * 下载工具类
 */
public class DownloadHttpTool {

    private static final String TAG = DownloadHttpTool.class.getSimpleName();
    // 线程数量
    private int threadCount;
    // URL地址
    private String urlstr;
    private Context mContext;
    private Handler mHandler;
    // 保存下载信息的类
    private List<DownloadInfo> downloadInfos;
    // 目录
    private String localPath;
    // 文件名
    private String fileName;
    // 文件大小
    private int fileSize;
    // 文件信息保存的数据库操作类
    private DownlaodSqlTool sqlTool;

    // 利用枚举表示下载的4种状态
    private enum Download_State {
        Downloading, Pause, Ready, Delete
    }

    // 当前下载状态
    private Download_State state = Download_State.Ready;
    // 所有线程下载的总数
    private int globalCompelete = 0;

    // 构造方法
    public DownloadHttpTool(
            int threadCount
            , String urlString
            , String localPath
            , String fileName
            , Context context
            , Handler handler) {
        super();
        this.threadCount = threadCount;
        this.urlstr = urlString;
        this.localPath = localPath;
        this.mContext = context;
        this.mHandler = handler;
        this.fileName = fileName;
        sqlTool = new DownlaodSqlTool(mContext);
    }

    // 在开始下载之前需要调用此ready()方法进行配置
    public void ready() {
        Log.i(TAG, "ready()");
        globalCompelete = 0;
        downloadInfos = sqlTool.getInfos(urlstr);
        if (downloadInfos.size() == 0) {
            initFirst();
        } else {
            File file = new File(localPath + "/" + fileName);
            if (!file.exists()) {
                sqlTool.delete(urlstr);
                initFirst();
            } else {
                fileSize = downloadInfos.get(downloadInfos.size() - 1)
                        .getEndPos();
                for (DownloadInfo info : downloadInfos) {
                    globalCompelete += info.getCompeleteSize();
                }
                Log.i(TAG, "完成度：" + globalCompelete);
            }
        }
    }

    // 开始下载
    public void start() {
        Log.i(TAG, "start()");
        if (downloadInfos != null) {
            if (state == Download_State.Downloading) {
                return;
            }
            state = Download_State.Downloading;
            for (DownloadInfo info : downloadInfos) {
                Log.i(TAG, "开启线程");
                new DownloadThread(info.getThreadId(), info.getStartPos(),
                        info.getEndPos(), info.getCompeleteSize(),
                        info.getUrl()).start();
            }
        }
    }

    // 暂停下载
    public void pause() {
        Log.i(TAG, "pause()");
        state = Download_State.Pause;
        sqlTool.closeDb();
    }

    // 删除下载
    public void delete() {
        Log.i(TAG, "delete()");
        state = Download_State.Delete;
        compelete();
        // 删除文件
        new File(localPath + File.separator + fileName).delete();
    }

    // 完成下载
    public void compelete() {
        Log.i(TAG, "compelete()");
        sqlTool.delete(urlstr);
        sqlTool.closeDb();
    }

    // 获取文件的大小
    public int getFileSize() {
        return fileSize;
    }

    // 获取完成度
    public int getCompeleteSize() {
        return globalCompelete;
    }

    /**
     * 自定义下载线程
     */
    private class DownloadThread extends Thread {

        private int threadId;
        private int startPos;
        private int endPos;
        private int compeleteSize;
        private String urlstr;
        private int totalThreadSize;// 总体大小

        public DownloadThread(int threadId, int startPos, int endPos,
                              int compeleteSize, String urlstr) {
            this.threadId = threadId;
            this.startPos = startPos;
            this.endPos = endPos;
            totalThreadSize = endPos - startPos + 1;
            this.urlstr = urlstr;
            this.compeleteSize = compeleteSize;
        }

        @Override
        public void run() {
            HttpURLConnection connection = null;
            RandomAccessFile randomAccessFile = null;
            InputStream is = null;
            try {
                randomAccessFile = new RandomAccessFile(localPath
                        + File.separator + fileName, "rwd");
                randomAccessFile.seek(startPos + compeleteSize);
                URL url = new URL(urlstr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Range", "bytes="
                        + (startPos + compeleteSize) + "-" + endPos);
                is = connection.getInputStream();
                byte[] buffer = new byte[1024];
                int length = -1;
                while ((length = is.read(buffer)) != -1) {
                    randomAccessFile.write(buffer, 0, length);
                    compeleteSize += length;
                    Message message = Message.obtain();
                    message.what = threadId;
                    message.obj = urlstr;
                    message.arg1 = length;
                    mHandler.sendMessage(message);
                    Log.w(TAG, "Threadid::" + threadId + "    compelete::"
                            + compeleteSize + "    total::" + totalThreadSize);
                    // 当程序不再是下载状态的时候，纪录当前的下载进度
                    if ((state != Download_State.Downloading)
                            || (compeleteSize >= totalThreadSize)) {
                        sqlTool.updataInfos(threadId, compeleteSize, urlstr);
                        break;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                sqlTool.updataInfos(threadId, compeleteSize, urlstr);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                    randomAccessFile.close();
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 第一次下载初始化
     */
    private void initFirst() {
        Log.i(TAG, "initFirst()");
        try {
            URL url = new URL(urlstr);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("GET");
            // 获取文件大小
            fileSize = connection.getContentLength();
            Log.i(TAG, "文件大小：" + fileSize);
            File fileParent = new File(localPath);
            // 如果目录不存在就新建一个
            if (!fileParent.exists()) {
                fileParent.mkdir();
            }
            File file = new File(fileParent, fileName);
            // 如果文件不存在就新建一个
            if (!file.exists()) {
                file.createNewFile();
            }
            // 本地访问文件
            RandomAccessFile accessFile = new RandomAccessFile(file, "rwd");
            accessFile.setLength(fileSize);
            accessFile.close();
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int range = fileSize / threadCount;
        downloadInfos = new ArrayList<DownloadInfo>();
        for (int i = 0; i < threadCount - 1; i++) {
            DownloadInfo info = new DownloadInfo(
                    i, i * range, (i + 1) * range - 1, 0, urlstr);
            downloadInfos.add(info);
        }
        DownloadInfo info = new DownloadInfo(
                threadCount - 1, (threadCount - 1) * range, fileSize - 1, 0, urlstr);
        downloadInfos.add(info);
        sqlTool.insertInfos(downloadInfos);
    }
}
