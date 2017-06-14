package ucla.remap.ndnfit.data;

import android.database.Cursor;
import android.util.Log;

import net.named_data.jndn.encrypt.Schedule;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.ndndb.NdnDBManager;
import ucla.remap.ndnfit.network.NetworkDaemon;

/**
 * Created by zhtaoxiang on 1/3/16.
 */
public class CatalogCreator implements Runnable {
    private static final String TAG = "CatalogCreator";
    // What is the last time to group the data
    private long lastRunTime = 0;
    private static NdnDBManager mNdnDBManager = NdnDBManager.getInstance();

    @Override
    public void run() {
        createCatalog();
    }

    public void createCatalog() {
        Log.d(TAG, "createCatalog is called, time: " + System.currentTimeMillis());
        lastRunTime = mNdnDBManager.getLastCatalogTimestamp() + NDNFitCommon.CATALOG_TIME_RANGE;
        if (lastRunTime == NDNFitCommon.CATALOG_TIME_RANGE) {
            //This indicates that it is the first time to run the code, do some initiation
            long theStartTime = mNdnDBManager.getFirstPointTimestamp();
            if (theStartTime == 0) { // No such type of data
                return;
            }
            lastRunTime = (theStartTime / NDNFitCommon.CATALOG_TIME_RANGE) * NDNFitCommon.CATALOG_TIME_RANGE;
            NetworkDaemon.registerOnDsu(lastRunTime);
        }
        // Group all the data till the timepoint (this timepoint should be a
        // multiple of timeInterval) before the current time
        while (lastRunTime + NDNFitCommon.CATALOG_TIME_RANGE < System.currentTimeMillis() * 1000) {
            // get all the data falling in the time interval and
            Cursor cursor = mNdnDBManager.getPoints(lastRunTime,
                    NDNFitCommon.CATALOG_TIME_RANGE);
            Catalog catalog = new Catalog();
            catalog.setCatalogTimePoint(lastRunTime);
            while (cursor.moveToNext()) {
                catalog.addPointTime(Schedule.toIsoString(cursor.getLong(0)/1000));
            }
            Log.e("the size of ","");
            if (catalog.getPointTime() != null && !catalog.getPointTime().isEmpty()) {
                mNdnDBManager.insertCatalog(catalog);
                NetworkDaemon.registerOnDsu(lastRunTime);
            }
            lastRunTime += NDNFitCommon.CATALOG_TIME_RANGE;
        }
    }

}
