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
package org.operaton.bpm.webapp.impl.security.auth;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.webapp.impl.WebappLogger;
import org.operaton.bpm.webapp.impl.util.ProcessEngineUtil;

import static org.operaton.bpm.engine.authorization.Permissions.ACCESS;
import static org.operaton.bpm.engine.authorization.Resources.APPLICATION;
import static org.operaton.bpm.webapp.impl.security.filter.util.HttpSessionMutexListener.AUTH_TIME_SESSION_MUTEX;

public final class AuthenticationUtil {

  protected static final WebappLogger LOGGER = WebappLogger.INSTANCE;

  protected static final String CAM_AUTH_SESSION_KEY = "authenticatedUser";

  private static final String[] APPS = new String[]{"cockpit", "tasklist", "admin"};
  private static final String APP_WELCOME = "welcome";

  private AuthenticationUtil() {
  }

  public static UserAuthentication createAuthentication(String engineName, String username) {
    return createAuthentication(engineName, username, null, null);
  }

  public static UserAuthentication createAuthentication(ProcessEngine processEngine, String username) {
    return createAuthentication(processEngine, username, null, null);
  }

  public static UserAuthentication createAuthentication(String engineName, String username, List<String> groupIds, List<String> tenantIds) {
    ProcessEngine processEngine = ProcessEngineUtil.lookupProcessEngine(engineName);

    if (processEngine == null) {
      throw LOGGER.invalidRequestEngineNotFoundForName(engineName);
    }

    return createAuthentication(processEngine, username, groupIds, tenantIds);
  }

  /**
   * <p>Creates and returns a {@link UserAuthentication} with the following information:
   * <ul>
   *   <li>{@code userId}
   *   <li>{@code processEngineName}
   *   <li>{@code groupIds}
   *   <li>{@code tenantIds}
   *   <li>{@code authorizedApps}
   */
  public static UserAuthentication createAuthentication(ProcessEngine processEngine, String username, List<String> groupIds, List<String> tenantIds) {

    String userId = username;

    User user = processEngine.getIdentityService()
      .createUserQuery()
      .userId(username)
      .singleResult();

    if (user == null) {
      return null;
    }

    if (user.getId() != null && !user.getId().isEmpty()) {
      userId = user.getId();
    }

    // make sure authentication is executed without authentication :)
    processEngine.getIdentityService().clearAuthentication();

    if (groupIds == null) {
      groupIds = getGroupsOfUser(processEngine, userId);
    }

    if (tenantIds == null) {
      tenantIds = getTenantsOfUser(processEngine, userId);
    }

    // check user's app authorizations
    AuthorizationService authorizationService = processEngine.getAuthorizationService();

    HashSet<String> authorizedApps = new HashSet<>();
    authorizedApps.add(APP_WELCOME);

    if (processEngine.getProcessEngineConfiguration().isAuthorizationEnabled()) {
      for (String application : APPS) {
        if (isAuthorizedForApp(authorizationService, userId, groupIds, application)) {
          authorizedApps.add(application);
        }
      }

    } else {
      Collections.addAll(authorizedApps, APPS);
    }

    // create new authentication
    UserAuthentication newAuthentication = new UserAuthentication(userId, processEngine.getName());
    newAuthentication.setGroupIds(groupIds);
    newAuthentication.setTenantIds(tenantIds);
    newAuthentication.setAuthorizedApps(authorizedApps);

    return newAuthentication;
  }

  public static List<String> getTenantsOfUser(ProcessEngine engine, String userId) {
    List<Tenant> tenants = engine.getIdentityService().createTenantQuery()
      .userMember(userId)
      .includingGroupsOfUser(true)
      .list();

    List<String> tenantIds = new ArrayList<>();
    for (Tenant tenant : tenants) {
      if (tenant != null && tenant.getId() != null) {
        tenantIds.add(tenant.getId());
      }
    }
    return tenantIds;
  }

  public static List<String> getGroupsOfUser(ProcessEngine engine, String userId) {
    List<Group> groups = engine.getIdentityService().createGroupQuery()
      .groupMember(userId)
      .list();

    List<String> groupIds = new ArrayList<>();
    for (Group group : groups) {
      if (group != null && group.getId() != null) {
        groupIds.add(group.getId());
      }
    }
    return groupIds;
  }

