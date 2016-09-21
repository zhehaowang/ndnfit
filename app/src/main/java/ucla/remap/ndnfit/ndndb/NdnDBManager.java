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
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.identity.AndroidSqlite3IdentityStorage;
import net.named_data.jndn.security.identity.FilePrivateKeyStorage;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.IdentityStorage;
import net.named_data.jndn.security.identity.PrivateKeyStorage;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;
import net.named_data.jndn.util.Blob;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

import ucla.remap.ndnfit.MainActivity;
import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.data.Catalog;
import ucla.remap.ndnfit.data.Position;
import ucla.remap.ndnfit.data.PositionListProcessor;

/**
 * Created by zhanght on 2015/12/27.
 */
public class NdnDBManager implements Serializable {
  private SQLiteDatabase mDB;
  private Context mCtx;
  private String mAppID;
  private Name mAppCertificateName;
  private KeyChain mKeyChain;

  private static final ObjectMapper objectMapper = new ObjectMapper();
//    private final ElementReader elementReader;

  private static final String DB_NAME = "ndndb";
  private static final String TAG = "NdnDBManager";
  private static final String POINT_TABLE = "point_table";
  private static final String TURN_TABLE = "turn_table";
  private static final String CATALOG_TABLE = "catalog_table";

  private static NdnDBManager instance = new NdnDBManager();

  private NdnDBManager() {
    mAppID = "";
  }

  public static NdnDBManager getInstance() {
    return instance;
  }

  public void init(Context ctx) {
    mCtx = ctx;
    prepareDB();
    createTable();
  }

  public void setAppID(String appID, Name certName) {
    mAppID = appID;
    mAppCertificateName = new Name(certName);

    if (mAppID != null && mAppID != "") {
      String dbPath = mCtx.getFilesDir().getAbsolutePath() + "/" + MainActivity.DB_NAME;
      String certDirPath = mCtx.getFilesDir().getAbsolutePath() + "/" + MainActivity.CERT_DIR;

      IdentityStorage identityStorage = new AndroidSqlite3IdentityStorage(dbPath);
      PrivateKeyStorage privateKeyStorage = new FilePrivateKeyStorage(certDirPath);
      IdentityManager identityManager = new IdentityManager(identityStorage, privateKeyStorage);

      // For now, the verification policy manager, and the face to fetch required cert does not matter
      // So we use SelfVerifyPolicyManager, and don't call KeyChain.setFace()
      mKeyChain = new KeyChain(identityManager, new SelfVerifyPolicyManager(identityStorage));
    }
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
  protected boolean isTurnTableCreated() {
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
          + " data BLOB NOT NULL, "
          + " uploaded integer DEFAULT 0);"
      );
    }

