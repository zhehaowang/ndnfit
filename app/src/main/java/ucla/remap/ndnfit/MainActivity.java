package ucla.remap.ndnfit;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.maps.GeoApiContext;
import com.google.maps.RoadsApi;
import com.google.maps.model.SnappedPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ucla.remap.ndnfit.db.DBManager;
import ucla.remap.ndnfit.db.TrackContract;
import ucla.remap.ndnfit.gps.GPSListener;
import ucla.remap.ndnfit.listview.TurnInfo;
import ucla.remap.ndnfit.ndndb.NdnDBManager;
import ucla.remap.ndnfit.data.Position;

// Background image
// https://www.flickr.com/photos/raulito39/15496039145/in/photolist-o2oRYy-pBkfyH-cvSNfq-hFFenV-7bT8dd-ngJys2-oSmyDE-cgkeSE-ouAHGp-oGsM3M-o3ahaj-dazQG9-kbfcFa-oQLQjB-qhuMod-nsDbE1-eBEW4Q-6xtFHP-38Cyk-8CfaTG-oHyeA6-e5q1Z7-38RAwA-pERacQ-mnaN6-j8ueQ7-oEMuT6-keuLVZ-oLR4PD-rmWKe2-7krLCG-6a8xN1-nUv7iL-f2ui7w-brPJFx-dZvuu5-f3Lm8j-hLXWxv-f3Ez2P-rLz5tZ-ezNNYm-6iSFY6-5D3w8E-oYYGu5-abBcJc-KawqF-pwa23L-8K5pN7-8Yzntf-dB6LxA

public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";
    public static final int TURN_ACTIVITY = 1002;

    GPSListener mGPSListener;
    DBManager mDBManager;
    NdnDBManager mNdnDBmanager;
    boolean mInTracking;
    ProgressDialog renderProgressDiag_;
    List<Position> debugPoints;
    CypherManager mCypherManager;

    String mAppId;

    // Debug data
    byte[] mEncrypted;

    /**
     * The API context used for the Roads and Geocoding web service APIs.
     */
    private GeoApiContext mContext;

    /**
     * The number of points allowed per API request. This is a fixed value.
     */
    private static final int PAGE_SIZE_LIMIT = 100;

    /**
     * Define the number of data points to re-send at the start of subsequent requests. This helps
     * to influence the API with prior data, so that paths can be inferred across multiple requests.
     * You should experiment with this value for your use-case.
     */
    private static final int PAGINATION_OVERLAP = 5;

    private static final int AUTHORIZE_REQUEST = 0;
    private static final int RESULT_OK = 0;
    private static final String APP_CATEGORY = "openmhealth";

    protected void showProgressDialg() {
        renderProgressDiag_ = ProgressDialog.show(MainActivity.this, "", "In Rendering...", true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == AUTHORIZE_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                mAppId = data.getStringExtra("signer_id");
                Log.e("zhehao", "Identity picked:" + mAppId);
            }
        }
    }

    public class AuthorizeOnClickListener implements DialogInterface.OnClickListener
    {
        String mCertName;
        String mAppCategory;
        public AuthorizeOnClickListener(String certName, String appCategory) {
            mCertName = certName;
            mAppCategory = appCategory;
        }

        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            Intent i = new Intent("com.ndn.jwtan.identitymanager.AUTHORIZE");
            i.putExtra("cert_name", mCertName);
            i.putExtra("app_category", mAppCategory);
            startActivityForResult(i, AUTHORIZE_REQUEST);
        }

    }

    // @param certName This string is intended to be the application's id in the future, left as "stub" for now
    public void requestSigningKeyName(String certName, String appCategory) {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage("Please choose an identity!");
        dlgAlert.setTitle("Choose an identity");
        dlgAlert.setPositiveButton("Ok", new AuthorizeOnClickListener(certName, appCategory));
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    public String createAppId() {
        return "Umimplemented";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup for DB Manager
        mDBManager = DBManager.getInstance();
        mDBManager.init(this);

        mNdnDBmanager = NdnDBManager.getInstance();
        mNdnDBmanager.init(this);
        /*
        Cursor idRecords = mDBManager.getIdRecord();
        int recordCount = idRecords.getCount();
        if (recordCount > 0) {
            if (idRecords.getString(2) == "") {
                requestSigningKeyName("Umimplemented", APP_CATEGORY);
            } else {
                mAppId = idRecords.getString(2);
            }
        } else {
            //createAppId();
            requestSigningKeyName("Umimplemented", APP_CATEGORY);
        }*/

        mGPSListener = new GPSListener(this);

        // Hite Actionbar
        ActionBar bar = this.getSupportActionBar();
        bar.hide();

        // Now, tracking is not started
        mInTracking = false;

        mContext = new GeoApiContext().setApiKey(getString(R.string.google_maps_key));

        // Cypher Manager
        try {
            mCypherManager = new CypherManager("AAA");
        } catch (Exception ex) {
            mCypherManager = null;
        }


        // btnStart
        Button btnStart = (Button)findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start tracking.
                startLocationService();
                mTaskSnapToRoads =
                        new AsyncTask<Void, Void, List<SnappedPoint>>() {

                            @Override
                            protected void onPreExecute() {
                                showProgressDialg();
                            }

                            @Override
                            protected List<SnappedPoint> doInBackground(Void... params) {
                                try {
                                    return snapToRoads(mContext);
                                } catch (final Exception ex) {
                                    ex.printStackTrace();
                                    return null;
                                }
                            }

                            @Override
                            // Move to Track Activity
                            protected void onPostExecute(List<SnappedPoint> snappedPoints) {
                                // 검색창이 열려있으면 닫음.
                                if(renderProgressDiag_ != null && renderProgressDiag_.isShowing()) {
                                    renderProgressDiag_.dismiss();
                                }
                                mGPSListener.stopTrack(snappedPoints);
                            }
                        };
            }
        });

        // btnStop
        Button btnStop = (Button)findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            // stop tracking
