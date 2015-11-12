package ucla.remap.ndnfit.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by nightzen on 5/25/15.
 */
public class TrackDBHelper extends SQLiteOpenHelper {

    public TrackDBHelper(Context context) {
        super(context, TrackContract.DB_NAME, null, TrackContract.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqlDB) {

        String sqlQuery =
                String.format("CREATE TABLE %s (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "%s TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                "%s TIMESTAMP DEFAULT CURRENT_TIMESTAMP)", TrackContract.TURN_TABLE,
                        TrackContract.TurnColumns.START_TIME,
                        TrackContract.TurnColumns.FINISH_TIME);

        Log.d("TrackDBHelper", "Query to form table: " + sqlQuery);
        sqlDB.execSQL(sqlQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqlDB, int i, int i2) {
        sqlDB.execSQL("DROP TABLE IF EXISTS " + TrackContract.TURN_TABLE);
        onCreate(sqlDB);
    }
}