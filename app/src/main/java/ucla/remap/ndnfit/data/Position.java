package ucla.remap.ndnfit.data;

import java.io.Serializable;

/**
 * Created by nightzen on 5/6/15.
 */
public class Position implements Comparable, Serializable {
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

    @Override
    public int compareTo(Object another) {
        if(!(another instanceof Position)) {
            //TOOD: do sometion
        }
        long anotherTimeStamp = ((Position) another).getTimeStamp();
        if(anotherTimeStamp > timeStamp) {
            return -1;
        }
        if(anotherTimeStamp == timeStamp) {
            return 0;
        }
        return 1;
    }
}
