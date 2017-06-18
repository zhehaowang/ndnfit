package ucla.remap.ndnfit.network;

import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;

import ucla.remap.ndnfit.ndndb.NdnDBManager;

public class ReceiveData implements OnData {
  private static final String TAG = "ReceiveData";

  public ReceiveData() {
    ndnDBManager = NdnDBManager.getInstance();
  }

  @Override
  public void onData(Interest interest, Data data) {
    Log.d(TAG, "<< D: " + data.getName().toUri());
    // TODO:Delete the data stored in database

    //
  }

  private NdnDBManager ndnDBManager;
}
