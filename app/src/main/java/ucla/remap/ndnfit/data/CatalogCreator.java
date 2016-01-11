package ucla.remap.ndnfit.data;

import android.database.Cursor;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Name;

import java.util.concurrent.TimeUnit;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.ndndb.NdnDBManager;

/**
 * Created by zhtaoxiang on 1/3/16.
 */
public class CatalogCreator implements Runnable {
    // What is the last time to group the data
    private long lastRunTime = 0;
    private static NdnDBManager mNdnDBManager = NdnDBManager.getInstance();

    @Override
    public void run() {
        createCatalog();
    }

    public void createCatalog() {
        Log.e("haitao", "createCatalog is called");
        if (lastRunTime == 0) {
            Log.e("haitao", Long.toString(lastRunTime));
            //This indicates that it is the first time to run the code, do some initiation
            long theStartTime = mNdnDBManager.getFirstPointTimestamp();
            if (theStartTime == 0) { // No such type of data
                return;
            }
            lastRunTime = (theStartTime / NDNFitCommon.CATALOG_TIME_RANGE) * NDNFitCommon.CATALOG_TIME_RANGE;
        }
        // Group all the data till the timepoint (this timepoint should be a
        // multiple of timeInterval) before the current time
        while (lastRunTime + NDNFitCommon.CATALOG_TIME_RANGE < System.currentTimeMillis()) {
            Log.e("haitao", Long.toString(lastRunTime));
            // get all the data falling in the time interval and
            Cursor cursor = mNdnDBManager.getPoints(lastRunTime,
                    NDNFitCommon.CATALOG_TIME_RANGE);
            lastRunTime += NDNFitCommon.CATALOG_TIME_RANGE;

            // no data falls into this time interval
            if (cursor.getColumnCount() == 0) {
                continue;
            }

            Catalog catalog = new Catalog();
            catalog.setCatalogTimePoint(lastRunTime - NDNFitCommon.CATALOG_TIME_RANGE);
            while (cursor.moveToNext()) {
                catalog.addPointTime(cursor.getLong(0));
            }
            mNdnDBManager.insertCatalog(catalog);
        }
    }

    public static void createDatalog(Turn turn) {
        long startTimepoint = (turn.getStartTimeStamp() / NDNFitCommon.CATALOG_TIME_RANGE) * NDNFitCommon.CATALOG_TIME_RANGE;
        long endTimepoint = (turn.getFinishTimeStamp() / NDNFitCommon.CATALOG_TIME_RANGE + 1) * NDNFitCommon.CATALOG_TIME_RANGE;
        while(startTimepoint < endTimepoint) {
            Cursor cursor = mNdnDBManager.getPoints(startTimepoint,
                    NDNFitCommon.CATALOG_TIME_RANGE);
            startTimepoint += NDNFitCommon.CATALOG_TIME_RANGE;
            // no data falls into this time interval
            if (cursor.getColumnCount() == 0) {
                continue;
            }
            Catalog catalog = new Catalog();
            catalog.setCatalogTimePoint(startTimepoint - NDNFitCommon.CATALOG_TIME_RANGE);
            while (cursor.moveToNext()) {
                catalog.addPointTime(cursor.getLong(0));
            }
            mNdnDBManager.insertCatalog(catalog);
        }
    }
}
