package ucla.remap.ndnfit.network;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnTimeout;

import ucla.remap.ndnfit.ndndb.NdnDBManager;

/**
 * Created by zhtaoxiang on 1/4/16.
 */
public class ReceiveInterest implements OnInterestCallback {
    private OnData onData;
    private OnTimeout onTimeout;
    private Face face;
    private NdnDBManager ndnDBManager;
    public ReceiveInterest(Face face) {
        this.face = face;
        ndnDBManager = NdnDBManager.getInstance();
        onData = new ReceiveData();
        onTimeout = new RequestDataTimeOut();
    }

    @Override
    public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {

    }
}
