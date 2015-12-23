package ucla.remap.ndnfit.schema;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by zhanght on 2015/12/23.
 */
public class TimeLocationList {
    private final List<TimeLocation> items = new ArrayList<>();

    public TimeLocationList() {
    }

    public List<TimeLocation> getTimeLocations() {
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
}
