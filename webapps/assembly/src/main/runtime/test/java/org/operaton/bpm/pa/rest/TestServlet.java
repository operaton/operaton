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
package org.operaton.bpm.pa.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.admin.impl.web.SetupResource;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.db.PersistenceSession;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.metrics.Meter;
import org.operaton.bpm.engine.rest.dto.identity.UserCredentialsDto;
import org.operaton.bpm.engine.rest.dto.identity.UserDto;
import org.operaton.bpm.engine.rest.dto.identity.UserProfileDto;

/**
 *
 * This servlet allows the testsuite to check whether the database is clean.
 * If the database is not clean the schema is dropped and re-created.
 *
 * <p>
 * Invoke using GET /operaton/ensureCleanDb/{engineName}
 * </p>
 *
 * <p>
 * Response is JSON: {"clean":true}
 * </p>
 *
 * @author Daniel Meyer
 *
 */
@WebServlet(urlPatterns = {"/ensureCleanDb/*"})
public class TestServlet extends HttpServlet {

  public static final Logger log = Logger.getLogger(TestServlet.class.getName());

  private static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = Arrays.asList(
      "ACT_GE_PROPERTY"
    );

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setHeader("content-type", "application/json");

    String requestURI = req.getRequestURI();

    int lastSlash = requestURI.lastIndexOf("/");
    String engineName = requestURI.substring(lastSlash+1, requestURI.length());

    ProcessEngine processEngine = BpmPlatform.getProcessEngineService().getProcessEngine(engineName);

    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    try {
      processEngineConfiguration.setAuthorizationEnabled(false);
      doClean(req, resp, processEngine);
   }
   finally {
     processEngineConfiguration.setAuthorizationEnabled(true);
   }

  }

  protected void doClean(HttpServletRequest req, HttpServletResponse resp, ProcessEngine processEngine) throws ServletException, IOException {
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();

    deleteAdminUser(processEngine);

    ManagementService managementService = processEngine.getManagementService();

    Collection<Meter> meters = processEngineConfiguration.getMetricsRegistry().getDbMeters().values();
    for (Meter meter : meters) {
      meter.getAndClear();
    }

    log.fine("verifying that db is clean after test");
    Map<String, Long> tableCounts = managementService.getTableCount();

    StringBuilder outputMessage = new StringBuilder();
    for (String tableName : tableCounts.keySet()) {
      String tableNameWithoutPrefix = tableName.replace(processEngineConfiguration.getDatabaseTablePrefix(), "");
      if (!TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.contains(tableNameWithoutPrefix)) {
        Long count = tableCounts.get(tableName);
        if (count != 0L) {
          outputMessage.append("  ")
            .append(tableName)
            .append(": ")
            .append(count)
            .append(" record(s)\n");
        }
      }
    }
    if (outputMessage.length() > 0) {
      outputMessage.insert(0, "DB NOT CLEAN: \n");
      log.severe("\n");
      log.severe(outputMessage.toString());

      log.info("dropping and recreating db");

      CommandExecutor commandExecutor = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration().getCommandExecutorTxRequired();
      commandExecutor.execute(new Command<Object>() {
        public Object execute(CommandContext commandContext) {
          PersistenceSession persistenceSession = commandContext.getSession(PersistenceSession.class);
          persistenceSession.dbSchemaDrop();
          persistenceSession.dbSchemaCreate();
          return null;
        }
      });

      createAdminUser(processEngine);
      resp.getWriter().write("{\"clean\": false}");
    }
    else {
      log.info("database was clean");

      createAdminUser(processEngine);
      resp.getWriter().write("{\"clean\": true}");
    }
  }

  protected void createAdminUser(ProcessEngine processEngine) {
    UserDto user = new UserDto();
    UserCredentialsDto userCredentialsDto = new UserCredentialsDto();
    userCredentialsDto.setPassword("admin");
    user.setCredentials(userCredentialsDto);

    UserProfileDto userProfileDto = new UserProfileDto();
    userProfileDto.setId("admin");
    userProfileDto.setFirstName("Steve");
    userProfileDto.setLastName("Hentschi");
    user.setProfile(userProfileDto);

    try {
      new SetupResource().createInitialUser(processEngine.getName(), user);

    } catch (IOException | ServletException e) {
     throw new RuntimeException(e);

    }
  }

  protected void deleteAdminUser(ProcessEngine processEngine) {

    IdentityService identityService = processEngine.getIdentityService();

    identityService.deleteMembership("admin", Groups.OPERATON_ADMIN);

    identityService.deleteGroup(Groups.OPERATON_ADMIN);
    identityService.deleteUser("admin");

  }

}
