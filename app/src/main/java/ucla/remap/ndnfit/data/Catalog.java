package ucla.remap.ndnfit.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by zhanght on 1/3/2016.
 */
public class Catalog {
    private long catalogTimePoint;
    private final List<String> pointTime = new ArrayList<>();

    public long getCatalogTimePoint() {
        return catalogTimePoint;
    }

    public void setCatalogTimePoint(long catalogTimePoint) {
        this.catalogTimePoint = catalogTimePoint;
    }

    public List<String> getPointTime() {
        return pointTime;
    }

    public void setPointTime(List<String> pointTime) {
        this.pointTime.addAll(pointTime);
        Collections.sort(pointTime);
    }

    public void addPointTime(String one) {
        pointTime.add(one);
        Collections.sort(pointTime);
    }

    public void sortItems() {
        Collections.sort(pointTime);
    }
}
