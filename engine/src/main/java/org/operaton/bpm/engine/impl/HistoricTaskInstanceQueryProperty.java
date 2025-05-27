/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * @author Tom Baeyens
 */
public interface HistoricTaskInstanceQueryProperty {

  QueryProperty HISTORIC_TASK_INSTANCE_ID = new QueryPropertyImpl("ID_");
  QueryProperty PROCESS_DEFINITION_ID = new QueryPropertyImpl("PROC_DEF_ID_");
  QueryProperty PROCESS_INSTANCE_ID = new QueryPropertyImpl("PROC_INST_ID_");
  QueryProperty EXECUTION_ID = new QueryPropertyImpl("EXECUTION_ID_");
  QueryProperty ACTIVITY_INSTANCE_ID = new QueryPropertyImpl("ACT_INST_ID_");
  QueryProperty TASK_NAME = new QueryPropertyImpl("NAME_");
  QueryProperty TASK_DESCRIPTION = new QueryPropertyImpl("DESCRIPTION_");
  QueryProperty TASK_ASSIGNEE = new QueryPropertyImpl("ASSIGNEE_");
  QueryProperty TASK_OWNER = new QueryPropertyImpl("OWNER_");
  QueryProperty TASK_DEFINITION_KEY = new QueryPropertyImpl("TASK_DEF_KEY_");
  QueryProperty DELETE_REASON = new QueryPropertyImpl("DELETE_REASON_");
  QueryProperty START = new QueryPropertyImpl("START_TIME_");
  QueryProperty END = new QueryPropertyImpl("END_TIME_");
  QueryProperty DURATION = new QueryPropertyImpl("DURATION_");
  QueryProperty TASK_PRIORITY = new QueryPropertyImpl("PRIORITY_");
  QueryProperty TASK_DUE_DATE = new QueryPropertyImpl("DUE_DATE_");
  QueryProperty TASK_FOLLOW_UP_DATE = new QueryPropertyImpl("FOLLOW_UP_DATE_");
  QueryProperty CASE_DEFINITION_ID = new QueryPropertyImpl("CASE_DEFINITION_ID_");
  QueryProperty CASE_INSTANCE_ID = new QueryPropertyImpl("CASE_INSTANCE_ID_");
  QueryProperty CASE_EXECUTION_ID = new QueryPropertyImpl("CASE_EXECUTION_ID_");
  QueryProperty TENANT_ID = new QueryPropertyImpl("TENANT_ID_");

}
