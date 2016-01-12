package ucla.remap.ndnfit.network;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnTimeout;

import java.io.IOException;

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
        System.out.println("<< I: " + interest.toUri());
        try {
            Name dataName = interest.getName();
            Data data = ndnDBManager.readData(dataName);
            System.out.println(dataName.toUri());
            if (data != null) {
                face.putData(data);
                System.out.println(">> D: " + data.getContent().toString());
            }
        } catch (IOException ex) {
            System.out.println("exception: " + ex.getMessage());
        }
    }
}
