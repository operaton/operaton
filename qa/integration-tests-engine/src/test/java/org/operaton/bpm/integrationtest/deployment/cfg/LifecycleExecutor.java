package org.operaton.bpm.integrationtest.deployment.cfg;

import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class LifecycleExecutor {

    protected static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:13.2");

    public void onContainerRegistryEvent(@Observes ContainerRegistry registry, ServiceLoader serviceLoader) {
        System.out.println("Starting database container...");
        //TODO: decide what database to start before container. Environment configuration?
        postgreSQLContainer.start();
        postgreSQLContainer.waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
        System.out.println("Attaching datasource...pass url as jvm argument for standalone configuration");
        //TODO: decide what container to manipulate after database started
        //Assumption that there is only one container in registry so we get first disregard the name
        registry.getContainers().stream().findFirst().ifPresent(container -> container.getContainerConfiguration().overrideProperty("javaVmArguments", "-Dengine-connection-url=" + postgreSQLContainer.getJdbcUrl()));
    }
}