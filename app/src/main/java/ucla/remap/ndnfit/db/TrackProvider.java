package ucla.remap.ndnfit.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Created by nightzen on 5/25/15.
 */
public class TrackProvider extends ContentProvider {
    private SQLiteDatabase db;
    private TrackDBHelper taskDBHelper;
    public static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(TrackContract.AUTHORITY,TrackContract.TURN_TABLE,TrackContract.TURN_LIST);
        uriMatcher.addURI(TrackContract.AUTHORITY,TrackContract.TURN_TABLE+"/#",TrackContract.TURN_ITEM);
    }

    @Override
    public boolean onCreate() {
        boolean ret = true;
        taskDBHelper = new TrackDBHelper(getContext());
        db = taskDBHelper.getWritableDatabase();

        if (db == null) {
            ret = false;
        }

        if (db.isReadOnly()) {
            db.close();
            db = null;
            ret = false;
        }

        return ret;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TrackContract.TURN_TABLE);

        switch (uriMatcher.match(uri)) {
            case TrackContract.TURN_LIST:
                break;

            case TrackContract.TURN_ITEM:
                qb.appendWhere(TrackContract.TurnColumns._ID + " = "+ uri.getLastPathSegment());
                break;

            default:
                throw new IllegalArgumentException("Invalid URI: " + uri);
        }

        Cursor cursor = qb.query(db,projection,selection,selectionArgs,null,null,null);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case TrackContract.TURN_LIST:
                return TrackContract.CONTENT_TURN_TYPE;

            case TrackContract.TURN_ITEM:
                return TrackContract.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Invalid URI: "+uri);
        }    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        if (uriMatcher.match(uri) != TrackContract.TURN_LIST) {
            throw new IllegalArgumentException("Invalid URI: "+uri);
        }

        long id = db.insert(TrackContract.TURN_TABLE,null,contentValues);

        if (id>0) {
            return ContentUris.withAppendedId(uri, id);
        }
        throw new SQLException("Error inserting into table: "+TrackContract.TURN_TABLE);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int deleted = 0;

        switch (uriMatcher.match(uri)) {
            case TrackContract.TURN_LIST:
                db.delete(TrackContract.TURN_TABLE,selection,selectionArgs);
                break;

            case TrackContract.TURN_ITEM:
                String where = TrackContract.TurnColumns._ID + " = " + uri.getLastPathSegment();
                if (!selection.isEmpty()) {
                    where += " AND "+selection;
                }

                deleted = db.delete(TrackContract.TURN_TABLE,where,selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Invalid URI: "+uri);
        }

        return deleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        int updated = 0;

        switch (uriMatcher.match(uri)) {
            case TrackContract.TURN_LIST:
                db.update(TrackContract.TURN_TABLE,contentValues,s,strings);
                break;

            case TrackContract.TURN_ITEM:
                String where = TrackContract.TurnColumns._ID + " = " + uri.getLastPathSegment();
                if (!s.isEmpty()) {
                    where += " AND "+s;
                }
                updated = db.update(TrackContract.TURN_TABLE,contentValues,where,strings);
                break;

            default:
                throw new IllegalArgumentException("Invalid URI: "+uri);
        }

        return updated;
    }
}
