package ucla.remap.ndnfit.network;

import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;

import ucla.remap.ndnfit.ndndb.NdnDBManager;

public class ReceiveInterest implements OnInterestCallback {
  private NdnDBManager ndnDBManager;
  private static final String TAG = "ReceiveInterest";

  public ReceiveInterest() {
    ndnDBManager = NdnDBManager.getInstance();
  }

  @Override
  public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
    Log.d(TAG, "<< I: " + interest.toUri());
    try {
      if (interest.getExclude().size() != 0) {
        Log.d(TAG, "excluder is not null");
        return;
      }
      Name dataName = interest.getName();
      if(dataName.isPrefixOf(NdnDBManager.mAppCertificateName)) {
        face.putData(NdnDBManager.mKeyChain.getCertificate(NdnDBManager.mAppCertificateName));
        Log.d(TAG, ">> D: " + NdnDBManager.mAppCertificateName);
        return;
      }
      Data data = ndnDBManager.readData(dataName);
      Log.d(TAG, " get D :" + (data == null ? "no data" : data.getName()));
      if (data != null) {
        face.putData(data);
        Log.d(TAG, ">> D: " + data.getName());
      }
    } catch (Exception ex) {
      Log.e(TAG, "exception: " + ex.getMessage());
    }
  }
}
