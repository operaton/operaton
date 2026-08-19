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
package org.operaton.bpm.webapp.neo.impl.web;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Providers;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import org.operaton.bpm.engine.rest.util.ProvidersUtil;
import org.operaton.bpm.webapp.neo.impl.WebappLogger;
import org.operaton.bpm.webapp.neo.impl.security.SecurityActions;
import org.operaton.bpm.webapp.neo.impl.security.SecurityActions.SecurityAction;
import org.operaton.bpm.webapp.neo.impl.util.ProcessEngineUtil;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;

/**
 * <p>Jax RS resource allowing to perform the setup steps.</p>
 *
 * <p>Ported from the legacy admin webapp so that a webapps-neo-only deployment can bootstrap its
 * first administrator. Without it, a database with no admin user (anything not seeded through
 * {@code operaton.bpm.admin-user}) leaves the SPA with no way in at all.</p>
 *
 * <p>Both endpoints are reachable without authentication — they have to be, since by definition
 * nobody can log in yet. {@link #ensureSetupAvailable} is the guard: once any user belongs to
 * {@link Groups#OPERATON_ADMIN}, or the identity provider is read-only, the resource closes itself
 * permanently and answers {@code FORBIDDEN}.</p>
 */
@Path("/setup/{engine}")
public class SetupResource {

  protected static final WebappLogger LOGGER = WebappLogger.INSTANCE;

  @Context
  protected Providers providers;

  /**
   * Whether an initial user can still be created. The SPA calls this before showing its setup
   * screen. The legacy webapp had no such endpoint: its {@code ProcessEnginesFilter} redirected
   * the HTML request server-side, and its dev server fell back to POSTing an empty body and
   * string-matching the error message. webapps-neo has no such filter, so it asks directly.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public SetupAvailabilityDto isSetupAvailable(final @PathParam("engine") String processEngineName)
      throws IOException, ServletException {

    final ProcessEngine processEngine = ProcessEngineUtil.lookupProcessEngine(processEngineName);
    if (processEngine == null) {
      throw LOGGER.invalidRequestEngineNotFoundForName(processEngineName);
    }

    // Without authentication, as the legacy ProcessEnginesFilter#needsInitialUser does: nobody is
    // logged in yet by definition, and with authorization enabled the user query would otherwise
    // be filtered down to nothing and always report "setup available".
    boolean available = SecurityActions.runWithoutAuthentication(
        (SecurityAction<Boolean>) () -> isSetupAvailableInternal(processEngine), processEngine);

    return new SetupAvailabilityDto(available);
  }

  @Path("/user/create")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public void createInitialUser(final @PathParam("engine") String processEngineName, final UserDto user)
      throws IOException, ServletException {

    final ProcessEngine processEngine = ProcessEngineUtil.lookupProcessEngine(processEngineName);
    if (processEngine == null) {
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

    // create the operaton admin group
    ensureOperatonAdminGroupExists(processEngine);

    // create group membership (add new user to admin group)
    processEngine.getIdentityService()
      .createMembership(user.getProfile().getId(), Groups.OPERATON_ADMIN);
  }

  protected ObjectMapper getObjectMapper() {
    if (providers != null) {
      return ProvidersUtil
        .resolveFromContext(providers, ObjectMapper.class, MediaType.APPLICATION_JSON_TYPE, this.getClass());
    } else {
      return null;
    }
  }

  protected void ensureOperatonAdminGroupExists(ProcessEngine processEngine) {

    final IdentityService identityService = processEngine.getIdentityService();
    final AuthorizationService authorizationService = processEngine.getAuthorizationService();

    // create group
    if (identityService.createGroupQuery().groupId(Groups.OPERATON_ADMIN).count() == 0) {
      Group operatonAdminGroup = identityService.newGroup(Groups.OPERATON_ADMIN);
      operatonAdminGroup.setName("Operaton BPM Administrators");
      operatonAdminGroup.setType(Groups.GROUP_TYPE_SYSTEM);
      identityService.saveGroup(operatonAdminGroup);
    }

    // create ADMIN authorizations on all built-in resources
    for (Resource resource : Resources.values()) {
      if (authorizationService.createAuthorizationQuery().groupIdIn(Groups.OPERATON_ADMIN).resourceType(resource).resourceId(ANY).count() == 0) {
        AuthorizationEntity userAdminAuth = new AuthorizationEntity(AUTH_TYPE_GRANT);
        userAdminAuth.setGroupId(Groups.OPERATON_ADMIN);
        userAdminAuth.setResource(resource);
        userAdminAuth.setResourceId(ANY);
        userAdminAuth.addPermission(ALL);
        authorizationService.saveAuthorization(userAdminAuth);
      }
    }
  }

  protected boolean isSetupAvailableInternal(ProcessEngine processEngine) {
    IdentityService identityService = processEngine.getIdentityService();
    return !identityService.isReadOnly()
        && identityService.createUserQuery().memberOfGroup(Groups.OPERATON_ADMIN).count() == 0;
  }

  protected void ensureSetupAvailable(ProcessEngine processEngine) {
    if (!isSetupAvailableInternal(processEngine)) {
      throw LOGGER.setupActionNotAvailable();
    }
  }

  /**
   * Response of {@link #isSetupAvailable}. A record rather than a bare boolean so the payload stays
   * a JSON object and can gain fields without breaking clients.
   */
  public record SetupAvailabilityDto(boolean setupAvailable) {
  }
}
