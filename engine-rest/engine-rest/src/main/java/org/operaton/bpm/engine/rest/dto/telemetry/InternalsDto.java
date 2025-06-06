/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.rest.dto.telemetry;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.engine.telemetry.Internals;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InternalsDto {

  public static final String SERIALIZED_APPLICATION_SERVER = "application-server";
  public static final String SERIALIZED_CAMUNDA_INTEGRATION = "operaton-integration";
  public static final String SERIALIZED_LICENSE_KEY = "license-key";
  public static final String SERIALIZED_TELEMETRY_DATA_COLLECTION_START_DATE = "data-collection-start-date";

  protected DatabaseDto database;
  @JsonProperty(value = SERIALIZED_APPLICATION_SERVER)
  protected ApplicationServerDto applicationServer;
  @JsonProperty(value = SERIALIZED_CAMUNDA_INTEGRATION)
  protected Set<String> operatonIntegration;
  @JsonProperty(value = SERIALIZED_TELEMETRY_DATA_COLLECTION_START_DATE)
  protected Date dataCollectionStartDate;
  protected Map<String, CommandDto> commands;
  protected Map<String, MetricDto> metrics;
  protected Set<String> webapps;

  protected JdkDto jdk;

  public InternalsDto(DatabaseDto database, ApplicationServerDto server, JdkDto jdk) {
    this.database = database;
    this.applicationServer = server;
    this.commands = new HashMap<>();
    this.jdk = jdk;
    this.operatonIntegration = new HashSet<>();
  }

  public DatabaseDto getDatabase() {
    return database;
  }

  public void setDatabase(DatabaseDto database) {
    this.database = database;
  }

  public ApplicationServerDto getApplicationServer() {
    return applicationServer;
  }

  public void setApplicationServer(ApplicationServerDto applicationServer) {
    this.applicationServer = applicationServer;
  }

  public Map<String, CommandDto> getCommands() {
    return commands;
  }

  public void setCommands(Map<String, CommandDto> commands) {
    this.commands = commands;
  }

  public Map<String, MetricDto> getMetrics() {
    return metrics;
  }

  public void setMetrics(Map<String, MetricDto> metrics) {
    this.metrics = metrics;
  }

  public JdkDto getJdk() {
    return jdk;
  }

  public void setJdk(JdkDto jdk) {
    this.jdk = jdk;
  }

  public Set<String> getOperatonIntegration() {
    return operatonIntegration;
  }

  public void setOperatonIntegration(Set<String> operatonIntegration) {
    this.operatonIntegration = operatonIntegration;
  }

  public Set<String> getWebapps() {
    return webapps;
  }

  public void setWebapps(Set<String> webapps) {
    this.webapps = webapps;
  }

  public Date getDataCollectionStartDate() {
    return dataCollectionStartDate;
  }

  public void setDataCollectionStartDate(Date dataCollectionStartDate) {
    this.dataCollectionStartDate = dataCollectionStartDate;
  }

  public static InternalsDto fromEngineDto(Internals other) {

    InternalsDto dto = new InternalsDto(
        DatabaseDto.fromEngineDto(other.getDatabase()),
        ApplicationServerDto.fromEngineDto(other.getApplicationServer()),
        JdkDto.fromEngineDto(other.getJdk()));

    dto.dataCollectionStartDate = other.getDataCollectionStartDate();

    dto.commands = new HashMap<>();
    other.getCommands().forEach((name, command) -> dto.commands.put(name, new CommandDto(command.getCount())));

    dto.metrics = new HashMap<>();
    other.getMetrics().forEach((name, metric) -> dto.metrics.put(name, new MetricDto(metric.getCount())));

    dto.setWebapps(other.getWebapps());
    dto.setOperatonIntegration(other.getOperatonIntegration());

    return dto;
  }

}
