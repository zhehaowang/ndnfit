package ucla.remap.ndnfit.network;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.data.CatalogCreator;
import ucla.remap.ndnfit.ndndb.NdnDBManager;

/**
 * Created by zhtaoxiang on 1/3/16.
 */
public class NetworkConnection {
    private static NdnDBManager mNdnDBManager;


    public NetworkConnection() {

    }

    public static void start() {
        mNdnDBManager = NdnDBManager.getInstance();
        CatalogCreator catalogCreator = new CatalogCreator();
        // Periodically create datalog
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        long currentTime = System.currentTimeMillis();
        long waitingTime = (currentTime / NDNFitCommon.CATALOG_TIME_RANGE + 1) * NDNFitCommon.CATALOG_TIME_RANGE
                - currentTime + TimeUnit.SECONDS.toMillis(10);
        scheduler.scheduleAtFixedRate(catalogCreator,  waitingTime,
                NDNFitCommon.CATALOG_TIME_RANGE, TimeUnit.MILLISECONDS);
    }

}
