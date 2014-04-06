package traffic.monitor.data;

import java.util.Date;

public class WayPoint extends Location implements Comparable<WayPoint> {

    private final Date date;

    public WayPoint(Double longitude, Double latitude, Date date) {
        super(longitude, latitude);
        this.date = date;
    }

    @Override
    public int compareTo(WayPoint o) {
        return date.compareTo(o.date);
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "WayPoint [longitude=" + getLongitude() + ", latitude=" + getLatitude() + ", date=" + date + "]";
    }

    public Location getLocation() {
        return new Location(getLatitude(), getLongitude());
    }

}
