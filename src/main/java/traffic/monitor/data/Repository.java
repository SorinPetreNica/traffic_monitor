package traffic.monitor.data;

public interface Repository<E> {

    void save(TrafficReport trafficReport);

    Long count();

    Long countByDrone(Long droneId);
}
