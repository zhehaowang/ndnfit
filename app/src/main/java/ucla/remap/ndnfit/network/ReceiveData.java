package ucla.remap.ndnfit.network;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;

import ucla.remap.ndnfit.ndndb.NdnDBManager;

/**
 * Created by zhtaoxiang on 1/4/16.
 */
public class ReceiveData implements OnData {

    public ReceiveData() {
        ndnDBManager = NdnDBManager.getInstance();
    }

    @Override
    public void onData(Interest interest, Data data) {
        System.out.println("<< D: " + data.getName().toUri());
        // TODO:Delete the data stored in database

        //
    }

    private NdnDBManager ndnDBManager;
}
