package traffic.monitor.agents;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import traffic.monitor.communication.Channel;
import traffic.monitor.communication.Envelope;
import traffic.monitor.communication.Envelope.MessageType;
import traffic.monitor.data.Location;
import traffic.monitor.data.Repository;
import traffic.monitor.data.TrafficReport;
import traffic.monitor.data.WayPoint;
import traffic.monitor.util.DateUtil;
import traffic.monitor.util.FileUtil;
import traffic.monitor.util.FileUtil.LineParser;

public class Dispatcher {

    private static final int                 INITIAL_DRONE_MEMORY_LOAD = 0;

    private static final Logger              LOG                       = Logger.getLogger(Dispatcher.class);

    @Value("#{environment['dispatcher.id']}")
    private Long                             id;

    @Value("#{environment['tube.stations.file.path']}")
    private String                           tubeStationsFilePath;

    @Value("#{environment['routes.file.path']}")
    private String                           routesFilePath;

    @Value("#{environment['max.drone.mem.capacity']}")
    private Integer                          maxDroneMemCapacity;

    @Value("#{environment['simulation.end.date']}")
    private String                           simulationEndDate;

    @Value("#{environment['max.distance.to.tube.station']}")
    private Long                             maxDistanceToTubeStation;

    @Resource
    private Channel                          channel;

    @Resource
    private Repository<TrafficReport>        reportsRepo;

    private final Map<Long, Queue<WayPoint>> routes;

    private final Set<Location>              tubeStationsCoordinates;

    /**
     * Mapping from active drone identifiers to the number of uploaded and still
     * not visited waypoints.
     */
    private final Map<Long, Integer>         activeDrones;

    public Dispatcher() {
        routes = new HashMap<>();
        tubeStationsCoordinates = new HashSet<>();
        activeDrones = new HashMap<>();
    }

    @PostConstruct
    private void init() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                loadTubeStationsCoordinates();
                loadRoutes();
                activateDrones();
                coordinateDrones();
                shutDownSystem();
            }
        });
        thread.setName("Dispatcher Thread");
        thread.start();
    }

    private void loadTubeStationsCoordinates() {
        LOG.info("loading tube stations locations...");
        LineParser<Location> lp = new LineParser<Location>() {
            @Override
            public Location deserialize(String[] tokens) {
                return new Location(Double.valueOf(tokens[1]), Double.valueOf(tokens[2]));
            }
        };
        tubeStationsCoordinates.addAll(FileUtil.parseFileLineByLine(tubeStationsFilePath, ",", lp));
        LOG.info("tube stations locations loading done.");
    }

    private void loadRoutes() {
        LOG.info("loading routes...");
        final Date shutDownDate = DateUtil.parseDate(simulationEndDate);
        LineParser<WayPoint> lp = new LineParser<WayPoint>() {
            @Override
            public WayPoint deserialize(String[] tokens) {
                Date time = DateUtil.parseDate(tokens[3].replaceAll("\"", ""));
                if (time.compareTo(shutDownDate) > 0) {
                    return null;
                }
                Double longitude = Double.valueOf(tokens[1].replaceAll("\"", ""));
                Double latitude = Double.valueOf(tokens[2].replaceAll("\"", ""));
                return new WayPoint(longitude, latitude, time);

            }
        };
        for (String droneRoutesFilePath : routesFilePath.split(",")) {
            Long droneId = Long.valueOf(droneRoutesFilePath.replace(".csv", ""));
            Queue<WayPoint> wayPoints = new PriorityQueue<>(FileUtil.parseFileLineByLine(droneRoutesFilePath, ",", lp));
            routes.put(droneId, wayPoints);
            LOG.info("total number of waypoints for drone :: " + droneId + " is " + wayPoints.size());
        }
        LOG.info("Routes loading done.");
    }

    private void activateDrones() {
        LOG.info("Activating drones...");
        for (Long droneId : routes.keySet()) {
            LOG.info("Sending activation message to drone :: " + droneId);
            channel.sendMessage(new Envelope(id, droneId, MessageType.ACTIVATE));
        }
        while (activeDrones.size() < routes.keySet().size()) {
            handleIncomingMessage();
        }
    }

    private void coordinateDrones() {
        while (!routes.isEmpty()) {
            Set<Long> exhaustedRoutes = new HashSet<>();
            for (Long droneId : routes.keySet()) {
                int remainingCapacity = maxDroneMemCapacity - activeDrones.get(droneId);
                LOG.info("drone :: " + droneId + " can store up to " + remainingCapacity + " new way points");
                if (remainingCapacity > 0) {
                    Queue<Location> wayPoints = new PriorityQueue<>();
                    while (remainingCapacity-- > 0) {
                        if (routes.get(droneId).isEmpty()) {
                            exhaustedRoutes.add(droneId);
                            break;
                        } else {
                            WayPoint wayPoint = routes.get(droneId).poll();
                            wayPoints.add(wayPoint);
                        }
                    }
                    LOG.info("sending " + wayPoints.size() + " new waypoints to drone :: " + droneId);
                    channel.sendMessage(new Envelope(id, droneId, MessageType.WAYPOINT, wayPoints));
                    activeDrones.put(droneId, activeDrones.get(droneId) + wayPoints.size());
                }
            }

            routes.keySet().removeAll(exhaustedRoutes);
            handleIncomingMessage();
        }
    }

    private void shutDownSystem() {
        LOG.info("shutting down drones...");
        for (Long droneId : activeDrones.keySet()) {
            LOG.info("sending shutdown signal to drone :: " + droneId);
            Envelope shutDownMsg = new Envelope(id, droneId, MessageType.SHUTDOWN);
            channel.sendMessage(shutDownMsg);
        }
        while (!activeDrones.isEmpty()) {
            handleIncomingMessage();
        }
        LOG.info("all drones inactive.");
        LOG.info("total number of reports submited is " + reportsRepo.count());
        LOG.info("simulation complete.");
    }

    private void handleIncomingMessage() {
        Envelope envelope = channel.retreiveMessage(id);
        switch (envelope.getType()) {
        case DRONE_ACTIVE:
            LOG.info("received confirmation, drone :: " + envelope.getSenderId() + " is active.");
            activeDrones.put(envelope.getSenderId(), INITIAL_DRONE_MEMORY_LOAD);
            break;
        case DRONE_INACTIVE:
            Long droneId = envelope.getSenderId();
            LOG.info("received confirmation, drone :: " + droneId + " is inactive.");
            activeDrones.remove(droneId);
            break;
        case TRAFFIC_REPORT:
            TrafficReport trafficReport = envelope.getMessage();
            LOG.info("received :: " + trafficReport);
            reportsRepo.save(trafficReport);
            activeDrones.put(envelope.getSenderId(), activeDrones.get(envelope.getSenderId()) - 1);
            break;
        case ASSERT_TUBE_NEARBY_REQUEST:
            Location location = envelope.getMessage();
            LOG.info("drone :: " + envelope.getSenderId() + " requires confirmation of tube station proximity for location :: " + location);
            channel.sendMessage(new Envelope(id, envelope.getSenderId(), MessageType.ASSERT_TUBE_NEARBY_RESPONSE, assertTubeNearBy(location)));
            break;
        default:
            break;
        }
    }

    private Boolean assertTubeNearBy(Location location) {
        LOG.info("searching for tube stations near :: " + location);
        for (Location tubeLocation : tubeStationsCoordinates) {
            if (tubeLocation.distanceTo(location) < maxDistanceToTubeStation) {
                LOG.info("found tube stations at :: " + tubeLocation);
                return true;
            }
        }
        return false;
    }
}
