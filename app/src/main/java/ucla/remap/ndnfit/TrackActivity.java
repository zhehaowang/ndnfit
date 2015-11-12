package ucla.remap.ndnfit;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.RoadsApi;
import com.google.maps.model.SnappedPoint;

import java.util.ArrayList;
import java.util.List;

import ucla.remap.ndnfit.db.DBManager;


public class TrackActivity extends ActionBarActivity {
    GoogleMap map;
    DBManager mDBManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        mDBManager = DBManager.getInstance();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Integer turnId = (Integer) getIntent().getSerializableExtra("turn_id");
//        mTaskSnapToRoads.execute(turnId);
        showMap(turnId.intValue());
    }

    private void showMap(int turnId) {
        List<LatLng> poly = preparePoly(turnId);
        if(poly.size() > 0) {
            drawMarker(poly);
            drawPoly(poly);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(poly.get(0), 15));
        } else {
            LatLng base = new LatLng(34.076448, -118.439971);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(base, 15));
        }
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    private void showMap(List<LatLng> poly) {
        if(poly.size() > 0) {
            drawMarker(poly);
            drawPoly(poly);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(poly.get(0), 15));
        } else {
            LatLng base = new LatLng(34.076448, -118.439971);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(base, 15));
        }
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_track, menu);
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

    private void drawMarker(List<LatLng> poly) {
        MarkerOptions options = new MarkerOptions();
        options.position(poly.get(0));
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_icon));
        map.addMarker(options);

        options = new MarkerOptions();
        options.position(poly.get(poly.size() - 1));
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.sweat));
        map.addMarker(options);
    }

    private void drawPoly(List<LatLng> poly) {
        PolylineOptions polyLineOptions = new PolylineOptions();
        polyLineOptions.addAll(poly);
        polyLineOptions.width(3);
        polyLineOptions.color(Color.BLUE);

        map.addPolyline(polyLineOptions);
    }

    private List<LatLng> preparePoly(int turnId) {

        List<LatLng> poly = new ArrayList<LatLng>();
        Cursor cursor = mDBManager.getPoints(turnId);
        int recordCount = cursor.getCount();
        double lat, lon;
        for(int idx=0; idx<recordCount; idx++) {
            cursor.moveToNext();
            lat = cursor.getDouble(1);
            lon = cursor.getDouble(2);
            LatLng point = new LatLng(lat, lon);
            poly.add(point);
        }

        return poly;
    }

    private List<com.google.maps.model.LatLng> preparePolyForRoad(int turnId) {

        List<com.google.maps.model.LatLng> poly = new ArrayList<>();
        Cursor cursor = mDBManager.getPoints(turnId);
        int recordCount = cursor.getCount();
        double lat, lon;
        for(int idx=0; idx<recordCount; idx++) {
            cursor.moveToNext();
            lat = cursor.getDouble(1);
            lon = cursor.getDouble(2);
            com.google.maps.model.LatLng point = new com.google.maps.model.LatLng(lat, lon);
            poly.add(point);
        }

        return poly;
    }

}
