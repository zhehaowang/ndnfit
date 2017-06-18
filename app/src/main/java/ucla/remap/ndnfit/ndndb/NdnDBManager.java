package ucla.remap.ndnfit.ndndb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encrypt.AndroidSqlite3ProducerDb;
import net.named_data.jndn.encrypt.EncryptError;
import net.named_data.jndn.encrypt.Producer;
import net.named_data.jndn.encrypt.ProducerDb;
import net.named_data.jndn.encrypt.Schedule;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.identity.AndroidSqlite3IdentityStorage;
import net.named_data.jndn.security.identity.FilePrivateKeyStorage;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.IdentityStorage;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.identity.PrivateKeyStorage;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;
import net.named_data.jndn.util.Blob;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import ucla.remap.ndnfit.MainActivity;
import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.data.Catalog;
import ucla.remap.ndnfit.data.Position;
import ucla.remap.ndnfit.data.PositionListProcessor;
//TODO: remove those Log.e("zhehao", ....)
public class NdnDBManager implements Serializable {
  private SQLiteDatabase mDB;
  private Context mCtx;
  private String mAppID;
  public static Name mAppCertificateName;
  public static KeyChain mKeyChain;
  private Producer dataProducer;
  private Face face;
  private String databaseFilePath;
  private ProducerDb database;
  private List<Name> CKeyList = new ArrayList<>();

  private static final ObjectMapper objectMapper = new ObjectMapper();
//    private final ElementReader elementReader;
  private static final String DB_NAME = "ndndb";
  private static final String TAG = "NdnDBManager";
  private static final String POINT_TABLE = "point_table";
  private static final String TURN_TABLE = "turn_table";
  private static final String CATALOG_TABLE = "catalog_table";
  private static final String CKEY_CATALOG_TABLE = "ckey_catalog_table";
  private static final String ENCRYPTED_CKEY_TABLE = "encrypted_ckey_table";

  private static NdnDBManager instance = new NdnDBManager();

