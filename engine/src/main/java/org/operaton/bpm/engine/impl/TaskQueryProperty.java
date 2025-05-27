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
import org.operaton.bpm.engine.task.TaskQuery;



/**
 * Contains the possible properties that can be used in a {@link TaskQuery}.
 *
 * @author Joram Barrez
 */
public interface TaskQueryProperty {

  QueryProperty TASK_ID = new QueryPropertyImpl("ID_");
  QueryProperty NAME = new QueryPropertyImpl("NAME_");
  QueryProperty NAME_CASE_INSENSITIVE = new QueryPropertyImpl("NAME_", "LOWER");
  QueryProperty DESCRIPTION = new QueryPropertyImpl("DESCRIPTION_");
  QueryProperty PRIORITY = new QueryPropertyImpl("PRIORITY_");
  QueryProperty ASSIGNEE = new QueryPropertyImpl("ASSIGNEE_");
  QueryProperty CREATE_TIME = new QueryPropertyImpl("CREATE_TIME_");
  QueryProperty LAST_UPDATED = new QueryPropertyImpl("LAST_UPDATED_");
  QueryProperty PROCESS_INSTANCE_ID = new QueryPropertyImpl("PROC_INST_ID_");
  QueryProperty CASE_INSTANCE_ID = new QueryPropertyImpl("CASE_INST_ID_");
  QueryProperty EXECUTION_ID = new QueryPropertyImpl("EXECUTION_ID_");
  QueryProperty CASE_EXECUTION_ID = new QueryPropertyImpl("CASE_EXECUTION_ID_");
  QueryProperty DUE_DATE = new QueryPropertyImpl("DUE_DATE_");
  QueryProperty FOLLOW_UP_DATE = new QueryPropertyImpl("FOLLOW_UP_DATE_");
  QueryProperty TENANT_ID = new QueryPropertyImpl("TENANT_ID_");

}
