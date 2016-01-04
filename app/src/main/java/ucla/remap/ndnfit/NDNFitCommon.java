package ucla.remap.ndnfit;

import net.named_data.jndn.Name;

import java.util.concurrent.TimeUnit;

/**
 * Created by zhtaoxiang on 1/3/16.
 */
public class NDNFitCommon {
    public static final Name DATA_PREFIX = new Name("/org/openmhealth/haitao/data/fitness/physical_activity/time_location");

    public static final Name CATALOG_PREFIX = new Name(DATA_PREFIX).append("catalog");

    public static final Name CONFIRM_PREFIX = new Name(DATA_PREFIX).append("comfirm");

    public static final long CATALOG_TIME_RANGE = TimeUnit.MINUTES.toMillis(10);
}
