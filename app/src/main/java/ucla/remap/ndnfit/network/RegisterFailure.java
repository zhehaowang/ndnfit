package ucla.remap.ndnfit.network;

import android.util.Log;

import net.named_data.jndn.Name;
import net.named_data.jndn.OnRegisterFailed;

public class RegisterFailure implements OnRegisterFailed {
  private static final String TAG = "RegisterFailure";

  @Override
  public void onRegisterFailed(Name prefix) {
    Log.w(TAG, "Failed to register prefix" + prefix.toUri());
  }

}
