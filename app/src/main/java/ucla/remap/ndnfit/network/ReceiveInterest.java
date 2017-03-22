package ucla.remap.ndnfit.network;

import android.util.Log;

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
    private static final String TAG = "ReceiveInterest";

    public ReceiveInterest(Face face) {
        this.face = face;
        ndnDBManager = NdnDBManager.getInstance();
        onData = new ReceiveData();
        onTimeout = new RequestDataTimeOut();
    }

    @Override
    public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
        Log.d(TAG, "<< I: " + interest.toUri());
        try {
            if(interest.getExclude().size() != 0) {
                Log.d(TAG, "excluder is not null");
                return;
            }
            Name dataName = interest.getName();
            Data data = ndnDBManager.readData(dataName);
            Log.d(TAG, " get D :" + (data == null ? "no data" : data.toString()));
            if (data != null) {
                face.putData(data);
                Log.d(TAG, ">> D: " + data.getContent().toString());
//                face.expressInterest();
            }
        } catch (IOException ex) {
            Log.e(TAG, "exception: " + ex.getMessage());
        }
    }
}
