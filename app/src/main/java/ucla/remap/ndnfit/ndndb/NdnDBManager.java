package ucla.remap.ndnfit.ndndb;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.named_data.jndn.Data;
import net.named_data.jndn.util.SignedBlob;

import java.io.Serializable;

/**
 * Created by zhanght on 2015/12/27.
 */
public class NdnDBManager implements Serializable {
    private SQLiteDatabase mDB;
    private Context mCtx;

    private static final String DB_NAME = "ndndb";
    private static final String TAG = "NdnDBManager";
    private static final String POINT_TABLE = "point_table";
    private static final String TURN_TABLE = "turn_table";
    private int currentTurn;

    private static NdnDBManager instance = new NdnDBManager();

    private NdnDBManager() {
    }

    public void insertData() {
    }

    public static NdnDBManager getInstance() {
        return instance;
    }

    public void init(Context ctx) {
        mCtx = ctx;
        prepareDB();
        createTable();
    }

    public void initForTest() {
        mDB = SQLiteDatabase.openOrCreateDatabase(DB_NAME, null, null);
        createTable();
    }

    public void prepareDB() {
        mDB = mCtx.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);
    }

    /**
     * Check if the tables are already created.
     * @return true if the tables are existed or false.
     */
    public boolean isTurnTableCreated() {
        Cursor cursor = mDB.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + TURN_TABLE + "'", null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    protected boolean isPointTableCreated() {
        Cursor cursor = mDB.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"+ POINT_TABLE +"'", null);
        if(cursor!=null) {
            if(cursor.getCount()>0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    /**
     * Create target tables.
     */
    public void createTable() {
        if(!isTurnTableCreated()) {
            mDB.execSQL("create table " + TURN_TABLE + "("
                            + " name TEXT PRIMARY KEY, "
                            + " data BLOB NOT NULL);"
            );
        }

        if(!isPointTableCreated()) {
            mDB.execSQL("create table " + POINT_TABLE + "("
                            + " id integer PRIMARY KEY autoincrement, "
                            + " lat double, "
                            + " lon double, "
                            + " turn_id integer, "
                            + " point_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                            + " FOREIGN KEY(turn_id) REFERENCES " + TURN_TABLE + "(id));"
            );
        }
    }
}
