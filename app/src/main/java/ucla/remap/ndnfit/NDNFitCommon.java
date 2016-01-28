package ucla.remap.ndnfit;

import net.named_data.jndn.Name;

import java.util.concurrent.TimeUnit;

/**
 * Created by zhtaoxiang on 1/3/16.
 */
public class NDNFitCommon {
    public static Name DATA_PREFIX = new Name("/org/openmhealth/haitao/data/fitness/physical_activity/time_location");

    public static Name CATALOG_PREFIX = new Name(DATA_PREFIX).append("catalog");

    public static Name UPDATE_INFO_PREFIX = new Name(DATA_PREFIX).append("update_info");

    public static Name UPDATE_INFO_CONFIRM_PREFIX = new Name("/edu/ucla/remap/dsu").append(UPDATE_INFO_PREFIX);

    public static Name CATALOG_CONFIRM_PREFIX = new Name("/edu/ucla/remap/dsu").append(new Name(CATALOG_PREFIX));

    public static Name DATA_CONFIRM_PREFIX =  new Name("/edu/ucla/remap/dsu").append(new Name(DATA_PREFIX));

    public static final Name REPO_COMMAND_PREFIX = new Name("/ndn/edu/ucla/remap/ndnfit/repo");;

    public static final long CATALOG_TIME_RANGE = TimeUnit.MINUTES.toMicros(10);

    public static final long UPLOAD_TIME_INTERVAL = TimeUnit.MINUTES.toMicros(1);

    public static void setDataPrefix(Name userPrefix) {
        DATA_PREFIX = new Name(userPrefix).append(new Name("data/fitness/physical_activity/time_location"));
        CATALOG_PREFIX = new Name(DATA_PREFIX).append("catalog");
        UPDATE_INFO_PREFIX = new Name(DATA_PREFIX).append("update_info");
        DATA_CONFIRM_PREFIX = new Name("/edu/ucla/remap").append(new Name(DATA_PREFIX));
        CATALOG_CONFIRM_PREFIX = new Name("/edu/ucla/remap").append(new Name(CATALOG_PREFIX));
        UPDATE_INFO_CONFIRM_PREFIX = new Name("/edu/ucla/remap/dsu").append(UPDATE_INFO_PREFIX);
    }
}
