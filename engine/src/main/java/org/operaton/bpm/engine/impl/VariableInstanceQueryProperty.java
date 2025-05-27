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
 * @author roman.smirnov
 */
public interface VariableInstanceQueryProperty {

  QueryProperty VARIABLE_NAME = new QueryPropertyImpl("NAME_");
  QueryProperty VARIABLE_TYPE = new QueryPropertyImpl("TYPE_");
  QueryProperty ACTIVITY_INSTANCE_ID = new QueryPropertyImpl("ACT_INST_ID_");
  QueryProperty EXECUTION_ID = new QueryPropertyImpl("EXECUTION_ID_");
  QueryProperty TASK_ID = new QueryPropertyImpl("TASK_ID_");
  QueryProperty CASE_EXECUTION_ID = new QueryPropertyImpl("CASE_EXECUTION_ID_");
  QueryProperty CASE_INSTANCE_ID = new QueryPropertyImpl("CASE_INST_ID_");
  QueryProperty TENANT_ID = new QueryPropertyImpl("TENANT_ID_");

  QueryProperty TEXT = new QueryPropertyImpl("TEXT_");
  QueryProperty TEXT_AS_LOWER = new QueryPropertyImpl("TEXT_", "LOWER");
  QueryProperty DOUBLE = new QueryPropertyImpl("DOUBLE_");
  QueryProperty LONG = new QueryPropertyImpl("LONG_");

}
