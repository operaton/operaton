/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.health;

import org.operaton.bpm.engine.health.FrontendHealthContributor;
import org.operaton.bpm.engine.health.HealthResult;
import org.operaton.bpm.engine.health.HealthService;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default, runtime-agnostic implementation that inspects JobExecutor and database connectivity.
 *
 * @author <a href="mailto:tomnm77@gmail.com">Tomasz Korcz</a>
 */
public class DefaultHealthService implements HealthService {

  private static final int DATASOURCE_CONNECTION_TIMEOUT_SECONDS = 2;

  private final DataSource dataSource;
  private final JobExecutor jobExecutor;
  private final String version;
  private final FrontendHealthContributor frontendHealthContributor;

  public DefaultHealthService(DataSource dataSource, JobExecutor jobExecutor) {
    this(dataSource, jobExecutor, null);
  }

  public DefaultHealthService(DataSource dataSource,
                              JobExecutor jobExecutor,
                              FrontendHealthContributor frontendHealthContributor) {
    this.dataSource = dataSource;
    this.jobExecutor = jobExecutor;
    this.frontendHealthContributor = frontendHealthContributor;
    this.version = DefaultHealthService.class.getPackage() != null
      ? DefaultHealthService.class.getPackage().getImplementationVersion()
      : null;
  }

  @Override
  public HealthResult check() {
    String timestamp = OffsetDateTime.now().toString();

    Map<String, Object> details = new LinkedHashMap<>();

    boolean jobExecutorActive = jobExecutor != null && jobExecutor.isActive();
    boolean engineRegistered = jobExecutor != null && jobExecutor.engineIterator().hasNext();
      Map<String, Object> jobExec = getStringObjectMap(jobExecutorActive, engineRegistered);
      details.put("jobExecutor", jobExec);

    boolean dbConnected = false;
    String dbError = null;
    if (dataSource != null) {
      try (Connection c = dataSource.getConnection()) {
        dbConnected = c != null && c.isValid(DATASOURCE_CONNECTION_TIMEOUT_SECONDS);
      } catch (Exception e) {
        dbConnected = false;
        dbError = e.getClass().getSimpleName() + ": " + e.getMessage();
      }
    }
    Map<String, Object> db = new LinkedHashMap<>();
    db.put("connected", dbConnected);
    if (dbError != null) {
      db.put("error", dbError);
    }
    details.put("database", db);

    Map<String, Object> queue = new LinkedHashMap<>();
    queue.put("available", engineRegistered);
    details.put("queue", queue);

    Map<String, Object> frontend;
    if (frontendHealthContributor != null) {
      frontend = new LinkedHashMap<>(frontendHealthContributor.frontendDetails());
    } else {
      frontend = new LinkedHashMap<>();
      frontend.put("operational", false);
    }
    details.put("frontend", frontend);

    boolean dbOk = (dataSource == null) || dbConnected;
    String status = dbOk ? "UP" : "DOWN";

    return new HealthResult(status, timestamp, version, details);
  }

    private Map<String, Object> getStringObjectMap(boolean jobExecutorActive, boolean engineRegistered) {
        Map<String, Object> jobExec = new LinkedHashMap<>();
        if (jobExecutor != null) {
          jobExec.put("name", jobExecutor.getName());
          jobExec.put("lockOwner", jobExecutor.getLockOwner());
          jobExec.put("lockTimeInMillis", jobExecutor.getLockTimeInMillis());
          jobExec.put("maxJobsPerAcquisition", jobExecutor.getMaxJobsPerAcquisition());
          jobExec.put("waitTimeInMillis", jobExecutor.getWaitTimeInMillis());
        }
        jobExec.put("active", jobExecutorActive);
        jobExec.put("engineRegistered", engineRegistered);
        return jobExec;
    }
}
