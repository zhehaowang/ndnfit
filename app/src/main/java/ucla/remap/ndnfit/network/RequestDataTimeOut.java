package ucla.remap.ndnfit.network;

import net.named_data.jndn.Interest;
import net.named_data.jndn.OnTimeout;

/**
 * Created by zhtaoxiang on 1/4/16.
 */
public class RequestDataTimeOut implements OnTimeout {

    @Override
    public void onTimeout(Interest interest) {
        System.out.println("Time out for interest " + interest.getName().toUri());
    }

}