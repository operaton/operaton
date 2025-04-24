package org.operaton.bpm.integrationtest.util;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FixedPortPostgresqlContainer extends FixedHostPortGenericContainer {

    public static final Integer POSTGRESQL_PORT = 5432;
    static final String DEFAULT_USER = "test";
    static final String DEFAULT_PASSWORD = "test";
    final String databaseName;
    final String username;
    final String password;

    private static final String FSYNC_OFF_OPTION = "fsync=off";

    protected Map<String, String> urlParameters = new HashMap<>();

    /**
     * @param dockerImageName
     */
    public FixedPortPostgresqlContainer(@NotNull String dockerImageName) {
        super(dockerImageName);
        this.databaseName = "test";
        this.username = DEFAULT_USER;
        this.password = DEFAULT_PASSWORD;
        this.waitStrategy = (new LogMessageWaitStrategy()).withRegEx(".*database system is ready to accept connections.*\\s").withTimes(2).withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS));
        this.setCommand("postgres", "-c", FSYNC_OFF_OPTION);
        this.addExposedPort(POSTGRESQL_PORT);
    }

    @Override
    public FixedPortPostgresqlContainer withFixedExposedPort(int hostPort, int containerPort) {
        return (FixedPortPostgresqlContainer) super.withFixedExposedPort(hostPort, containerPort);
    }

    protected void configure() {
        this.addEnv("POSTGRES_DB", this.databaseName);
        this.addEnv("POSTGRES_USER", this.username);
        this.addEnv("POSTGRES_PASSWORD", this.password);
    }

    protected String constructUrlParameters(String startCharacter, String delimiter) {
        return this.constructUrlParameters(startCharacter, delimiter, "");
    }

    public String getJdbcUrl() {
        String additionalUrlParams = this.constructUrlParameters("?", "&");
        return "jdbc:postgresql://" + this.getHost() + ":" + this.getMappedPort(POSTGRESQL_PORT) + "/" + this.databaseName + additionalUrlParams;
    }

    protected String constructUrlParameters(String startCharacter, String delimiter, String endCharacter) {
        String urlParameters = "";
        if (!this.urlParameters.isEmpty()) {
            String additionalParameters = this.urlParameters.entrySet().stream().map(Object::toString).collect(Collectors.joining(delimiter));
            urlParameters = startCharacter + additionalParameters + endCharacter;
        }

        return urlParameters;
    }
}
