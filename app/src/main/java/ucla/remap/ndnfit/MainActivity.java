package ucla.remap.ndnfit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.Sha256WithRsaSignature;
import net.named_data.jndn.security.certificate.IdentityCertificate;
import net.named_data.jndn.security.identity.AndroidSqlite3IdentityStorage;
import net.named_data.jndn.security.identity.FilePrivateKeyStorage;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.IdentityStorage;
import net.named_data.jndn.security.identity.PrivateKeyStorage;
import net.named_data.jndn.util.Blob;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import ucla.remap.ndnfit.data.CatalogDaemon;
import ucla.remap.ndnfit.gps.GPSListener;
import ucla.remap.ndnfit.ndndb.NdnDBManager;
import ucla.remap.ndnfit.network.NetworkDaemon;

// Background image
// https://www.flickr.com/photos/raulito39/15496039145/in/photolist-o2oRYy-pBkfyH-cvSNfq-hFFenV-7bT8dd-ngJys2-oSmyDE-cgkeSE-ouAHGp-oGsM3M-o3ahaj-dazQG9-kbfcFa-oQLQjB-qhuMod-nsDbE1-eBEW4Q-6xtFHP-38Cyk-8CfaTG-oHyeA6-e5q1Z7-38RAwA-pERacQ-mnaN6-j8ueQ7-oEMuT6-keuLVZ-oLR4PD-rmWKe2-7krLCG-6a8xN1-nUv7iL-f2ui7w-brPJFx-dZvuu5-f3Lm8j-hLXWxv-f3Ez2P-rLz5tZ-ezNNYm-6iSFY6-5D3w8E-oYYGu5-abBcJc-KawqF-pwa23L-8K5pN7-8Yzntf-dB6LxA

public class MainActivity extends ActionBarActivity {

  private static final String TAG = "MainActivity";
  private static final int AUTHORIZE_REQUEST = 1003;
  private static final int SIGN_CERT_REQUEST = 1004;

  GPSListener mGPSListener;
  NdnDBManager mNdnDBmanager;
  boolean mInTracking;
  ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

  String mAppId;
  Name mAppCertificateName;

  private static final String APP_NAME = "ndnfit";

