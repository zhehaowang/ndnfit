package ucla.remap.ndnfit.network;

import android.os.AsyncTask;
import android.util.Log;

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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.ndndb.NdnDBManager;

/**
 * Created by zhtaoxiang on 1/3/16.
 */
public class NetworkDaemon {
    private static final String TAG = "NetworkDaemon";
    private static NdnDBManager mNdnDBManager;
    private static final Face face = new Face();
//    /////////////////////////////////
//    private static Name repoCommandPrefix = new Name("/example/repo/1");
//    private static Name repoDataPrefix = new Name("/example/data/1");
//    private static Name fetchPrefix = new Name(repoDataPrefix).append("testinsert");
//    private static long startBlockId = 0;
//    private static long endBlockId = 1;
//    //////////////////////////////////


    public NetworkDaemon() {

    }

    public static void startNetworkService(ScheduledExecutorService scheduler) {
        // deal with data requests
        try {
            // Zhehao: here we don't necessarily associate the appID with the face, it's okay to use a temporary ID
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

            final ReceiveInterest receiveInterest = new ReceiveInterest(face);
            final RegisterFailure registerFailure = new RegisterFailure();

//            ///////////////////////////////////
//            // Register the prefix and send the repo insert command at the same time
//
//            final BasicInsertion.ProduceSegments produceSegments = new BasicInsertion.ProduceSegments
//                    (keyChain, certificateName, startBlockId, endBlockId,
//                            new BasicInsertion.SimpleCallback() {
//                                public void exec() {
//                                    System.out.println("All data was inserted.");
//                                }
//                            });
//            /////////////////////////////////////////


            AsyncTask<Void, Void, Void> networkTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        face.registerPrefix(NDNFitCommon.DATA_PREFIX, receiveInterest,
                                registerFailure);

                        face.registerPrefix(NDNFitCommon.CATALOG_PREFIX, receiveInterest,
                                registerFailure);
                        face.registerPrefix(NDNFitCommon.UPDATE_INFO_PREFIX, receiveInterest,
                                registerFailure);

//                        System.out.println("Register prefix " + fetchPrefix.toUri());
//                        face.registerPrefix
//                                (fetchPrefix, produceSegments,
//                                        new OnRegisterFailed() {
//                                            public void onRegisterFailed(Name prefix) {
//                                                System.out.println("Register failed for prefix " + prefix.toUri());
//                                            }
//                                        });

                        while (true) {
                            face.processEvents();
                            Thread.sleep(5);
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
            Log.e(TAG, "exception: " + e.getMessage());
        }
    }

    public static void insertIntoRepo(ScheduledExecutorService scheduler) {
        DataUploader dataUploader = new DataUploader();
        dataUploader.setFace(face);
        scheduler.scheduleAtFixedRate(dataUploader, 60000,
                NDNFitCommon.UPLOAD_TIME_INTERVAL, TimeUnit.MILLISECONDS);
    }
}
