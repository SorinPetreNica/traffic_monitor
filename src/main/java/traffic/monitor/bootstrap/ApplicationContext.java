package traffic.monitor.bootstrap;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import traffic.monitor.agents.Dispatcher;
import traffic.monitor.agents.Drone;
import traffic.monitor.communication.Channel;
import traffic.monitor.communication.InMemoryChannel;
import traffic.monitor.data.InMemoryTrafficReportRepository;
import traffic.monitor.data.Repository;
import traffic.monitor.data.TrafficReport;

@Configuration
@PropertySource("classpath:application.properties")
public class ApplicationContext {

    @Resource
    private Environment env;

    @Bean
    public Dispatcher dispatcher() {
        return new Dispatcher();
    }

    @Bean
    public Repository<TrafficReport> trafficReportsRepo() {
        return new InMemoryTrafficReportRepository();
    }

    @Bean
    public Channel channel() {
        return new InMemoryChannel();
    }

    @Bean(name = "firstDrone")
    public Drone firstDrone() {
        return new Drone(env.getProperty("first.drone.id", Long.class));
    }

    @Bean(name = "secondDrone")
    public Drone secondDrone() {
        return new Drone(env.getProperty("second.drone.id", Long.class));
    }
}