  // Storage for app keys
  public static final String DB_NAME = "certDb.db";
  public static final String CERT_DIR = "certDir";

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Check which request we're responding to
    if (requestCode == AUTHORIZE_REQUEST) {
      // Make sure the request was successful
      if (resultCode == Activity.RESULT_OK) {
        String signerID = data.getStringExtra("prefix");
        Name appID = new Name(signerID).append(APP_NAME);
        mAppId = appID.toUri();
        try {
          String encodedString = generateKey(appID.toString());
          requestSignature(encodedString, signerID);
        } catch (Exception e) {
          Log.e(TAG, "Exception in identity generation/request");
          Log.e(TAG, e.getMessage());
        }
      }
    } else if (requestCode == SIGN_CERT_REQUEST) {
      if (resultCode == Activity.RESULT_OK) {
        String signedCert = data.getStringExtra("signed_cert");
        byte[] decoded = Base64.decode(signedCert, Base64.DEFAULT);
        Blob blob = new Blob(decoded);
        Data certData = new Data();
        try {
          if (mAppId != "") {
            certData.wireDecode(blob);
            IdentityCertificate certificate = new IdentityCertificate(certData);
            String signerKey = ((Sha256WithRsaSignature) certificate.getSignature()).getKeyLocator().getKeyName().toUri();
            Log.d(TAG, "Signer key name " + signerKey + "; App certificate name: " + certificate.getName().toUri());
//                        mDBManager.insertID(mAppId, certificate.getName().toUri(), signerKey);
            mAppCertificateName = new Name(certificate.getName());
            mNdnDBmanager.setAppID(mAppId, mAppCertificateName);
            NDNFitCommon.setDataPrefix(new Name(mAppId).getPrefix(-1));
          } else {
            Log.e("zhehao", "mAppId empty for result of SIGN_CERT_REQUEST");
          }
        } catch (Exception e) {
          Log.e(getResources().getString(R.string.app_name), e.getMessage());
        }
      }
    }
  }

  private void requestSignature(String encodedString, String signerID) {
    Intent i = new Intent("com.ndn.jwtan.identitymanager.SIGN_CERTIFICATE");
    i.putExtra("cert", encodedString);
    i.putExtra("signer_id", signerID);
    i.putExtra("app_id", APP_NAME);
    startActivityForResult(i, SIGN_CERT_REQUEST);
  }

  private String generateKey(String appID) throws net.named_data.jndn.security.SecurityException {
    String dbPath = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + MainActivity.DB_NAME;
    String certDirPath = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + MainActivity.CERT_DIR;

    IdentityStorage identityStorage = new AndroidSqlite3IdentityStorage(dbPath);
    PrivateKeyStorage privateKeyStorage = new FilePrivateKeyStorage(certDirPath);
    IdentityManager identityManager = new IdentityManager(identityStorage, privateKeyStorage);

    Name identityName = new Name(appID);

    Name keyName = identityManager.generateRSAKeyPairAsDefault(identityName, true);
    IdentityCertificate certificate = identityManager.selfSign(keyName);

    String encodedString = Base64.encodeToString(certificate.wireEncode().getImmutableArray(), Base64.DEFAULT);
    return encodedString;
  }

  public class AuthorizeOnClickListener implements DialogInterface.OnClickListener {
    String mAppName;

    public AuthorizeOnClickListener(String appName) {
      mAppName = appName;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
      Intent i = new Intent("com.ndn.jwtan.identitymanager.AUTHORIZE");
      i.putExtra("app_id", mAppName);
      startActivityForResult(i, AUTHORIZE_REQUEST);
    }
  }

  // @param certName This string is intended to be the application's id in the future, left as "stub" for now
  public void requestAuthorization() {
    AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
    dlgAlert.setMessage("Please choose an identity!");
    dlgAlert.setTitle("Choose an identity");
    dlgAlert.setPositiveButton("Ok", new AuthorizeOnClickListener(APP_NAME));
    dlgAlert.setCancelable(true);
    dlgAlert.create().show();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if (android.os.Build.VERSION.SDK_INT > 9) {
      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
      StrictMode.setThreadPolicy(policy);
    }

    mAppId = "";

    // this is the NDN data manager
    mNdnDBmanager = NdnDBManager.getInstance();
    mNdnDBmanager.init(this, NetworkDaemon.getFace());

    //TODO: temporarily comment this part to simplify the debug process, need to get it back
    // require the user to choose a cert here because the cert is used later.
//        Cursor idRecords = mDBManager.getIdRecord();
//        if (idRecords.moveToNext()) {
//            mAppId = idRecords.getString(0);
//            mAppCertificateName = new Name(idRecords.getString(1));
//            Log.e("zhehao", mAppId);
//            mNdnDBmanager.setAppID(mAppId, mAppCertificateName);
//            // omit the app name component from mAppId
//            NDNFitCommon.setDataPrefix(new Name(mAppId).getPrefix(-1));
//            idRecords.close();
//        } else {
//            requestAuthorization();
//        }

    mGPSListener = new GPSListener(this);

    // Hide Actionbar
    ActionBar bar = this.getSupportActionBar();
    bar.hide();

    // Now, tracking is not started
    mInTracking = false;

    // get network daemon running
    // (as those functions is not used by other apps, for simplicity, service is not used, instead,
    // use multi-threads)
    NetworkDaemon.startNetworkService(scheduler);
    NetworkDaemon.checkInsertionStatus(scheduler);
    CatalogDaemon.startCreatingCatalog(scheduler);


    // btnStart
    Button btnStart = (Button) findViewById(R.id.btnStart);
    btnStart.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // start tracking.
        if (mInTracking)
          return;
        startLocationService();
      }
    });

    // btnStop
    Button btnStop = (Button) findViewById(R.id.btnStop);
    btnStop.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (!mInTracking)
          return;
        stopLocationService();
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  private void startLocationService() {
    mGPSListener.startTrack();

    long minTime = 0;
    float minDistance = 0;
    LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    try {
      manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, mGPSListener);
      manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, mGPSListener);
    } catch (SecurityException e) {
      e.printStackTrace();
    }

    Toast.makeText(getApplicationContext(), "Location Service Started", Toast.LENGTH_SHORT).show();
    mInTracking = true;
  }

  private void stopLocationService() {
    LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    manager.removeUpdates(mGPSListener);
    mGPSListener.stopTrack();
    mInTracking = false;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    this.stopLocationService();
  }
}
