package ucla.remap.ndnfit.position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by zhanght on 1/1/2016.
 */
public class PositionList {
    private final List<Position> items = new ArrayList<>();

    private long startTime;
    private long finishTime;

    public List<Position> getItems() {
        return items;
    }
    public void setItems(List<Position> items) {
        this.items.addAll(items);
        Collections.sort(this.items);
    }

    public void addItem(Position one) {
        this.items.add(one);
        Collections.sort(this.items);
    }

    public void sortItems() {
        Collections.sort(this.items);
        startTime = items.get(0).getTimeStamp();
        finishTime = items.get(items.size()-1).getTimeStamp();
    }

    public long getStartTime() {
        return startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }
}
