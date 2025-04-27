package org.operaton.bpm.integrationtest.deployment.cfg;

import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link org.jboss.arquillian.core.api.annotation.Observer} for Arquillian lifecycle events.
 * Observes {@link ContainerRegistry} event and facilitates ability to start jdbc database
 * container before and provide connection information to Arquillian container.
 *
 */
@SuppressWarnings("rawtypes")
public class ArquillianEventObserver {

    private static final String POSTGRES = "postgres";
    private static final String POSTGRES_VERSION = "13.2";
    private static final String MYSQL = "mysql";
    private static final String MARIADB = "mariadb";
    private static final String ORACLE = "oracle";
    private static final String DB2 = "db2";
    private static final String SQLSERVER = "sqlserver";

    private final static Map<String, JdbcDatabaseContainer> AVAILABLE_DB_CONTAINERS = new HashMap<>();
    private final static Map<String, WaitStrategy> CONTAINER_WAIT_STRATEGIES = new HashMap<>();

    static {
        AVAILABLE_DB_CONTAINERS.put(POSTGRES, new PostgreSQLContainer(POSTGRES + ":" + POSTGRES_VERSION));
        CONTAINER_WAIT_STRATEGIES.put(POSTGRES, Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
    }

    private static JdbcDatabaseContainer dbContainer;

    /**
     * Listens for the Arquillian ContainerRegistry event to start the appropriate jdbc database container
     * via testcontainers before the actual Arquillian container is started. Which database container is
     * started depends on two environment variables:
     * <code>database.type</code>
     * These should be passed as VM arguments for local testing and are preconfigured in the appropriate
     * Maven profiles
     *
     * @param registry arquillian container registry
     * @param serviceLoader arquillian server loader
     */
    public void onContainerRegistryEvent(@Observes ContainerRegistry registry, ServiceLoader serviceLoader) {
        var containerName = System.getProperty("database.type");

        if(containerName != null && AVAILABLE_DB_CONTAINERS.containsKey(containerName)) {
            dbContainer = AVAILABLE_DB_CONTAINERS.get(containerName);
            dbContainer.start();
            dbContainer.waitingFor(CONTAINER_WAIT_STRATEGIES.get(containerName));
            //Assume that there is only one container in the registry
            registry.getContainers()
                    .stream()
                    .findFirst()
                    .ifPresent(container -> container.getContainerConfiguration()
                            .overrideProperty("javaVmArguments", "-Dengine-connection-url=" + dbContainer.getJdbcUrl()));
        }
    }
}