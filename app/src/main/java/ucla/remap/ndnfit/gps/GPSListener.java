package ucla.remap.ndnfit.gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.data.Position;
import ucla.remap.ndnfit.ndndb.NdnDBManager;

/**
 * Created by nightzen on 4/22/15.
 */
public class GPSListener implements LocationListener{
    private Context mainCtx;
    private NdnDBManager mNdnDBManager;
    private List<Position> trackPointsForNDN;

    private static final String TAG = "GPSListener";

    public GPSListener(Context ctx) {
        this.mainCtx = ctx;
        mNdnDBManager = NdnDBManager.getInstance();
        trackPointsForNDN = new ArrayList<>();
    }

    public void startTrack() {
        trackPointsForNDN.clear();
    }

    public void stopTrack() {
        if(trackPointsForNDN.size() > 0) {
            long startMinute = trackPointsForNDN.get(0).getTimeStamp() / NDNFitCommon.ONE_MINUTE;
            mNdnDBManager.recordPoints(trackPointsForNDN, startMinute * NDNFitCommon.ONE_MINUTE);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "location changed called");
        Double lat = location.getLatitude();
        Double lng = location.getLongitude();
        long pointTime = location.getTime() * 1000;
        String msg = "Lat: " + lat + ", Lng: " + lng;
        Toast.makeText(mainCtx, msg, Toast.LENGTH_SHORT).show();

        Position position = new Position(lat, lng, pointTime);

        packDataPoints(trackPointsForNDN, position);
        trackPointsForNDN.add(position);

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
        if(newestMin - startMinute >= 1 || newestMin - startMinute >= 10 || (newestMin - startMinute > 0 && trackPoints.size() > 100)) {
            mNdnDBManager.recordPoints(trackPoints, startMinute * NDNFitCommon.ONE_MINUTE);
            trackPoints.clear();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}
