package ucla.remap.ndnfit.db;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by nightzen on 5/25/15.
 */
public class TrackContract {
//    public static final String DB_NAME = "edu.ucla.remap.ndnfit.db.track";
    public static final String DB_NAME = "ndnfit2.db";
    public static final int DB_VERSION = 1;
    public static final String TURN_TABLE = "t_turn";
    public static final String POINT_TABLE = "t_point";

    // 새로 추가
    public static final String AUTHORITY = "edu.ucal.remap.ndnfit";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TURN_TABLE);
    public static final int TURN_LIST = 1;
    public static final int TURN_ITEM = 2;
    public static final String CONTENT_TURN_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/ndnfit.turnDB/"+TURN_TABLE;
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/ndnfit/turnDB" + TURN_TABLE;
    // 새로 추가

    public class TurnColumns {
        public static final String START_TIME = "start_time";
        public static final String FINISH_TIME = "finish_time";
        public static final String _ID = BaseColumns._ID;
    }
}