  protected static boolean isAuthorizedForApp(AuthorizationService authorizationService, String username, List<String> groupIds, String application) {
    return authorizationService.isUserAuthorized(username, groupIds, ACCESS, APPLICATION, application);
  }

  /**
   * Allows obtaining an {@link Authentications} object from the
   * {@link HttpSession}. If no such object exists in the session, a new
   * instance is created and returned.
   *
   * @param session the {@link HttpSession} instance from which to retrieve the
   *                {@link Authentications}.
   */
  public static Authentications getAuthsFromSession(HttpSession session) {
    Authentications authentications = (Authentications) session.getAttribute(CAM_AUTH_SESSION_KEY);
    if (authentications == null) {
      authentications = new Authentications();
      session.setAttribute(CAM_AUTH_SESSION_KEY, authentications);
    }
    return authentications;
  }

  /**
   * Invalidates the old {@link HttpSession} of the current request and creates
   * a new one. Additionally, transfers the existing authentications to the new
   * session and adds a new one.
   *
   * @param request        the {@link HttpServletRequest} instance from which the session
   *                       is obtained and a new {@link HttpSession} created.
   * @param authentication the new {@link Authentication} instance that is created
   *                       through user login. It is added to the existing authentications.
   */
  public static void revalidateSession(HttpServletRequest request, UserAuthentication authentication) {
    HttpSession session = request.getSession();
    Authentications authentications = getAuthsFromSession(session);

    // invalidate old & create new session
    session.invalidate();
    session = request.getSession(true);

    if (authentication != null) {
      authentications.addOrReplace(authentication);
      session.setAttribute(CAM_AUTH_SESSION_KEY, authentications);
    }
  }

  /**
   * Store authentications in current session.
   */
  public static void updateSession(HttpSession session, Authentications authentications) {
    if (session != null) {
      session.setAttribute(CAM_AUTH_SESSION_KEY, authentications);
    }
  }

  /**
   * <p>Update/remove authentications when cache validation time (= x + TTL) is due.
   *
   * <p>The following information is updated:<ul>
   *   <li>{@code groupIds}
   *   <li>{@code tenantIds}
   *   <li>{@code authorizedApps}
   *
   * <p>An authorization is only removed if the user doesn't exist anymore (user was deleted).
   */
  public static void updateCache(Authentications authentications,
                                 HttpSession session,
                                 long cacheTimeToLive) {
    synchronized (getSessionMutex(session)) {
      for (UserAuthentication authentication : authentications.getAuthentications()) {
        Date cacheValidationTime = authentication.getCacheValidationTime();
        if (cacheValidationTime == null ||
          ClockUtil.getCurrentTime().after(cacheValidationTime)) {
          String userId = authentication.getIdentityId();
          String engineName = authentication.getProcessEngineName();
          UserAuthentication updatedAuth = createAuthentication(engineName, userId);
          if (updatedAuth != null) {
            if (cacheTimeToLive > 0) {
              Date newCacheValidationTime = new Date(ClockUtil.getCurrentTime().getTime() + cacheTimeToLive);
              updatedAuth.setCacheValidationTime(newCacheValidationTime);
              LOGGER.traceCacheValidationTimeUpdated(cacheValidationTime, newCacheValidationTime);
            }
            LOGGER.traceAuthenticationUpdated(engineName);
            authentications.addOrReplace(updatedAuth);

          } else {
            authentications.removeByEngineName(engineName);
            LOGGER.traceAuthenticationRemoved(engineName);

          }
        }
      }
    }
  }

  /**
   * <p>Returns the session mutex to synchronize on.
   * <p>Avoids updating the auth cache by multiple HTTP requests in parallel.
   */
  protected static Object getSessionMutex(HttpSession session) {
    Object mutex = session.getAttribute(AUTH_TIME_SESSION_MUTEX);
    if (mutex == null) {
      mutex = session; // synchronize on session if session mutex doesn't exist
    }
    return mutex;
  }

}
