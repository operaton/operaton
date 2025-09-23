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
package org.operaton.bpm.engine.impl.persistence.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.impl.persistence.AbstractManager;

/**
 * @author Tom Baeyens
 * @author Saeid Mirzaei
 */
public class IdentityLinkManager extends AbstractManager {
  private static final String GROUP_ID = "groupId";
  private static final String USER_ID = "userId";
  private static final String TYPE = "type";


  @SuppressWarnings("unchecked")
  public List<IdentityLinkEntity> findIdentityLinksByTaskId(String taskId) {
    return getDbEntityManager().selectList("selectIdentityLinksByTask", taskId);
  }

  @SuppressWarnings("unchecked")
  public List<IdentityLinkEntity> findIdentityLinksByProcessDefinitionId(String processDefinitionId) {
    return getDbEntityManager().selectList("selectIdentityLinksByProcessDefinition", processDefinitionId);
  }

  @SuppressWarnings("unchecked")
  public List<IdentityLinkEntity> findIdentityLinkByTaskUserGroupAndType(String taskId, String userId, String groupId, String type) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(TASK_ID, taskId);
    parameters.put(USER_ID, userId);
    parameters.put(GROUP_ID, groupId);
    parameters.put(TYPE, type);
    return getDbEntityManager().selectList("selectIdentityLinkByTaskUserGroupAndType", parameters);
  }

  @SuppressWarnings("unchecked")
  public List<IdentityLinkEntity> findIdentityLinkByProcessDefinitionUserAndGroup(String processDefinitionId, String userId, String groupId) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(PROCESS_DEFINITION_ID, processDefinitionId);
    parameters.put(USER_ID, userId);
    parameters.put(GROUP_ID, groupId);
    return getDbEntityManager().selectList("selectIdentityLinkByProcessDefinitionUserAndGroup", parameters);
  }

  public void deleteIdentityLinksByProcDef(String processDefId) {
    getDbEntityManager().delete(IdentityLinkEntity.class, "deleteIdentityLinkByProcDef", processDefId);
  }

}
