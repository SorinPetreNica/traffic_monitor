package traffic.monitor.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Naive implementation of a repository that works with a simple collection to
 * store and retrieve TrafficReport objects. Instances of this type should be
 * used only for simulations or tests.
 * 
 * @author Sorin Petre Nica
 * 
 */
public class InMemoryTrafficReportRepository implements Repository<TrafficReport> {

    private final Map<Long, List<TrafficReport>> inMemoryDb;

    public InMemoryTrafficReportRepository() {
        inMemoryDb = new HashMap<>();
    }

    @Override
    public void save(TrafficReport trafficReport) {
        if (!inMemoryDb.containsKey(trafficReport.getDroneId())) {
            inMemoryDb.put(trafficReport.getDroneId(), new ArrayList<TrafficReport>());
        }
        inMemoryDb.get(trafficReport.getDroneId()).add(trafficReport);
    }

    @Override
    public Long count() {
        long totalCount = 0;
        for (Long droneId : inMemoryDb.keySet()) {
            totalCount += countByDrone(droneId);
        }
        return totalCount;
    }

    @Override
    public Long countByDrone(Long droneId) {
        return new Long(inMemoryDb.get(droneId).size());
    }

}