  private NdnDBManager() {
    mAppID = "";
    try {
//      databaseFilePath = mCtx.getFilesDir().getAbsolutePath() + "/" + "producer.db";
//      database = new AndroidSqlite3ProducerDb(databaseFilePath);
      MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
      MemoryPrivateKeyStorage privateKeyStorage
        = new MemoryPrivateKeyStorage();
      mKeyChain = new KeyChain(
        new IdentityManager(identityStorage, privateKeyStorage),
        new SelfVerifyPolicyManager(identityStorage));
      /*mKeyChain.setFace(face);
      Name identityName = NDNFitCommon.USER_PREFIX;
      Name keyName = mKeyChain.generateRSAKeyPairAsDefault(identityName);
      Name certificateName = keyName.getSubName(0, keyName.size() - 1)
        .append("KEY").append(keyName.get(-1)).append("ID-CERT")
        .append("0");
      face.setCommandSigningInfo(mKeyChain, certificateName);*/
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public static NdnDBManager getInstance() {
    return instance;
  }

  public void init(Context ctx, Face face) {
    this.face = face;
    mCtx = ctx;
    databaseFilePath = mCtx.getFilesDir().getAbsolutePath() + "/" + "producer.db";
    database = new AndroidSqlite3ProducerDb(databaseFilePath);
    dataProducer = new Producer(
        NDNFitCommon.USER_PREFIX, NDNFitCommon.DATA_TYPE, face, mKeyChain, database);
    prepareDB();
    createTable();
  }

  public void setAppID(String appID, Name certName) {
    Log.d(TAG, "setAppId is called");
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
      dataProducer = new Producer(
        NDNFitCommon.USER_PREFIX, NDNFitCommon.DATA_TYPE, face, mKeyChain, database);
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

  protected boolean isCKeyCatalogTableCreated() {
    Cursor cursor = mDB.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + CKEY_CATALOG_TABLE + "'", null);
    if (cursor != null) {
      if (cursor.getCount() > 0) {
        cursor.close();
        return true;
      }
      cursor.close();
    }
    return false;
  }

  protected boolean isEncryptedCKeyTableCreated() {
    Cursor cursor = mDB.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + ENCRYPTED_CKEY_TABLE + "'", null);
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

    if(!isCKeyCatalogTableCreated()) {
      mDB.execSQL("create table " + CKEY_CATALOG_TABLE + "("
          + " timepoint TIMESTAMP PRIMARY KEY, "
          + " data BLOB NOT NULL, "
          + " uploaded integer DEFAULT 0);"
      );
    }

    if(!isEncryptedCKeyTableCreated()) {
      mDB.execSQL("create table " + ENCRYPTED_CKEY_TABLE + "("
          + " name TEXT PRIMARY KEY, "
          + " data BLOB NOT NULL, "
          + " uploaded integer DEFAULT 0);"
      );
    }
  }

  private void saveCKey(long hourPoint, List keys) {

    Log.d("saveCKey", "got " + keys.size() + " keys");
    ContentValues keyCatalogRecord = new ContentValues();
    keyCatalogRecord.put("timepoint", hourPoint);
    List<String> keyNameList = new ArrayList<>();
    for (Object key: keys) {
      keyNameList.add(((Data) key).getName().toUri());
    }
    Data keyNames = new Data();
    keyNames.setName(new Name(NDNFitCommon.CKEY_CATALOG_PREFIX).append(Schedule.toIsoString(hourPoint)));
    try {
      String documentAsString = objectMapper.writeValueAsString(keyNameList);
      keyNames.setContent(new Blob(documentAsString));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    if (mAppID != null && mAppID != "") {
      try {
        mKeyChain.sign(keyNames, mAppCertificateName);
        Log.e("zhehao", "Signing data point with ID: " + mAppCertificateName.toUri());
        Log.e("zhehao", "Produced: " + keyNames.getName().toUri());
      } catch (Exception e) {
        Log.e("zhehao", "Signing exception: " + e.getMessage());
      }
    }
    ByteBuffer original = keyNames.wireEncode().buf();
    ByteBuffer clone = ByteBuffer.allocate(original.capacity());
    original.rewind();//copy from the beginning
    clone.put(original);
    original.rewind();
    clone.flip();
    byte[] buffer = clone.array();
    keyCatalogRecord.put("data", buffer);
    keyCatalogRecord.put("uploaded", 0);
    mDB.insert(CKEY_CATALOG_TABLE, null, keyCatalogRecord);

    for (Object key: keys) {
      Data keyData = (Data) key;
      ContentValues keyRecord = new ContentValues();
      keyRecord.put("name", keyData.getName().toUri());
      original = keyData.wireEncode().buf();
      clone = ByteBuffer.allocate(original.capacity());
      original.rewind();//copy from the beginning
      clone.put(original);
      original.rewind();
      clone.flip();
      buffer = clone.array();
      keyRecord.put("data", buffer);
      keyRecord.put("uploaded", 0);
      mDB.insert(ENCRYPTED_CKEY_TABLE, null, keyRecord);
    }
  }

  /**
   * Given a list of positions, this function creates data and saves it into database
   * @param positionList
   * @param timepoint
   */
  public void recordPoints(List<Position> positionList, long timepoint) {
    Log.d(TAG, "recordPoints called, time: " + System.currentTimeMillis());
    Data previousData = getPoint(timepoint);
    ContentValues record = new ContentValues();
//    Name name = new Name(NDNFitCommon.DATA_PREFIX).appendTimestamp(timepoint);
    Data data = new Data();
//    data.setName(name);
    String documentAsString = null;
    try {
      if(dataProducer == null)
        System.out.println("the pointer is null");
      final long hourpoint = timepoint / 1000 / 3600000 * 3600000;
      Name contentKeyName = dataProducer.createContentKey((double)timepoint/1000,
        new Producer.OnEncryptedKeys() {
          @Override
          public void onEncryptedKeys(List keys) {
            Log.d(TAG, "onEncryptedKeys");
            saveCKey(hourpoint, keys);
          }
        },
      new EncryptError.OnError() {
        @Override
        public void onError(EncryptError.ErrorCode errorCode, String message) {
          Log.e(TAG, errorCode.toString() + message);
        }
      });
      /*
      // fake a c-key, for debug purpose
      if(!CKeyList.contains(contentKeyName)) {
        List<Data> keys = new ArrayList<>();
        Data fakedCkey = new Data();
        fakedCkey.setName(NDNFitCommon.CKEY_PREFIX.append("0"));
        fakedCkey.setContent(new Blob("OK"));
        keys.add(fakedCkey);
        saveCKey(hourpoint, keys);
        CKeyList.add(contentKeyName);
      }*/
      documentAsString = objectMapper.writeValueAsString(positionList);
      if(previousData == null)
//        data.setContent(new Blob(documentAsString));
        dataProducer.produce(data, (double)timepoint/1000, new Blob(documentAsString));
      else {
        String previousDataContent = previousData.getContent().toString();
        String content = previousDataContent.concat(documentAsString).replace("][",",");
//        data.setContent(new Blob(content));
        dataProducer.produce(data, (double)timepoint/1000, new Blob(content));
      }

      if (mAppID != null && mAppID != "") {
        try {
          mKeyChain.sign(data, mAppCertificateName);
          Log.e("zhehao", "Signing data point with ID: " + mAppCertificateName.toUri());
          Log.e("zhehao", "Produced: " + data.getName().toUri());
        } catch (Exception e) {
          Log.e("zhehao", "Signing exception: " + e.getMessage());
        }
      }

      Log.d("insert data point", data.getName().toUri());
      Log.d("timestamp", "" + timepoint);
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
    } catch (Exception e) {
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
      Log.d("insert data point", name.toUri());
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
      Cursor cursor;
      if(name.compare(NDNFitCommon.DATA_PREFIX) == 0) {
        cursor = mDB.query(POINT_TABLE, columns, null, null, null, null, "timepoint ASC");
      }
      else {
        cursor = mDB.query(POINT_TABLE, columns, "timepoint = " + (long) Schedule.fromIsoString(name.get(7).toEscapedString()) * 1000, null, null, null, null);
      }
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
    Name name = new Name(NDNFitCommon.CATALOG_PREFIX).append(Schedule.toIsoString(catalog.getCatalogTimePoint() / 1000));
    data.setName(name);
    Log.d("insert catalog", name.toUri());
    Log.d("timestamp", "" + catalog.getCatalogTimePoint());
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
        cursor = mDB.query(CATALOG_TABLE, columns, "timepoint = " + (long) Schedule.fromIsoString(name.get(8).toEscapedString()) * 1000,
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

  public Data getCKeyCatalog(Name name) {
    if (!NDNFitCommon.CKEY_CATALOG_PREFIX.match(name))
      return null;
    String[] columns = {"timepoint", "data", "uploaded"};
    try {
      Cursor cursor;
      if(name.compare(NDNFitCommon.CKEY_CATALOG_PREFIX) == 0) {
        cursor = mDB.query(CKEY_CATALOG_TABLE, columns, null, null, null, null, "timepoint ASC");
      } else {
        cursor = mDB.query(CKEY_CATALOG_TABLE, columns, "timepoint = " + (long) Schedule.fromIsoString(name.get(9).toEscapedString()), null, null, null, null);
      }
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

  public Data getCKey(Name name) {
    if (!NDNFitCommon.CKEY_PREFIX.match(name))
      return null;
    String[] columns = {"name", "data", "uploaded"};
    try {
      Cursor cursor = mDB.query(ENCRYPTED_CKEY_TABLE, columns, "name = \"" + name.toUri() + "\"", null, null, null, null);
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

  public Data readData(Name name) {
    if (NDNFitCommon.CKEY_CATALOG_PREFIX.match(name))
      return getCKeyCatalog(name);
    if (NDNFitCommon.CKEY_PREFIX.match(name))
      return getCKey(name);
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

  public Cursor getAllUnuploaedCKeyCatalog() {
    String[] columns = {"timepoint", "data", "uploaded"};
    Cursor cursor = mDB.query(CKEY_CATALOG_TABLE, columns, "uploaded = 0", null, null, null, null);
    return cursor;
  }

  public Cursor getAllUnuploadedCKey() {
    String[] columns = {"name", "data", "uploaded"};
    Cursor cursor = mDB.query(ENCRYPTED_CKEY_TABLE, columns, "uploaded = 0", null, null, null, null);
    return cursor;
  }

  public void markPointUploaded(Name name) {
    if (!NDNFitCommon.DATA_PREFIX.match(name))
      return;
    try {
      ContentValues record = new ContentValues();
      record.put("uploaded", 1);
      mDB.update(POINT_TABLE, record, "timepoint = " + (long) Schedule.fromIsoString(name.get(7).toEscapedString()) * 1000, null);
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
      mDB.update(CATALOG_TABLE, record, "timepoint = " + (long) Schedule.fromIsoString(name.get(8).toEscapedString()) * 1000 + " AND version = 1", null);
//            mDB.delete(CATALOG_TABLE, "timepoint = " + name.get(-2).toTimestamp() + " AND version = " + name.get(-1).toVersion(), null);
    } catch (EncodingException e) {
      e.printStackTrace();
    }
  }

  public void markCKeyCatalogUploaded(Name name) {
    if (!NDNFitCommon.CKEY_CATALOG_PREFIX.match(name))
      return;
    try {
      ContentValues record = new ContentValues();
      record.put("uploaded", 1);
      mDB.update(CKEY_CATALOG_TABLE, record, "timepoint = " + (long) Schedule.fromIsoString(name.get(9).toEscapedString()), null);
    } catch (EncodingException e) {
      e.printStackTrace();
    }
  }

  public void markCKeyUploaded(Name name) {
    if (!NDNFitCommon.CKEY_PREFIX.match(name))
      return;
    try {
      ContentValues record = new ContentValues();
      record.put("uploaded", 1);
      mDB.update(ENCRYPTED_CKEY_TABLE, record, "name = \"" + name.toUri() + "\"", null);
    } catch (Exception e) {
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
    mDB.execSQL("delete from " + CKEY_CATALOG_TABLE);
    mDB.execSQL("delete from " + ENCRYPTED_CKEY_TABLE);
    Log.i(TAG, "RESET Table");
  }
}
