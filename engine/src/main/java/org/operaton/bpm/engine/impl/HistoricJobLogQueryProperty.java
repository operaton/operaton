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
package org.operaton.bpm.engine.impl;

import org.operaton.bpm.engine.query.QueryProperty;

/**
 * @author Roman Smirnov
 *
 */
public interface HistoricJobLogQueryProperty {

  QueryProperty JOB_ID = new QueryPropertyImpl("JOB_ID_");
  QueryProperty JOB_DEFINITION_ID = new QueryPropertyImpl("JOB_DEF_ID_");
  QueryProperty TIMESTAMP = new QueryPropertyImpl("TIMESTAMP_");
  QueryProperty ACTIVITY_ID = new QueryPropertyImpl("ACT_ID_");
  QueryProperty EXECUTION_ID = new QueryPropertyImpl("EXECUTION_ID_");
  QueryProperty PROCESS_INSTANCE_ID = new QueryPropertyImpl("PROCESS_INSTANCE_ID_");
  QueryProperty PROCESS_DEFINITION_ID = new QueryPropertyImpl("PROCESS_DEF_ID_");
  QueryProperty PROCESS_DEFINITION_KEY = new QueryPropertyImpl("PROCESS_DEF_KEY_");
  QueryProperty DEPLOYMENT_ID = new QueryPropertyImpl("DEPLOYMENT_ID_");
  QueryProperty DUEDATE = new QueryPropertyImpl("JOB_DUEDATE_");
  QueryProperty RETRIES = new QueryPropertyImpl("JOB_RETRIES_");
  QueryProperty PRIORITY = new QueryPropertyImpl("JOB_PRIORITY_");
  QueryProperty SEQUENCE_COUNTER = new QueryPropertyImpl("SEQUENCE_COUNTER_");
  QueryProperty TENANT_ID = new QueryPropertyImpl("TENANT_ID_");
  QueryProperty HOSTNAME = new QueryPropertyImpl("HOSTNAME_");

}
