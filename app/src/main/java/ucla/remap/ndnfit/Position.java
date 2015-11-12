package ucla.remap.ndnfit;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by nightzen on 5/6/15.
 */
public class Position {
    private double lat;
    private double lng;
    long timeStamp;

    public Position(double lat, double lng, long timeStamp) {
        this.lat = lat;
        this.lng = lng;
        this.timeStamp = timeStamp;
    }

    public Position(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
        this.timeStamp = 0;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
