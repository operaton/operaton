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
package org.operaton.bpm.engine.impl.plugin;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationEntity;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;

/**
 * @author Daniel Meyer
 *
 */
public class AdministratorAuthorizationPlugin extends AbstractProcessEnginePlugin {

  private static final AdministratorAuthorizationPluginLogger LOG = ProcessEngineLogger.ADMIN_PLUGIN_LOGGER;

  /** The name of the administrator group.
   *
   * If this name is set to a non-null and non-empty value,
   * the plugin will create group-level Administrator authorizations
   * on all built-in resources. */
  protected String administratorGroupName;

  /** The name of the administrator user.
   *
   * If this name is set to a non-null and non-empty value,
   * the plugin will create group-level Administrator authorizations
   * on all built-in resources. */
  protected String administratorUserName;

  protected boolean authorizationEnabled;

  @Override
  public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    authorizationEnabled = processEngineConfiguration.isAuthorizationEnabled();
    if (administratorGroupName != null && !administratorGroupName.isEmpty()) {
      processEngineConfiguration.getAdminGroups().add(administratorGroupName);
    }
    if (administratorUserName != null && !administratorUserName.isEmpty()) {
      processEngineConfiguration.getAdminUsers().add(administratorUserName);
    }
  }

  @Override
  public void postProcessEngineBuild(ProcessEngine processEngine) {
    if(!authorizationEnabled) {
      return;
    }

    final AuthorizationService authorizationService = processEngine.getAuthorizationService();

    if(administratorGroupName != null && !administratorGroupName.isEmpty()) {
      // create ADMIN authorizations on all built-in resources for configured group
      for (Resource resource : Resources.values()) {
        if(authorizationService.createAuthorizationQuery().groupIdIn(administratorGroupName).resourceType(resource).resourceId(ANY).count() == 0) {
          AuthorizationEntity adminGroupAuth = new AuthorizationEntity(AUTH_TYPE_GRANT);
          adminGroupAuth.setGroupId(administratorGroupName);
          adminGroupAuth.setResource(resource);
          adminGroupAuth.setResourceId(ANY);
          adminGroupAuth.addPermission(ALL);
          authorizationService.saveAuthorization(adminGroupAuth);
          LOG.grantGroupPermissions(administratorGroupName, resource.resourceName());

        }
      }
    }

    if(administratorUserName != null && !administratorUserName.isEmpty()) {
      // create ADMIN authorizations on all built-in resources for configured user
      for (Resource resource : Resources.values()) {
        if(authorizationService.createAuthorizationQuery().userIdIn(administratorUserName).resourceType(resource).resourceId(ANY).count() == 0) {
          AuthorizationEntity adminUserAuth = new AuthorizationEntity(AUTH_TYPE_GRANT);
          adminUserAuth.setUserId(administratorUserName);
          adminUserAuth.setResource(resource);
          adminUserAuth.setResourceId(ANY);
          adminUserAuth.addPermission(ALL);
          authorizationService.saveAuthorization(adminUserAuth);
          LOG.grantUserPermissions(administratorUserName, resource.resourceName());
        }
      }
    }

  }



  // getter / setters ////////////////////////////////////

  public String getAdministratorGroupName() {
    return administratorGroupName;
  }

  public void setAdministratorGroupName(String administratorGroupName) {
    this.administratorGroupName = administratorGroupName;
  }

  public String getAdministratorUserName() {
    return administratorUserName;
  }

  public void setAdministratorUserName(String administratorUserName) {
    this.administratorUserName = administratorUserName;
  }

}
