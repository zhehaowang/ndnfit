package ucla.remap.ndnfit.gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.maps.model.LatLng;
import com.google.maps.model.SnappedPoint;

import java.util.ArrayList;
import java.util.List;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.ndndb.NdnDBManager;
import ucla.remap.ndnfit.data.Position;
import ucla.remap.ndnfit.db.DBManager;

/**
 * Created by nightzen on 4/22/15.
 */
public class GPSListener implements LocationListener{

    Context mainCtx;
    DBManager mDBManager;
    NdnDBManager mNdnDBManager;
    List<Position> trackPoints;
    //
    List<Position> trackPointsForNDN;

    private static String TAG = "GPSListener";

    public GPSListener(Context ctx) {
        this.mainCtx = ctx;
        mDBManager = DBManager.getInstance();
        mNdnDBManager = NdnDBManager.getInstance();
        trackPoints = new ArrayList<>();
        trackPointsForNDN = new ArrayList();
    }

    public void startTrack() {
        trackPoints.clear();
        trackPointsForNDN.clear();
    }

    public void stopTrack(List<SnappedPoint> snappedPoints) {
        // Sava to DB
        // 저장할 때, 당분간 두가지 버전으로 만들 것. 그래야 비교가 쉬움.

        // Save Original Points
        if (trackPoints.size() == 0) {
            Toast.makeText(mainCtx, "No points captured by GPS", Toast.LENGTH_SHORT).show();
            return;
        }
        //Wang Yang: Avoid duplicate recording
        //mDBManager.recordPoints(trackPoints);

        // Save Rendering Points
        List<Position> renderedPoints = new ArrayList<>();
        int idx = 0;
        int lastIdx = 0;
        for(SnappedPoint point : snappedPoints) {
            Position position = new Position(point.location.lat, point.location.lng);
            if(point.originalIndex == -1) {
                position.setTimeStamp(trackPoints.get(lastIdx).getTimeStamp());
            } else {
                position.setTimeStamp(trackPoints.get(point.originalIndex).getTimeStamp());
                lastIdx = point.originalIndex;
            }
            renderedPoints.add(position);
            Log.d(TAG, "Render " + point.originalIndex + "->" + idx);
            idx++;
        }

        mDBManager.recordPoints(renderedPoints);
//        mNdnDBManager.recordPoints(renderedPoints);
        if(trackPointsForNDN.size() > 0) {
            long startMinute = trackPointsForNDN.get(0).getTimeStamp() / NDNFitCommon.ONE_MINUTE;
            mNdnDBManager.recordPoints(trackPointsForNDN, startMinute * NDNFitCommon.ONE_MINUTE);
        }
    }

    public List<Position> getTrackPoints() {
        return trackPoints;
    }

    public List<LatLng> prepareRendering() {
        List<LatLng> list = new ArrayList<>();
        for (Position position : trackPoints) {
            LatLng point = new LatLng(position.getLat(), position.getLng());
            list.add(point);
        }
        return list;
    }

    public void onLocationChanged(Location location) {
        Log.d(TAG, "location changed called");
        Double lat = location.getLatitude();
        Double lng = location.getLongitude();
        long pointTime = location.getTime() * 1000;

        Position position = new Position(lat, lng, pointTime);

//        mDBManager.recordPoint(lat, lon);
//
//        String msg = "Lat: " + lat + ", lon" + lon;
//        Log.d(TAG, msg);
//        String msg = "record got inserted:" + currentTurn + "-" + String.valueOf(rowPosition);
//        Toast.makeText(mCtx, msg, Toast.LENGTH_SHORT).show();
        trackPoints.add(position);

        packDataPoints(trackPointsForNDN, position);
        trackPointsForNDN.add(position);
        String msg = "Lat: " + lat + ", Lng: " + lng;
        Toast.makeText(mainCtx, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * pack origin data to create NDN data packets
     * @param trackPoints
     */
    private void packDataPoints(List<Position> trackPoints, Position newestPosition) {
        if(trackPoints.size() <= 1) {
            return;
        }
        long startMinute = trackPoints.get(0).getTimeStamp() / NDNFitCommon.ONE_MINUTE;
        long endMinute = trackPoints.get(trackPoints.size()-1).getTimeStamp() / NDNFitCommon.ONE_MINUTE;
        long newestMin = newestPosition.getTimeStamp() / NDNFitCommon.ONE_MINUTE;
        if (endMinute == newestMin)
            return;
        //longer than 10 minutes or
        //more than 20 points and not in the same minute
        if(newestMin - startMinute >= 1 || newestMin - startMinute >= 10 || (newestMin - startMinute > 0 && trackPoints.size() > 20)) {
            mNdnDBManager.recordPoints(trackPoints, startMinute * NDNFitCommon.ONE_MINUTE);
            trackPoints.clear();
        }
    }

    public void onProviderDisabled(String provider) {

    }

    public void onProviderEnabled(String provider) {

    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}
