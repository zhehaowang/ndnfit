package ucla.remap.ndnfit.ndndb;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.data.Turn;

/**
 * Created by zhtaoxiang on 1/31/16.
 */
public class NdnDBDHelper {
    private static NdnDBManager ndnDBManager = NdnDBManager.getInstance();
    private static final String TAG = "NdnDBDHelper";
    /**
     * Based on last turn's end time (micro seconds), delete useless data
     * @param lastTurn
     */
    public static void deleteUselessData(Turn lastTurn) {
        long timepoint = (lastTurn.getStartTimeStamp() /
                NDNFitCommon.CATALOG_TIME_RANGE) * NDNFitCommon.CATALOG_TIME_RANGE;
        ndnDBManager.deleteUploadedUpdateInfoExceptLast();
        ndnDBManager.deleteUploadedCatalogBefore(timepoint);
        ndnDBManager.deleteUploadedPointsBefore(timepoint);
    }
}
