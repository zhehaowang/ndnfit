package ucla.remap.ndnfit.schema;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by zhanght on 2015/12/21.
 */
public class TimeLocation implements Comparable, Serializable {

    private Date timestamp;
    private double longitude;
    private double latitude;
    private double altitude; //meter


    public TimeLocation() {
    }

    public TimeLocation(double longitude, double latitude, double altitude, Date timestamp) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.timestamp = timestamp;
    }

    public TimeLocation(double longitude, double latitude, double altitude, long timestamp) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.timestamp = new Date();
        this.timestamp.setTime(timestamp);
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setDateTime(long timestamp) {
        this.timestamp = new Date();
        this.timestamp.setTime(timestamp);
    }

    @Override
    public int compareTo(Object another) {
        if(!(another instanceof TimeLocation)) {
            // TODO: do something
        }
        Date anotherDateTime = ((TimeLocation) another).getTimestamp();
        return timestamp.compareTo(anotherDateTime);
    }
}
