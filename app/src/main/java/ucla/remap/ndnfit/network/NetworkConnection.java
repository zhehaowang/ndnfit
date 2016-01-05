package ucla.remap.ndnfit.network;

import android.os.AsyncTask;

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.*;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.data.CatalogCreator;
import ucla.remap.ndnfit.ndndb.NdnDBManager;

/**
 * Created by zhtaoxiang on 1/3/16.
 */
public class NetworkConnection {
    private static NdnDBManager mNdnDBManager;


    public NetworkConnection() {

    }

    public static void startCreatingCatalog(ScheduledExecutorService scheduler) {
        mNdnDBManager = NdnDBManager.getInstance();
        CatalogCreator catalogCreator = new CatalogCreator();
        // Periodically create datalog
        long currentTime = System.currentTimeMillis();
        long waitingTime = (currentTime / NDNFitCommon.CATALOG_TIME_RANGE + 1) * NDNFitCommon.CATALOG_TIME_RANGE
                - currentTime + TimeUnit.SECONDS.toMillis(10);
        scheduler.scheduleAtFixedRate(catalogCreator,  waitingTime,
                NDNFitCommon.CATALOG_TIME_RANGE, TimeUnit.MILLISECONDS);
    }

    public static void startNetworkService(ScheduledExecutorService scheduler) {
        // deal with data requests
        try {
            final Face face = new Face();

            MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
            MemoryPrivateKeyStorage privateKeyStorage
                    = new MemoryPrivateKeyStorage();
            KeyChain keyChain = new KeyChain(
                    new IdentityManager(identityStorage, privateKeyStorage),
                    new SelfVerifyPolicyManager(identityStorage));
            Name identityName = new Name("/org/openmhealth/haitao/");
            Name keyName = keyChain.generateRSAKeyPairAsDefault(identityName);
            Name certificateName = keyName.getSubName(0, keyName.size() - 1)
                    .append("KEY").append(keyName.get(-1)).append("ID-CERT")
                    .append("0");

            face.setCommandSigningInfo(keyChain, certificateName);

            final ReceiveInterest receiveInterest = new ReceiveInterest(face);
            final RegisterFailure registerFailure = new RegisterFailure();

            AsyncTask<Void, Void, Void> networkTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        face.registerPrefix(NDNFitCommon.DATA_PREFIX, receiveInterest,
                                registerFailure);

                        face.registerPrefix(NDNFitCommon.CATALOG_PREFIX, receiveInterest,
                                registerFailure);

                        while (true) {
                            face.processEvents();
                            Thread.sleep(5000);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (EncodingException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            networkTask.executeOnExecutor(scheduler);

        } catch (SecurityException e) {
            System.out.println("exception: " + e.getMessage());
        }
    }
}
