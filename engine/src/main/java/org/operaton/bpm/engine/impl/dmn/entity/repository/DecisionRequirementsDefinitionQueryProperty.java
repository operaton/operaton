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
package org.operaton.bpm.engine.impl.dmn.entity.repository;

import org.operaton.bpm.engine.impl.QueryPropertyImpl;
import org.operaton.bpm.engine.query.QueryProperty;

/**
 * Properties to sort decision requirements definition queries by.
 */
public interface DecisionRequirementsDefinitionQueryProperty {

  QueryProperty DECISION_REQUIREMENTS_DEFINITION_ID = new QueryPropertyImpl("ID_");
  QueryProperty DECISION_REQUIREMENTS_DEFINITION_KEY = new QueryPropertyImpl("KEY_");
  QueryProperty DECISION_REQUIREMENTS_DEFINITION_NAME = new QueryPropertyImpl("NAME_");
  QueryProperty DECISION_REQUIREMENTS_DEFINITION_VERSION = new QueryPropertyImpl("VERSION_");
  QueryProperty DECISION_REQUIREMENTS_DEFINITION_CATEGORY = new QueryPropertyImpl("CATEGORY_");
  QueryProperty DEPLOYMENT_ID = new QueryPropertyImpl("DEPLOYMENT_ID_");
  QueryProperty TENANT_ID = new QueryPropertyImpl("TENANT_ID_");

}
