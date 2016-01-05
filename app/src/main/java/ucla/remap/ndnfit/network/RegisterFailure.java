package ucla.remap.ndnfit.network;

import net.named_data.jndn.Name;
import net.named_data.jndn.OnRegisterFailed;

/**
 * Created by zhtaoxiang on 1/4/16.
 */
public class RegisterFailure implements OnRegisterFailed {

    @Override
    public void onRegisterFailed(Name prefix) {
        System.out.println("Failed to register prefix" + prefix.toUri());
    }

}
