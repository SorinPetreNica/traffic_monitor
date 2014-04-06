package traffic.monitor.bootstrap;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public final class Bootstrap {

    private static final Logger LOG = Logger.getLogger(Bootstrap.class);

    private Bootstrap() {
    }

    public static void main(final String[] args) {
        LOG.info("Initializing system");

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(ApplicationContext.class);
            context.refresh();
            context.registerShutdownHook();
        }
    }

}