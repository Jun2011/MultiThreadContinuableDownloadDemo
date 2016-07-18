package jun.multithreadcontinuabledownloaddemo.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import jun.multithreadcontinuabledownloaddemo.tool.DownloadHttpTool;

/**
 * Created by Jun on 2016/7/18.
 * <p/>
 * 下载封装类
 */
public class DownloadUtil {

    private static final String TAG = DownloadUtil.class.getSimpleName();

    private DownloadHttpTool mDownloadHttpTool;
    private OnDownloadListener onDownloadListener;

    private int fileSize;
    private int downloadedSize = 0;// 已下载多少

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            int length = msg.arg1;
            synchronized (this) {// 加锁保证已下载的正确性
                downloadedSize += length;
            }
            if (onDownloadListener != null) {
                onDownloadListener.downloadProgress(downloadedSize);
            }
            if (downloadedSize >= fileSize) {
                mDownloadHttpTool.compelete();
                if (onDownloadListener != null) {
                    onDownloadListener.downloadEnd();
                }
            }
        }
    };

    // 构造方法
    public DownloadUtil(int threadCount, String filePath, String filename,
                        String urlString, Context context) {

        mDownloadHttpTool = new DownloadHttpTool(threadCount, urlString,
                filePath, filename, context, mHandler);
    }

    /**
     * 开始下载
     * <p/>
     * 下载之前首先异步线程调用DownloadHttpTool的ready()方法获得文件大小信息，
     * 之后再调用start()方法。
     */
    public void start() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... arg0) {
                mDownloadHttpTool.ready();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                fileSize = mDownloadHttpTool.getFileSize();
                downloadedSize = mDownloadHttpTool.getCompeleteSize();
                Log.i(TAG, "已下载：" + downloadedSize);
                if (onDownloadListener != null) {
                    onDownloadListener.downloadStart(fileSize);
                }
                mDownloadHttpTool.start();
            }
        }.execute();
    }

    /**
     * 暂停下载
     */
    public void pause() {
        mDownloadHttpTool.pause();
    }

    /**
     * 删除下载
     */
    public void delete() {
        mDownloadHttpTool.delete();
    }

    /**
     * 重新下载
     */
    public void reset() {
        mDownloadHttpTool.delete();
        start();
    }

    // 设置下载监听器
    public void setOnDownloadListener(OnDownloadListener onDownloadListener) {
        this.onDownloadListener = onDownloadListener;
    }

    // 下载回调接口
    public interface OnDownloadListener {
        public void downloadStart(int fileSize);

        public void downloadProgress(int downloadedSize);

        public void downloadEnd();
    }
}
