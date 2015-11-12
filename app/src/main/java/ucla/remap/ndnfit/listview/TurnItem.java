package ucla.remap.ndnfit.listview;

import android.graphics.drawable.Drawable;

import java.io.Serializable;

/**
 * Created by nightzen on 5/4/15.
 */
public class TurnItem implements Serializable {
    /**
     * Icon
     */
    private Drawable mIcon;

    /**
     * Data array
     */
    private TurnInfo mData;

    /**
     * True if this item is selectable
     */
    private boolean mSelectable = true;

    /**
     * Initialize with icon and data array
     *
     * @param icon
     * @param info
     */
    public TurnItem(Drawable icon, TurnInfo info) {
        mIcon = icon;
        mData = info;
    }

    /**
     * Initialize with icon and strings
     *
     * @param icon
     * @param turnId
     * @param startTime
     * @param endTime
     */
    public TurnItem(Drawable icon, int turnId, String startTime, String endTime) {
        mIcon = icon;
        mData = new TurnInfo("run", turnId, startTime, endTime);
    }

    /**
     * True if this item is selectable
     */
    public boolean isSelectable() {
        return mSelectable;
    }

    /**
     * Set selectable flag
     */
    public void setSelectable(boolean selectable) {
        mSelectable = selectable;
    }

    /**
     * Get data array
     *
     * @return
     */
    public TurnInfo getData() {
        return mData;
    }

    /**
     * Get Start Time
     */
    public String getStartTime() {
        if (mData == null) {
            return null;
        }

        return mData.getStartTime();
    }

    /**
     * Get End Time
     */
    public String getEndTime() {
        if (mData == null) {
            return null;
        }

        return mData.getEndTime();
    }

    /**
     * Get ID
     */
    public int getId() {
        if (mData == null) {
            return 0;
        }

        return mData.getTurnId();
    }

    /**
     * Set data array
     *
     * @param info
     */
    public void setData(TurnInfo info) {
        mData = info;
    }


    public Drawable getIcon() {
        return mIcon;
    }

    public void setIcon(Drawable mIcon) {
        this.mIcon = mIcon;
    }


    //    /**
//     * Compare with the input object
//     *
//     * @param other
//     * @return
//     */
//    public int compareTo(TurnItem other) {
//        if (mData != null) {
//            String[] otherData = other.getData();
//            if (mData.length == otherData.length) {
//                for (int i = 0; i < mData.length; i++) {
//                    if (!mData[i].equals(otherData[i])) {
//                        return -1;
//                    }
//                }
//            } else {
//                return -1;
//            }
//        } else {
//            throw new IllegalArgumentException();
//        }
//
//        return 0;
//    }
}