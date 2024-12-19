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

import java.util.List;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.AuthorizationQuery;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.impl.cmd.AuthorizationCheckCmd;
import org.operaton.bpm.engine.impl.cmd.CreateAuthorizationCommand;
import org.operaton.bpm.engine.impl.cmd.DeleteAuthorizationCmd;
import org.operaton.bpm.engine.impl.cmd.SaveAuthorizationCmd;

/**
 * @author Daniel Meyer
 *
 */
public class AuthorizationServiceImpl extends ServiceImpl implements AuthorizationService {

  @Override
  public AuthorizationQuery createAuthorizationQuery() {
    return new AuthorizationQueryImpl(commandExecutor);
  }

  @Override
  public Authorization createNewAuthorization(int type) {
    return commandExecutor.execute(new CreateAuthorizationCommand(type));
  }

  @Override
  public Authorization saveAuthorization(Authorization authorization) {
    return commandExecutor.execute(new SaveAuthorizationCmd(authorization));
  }

  @Override
  public void deleteAuthorization(String authorizationId) {
    commandExecutor.execute(new DeleteAuthorizationCmd(authorizationId));    
  }

  @Override
  public boolean isUserAuthorized(String userId, List<String> groupIds, Permission permission, Resource resource) {
    return commandExecutor.execute(new AuthorizationCheckCmd(userId, groupIds, permission, resource, null));
  }

  @Override
  public boolean isUserAuthorized(String userId, List<String> groupIds, Permission permission, Resource resource, String resourceId) {
    return commandExecutor.execute(new AuthorizationCheckCmd(userId, groupIds, permission, resource, resourceId));
  }
  
}
