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
package org.operaton.bpm.admin.impl.web;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;
import jakarta.servlet.ServletException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Providers;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.operaton.bpm.engine.rest.dto.identity.UserDto;
import org.operaton.bpm.engine.rest.impl.UserRestServiceImpl;
import org.operaton.bpm.engine.rest.spi.ProcessEngineProvider;
import org.operaton.bpm.engine.rest.util.ProvidersUtil;
import org.operaton.bpm.webapp.impl.WebappLogger;
import org.operaton.bpm.webapp.impl.security.SecurityActions;
import org.operaton.bpm.webapp.impl.security.SecurityActions.SecurityAction;

/**
 * <p>Jax RS resource allowing to perform the setup steps.</p>
 *
 * <p>All methods of this class must throw Status.FORBIDDEN exception if
 * setup actions are unavailable.</p>
 *
 * @author Daniel Meyer
 *
 */
@Path("/setup/{engine}")
public class SetupResource {

  protected static final WebappLogger LOGGER = WebappLogger.INSTANCE;

  @Context
  protected Providers providers;

  @Path("/user/create")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public void createInitialUser(final @PathParam("engine") String processEngineName, final UserDto user) throws IOException, ServletException {

    final ProcessEngine processEngine = lookupProcessEngine(processEngineName);
    if(processEngine == null) {
      throw LOGGER.invalidRequestEngineNotFoundForName(processEngineName);
    }

    SecurityActions.runWithoutAuthentication((SecurityAction<Void>) () -> {
      createInitialUserInternal(processEngineName, user, processEngine);
      return null;
    }, processEngine);

  }

  protected void createInitialUserInternal(String processEngineName, UserDto user, ProcessEngine processEngine) {

    ObjectMapper objectMapper = getObjectMapper();

    // make sure we can process this request at this time
    ensureSetupAvailable(processEngine);

    // reuse logic from rest api implementation
    UserRestServiceImpl userRestServiceImpl = new UserRestServiceImpl(processEngineName, objectMapper);
    userRestServiceImpl.createUser(user);

    // crate the operaton admin group
    ensureOperatonAdminGroupExists(processEngine);

    // create group membership (add new user to admin group)
    processEngine.getIdentityService()
      .createMembership(user.getProfile().getId(), Groups.OPERATON_ADMIN);
  }

  protected ObjectMapper getObjectMapper() {
    if(providers != null) {
      return ProvidersUtil
        .resolveFromContext(providers, ObjectMapper.class, MediaType.APPLICATION_JSON_TYPE, this.getClass());
    }
    else {
      return null;
    }
  }

  protected void ensureOperatonAdminGroupExists(ProcessEngine processEngine) {

    final IdentityService identityService = processEngine.getIdentityService();
    final AuthorizationService authorizationService = processEngine.getAuthorizationService();

    // create group
    if(identityService.createGroupQuery().groupId(Groups.OPERATON_ADMIN).count() == 0) {
      Group operatonAdminGroup = identityService.newGroup(Groups.OPERATON_ADMIN);
      operatonAdminGroup.setName("operaton BPM Administrators");
      operatonAdminGroup.setType(Groups.GROUP_TYPE_SYSTEM);
      identityService.saveGroup(operatonAdminGroup);
    }

    // create ADMIN authorizations on all built-in resources
    for (Resource resource : Resources.values()) {
      if(authorizationService.createAuthorizationQuery().groupIdIn(Groups.OPERATON_ADMIN).resourceType(resource).resourceId(ANY).count() == 0) {
        AuthorizationEntity userAdminAuth = new AuthorizationEntity(AUTH_TYPE_GRANT);
        userAdminAuth.setGroupId(Groups.OPERATON_ADMIN);
        userAdminAuth.setResource(resource);
        userAdminAuth.setResourceId(ANY);
        userAdminAuth.addPermission(ALL);
        authorizationService.saveAuthorization(userAdminAuth);
      }
    }

  }

  protected void ensureSetupAvailable(ProcessEngine processEngine) {
    if (processEngine.getIdentityService().isReadOnly()
        || (processEngine.getIdentityService().createUserQuery().memberOfGroup(Groups.OPERATON_ADMIN).count() > 0)) {

      throw LOGGER.setupActionNotAvailable();
    }
  }

  protected ProcessEngine lookupProcessEngine(String engineName) {

    ServiceLoader<ProcessEngineProvider> serviceLoader = ServiceLoader.load(ProcessEngineProvider.class);
    Iterator<ProcessEngineProvider> iterator = serviceLoader.iterator();

    if (iterator.hasNext()) {
      ProcessEngineProvider provider = iterator.next();
      return provider.getProcessEngine(engineName);

    } else {
      throw LOGGER.processEngineProviderNotFound();

    }

  }


}
