package ucla.remap.ndnfit.timelocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by zhanght on 2015/12/23.
 */
public class TimeLocationList {
    private final List<TimeLocation> items = new ArrayList<>();

    private Date startTime;
    private Date finishTime;

    public TimeLocationList() {
    }

    public List<TimeLocation> getItems() {
        return items;
    }

    public void setItems(List<TimeLocation> items) {
        this.items.addAll(items);
        Collections.sort(this.items);
    }

    public void addItem(TimeLocation one) {
        this.items.add(one);
        Collections.sort(this.items);
    }

    public void sortItems() {
        Collections.sort(this.items);
        startTime = items.get(0).getTimestamp();
        finishTime = items.get(items.size()-1).getTimestamp();
    }
}