//            renderPositions();
            stopLocationService();
            }
        });

        // btnShow
        Button btnShow = (Button)findViewById(R.id.btnShow);
        btnShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show tracking.
                // showLocation();
                Intent intent = new Intent(getBaseContext(), TurnActivity.class);
                ArrayList list = prepareData();
                intent.putExtra("turns", list);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent, TURN_ACTIVITY);
            }
        });

        // btnReset
        Button btnReset = (Button)findViewById(R.id.btnReset);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show tracking.
                // showLocation();
                resetData();
            }
        });

        // btnEncrypt
        Button btnEncrypt = (Button)findViewById(R.id.btnEncrypt);
        btnEncrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show tracking.
                // showLocation();
                encryptData();
            }
        });

        // btnDecrypt
        Button btnDecrypt = (Button)findViewById(R.id.btnDecrypt);
        btnDecrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show tracking.
                // showLocation();
                decryptData();
            }
        });

        // btnContentProvider
//        Button btnContentProvider = (Button)findViewById(R.id.btnContentProvider);
//        btnContentProvider.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // show tracking.
//                // showLocation();
//                testContentProvider();
//            }
//        });
    }

    private ArrayList prepareData() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        ArrayList<TurnInfo> list = new ArrayList<>();
        Cursor turnCursor = mDBManager.getAllTurn();
        int turnRecord = turnCursor.getCount();
        for(int idx=0; idx<turnRecord; idx++) {
            turnCursor.moveToNext();
            int turnId = turnCursor.getInt(0);
            Cursor cursor = mDBManager.getPoints(turnId);
            int recordCount = cursor.getCount();
            if(recordCount > 0) {
                TurnInfo info = new TurnInfo();
                info.setTurnId(turnCursor.getInt(0));
                long startTime = turnCursor.getLong(1);
                Date startDate = new Date(startTime);
                long endTime = turnCursor.getLong(2);
                Date endDate = new Date(endTime);
                info.setStartTime("Start: " + sdf.format(startDate));
                info.setEndTime("Finish: " + sdf.format(endDate));
                list.add(info);
            }
        }
        return list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        debugLocation();
    }

    private void startLocationService() {
        long minTime = 1000;
        float minDistance = 0;
//        DBManager dbManager = DBManager.getInstance();
//        dbManager.startTrack();
        mGPSListener.startTrack();
        LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        // Zhehao: having both on seems to crash AsyncTask sometimes, investigating; GPS_PROVIDER never worked for me but NETWORK provider did
        //manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, mGPSListener);
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, mGPSListener);

        Toast.makeText(getApplicationContext(), "Location Service Started", Toast.LENGTH_SHORT).show();
        mInTracking = true;
    }

    private void stopLocationService() {
        LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        manager.removeUpdates(mGPSListener);
        mInTracking = false;
        mTaskSnapToRoads.execute();
//        DBManager dbManager = DBManager.getInstance();
//        dbManager.finishTrack();
//        mGPSListener.stopTrack();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.stopLocationService();
    }

    private void debugLocation() {
        Cursor turnCursor = mDBManager.getAllTurn();
        int turnRecord = turnCursor.getCount();
        for(int idx=0; idx<turnRecord; idx++) {
            turnCursor.moveToNext();
            int id = turnCursor.getInt(0);
            long start = turnCursor.getLong(1);
            long end = turnCursor.getLong(2);
            Cursor cursor = mDBManager.getPoints(id);
            int recordCount = cursor.getCount();
            for (int idx2 = 0; idx2 < recordCount; idx2++) {
                cursor.moveToNext();
                int turn_id = cursor.getInt(0);
                double lat = cursor.getDouble(1);
                double lon = cursor.getDouble(2);
                String msg = turn_id + "-(lat:" + lat + ", lon:" + lon + ")";
                Log.d(TAG, msg);
            }
        }
    }

    private void resetData() {
        mDBManager.resetData();
        mNdnDBmanager.resetData();
    }

    /**
     * Snaps the points to their most likely position on roads using the Roads API.
     */
    private List<SnappedPoint> snapToRoads(GeoApiContext context) throws Exception {

        List<com.google.maps.model.LatLng> mCapturedLocations = mGPSListener.prepareRendering();
        List<SnappedPoint> snappedPoints = new ArrayList<>();

        int offset = 0;
        while (offset < mCapturedLocations.size()) {
            // Calculate which points to include in this request. We can't exceed the APIs
            // maximum and we want to ensure some overlap so the API can infer a good location for
            // the first few points in each request.
            if (offset > 0) {
                offset -= PAGINATION_OVERLAP;   // Rewind to include some previous points
            }
            int lowerBound = offset;
            int upperBound = Math.min(offset + PAGE_SIZE_LIMIT, mCapturedLocations.size());

            // Grab the data we need for this page.
            com.google.maps.model.LatLng[] page = mCapturedLocations
                    .subList(lowerBound, upperBound)
                    .toArray(new com.google.maps.model.LatLng[upperBound - lowerBound]);

            // Perform the request. Because we have interpolate=true, we will get extra data points
            // between our originally requested path. To ensure we can concatenate these points, we
            // only start adding once we've hit the first new point (i.e. skip the overlap).
            SnappedPoint[] points = RoadsApi.snapToRoads(context, true, page).await();
            boolean passedOverlap = false;
            for (SnappedPoint point : points) {
                if (offset == 0 || point.originalIndex >= PAGINATION_OVERLAP - 1) {
                    passedOverlap = true;
                }
                if (passedOverlap) {
                    snappedPoints.add(point);
                }
            }

            offset = upperBound;
        }

        return snappedPoints;
    }

    /**
     * Snaps the points to their most likely position on roads using the Roads API.
     */
    private List<SnappedPoint> snapToDebug(GeoApiContext context) throws Exception {

        List<com.google.maps.model.LatLng> list = new ArrayList<>();
        for (Position position : debugPoints) {
            com.google.maps.model.LatLng point = new com.google.maps.model.LatLng(position.getLat(), position.getLng());
            list.add(point);
        }

        List<SnappedPoint> snappedPoints = new ArrayList<>();

        int offset = 0;
        while (offset < list.size()) {
            // Calculate which points to include in this request. We can't exceed the APIs
            // maximum and we want to ensure some overlap so the API can infer a good location for
            // the first few points in each request.
            if (offset > 0) {
                offset -= PAGINATION_OVERLAP;   // Rewind to include some previous points
            }
            int lowerBound = offset;
            int upperBound = Math.min(offset + PAGE_SIZE_LIMIT, list.size());

            // Grab the data we need for this page.
            com.google.maps.model.LatLng[] page = list
                    .subList(lowerBound, upperBound)
                    .toArray(new com.google.maps.model.LatLng[upperBound - lowerBound]);

            // Perform the request. Because we have interpolate=true, we will get extra data points
            // between our originally requested path. To ensure we can concatenate these points, we
            // only start adding once we've hit the first new point (i.e. skip the overlap).
            SnappedPoint[] points = RoadsApi.snapToRoads(context, true, page).await();
            boolean passedOverlap = false;
            for (SnappedPoint point : points) {
                if (offset == 0 || point.originalIndex >= PAGINATION_OVERLAP - 1) {
                    passedOverlap = true;
                }
                if (passedOverlap) {
                    snappedPoints.add(point);
                }
            }

            offset = upperBound;
        }

        return snappedPoints;
    }


    AsyncTask<Void, Void, List<SnappedPoint>> mTaskSnapToRoads = null;
    /*        new AsyncTask<Void, Void, List<SnappedPoint>>() {

                @override
                protected void onPreExecute() {
                    showProgressDialg();
                }

                @Override
                protected List<SnappedPoint> doInBackground(Void... params) {
                    try {
                        return snapToRoads(mContext);
                    } catch (final Exception ex) {
                        ex.printStackTrace();
                        return null;
                    }
                }

                @Override
                // Move to Track Activity
                protected void onPostExecute(List<SnappedPoint> snappedPoints) {
                    // 검색창이 열려있으면 닫음.
                    if(renderProgressDiag_ != null && renderProgressDiag_.isShowing()) {
                        renderProgressDiag_.dismiss();
                    }
                    mGPSListener.stopTrack(snappedPoints);
                }
            };*/

    AsyncTask<Void, Void, List<SnappedPoint>> mTaskDebug =
            new AsyncTask<Void, Void, List<SnappedPoint>>() {

                @Override
                protected void onPreExecute() {
                    showProgressDialg();
                }

                @Override
                protected List<SnappedPoint> doInBackground(Void... params) {
                    try {
                        return snapToDebug(mContext);
                    } catch (final Exception ex) {
                        ex.printStackTrace();
                        return null;
                    }
                }

                @Override
                // Move to Track Activity
                protected void onPostExecute(List<SnappedPoint> snappedPoints) {
                    // 검색창이 열려있으면 닫음.
                    if(renderProgressDiag_ != null && renderProgressDiag_.isShowing()) {
                        renderProgressDiag_.dismiss();
                    }
                    // Save Rendering Points
                    List<Position> renderedPoints = new ArrayList<>();
                    int lastIdx = 0;
                    int idx = 0;
                    for(SnappedPoint point : snappedPoints) {
                        Position position = new Position(point.location.lat, point.location.lng);
                        if(point.originalIndex == -1) {
                            position.setTimeStamp(debugPoints.get(lastIdx).getTimeStamp());
                        } else {
                            position.setTimeStamp(debugPoints.get(point.originalIndex).getTimeStamp());
                            lastIdx = point.originalIndex;
                        }
                        renderedPoints.add(position);
                        Log.d(TAG, "Render " + point.originalIndex + "->" + idx);
                        idx++;
                    }
                    Log.e("haitao", "start to record points");
                    mDBManager.recordPoints(renderedPoints);
                    mNdnDBmanager.recordPoints(renderedPoints);
                    Log.e("haitao", "end to record points");
                }
            };

    // debug method
    private void renderData() {
        // 일단 데이터를 긁어오고..
        debugPoints = new ArrayList<>();

        int turnId = mDBManager.getLastTurn();
        Cursor cursor = mDBManager.getPoints(turnId);
        int count = cursor.getCount();
        int idx = 0;
        while(cursor.moveToNext()) {
            Position position = new Position(cursor.getDouble(1), cursor.getDouble(2), cursor.getLong(3));
            debugPoints.add(position);
            idx++;
        }

        Log.d(TAG, "count:" + count + "- idx:" + idx);

        // AsyncTask 출동.
        mTaskDebug.execute();
    }

    // Encrypt Data with AES
    private void encryptData() {
        try {
            if(mCypherManager != null) {
                mEncrypted = mCypherManager.encrypt("Hello World, You Crazy Guy");
                Toast.makeText(getApplicationContext(), new String(mEncrypted), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex) {
            Log.e(TAG, "Encrypt Err");
        }

    }

    // Decrypt Data with AES
    private void decryptData() {
        try {
            if(mCypherManager != null) {
                String plainText = mCypherManager.decryptStr(mEncrypted);
                Toast.makeText(getApplicationContext(), plainText, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex) {
            Log.e(TAG, "Encrypt Err");
        }
    }

    private void testContentProvider() {
        // 데이터를 넣어 봄.
//        ContentValues values = new ContentValues();
//        values.clear();
//        values.put(TrackContract.TurnColumns.START_TIME, System.currentTimeMillis());
//        values.put(TrackContract.TurnColumns.FINISH_TIME, System.currentTimeMillis() + 100);
//
        Uri uri = TrackContract.CONTENT_URI;
//        this.getContentResolver().insert(uri,values);

//        Uri uri = Uri.parse("content://edu.ucal.remap.ndnfit/t_turn");

        Cursor cursor = this.getContentResolver().query(uri, null, null, null, null);

        if(cursor.moveToNext()) {
            int startColumn = cursor.getColumnIndex(TrackContract.TurnColumns.START_TIME);
            do {
                Long start = cursor.getLong(startColumn);
                Log.e(TAG, start.toString());
            } while(cursor.moveToNext());
        }
    }
}
