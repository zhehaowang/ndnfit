package ucla.remap.ndnfit.data;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.data.CatalogCreator;
import ucla.remap.ndnfit.ndndb.NdnDBManager;

/**
 * Created by zhtaoxiang on 1/7/16.
 */
public class CatalogDaemon {

    public static void startCreatingCatalog(ScheduledExecutorService scheduler) {
        CatalogCreator catalogCreator = new CatalogCreator();
        // Periodically create datalog
        long currentTime = System.currentTimeMillis();
        long waitingTime = (currentTime / NDNFitCommon.CATALOG_TIME_RANGE + 1) * NDNFitCommon.CATALOG_TIME_RANGE
                - currentTime + TimeUnit.SECONDS.toMillis(10);
        scheduler.scheduleAtFixedRate(catalogCreator, waitingTime,
                NDNFitCommon.CATALOG_TIME_RANGE, TimeUnit.MILLISECONDS);
    }


}
