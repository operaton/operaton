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
package org.operaton.bpm.engine.rest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.impl.identity.Authentication;

/**
 * @author Daniel Meyer
 *
 */
public abstract class AbstractAuthorizedRestResource extends AbstractRestProcessEngineAware {

  protected final Resource resource;
  protected final String resourceId;

  protected AbstractAuthorizedRestResource(String processEngineName, Resource resource, String resourceId, ObjectMapper objectMapper) {
    super(processEngineName, objectMapper);
    this.resource = resource;
    this.resourceId = resourceId;
  }

  protected boolean isAuthorized(Permission permission, Resource resource, String resourceId) {
    ProcessEngine processEngine = getProcessEngine();
    if (!processEngine.getProcessEngineConfiguration().isAuthorizationEnabled()) {
      // if authorization is disabled everyone is authorized
      return true;
    }

    final IdentityService identityService = processEngine.getIdentityService();
    final AuthorizationService authorizationService = processEngine.getAuthorizationService();

    Authentication authentication = identityService.getCurrentAuthentication();
    if(authentication == null) {
      return true;

    } else {
      return authorizationService
         .isUserAuthorized(authentication.getUserId(), authentication.getGroupIds(), permission, resource, resourceId);
    }
  }

  protected boolean isAuthorized(Permission permission, Resource resource) {
    return isAuthorized(permission, resource, resourceId);
  }

  protected boolean isAuthorized(Permission permission) {
    return isAuthorized(permission, resource);
  }

}
