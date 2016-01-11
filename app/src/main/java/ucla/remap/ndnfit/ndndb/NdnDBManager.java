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

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.data.Catalog;
import ucla.remap.ndnfit.data.CatalogCreator;
import ucla.remap.ndnfit.data.Position;
import ucla.remap.ndnfit.data.PositionListProcessor;
import ucla.remap.ndnfit.network.NetworkDaemon;

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
                            + " id integer PRIMARY KEY autoincrement, "
                            + " data BLOB NOT NULL);"
            );
        }

        if (!isPointTableCreated()) {
            mDB.execSQL("create table " + POINT_TABLE + "("
                            + " timepoint TIMESTAMP PRIMARY KEY, "
                            + " data BLOB NOT NULL);"
            );
        }

        if (!isCatalogTableCreated()) {
            mDB.execSQL("create table " + CATALOG_TABLE + "("
                            + " timepoint TIMESTAMP, "
                            + " version integer, "
                            + " data BLOB NOT NULL, "
                            + " primary key (timepoint, version));"
            );
        }
    }

    public void recordPoints(List<Position> positionList) {
        Log.e("haitao", "recordPoints called");
        PositionListProcessor plist = new PositionListProcessor();
        plist.setItems(positionList);
        plist.processItems();
        ContentValues record = new ContentValues();
        for (List<Position> oneList : plist.getGroupItems()) {
            Name name = new Name(NDNFitCommon.DATA_PREFIX).appendTimestamp(oneList.get(0).getTimeStamp());
            Data data = new Data();
            data.setName(name);
            String documentAsString = null;
            try {
                documentAsString = objectMapper.writeValueAsString(oneList);
                data.setContent(new Blob(documentAsString));
                record.put("timepoint", oneList.get(0).getTimeStamp());
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
                Log.e("haitao", Integer.toString(buffer.length));
                getPoints(name);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        CatalogCreator.createDatalog(plist.getTurn());
    }

    public Data getPoints(Name name) {
        if (name.getPrefix(-1).compare(NDNFitCommon.DATA_PREFIX) != 0)
            return null;
        String[] columns = {"timepoint", "data"};


        try {
            Cursor cursor = mDB.query(POINT_TABLE, columns, "timepoint = " + name.get(-1).toTimestamp(), null, null, null, null);
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

    public long getFirstPointTimestamp() {
        String[] columns = {"timepoint", "data"};
        Cursor cursor = mDB.query(POINT_TABLE, columns, null, null, null, null, "timepoint ASC");
        if (cursor.moveToNext()) {
            return cursor.getLong(0);
        }
        return 0;
    }

    public Cursor getPoints(long startTimestamp, long interval) {
        String[] columns = {"timepoint", "data"};
        long endTimestamp = startTimestamp + interval;
        Cursor cursor = mDB.query(POINT_TABLE, columns, "timepoint >= " + startTimestamp +
                " AND timepoint < " + endTimestamp, null, null, null, "timepoint ASC");
        return cursor;
    }

    public void insertCatalog(Catalog catalog) {
        Log.e("haitao", "insertCatalog called");
        //check the old version
        String[] columns = {"timepoint", "version", "data"};
        int version = 1;
        Cursor cursor = mDB.query(CATALOG_TABLE, columns, "timepoint = " + catalog.getCatalogTimePoint(),
                null, null, null, "version DESC");
        if (cursor.moveToNext()) {
            version = cursor.getInt(1) + 1;
        }


        //insert
        ContentValues record = new ContentValues();
        catalog.sortItems();
        Data data = new Data();
        Name name = new Name(NDNFitCommon.CATALOG_PREFIX).appendTimestamp(catalog.getCatalogTimePoint()).appendVersion(version);
        data.setName(name);
        String documentAsString = null;
        try {
            documentAsString = objectMapper.writeValueAsString(catalog.getPointTime());
            data.setContent(new Blob(documentAsString));
            record.put("timepoint", catalog.getCatalogTimePoint());
            record.put("version", version);
            ByteBuffer original = data.wireEncode().buf();
            ByteBuffer clone = ByteBuffer.allocate(original.capacity());
            original.rewind();//copy from the beginning
            clone.put(original);
            original.rewind();
            clone.flip();
            byte[] buffer = clone.array();
            record.put("data", buffer);
            mDB.insert(CATALOG_TABLE, null, record);
            Log.e("haitao", "finish recording");
            Log.e("haitao", Integer.toString(buffer.length));
            getCatalog(name);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public Data getCatalog(Name name) {
        if (name.getPrefix(-2).compare(NDNFitCommon.CATALOG_PREFIX) != 0)
            return null;
        String[] columns = {"timepoint", "version", "data"};


        try {
            Cursor cursor = mDB.query(CATALOG_TABLE, columns, "timepoint = " + name.get(-2).toTimestamp()
                    + " AND version = " + name.get(-1).toVersion(), null, null, null, null);
            if (cursor.moveToNext()) {
                byte[] raw = cursor.getBlob(2);
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

    public Data readData(Name name) {
        if (name.getPrefix(-1).compare(NDNFitCommon.DATA_PREFIX) == 0)
            return getPoints(name);
        if (name.getPrefix(-2).compare(NDNFitCommon.CATALOG_PREFIX) == 0)
            return getCatalog(name);
        return null;
    }

    public Cursor getAllPoints() {
        String[] columns = {"timepoint", "data"};
        Cursor cursor = mDB.query(POINT_TABLE, columns, null, null, null, null, null);
        return cursor;
    }

    public Cursor getAllCatalog() {
        String[] columns = {"timepoint", "version", "data"};
        Cursor cursor = mDB.query(CATALOG_TABLE, columns, null, null, null, null, null);
        return cursor;
    }

    public void deletePoint(Name name) {
        if (name.getPrefix(-1).compare(NDNFitCommon.DATA_PREFIX) != 0)
            return;
        try {
            mDB.delete(POINT_TABLE, "timepoint = " + name.get(-1).toTimestamp(), null);
        } catch (EncodingException e) {
            e.printStackTrace();
        }
    }

    public void deleteCatalog(Name name) {
        if (name.getPrefix(-2).compare(NDNFitCommon.CATALOG_PREFIX) != 0)
            return;
        try {
            mDB.delete(CATALOG_TABLE, "timepoint = " + name.get(-2).toTimestamp() + " AND version = " + name.get(-1).toVersion(), null);
        } catch (EncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reset all data
     */
    public void resetData() {
        mDB.execSQL("delete from " + TURN_TABLE);
        mDB.execSQL("delete from " + POINT_TABLE);
        mDB.execSQL("delete from " + CATALOG_TABLE);
        Log.i(TAG, "RESET Table");
    }
}
