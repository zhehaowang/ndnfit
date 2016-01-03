/**
 * Created by nightzen on 4/22/15.
 */

package ucla.remap.ndnfit.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


import java.io.Serializable;
import java.util.List;

import ucla.remap.ndnfit.data.Position;


public class DBManager implements Serializable{

    private SQLiteDatabase mDB;
    private Context mCtx;

    private static final String DB_NAME = "ndnfit.db";
    private static final String POINT_TABLE = "t_point";
    private static final String TAG = "DBManager";
    private static final String TURN_TABLE = "t_turn";
    private static final String ID_TABLE = "t_id";
    private int currentTurn;

    private static DBManager instance = new DBManager();

    private DBManager(){
    }

    public static DBManager getInstance( ) {
        return instance;
    }

    public void init(Context ctx) {
        mCtx = ctx;
        prepareDB();
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

    protected boolean isIdTableCreated() {
        Cursor cursor = mDB.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + ID_TABLE + "'", null);
        if(cursor!=null) {
            if(cursor.getCount()>0) {
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
                            + " id integer PRIMARY KEY autoincrement, "
                            + " start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                            + " finish_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP);"
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

        if (!isIdTableCreated()) {
            mDB.execSQL("create table " + ID_TABLE + "("
                    + " id integer PRIMARY KEY autoincrement, "
                    + " app_id text, "
                    + " app_cert_name text, "
                    + " signer_id text)");
        }
    }

    public Cursor getIdRecord() {
        String[] columns = {"app_id", "app_cert_name", "signer_id"};
        return mDB.query(ID_TABLE, columns, null, null, null, null, null);
    }

    /**
     * When a tracking is started, new turn record is inserted.
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    public void startTrack() {
        ContentValues record = new ContentValues();

    }

    /**
     * When a tracking is finished, the finish time is updated.
     */
    public void finishTrack() {
        ContentValues record = new ContentValues();

        //record.put("finish_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) );
        record.put("finish_time", System.currentTimeMillis());
        //record.put("finish_time", anchorPosition.getTimeStamp());
        int updatedRows = mDB.update(TURN_TABLE, record, "id "+"="+currentTurn, null);
    }

    public void recordPoints(List<Position> positionList) {
        ContentValues record = new ContentValues();

        Log.e("zhehao", "record points called");
        Log.e("zhehao", Integer.toString(positionList.size()));
        if (positionList.size() == 0) {
            return;
        }

        Position anchorPosition = positionList.get(0);
        record.put("start_time", anchorPosition.getTimeStamp());
        int turnId = (int)mDB.insert(TURN_TABLE, null, record);
        //Wang Yang: update currentTurn to reflect the current turn_id
        currentTurn = turnId;
        record.clear();

        for(Position position : positionList) {
            record.put("turn_id", turnId);
            record.put("lat", position.getLat());
            record.put("lon", position.getLng());
            record.put("point_time", position.getTimeStamp());
            mDB.insert(POINT_TABLE, null, record);
            record.clear();
        }

        anchorPosition = positionList.get(positionList.size() - 1);
        record.put("finish_time", anchorPosition.getTimeStamp());
        mDB.update(TURN_TABLE, record, "id " + "=" + currentTurn, null);
    }

    public void recordPoint(double lat, double lon) {
        ContentValues record = new ContentValues();

        record.put("turn_id", currentTurn);
        record.put("lat", lat);
        record.put("lon", lon);
        record.put("point_time", System.currentTimeMillis());


        int rowPosition = (int) mDB.insert(POINT_TABLE, null, record);
    }

    public Cursor getPoints(int turnId) {
        String[] columns = {"turn_id", "lat", "lon", "point_time"};
        return mDB.query(POINT_TABLE, columns, "turn_id "+"="+turnId, null, null, null, null);
    }

    public Cursor getAllPoints() {
        String[] columns = {"turn_id", "lat", "lon", "point_time"};
        return mDB.query(POINT_TABLE, columns, null, null, null, null, null);
    }

    public Cursor getTurn(int rowId) {
        String[] columns = {"start_time", "finish_time"};
        Cursor cursor = mDB.query(TURN_TABLE, columns, "id "+"="+rowId, null, null, null, null);
        return cursor;
    }

    public Cursor getAllTurn() {
        String[] columns = {"id", "start_time", "finish_time"};
        String orderBy = "id" + " DESC";
        Cursor cursor = mDB.query(TURN_TABLE, columns, null, null, null, null, orderBy);
        return cursor;
    }

    /**
     * Reset all data
     */
    public void resetData() {
        mDB.execSQL("delete from " + TURN_TABLE);
        mDB.execSQL("delete from " + POINT_TABLE);
        mDB.execSQL("delete from " + ID_TABLE);
        Log.i(TAG, "RESET Table");
    }

    /**
     * Get the last turn of tracking
     * @return  last turn's row id
     */
    public int getLastTurn() {
        String[] columns = {"id"};
        Cursor cursor = mDB.query(TURN_TABLE, columns, null, null, null, null, null);
        cursor.moveToLast();
        return cursor.getInt(0);
    }
}
