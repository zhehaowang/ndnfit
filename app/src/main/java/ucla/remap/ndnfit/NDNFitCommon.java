package ucla.remap.ndnfit;

import net.named_data.jndn.Link;
import net.named_data.jndn.Name;

import java.util.concurrent.TimeUnit;

public class NDNFitCommon {
  public static Name USER_PREFIX = new Name("/org/openmhealth/haitao");

  public static Name DATA_TYPE = new Name("/fitness/physical_activity/time_location");

  public static Name DATA_PREFIX = new Name("/org/openmhealth/haitao/SAMPLE/fitness/physical_activity/time_location");

  public static Name CATALOG_PREFIX = new Name(DATA_PREFIX).append("catalog");

  public static Name UPDATE_INFO_PREFIX = new Name(DATA_PREFIX).append("update_info");

//    public static Name UPDATE_INFO_CONFIRM_PREFIX = new Name("/org/openmhealth/dsu/confirm").append(UPDATE_INFO_PREFIX);
//
//    public static Name CATALOG_CONFIRM_PREFIX = new Name("/org/openmhealth/dsu/confirm").append(new Name(CATALOG_PREFIX));
//
//    public static Name DATA_CONFIRM_PREFIX =  new Name("/org/openmhealth/dsu/confirm").append(new Name(DATA_PREFIX));

  public static Name CKEY_PREFIX = new Name(DATA_PREFIX).append("C-KEY");

  public static Name CKEY_CATALOG_PREFIX = new Name(CKEY_PREFIX).append("catalog");

  public static Name LOCAL_HUB_PREFIX = new Name();

  public static Link LINK_OBJECT = null;

  public static final Name CONFIRM_PREFIX = new Name("/org/openmhealth/dsu/confirm");

  public static final Name REGISTER_PREFIX = new Name("/org/openmhealth/dsu/register");

  public static final Name DISCOVER_LOCAL_HUB_PREFIX = new Name("/localhop/nfd/rib/routable-prefixes");

  public static final long CATALOG_TIME_RANGE = TimeUnit.MINUTES.toMicros(2);

  public static final long FETCH_CONFIRMATION_TIME_INTERVAL = TimeUnit.MINUTES.toMicros(1);

  public static final long ONE_MINUTE = TimeUnit.MINUTES.toMicros(1);

  public static void setDataPrefix(Name userPrefix) {
    USER_PREFIX = new Name(userPrefix);
    DATA_PREFIX = new Name(userPrefix).append(new Name("SAMPLE/fitness/physical_activity/time_location"));
    CATALOG_PREFIX = new Name(DATA_PREFIX).append("catalog");
    UPDATE_INFO_PREFIX = new Name(DATA_PREFIX).append("update_info");
    CKEY_PREFIX = new Name(DATA_PREFIX).append("C-KEY");
    CKEY_CATALOG_PREFIX = new Name(CKEY_PREFIX).append("catalog");
//        DATA_CONFIRM_PREFIX = new Name("/org/openmhealth/dsu/confirm").append(new Name(DATA_PREFIX));
//        CATALOG_CONFIRM_PREFIX = new Name("/org/openmhealth/dsu/confirm").append(new Name(CATALOG_PREFIX));
//        UPDATE_INFO_CONFIRM_PREFIX = new Name("/org/openmhealth/dsu/confirm").append(UPDATE_INFO_PREFIX);
  }
}
