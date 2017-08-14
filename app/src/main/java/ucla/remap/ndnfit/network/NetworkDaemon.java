package ucla.remap.ndnfit.network;

import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.ControlParameters;
import net.named_data.jndn.ControlResponse;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Link;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encrypt.Schedule;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.ndndb.NdnDBManager;

public class NetworkDaemon {
  private static final String TAG = "NetworkDaemon";
  private static final Face face = new Face();

  public NetworkDaemon() {

  }

  public static Face getFace() {
    return face;
  }

  public static void startNetworkService(ScheduledExecutorService scheduler) {
    // deal with data requests
    try {
      // Zhehao: here we don't necessarily associate the appID with the face, it's okay to use a temporary ID
      // Haitao: we can also use the user-chosen cert, as it is guaranteed to be set prior to this
      MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
      MemoryPrivateKeyStorage privateKeyStorage
        = new MemoryPrivateKeyStorage();
      KeyChain keyChain = new KeyChain(
        new IdentityManager(identityStorage, privateKeyStorage),
        new SelfVerifyPolicyManager(identityStorage));
      keyChain.setFace(face);

      Name identityName = new Name("/org/openmhealth/haitao/");
      Name keyName = keyChain.generateRSAKeyPairAsDefault(identityName);
      Name certificateName = keyName.getSubName(0, keyName.size() - 1)
        .append("KEY").append(keyName.get(-1)).append("ID-CERT")
        .append("0");
      face.setCommandSigningInfo(keyChain, certificateName);

      final ReceiveInterest receiveInterest = new ReceiveInterest();
      final RegisterFailure registerFailure = new RegisterFailure();

      AsyncTask<Void, Void, Void> networkTask = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... voids) {
          try {
            face.registerPrefix(NDNFitCommon.DATA_PREFIX, receiveInterest,
              registerFailure);

            face.registerPrefix(NdnDBManager.mAppCertificateName.getPrefix(-1), receiveInterest,
              registerFailure);

            //TODO: these two functions should not be invoked only once
            //Whenever network connectivity changes, these two functions needs to be invoked.
            discoverLocalHubPrefix();
            registerRemotePrefix();

            while (true) {
              face.processEvents();
              Thread.sleep(5);
            }
          } catch (IOException | SecurityException | InterruptedException | EncodingException e) {
            e.printStackTrace();
          }

          return null;
        }
      };
      networkTask.executeOnExecutor(scheduler);

    } catch (SecurityException e) {
      Log.e(TAG, "exception: " + e.getMessage());
    }
  }

  public static void checkInsertionStatus(ScheduledExecutorService scheduler) {
    InsertionStatusChecker insertionStatusChecker = new InsertionStatusChecker();
    insertionStatusChecker.setFace(face);
    scheduler.scheduleAtFixedRate(insertionStatusChecker, 60 * 1000000,
      NDNFitCommon.FETCH_CONFIRMATION_TIME_INTERVAL, TimeUnit.MICROSECONDS);
  }

  /**
   * Every data catalog is registered with the DSU. According to naming convention, the DSU can fetch
   * not only data and its catalog, but also C-KEY, E-KEY and D-KEY
   * C-KEY is provided by this app, while E-KEY and D-KEY are provided by NAC Access Manager:
   * https://github.com/zhtaoxiang/AccessManager
   * @param timestamp
   */
  public static void registerOnDsu(long timestamp) {
    Name registerName = new Name(NDNFitCommon.REGISTER_PREFIX)
      .append(NDNFitCommon.USER_PREFIX).append(Schedule.toIsoString(timestamp / 1000));
    // Link object is used for routing purpose, the DSU must decode this Link Object and attaches it
    // with all Interests
    // Notice that there is redundancy, not all the registrations Interests need to be attached with
    // Link Object
    if(NDNFitCommon.LINK_OBJECT != null) {
      registerName.append(NDNFitCommon.LINK_OBJECT.wireEncode());
    }
    Interest registerInterest = new Interest();
    registerInterest.setName(registerName);
    try {
      face.expressInterest(registerInterest, new OnData() {
        @Override
        public void onData(Interest interest, Data data) {
        }
      }, new RequestDataTimeOut());
      Log.d(TAG, "register" + registerInterest.getName().toUri());
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  /**
   * fetch remote NFD's routable prefix, used for link object
   */
  private static void discoverLocalHubPrefix() {
    Name discoverLocalHubPrefixName = new Name(NDNFitCommon.DISCOVER_LOCAL_HUB_PREFIX);
    Interest discoverLocalHubPrefixInterest = new Interest();
    discoverLocalHubPrefixInterest.setName(discoverLocalHubPrefixName);
    try {
      face.expressInterest(discoverLocalHubPrefixInterest, new OnData() {
        @Override
        public void onData(Interest interest, Data data) {
          try {
            // create link, carried by registration interest
            NDNFitCommon.LOCAL_HUB_PREFIX.wireDecode(data.getContent());
            Log.d("local hub prefix", NDNFitCommon.LOCAL_HUB_PREFIX.toString());
            NDNFitCommon.LINK_OBJECT = new Link();
            NDNFitCommon.LINK_OBJECT.setName(NDNFitCommon.USER_PREFIX);
            NDNFitCommon.LINK_OBJECT.addDelegation(10, NDNFitCommon.LOCAL_HUB_PREFIX);
            try {
              NdnDBManager.mKeyChain.sign(NDNFitCommon.LINK_OBJECT);
            } catch (SecurityException e) {
              e.printStackTrace();
            }

          } catch (EncodingException e) {
            e.printStackTrace();
          }
        }
      }, new RequestDataTimeOut());
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  /**
   * register a back prefix at the remote NFD
   */
  private static void registerRemotePrefix() {
    Name remotePrefixRegisterPrefix = new Name("/localhop/nfd/rib/register");
    ControlParameters params = new ControlParameters();
    params.setName(NDNFitCommon.USER_PREFIX);
    remotePrefixRegisterPrefix.append(params.wireEncode());
    Interest remotePrefixRegisterInterest = new Interest(remotePrefixRegisterPrefix);
    Log.d("registerRemotePrefix", "try");
    try {
      NdnDBManager.mKeyChain.sign(remotePrefixRegisterInterest);
      face.expressInterest(remotePrefixRegisterInterest, new OnData() {
        @Override
        public void onData(Interest interest, Data data) {
          ControlResponse resp = new ControlResponse();
          try {
            resp.wireDecode(data.getContent());
            if (resp.getStatusCode() == 200) {
              Log.d("registerRemotePrefix", "succeeded");
            } else {
              Log.d("registerRemotePrefix", "failed");
            }
          } catch (EncodingException e) {
            e.printStackTrace();
          }
        }
      }, new RequestDataTimeOut());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
