package ucla.remap.ndnfit.listview;

import java.io.Serializable;

/**
 * Created by nightzen on 5/4/15.
 */
public class TurnInfo implements Serializable {
    private String iconName;
    private int turnId;
    private String startTime;
    private String endTime;

    public TurnInfo() {
    }

    public TurnInfo(String iconName, int turnId, String startTime, String endTime) {
        this.iconName = iconName;
        this.turnId = turnId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public int getTurnId() {
        return turnId;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;

    }

    public void setTurnId(int turnId) {
        this.turnId = turnId;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
