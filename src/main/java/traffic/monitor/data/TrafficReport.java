package traffic.monitor.data;

import java.io.Serializable;
import java.util.Date;
import java.util.Random;

public class TrafficReport implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Congestion {
        HEAVY,
        LIGHT,
        MODERATE;
    }

    private final Long       droneId;

    private final Date       time;

    private final Double     speed;

    private final Congestion congestion;

    public TrafficReport(Long droneId, Date time, Double speed, Congestion congestion) {
        this.droneId = droneId;
        this.time = time;
        this.speed = speed;
        this.congestion = congestion;
    }

    public static TrafficReport randomInstance(Long droneId, Date time) {
        Random rn = new Random();
        return new TrafficReport(droneId, time, Integer.valueOf(rn.nextInt(140)).doubleValue(), Congestion.values()[rn.nextInt(Congestion.values().length)]);
    }

    public Long getDroneId() {
        return droneId;
    }

    public Date getTime() {
        return time;
    }

    public Double getSpeed() {
        return speed;
    }

    public Congestion getCongestion() {
        return congestion;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((congestion == null) ? 0 : congestion.hashCode());
        result = prime * result + ((droneId == null) ? 0 : droneId.hashCode());
        result = prime * result + ((speed == null) ? 0 : speed.hashCode());
        result = prime * result + ((time == null) ? 0 : time.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TrafficReport other = (TrafficReport) obj;
        if (congestion != other.congestion)
            return false;
        if (droneId == null) {
            if (other.droneId != null)
                return false;
        } else if (!droneId.equals(other.droneId))
            return false;
        if (speed == null) {
            if (other.speed != null)
                return false;
        } else if (!speed.equals(other.speed))
            return false;
        if (time == null) {
            if (other.time != null)
                return false;
        } else if (!time.equals(other.time))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TrafficReport [droneId=" + droneId + ", time=" + time + ", speed=" + speed + ", congestion=" + congestion + "]";
    }

}
