package org.operaton.bpm.quarkus.engine.extension;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import javax.sql.DataSource;
import org.operaton.bpm.engine.health.HealthService;
import org.operaton.bpm.engine.impl.health.DefaultHealthService;

@ApplicationScoped
public class HealthServiceProducer {

  @Produces
  @ApplicationScoped
  public HealthService healthService(DataSource dataSource) {
    return new DefaultHealthService(dataSource, null, null);
  }
}