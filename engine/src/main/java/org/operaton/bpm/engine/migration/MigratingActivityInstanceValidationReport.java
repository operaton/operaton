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
package org.operaton.bpm.engine.migration;

import java.util.List;

/**
 * Collects all failures for a migrating activity instance.
 */
public interface MigratingActivityInstanceValidationReport {

  /**
   * @return the id of the source scope of the migrated activity instance
   */
  String getSourceScopeId();

  /**
   * @return the activity instance id of this report
   */
  String getActivityInstanceId();

  /**
   * @return the migration instruction that cannot be applied
   */
  MigrationInstruction getMigrationInstruction();

  /**
   * @return true if the reports contains failures, false otherwise
   */
  boolean hasFailures();

  /**
   * @return the list of failures
   */
  List<String> getFailures();

}
