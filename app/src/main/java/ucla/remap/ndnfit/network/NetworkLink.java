package ucla.remap.ndnfit.network;

import net.named_data.jndn.Face;
import net.named_data.jndn.Link;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.*;

import ucla.remap.ndnfit.NDNFitCommon;

/**
 * Moved link object related operations here.
 * The application needs to fetch network region from edge router, and create link object for
 * other components who what to send interest to this application
 * Created by zhtaoxiang on 10/30/16.
 */
public class NetworkLink {

  //TODO: fetch network region from edge router
  private Link link;
  private static NetworkLink instance = new NetworkLink();

  private NetworkLink() {

  }

  public static NetworkLink getInstance() {
    return instance;
  }

  /**
   *
   * @param keyChain keyChain that used to sign the link
   * @param face the face used to fetch network region
   * @return
   */
  public Link initLink(KeyChain keyChain, Face face) {
    link = new Link();
    link.setName(NDNFitCommon.USER_PREFIX);
    link.addDelegation(10, new Name("/Amazon"));

    try {
      keyChain.sign(link);
    } catch (net.named_data.jndn.security.SecurityException e) {
      e.printStackTrace();
    }
    return link;
  }

  public Link getLink() {
    return link;
  }

  //when local NFD connects to a new edge router, should use this method to update the link object
  public void updateLink() {

  }
}
