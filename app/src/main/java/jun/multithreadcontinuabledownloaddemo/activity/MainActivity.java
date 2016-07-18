package jun.multithreadcontinuabledownloaddemo.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import jun.multithreadcontinuabledownloaddemo.R;
import jun.multithreadcontinuabledownloaddemo.util.DownloadUtil;

/**
 * 在Activity中使用
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ImageView showImage;
    private Button startDownload, pauseDownload, deleteDownload, reSetDownload;
    private ProgressBar progressBar;
    private TextView progressPercent;

    private DownloadUtil mDownloadUtil;
    private int max;// 要下载的文件大小
    private int downloaded;// 已经下载的文件大小

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化下载
        initDownload();

        // 初始化视图
        initView();
    }

    private void initView() {
        showImage = (ImageView) findViewById(R.id.show_image);
        startDownload = (Button) findViewById(R.id.start_download);
        pauseDownload = (Button) findViewById(R.id.pause_download);
        deleteDownload = (Button) findViewById(R.id.delete_download);
        reSetDownload = (Button) findViewById(R.id.reset_download);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressPercent = (TextView) findViewById(R.id.progress_percent);

        startDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 开始下载
                mDownloadUtil.start();
                // 开始下载之后暂停、删除、重新下载按钮才能生效
                pauseDownload.setEnabled(true);
                deleteDownload.setEnabled(true);
                reSetDownload.setEnabled(true);
            }
        });
        pauseDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 暂停下载
                mDownloadUtil.pause();
            }
        });
        deleteDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 删除下载
                mDownloadUtil.delete();
            }
        });
        reSetDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 重新下载
                mDownloadUtil.reset();
            }
        });
    }

    private void initDownload() {
        // 下载地址
        String urlString = "https://pic1.zhimg.com/d01826e27f2c6a4e442ce88f7d37bf03.jpg";
        // 判断是否有SD卡
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.i(TAG, "没有SD卡");
        }
        // 保存路径
        final String localPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "TestDownload";
        Log.d(TAG, localPath);
        // 开启2条线程下载
        mDownloadUtil = new DownloadUtil(2, localPath, "test_image.jpg", urlString, this);
        mDownloadUtil.setOnDownloadListener(new DownloadUtil.OnDownloadListener() {

            @Override
            public void downloadStart(int fileSize) {
                max = fileSize;
                progressBar.setMax(fileSize);
            }

            @Override
            public void downloadProgress(int downloadedSize) {
                downloaded = downloadedSize;
                progressBar.setProgress(downloadedSize);
                progressPercent.setText((int) downloadedSize * 100 / max + "%");
            }

            @Override
            public void downloadEnd() {
                // 已经下载完成
                if (max == downloaded) {
                    try {
                        InputStream is = new FileInputStream(
                                new File(localPath + File.separator + "test_image.jpg"));
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        // 展示下载好的图片
                        showImage.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
