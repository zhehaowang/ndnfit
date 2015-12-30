package ucla.remap.ndnfit.ndndb;

import android.database.sqlite.SQLiteDatabase;

import org.junit.Test;

/**
 * Created by zhanght on 12/30/2015.
 */
public class NdnDBManagerTest {
    @Test
    public void test() {
        NdnDBManager manager = NdnDBManager.getInstance();
        manager.initForTest();
    }
}
