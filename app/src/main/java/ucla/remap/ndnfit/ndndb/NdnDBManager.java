package ucla.remap.ndnfit.ndndb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

import ucla.remap.ndnfit.position.Position;
import ucla.remap.ndnfit.position.PositionList;

/**
 * Created by zhanght on 2015/12/27.
 */
public class NdnDBManager implements Serializable {
    private SQLiteDatabase mDB;
    private Context mCtx;
    private static final ObjectMapper objectMapper = new ObjectMapper();
//    private final ElementReader elementReader;

    private static final String DB_NAME = "ndndb";
    private static final String TAG = "NdnDBManager";
    private static final String POINT_TABLE = "point_table";
    private static final String TURN_TABLE = "turn_table";
    private static final String CATALOG_TABLE = "catalog_table";

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
     *
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
        Cursor cursor = mDB.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + POINT_TABLE + "'", null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    protected boolean isCatalogTableCreated() {
        Cursor cursor = mDB.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + CATALOG_TABLE + "'", null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
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
        if (!isTurnTableCreated()) {
            mDB.execSQL("create table " + TURN_TABLE + "("
                            + " name TEXT PRIMARY KEY, "
                            + " data BLOB NOT NULL);"
            );
        }

        if (!isPointTableCreated()) {
            mDB.execSQL("create table " + POINT_TABLE + "("
                            + " name TEXT PRIMARY KEY, "
                            + " data BLOB NOT NULL);"
            );
        }

        if (!isCatalogTableCreated()) {
            mDB.execSQL("create table " + CATALOG_TABLE + "("
                            + " name TEXT PRIMARY KEY, "
                            + " data BLOB NOT NULL);"
            );
        }
    }

    public void recordPoints(List<Position> positionList) {
        Log.e("haitao", "recordPoints called");
        PositionList plist = new PositionList();
        plist.setItems(positionList);
        plist.sortItems();
        ContentValues record = new ContentValues();
        Name name = new Name(Long.toString(plist.getStartTime()));
        Data data = new Data();
        data.setName(name);
        String documentAsString = null;
        try {
            documentAsString = objectMapper.writeValueAsString(plist.getItems());
            data.setContent(new Blob(documentAsString));
            record.put("name", name.toUri());

            ByteBuffer original = data.wireEncode().buf();
            ByteBuffer clone = ByteBuffer.allocate(original.capacity());
            original.rewind();//copy from the beginning
            clone.put(original);
            original.rewind();
            clone.flip();
            byte[] buffer = clone.array();
            record.put("data", buffer);

            mDB.insert(POINT_TABLE, null, record);
            Log.e("haitao", "finish recording");
            Log.e("haitao", data.wireEncode().buf().toString());
            Log.e("haitao", new String(buffer));
            Log.e("haitao", Integer.toString(buffer.length));

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public Data getPoints(Name name) {
        String[] columns = {"name", "data"};
        Cursor cursor = mDB.query(POINT_TABLE, columns, "name = \"" + name.toUri() + "\"", null, null, null, null);

        try {
            if (cursor.moveToNext()) {
                byte[] raw = cursor.getBlob(1);
                Data data = new Data();
                data.wireDecode(new Blob(raw));
                Log.e("haitao", data.getName().toUri());
                Log.e("haitao", data.getContent().toString());
                return data;
            }
        } catch (EncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Reset all data
     */
    public void resetData() {
        mDB.execSQL("delete from " + TURN_TABLE);
        mDB.execSQL("delete from " + POINT_TABLE);
        Log.i(TAG, "RESET Table");
    }
}
