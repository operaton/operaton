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
 * @author Thorben Lindhauer
 *
 */
public interface ExternalTaskQueryProperty {

  QueryProperty ID = new QueryPropertyImpl("ID_");
  QueryProperty LOCK_EXPIRATION_TIME = new QueryPropertyImpl("LOCK_EXP_TIME_");
  QueryProperty PROCESS_INSTANCE_ID = new QueryPropertyImpl("PROC_INST_ID_");
  QueryProperty PROCESS_DEFINITION_ID = new QueryPropertyImpl("PROC_DEF_ID_");
  QueryProperty PROCESS_DEFINITION_KEY = new QueryPropertyImpl("PROC_DEF_KEY_");
  QueryProperty TENANT_ID = new QueryPropertyImpl("TENANT_ID_");
  QueryProperty PRIORITY = new QueryPropertyImpl("PRIORITY_");
  QueryProperty CREATE_TIME = new QueryPropertyImpl("CREATE_TIME_");

}
