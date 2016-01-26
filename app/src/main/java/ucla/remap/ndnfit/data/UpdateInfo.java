package ucla.remap.ndnfit.data;

/**
 * Created by zhtaoxiang on 1/26/16.
 */
public class UpdateInfo {
    private long timepoint;
    private int version;

    public UpdateInfo(long timepoint, int version) {
        this.timepoint = timepoint;
        this.version = version;
    }

    public long getTimepoint() {
        return timepoint;
    }

    public void setTimepoint(long timepoint) {
        this.timepoint = timepoint;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
