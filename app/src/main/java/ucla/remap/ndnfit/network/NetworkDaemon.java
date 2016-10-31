package ucla.remap.ndnfit.network;

import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Link;
import net.named_data.jndn.Name;
import net.named_data.jndn.NetworkNack;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnNetworkNack;
import net.named_data.jndn.OnTimeout;
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
import java.util.logging.Level;

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

    public static Face getFace() {
      return face;
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

          NetworkLink networkLink = NetworkLink.getInstance();
          final Link link = networkLink.initLink(keyChain, face);

            AsyncTask<Void, Void, Void> networkTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        face.registerPrefix(NDNFitCommon.DATA_PREFIX, receiveInterest,
                                registerFailure);

                        face.registerPrefix(NDNFitCommon.CATALOG_PREFIX, receiveInterest,
                                registerFailure);
//                        face.registerPrefix(NDNFitCommon.UPDATE_INFO_PREFIX, receiveInterest,
//                                registerFailure);
                        Name registerName = new Name(NDNFitCommon.REGISTER_PREFIX)
                          .append(NDNFitCommon.USER_PREFIX).append(link.wireEncode());
                        Interest registerInterest = new Interest();
                        registerInterest.setName(registerName);
                            face.expressInterest(registerInterest, new OnData() {
                                @Override
                                public void onData(Interest interest, Data data) {

                                }
                            }, new RequestDataTimeOut());
                        Log.e("register", registerInterest.getName().toUri());
/*
                        OnData onKey = new OnData() {
                            public void onData(Interest interest, final Data data) {
                                try {
                                    System.out.println("receive data: " + data.getName());
                                } catch (Exception ex) {
                                }
                            }
                        };

                        OnTimeout onTimeout = new OnTimeout() {
                            public void onTimeout(Interest interest) {
                                try {
                                    System.out.println("time out: " + interest.getName());
                                } catch (Exception ex) {
                                }
                            }
                        };

                        OnNetworkNack onNetworkNack = new OnNetworkNack() {
                            public void onNetworkNack(Interest interest, NetworkNack networkNack) {
                                System.out.println("network nack: " + interest.getName());
                            }
                        };

                        face.expressInterest(new Interest(new Name("/org/openmhealth/haitao/READ/fitness/E-KEY")), onKey, onTimeout, onNetworkNack);
*/
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
        scheduler.scheduleAtFixedRate(dataUploader, 60*1000000,
                NDNFitCommon.FETCH_CONFIRMATION_TIME_INTERVAL, TimeUnit.MICROSECONDS);
    }

    public static void checkInsertionStatus(ScheduledExecutorService scheduler) {
        InsertionStatusChecker insertionStatusChecker = new InsertionStatusChecker();
        insertionStatusChecker.setFace(face);
        scheduler.scheduleAtFixedRate(insertionStatusChecker, 60 * 1000000,
                NDNFitCommon.FETCH_CONFIRMATION_TIME_INTERVAL, TimeUnit.MICROSECONDS);
    }
}
