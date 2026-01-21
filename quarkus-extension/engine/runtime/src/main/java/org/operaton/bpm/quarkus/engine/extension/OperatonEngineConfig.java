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
package org.operaton.bpm.quarkus.engine.extension;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.operaton")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OperatonEngineConfig {

  /**
   * The Operaton ProcessEngineConfiguration properties. For more details,
   * @see <a href="https://docs.operaton.org/manual/latest/reference/deployment-descriptors/tags/process-engine/#configuration-properties">Process Engine Configuration Properties</a>.
   */
  Map<String, String> genericConfig();

  /**
   * The Operaton JobExecutor config. It provides available job acquisition thread configuration
   * properties. These properties only take effect in a Quarkus environment.
   *
   * <p>
   * The JobExecutor is responsible for running Operaton jobs.
   * </p>
   */
  OperatonJobExecutorConfig jobExecutor();

  /**
   * Select a datasource by name or the default datasource is used.
   */
  Optional<String> datasource();

  /**
   * Enables experimental preview features on the process engine level. Defaults to false.
   */
  Optional<Boolean> previewFeaturesEnabled();

}