    if (!isCatalogTableCreated()) {
      mDB.execSQL("create table " + CATALOG_TABLE + "("
          + " timepoint TIMESTAMP, "
          + " version integer, "
          + " data BLOB NOT NULL, "
          + " uploaded integer DEFAULT 0, "
          + " primary key (timepoint, version));"
      );
    }
  }

  public void recordPoints(List<Position> positionList, long timepoint) {
    Log.d(TAG, "recordPoints called, time: " + System.currentTimeMillis());
    Data previousData = getPoint(timepoint);
    ContentValues record = new ContentValues();
    Name name = new Name(NDNFitCommon.DATA_PREFIX).appendTimestamp(timepoint);
    Data data = new Data();
    data.setName(name);
    String documentAsString = null;
    try {
      documentAsString = objectMapper.writeValueAsString(positionList);
      if(previousData == null)
        data.setContent(new Blob(documentAsString));
      else {
        String previousDataContent = previousData.getContent().toString();
        String content = previousDataContent.concat(documentAsString).replace("][",",");
        data.setContent(new Blob(content));
      }

      if (mAppID != null && mAppID != "") {
        try {
          mKeyChain.sign(data, mAppCertificateName);
          Log.e("zhehao", "Signing data point with ID: " + mAppCertificateName.toUri());
          Log.e("zhehao", "Produced: " + name.toUri());
        } catch (Exception e) {
          Log.e("zhehao", "Signing exception: " + e.getMessage());
        }
      }

      Log.e("insert data point", name.toUri());
      Log.e("timestamp", "" + timepoint);
      record.put("timepoint", timepoint);
      ByteBuffer original = data.wireEncode().buf();
      ByteBuffer clone = ByteBuffer.allocate(original.capacity());
      original.rewind();//copy from the beginning
      clone.put(original);
      original.rewind();
      clone.flip();
      byte[] buffer = clone.array();
      record.put("data", buffer);
      record.put("uploaded", 0);
      if(previousData == null)
        mDB.insert(POINT_TABLE, null, record);
      else
        mDB.update(POINT_TABLE, record, "timepoint = " + timepoint, null);
      Log.d(TAG, "finish recording");
      Log.d(TAG, Integer.toString(buffer.length));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  public void recordPoints(List<Position> positionList) {
    Log.d(TAG, "recordPoints called, time: " + System.currentTimeMillis());
    PositionListProcessor plist = new PositionListProcessor();
    plist.setItems(positionList);
    plist.processItems();
    ContentValues record = new ContentValues();
    for (List<Position> oneList : plist.getGroupItems()) {
      Name name = new Name(NDNFitCommon.DATA_PREFIX).appendTimestamp(oneList.get(0).getTimeStamp());
      Data data = new Data();
      data.setName(name);
      Log.e("insert data point", name.toUri());
      String documentAsString = null;
      try {
        documentAsString = objectMapper.writeValueAsString(oneList);
        data.setContent(new Blob(documentAsString));

        if (mAppID != null && mAppID != "") {
          try {
            mKeyChain.sign(data, mAppCertificateName);
            Log.e("zhehao", "Signing data point with ID: " + mAppCertificateName.toUri());
            Log.e("zhehao", "Produced: " + name.toUri());
          } catch (Exception e) {
            Log.e("zhehao", "Signing exception: " + e.getMessage());
          }
        }

        record.put("timepoint", oneList.get(0).getTimeStamp());
        ByteBuffer original = data.wireEncode().buf();
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();//copy from the beginning
        clone.put(original);
        original.rewind();
        clone.flip();
        byte[] buffer = clone.array();
        record.put("data", buffer);
        record.put("uploaded", 0);
        mDB.insert(POINT_TABLE, null, record);
        Log.d(TAG, "finish recording");
        Log.d(TAG, Integer.toString(buffer.length));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }
    //create catalog and update information, it should be together with data point recording
    // function, that's why we put it here
//    CatalogAndUpdateInfoCreator.createDatalogAndUpdateInfo(plist.getTurn());
    // delete useless data
    // TODO: move this to a proper place
    NdnDBDHelper.deleteUselessData(plist.getTurn());
  }

  /**
   * Use this method to combine data in the same timepoint together
   *
   * @param timepoint
   * @return
   */
  public Data getPoint(long timepoint) {
    String[] columns = {"timepoint", "data", "uploaded"};
    try {
      Cursor cursor = mDB.query(POINT_TABLE, columns, "timepoint = " + timepoint, null, null, null, null);
      if (cursor.moveToNext()) {
        byte[] raw = cursor.getBlob(1);
        Data data = new Data();
        data.wireDecode(new Blob(raw));
        return data;
      }
    } catch (EncodingException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Data getPoints(Name name) {
    if (!NDNFitCommon.DATA_PREFIX.match(name))
      return null;
    String[] columns = {"timepoint", "data", "uploaded"};


    try {
      Cursor cursor = mDB.query(POINT_TABLE, columns, "timepoint = " + name.get(-1).toTimestamp(), null, null, null, null);
      if (cursor.moveToNext()) {
        byte[] raw = cursor.getBlob(1);
        Data data = new Data();
        data.wireDecode(new Blob(raw));
        return data;
      }
    } catch (EncodingException e) {
      e.printStackTrace();
    }
    return null;
  }

  public long getFirstPointTimestamp() {
    String[] columns = {"timepoint", "data", "uploaded"};
    Cursor cursor = mDB.query(POINT_TABLE, columns, null, null, null, null, "timepoint ASC");
    if (cursor.moveToNext()) {
      return cursor.getLong(0);
    }
    return 0;
  }

  public long getLastCatalogTimestamp() {
    String[] columns = {"timepoint", "version", "data", "uploaded"};
    Cursor cursor = mDB.query(CATALOG_TABLE, columns, null, null, null, null, "timepoint DESC");
    if (cursor.moveToNext()) {
      return cursor.getLong(0);
    }
    return 0;
  }

  /**
   * get all the data points falling into {startTimestamp, startTimestamp+interval}, both "uploaded"
   * and not "uploaded"
   *
   * @param startTimestamp
   * @param interval
   * @return
   */
  public Cursor getPoints(long startTimestamp, long interval) {
    String[] columns = {"timepoint", "data", "uploaded"};
    long endTimestamp = startTimestamp + interval;
    Cursor cursor = mDB.query(POINT_TABLE, columns, "timepoint >= " + startTimestamp +
      " AND timepoint < " + endTimestamp, null, null, null, "timepoint ASC");
    return cursor;
  }

  public int insertCatalog(Catalog catalog) {
    Log.d(TAG, "insertCatalog called, time: " + System.currentTimeMillis());
    int version = 1;

    //insert
    ContentValues record = new ContentValues();
    catalog.sortItems();
    Data data = new Data();
    Name name = new Name(NDNFitCommon.CATALOG_PREFIX).appendTimestamp(catalog.getCatalogTimePoint());
    data.setName(name);
    Log.e("insert catalog", name.toUri());
    Log.e("timestamp", "" + catalog.getCatalogTimePoint());
    String documentAsString = null;
    try {
      documentAsString = objectMapper.writeValueAsString(catalog.getPointTime());
      data.setContent(new Blob(documentAsString));

      if (mAppID != null && mAppID != "") {
        try {
          mKeyChain.sign(data, mAppCertificateName);
          Log.e("zhehao", "Signing catalog with ID: " + mAppCertificateName.toUri());
        } catch (Exception e) {
          Log.e("zhehao", "Signing exception: " + e.getMessage());
        }
      }

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
      record.put("uploaded", 0);
      mDB.insert(CATALOG_TABLE, null, record);
      Log.d(TAG, "finish recording");
      Log.d(TAG, Integer.toString(buffer.length));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return version;
  }

  public Data getCatalog(Name name) {
    if (!NDNFitCommon.CATALOG_PREFIX.match(name))
      return null;
    String[] columns = {"timepoint", "version", "data", "uploaded"};


    try {
      Cursor cursor;
      if(name.compare(NDNFitCommon.CATALOG_PREFIX) == 0) {
        cursor = mDB.query(CATALOG_TABLE, columns, null, null, null, null, "timepoint ASC");
      }
      else{
        cursor = mDB.query(CATALOG_TABLE, columns, "timepoint = " + name.get(-1).toTimestamp(),
          null, null, null, null);
      }
      if (cursor.moveToNext()) {
        byte[] raw = cursor.getBlob(2);
        Data data = new Data();
        data.wireDecode(new Blob(raw));
        return data;
      }
    } catch (EncodingException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Data readData(Name name) {
    if (NDNFitCommon.CATALOG_PREFIX.match(name))
      return getCatalog(name);
    if (NDNFitCommon.DATA_PREFIX.match(name))
      return getPoints(name);

    return null;
  }

  public Cursor getAllUnuploadedPoints() {
    String[] columns = {"timepoint", "data", "uploaded"};
    Cursor cursor = mDB.query(POINT_TABLE, columns, "uploaded = 0", null, null, null, null);
    return cursor;
  }

  public Cursor getAllUnuploadedCatalog() {
    String[] columns = {"timepoint", "version", "data", "uploaded"};
    Cursor cursor = mDB.query(CATALOG_TABLE, columns, "uploaded = 0", null, null, null, null);
    return cursor;
  }

  public void markPointUploaded(Name name) {
    if (name.getPrefix(-1).compare(NDNFitCommon.DATA_PREFIX) != 0)
      return;
    try {
      ContentValues record = new ContentValues();
      record.put("uploaded", 1);
      mDB.update(POINT_TABLE, record, "timepoint = " + name.get(-1).toTimestamp(), null);
//            mDB.delete(POINT_TABLE, "timepoint = " + name.get(-1).toTimestamp(), null);
    } catch (EncodingException e) {
      e.printStackTrace();
    }
  }

  public void markCatalogUploaded(Name name) {
    if (name.getPrefix(-1).compare(NDNFitCommon.CATALOG_PREFIX) != 0)
      return;
    try {
      ContentValues record = new ContentValues();
      record.put("uploaded", 1);
      mDB.update(CATALOG_TABLE, record, "timepoint = " + name.get(-1).toTimestamp() + " AND version = 1", null);
//            mDB.delete(CATALOG_TABLE, "timepoint = " + name.get(-2).toTimestamp() + " AND version = " + name.get(-1).toVersion(), null);
    } catch (EncodingException e) {
      e.printStackTrace();
    }
  }

  public void deleteUploadedPointsBefore(long timestamp) {
    Log.d(TAG, "deleteUploadedPointsBefore");
    mDB.delete(POINT_TABLE, "timepoint < " + timestamp + " AND uploaded = 1", null);
  }

  public void deleteUploadedCatalogBefore(long timestamp) {
    Log.d(TAG, "deleteUploadedCatalogBefore");
    mDB.delete(CATALOG_TABLE, "timepoint < " + timestamp + " AND uploaded = 1", null);
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
