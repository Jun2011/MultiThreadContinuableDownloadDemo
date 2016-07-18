package jun.multithreadcontinuabledownloaddemo.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Jun on 2016/7/18.
 * <p/>
 * Sql帮助类
 */
public class DownLoadHelper extends SQLiteOpenHelper {

    // 数据库名
    private static final String SQL_NAME = "download.db";
    // 数据库版本
    private static final int DOWNLOAD_VERSION = 1;

    // 只传一个参数（上下文）的构造方法
    public DownLoadHelper(Context context) {
        // 调用父类的4个参数的构造方法（上下文，数据库名，一般传null即可，数据库版本）
        // 第3个参数：允许我们在查询数据的时候返回一个自定义的Cursor
        super(context, SQL_NAME, null, DOWNLOAD_VERSION);
    }

    // 在download.db数据库下创建一个download_info表存储下载信息
    @Override
    public void onCreate(SQLiteDatabase db) {
        // id 主键，自增长
        // thread_id 线程号
        // start_pos 开始位置
        // end_pos 结束位置
        // compelete_size 完成度
        // url 下载地址
        db.execSQL("create table download_info(" +
                "_id integer PRIMARY KEY AUTOINCREMENT, " +
                "thread_id integer, " +
                "start_pos integer, " +
                "end_pos integer, " +
                "compelete_size integer," +
                "url char)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
