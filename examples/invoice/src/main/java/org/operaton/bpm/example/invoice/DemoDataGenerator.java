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
package org.operaton.bpm.example.invoice;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.ACCESS;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.APPLICATION;
import static org.operaton.bpm.engine.authorization.Resources.FILTER;
import static org.operaton.bpm.engine.authorization.Resources.TASK;
import static org.operaton.bpm.engine.authorization.Resources.USER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.IdentityServiceImpl;
import org.operaton.bpm.engine.impl.persistence.entity.AuthorizationEntity;
import org.operaton.bpm.engine.task.TaskQuery;

/**
 * Creates demo credentials to be used in the invoice showcase.
 *
 * @author drobisch
 */
public class DemoDataGenerator {

  private static final String GROUP_ACCOUNTING = "accounting";
  private static final String GROUP_MANAGEMENT = "management";
  private static final String GROUP_SALES = "sales";
  private static final String GROUP_TYPE_WORKFLOW = "WORKFLOW";
  private static final String RESOURCE_ID_DEMO = "demo";
  private static final String RESOURCE_ID_INVOICE = "invoice";
  private static final String RESOURCE_ID_TASKLIST = "tasklist";

  private static final Logger LOGGER = Logger.getLogger(DemoDataGenerator.class.getName());

  public void createUsers(ProcessEngine engine) {

      final IdentityServiceImpl identityService = (IdentityServiceImpl) engine.getIdentityService();

      if(identityService.isReadOnly()) {
        LOGGER.info("Identity service provider is Read Only, not creating any demo users.");
        return;
      }

      User singleResult = identityService.createUserQuery().userId(RESOURCE_ID_DEMO).singleResult();
      if (singleResult != null) {
        return;
      }

      LOGGER.info("Generating demo data for invoice showcase");

      User user = identityService.newUser(RESOURCE_ID_DEMO);
      user.setFirstName("Demo");
      user.setLastName("Demo");
      user.setPassword(RESOURCE_ID_DEMO);
      user.setEmail("demo@operaton.org");
      identityService.saveUser(user, true);

      User user2 = identityService.newUser("john");
      user2.setFirstName("John");
      user2.setLastName("Doe");
      user2.setPassword("john");
      user2.setEmail("john@operaton.org");
      identityService.saveUser(user2, true);

      User user3 = identityService.newUser("mary");
      user3.setFirstName("Mary");
      user3.setLastName("Anne");
      user3.setPassword("mary");
      user3.setEmail("mary@operaton.org");
      identityService.saveUser(user3, true);

      User user4 = identityService.newUser("peter");
      user4.setFirstName("Peter");
      user4.setLastName("Meter");
      user4.setPassword("peter");
      user4.setEmail("peter@operaton.org");
      identityService.saveUser(user4, true);

      Group salesGroup = identityService.newGroup(GROUP_SALES);
      salesGroup.setName("Sales");
      salesGroup.setType(GROUP_TYPE_WORKFLOW);
      identityService.saveGroup(salesGroup);

      Group accountingGroup = identityService.newGroup(GROUP_ACCOUNTING);
      accountingGroup.setName("Accounting");
      accountingGroup.setType(GROUP_TYPE_WORKFLOW);
      identityService.saveGroup(accountingGroup);

      Group managementGroup = identityService.newGroup(GROUP_MANAGEMENT);
      managementGroup.setName("Management");
      managementGroup.setType(GROUP_TYPE_WORKFLOW);
      identityService.saveGroup(managementGroup);

      final AuthorizationService authorizationService = engine.getAuthorizationService();

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

      identityService.createMembership(RESOURCE_ID_DEMO, GROUP_SALES);
      identityService.createMembership(RESOURCE_ID_DEMO, GROUP_ACCOUNTING);
      identityService.createMembership(RESOURCE_ID_DEMO, GROUP_MANAGEMENT);
      identityService.createMembership(RESOURCE_ID_DEMO, "operaton-admin");

      identityService.createMembership("john", GROUP_SALES);
      identityService.createMembership("mary", GROUP_ACCOUNTING);
      identityService.createMembership("peter", GROUP_MANAGEMENT);


      // authorize groups for tasklist only:

      Authorization salesTasklistAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      salesTasklistAuth.setGroupId(GROUP_SALES);
      salesTasklistAuth.addPermission(ACCESS);
      salesTasklistAuth.setResourceId(RESOURCE_ID_TASKLIST);
      salesTasklistAuth.setResource(APPLICATION);
      authorizationService.saveAuthorization(salesTasklistAuth);

      Authorization salesReadProcessDefinition = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      salesReadProcessDefinition.setGroupId(GROUP_SALES);
      salesReadProcessDefinition.addPermission(Permissions.READ);
      salesReadProcessDefinition.addPermission(Permissions.READ_HISTORY);
      salesReadProcessDefinition.setResource(Resources.PROCESS_DEFINITION);
      // restrict to invoice process definition only
      salesReadProcessDefinition.setResourceId(RESOURCE_ID_INVOICE);
      authorizationService.saveAuthorization(salesReadProcessDefinition);

      Authorization accountingTasklistAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      accountingTasklistAuth.setGroupId(GROUP_ACCOUNTING);
      accountingTasklistAuth.addPermission(ACCESS);
      accountingTasklistAuth.setResourceId(RESOURCE_ID_TASKLIST);
      accountingTasklistAuth.setResource(APPLICATION);
      authorizationService.saveAuthorization(accountingTasklistAuth);

      Authorization accountingReadProcessDefinition = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      accountingReadProcessDefinition.setGroupId(GROUP_ACCOUNTING);
      accountingReadProcessDefinition.addPermission(Permissions.READ);
      accountingReadProcessDefinition.addPermission(Permissions.READ_HISTORY);
      accountingReadProcessDefinition.setResource(Resources.PROCESS_DEFINITION);
      // restrict to invoice process definition only
      accountingReadProcessDefinition.setResourceId(RESOURCE_ID_INVOICE);
      authorizationService.saveAuthorization(accountingReadProcessDefinition);

      Authorization managementTasklistAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      managementTasklistAuth.setGroupId(GROUP_MANAGEMENT);
      managementTasklistAuth.addPermission(ACCESS);
      managementTasklistAuth.setResourceId(RESOURCE_ID_TASKLIST);
      managementTasklistAuth.setResource(APPLICATION);
      authorizationService.saveAuthorization(managementTasklistAuth);

      Authorization managementReadProcessDefinition = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      managementReadProcessDefinition.setGroupId(GROUP_MANAGEMENT);
      managementReadProcessDefinition.addPermission(Permissions.READ);
      managementReadProcessDefinition.addPermission(Permissions.READ_HISTORY);
      managementReadProcessDefinition.setResource(Resources.PROCESS_DEFINITION);
      // restrict to invoice process definition only
      managementReadProcessDefinition.setResourceId(RESOURCE_ID_INVOICE);
      authorizationService.saveAuthorization(managementReadProcessDefinition);

      Authorization salesDemoAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      salesDemoAuth.setGroupId(GROUP_SALES);
      salesDemoAuth.setResource(USER);
      salesDemoAuth.setResourceId(RESOURCE_ID_DEMO);
      salesDemoAuth.addPermission(READ);
      authorizationService.saveAuthorization(salesDemoAuth);

      Authorization salesJohnAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      salesJohnAuth.setGroupId(GROUP_SALES);
      salesJohnAuth.setResource(USER);
      salesJohnAuth.setResourceId("john");
      salesJohnAuth.addPermission(READ);
      authorizationService.saveAuthorization(salesJohnAuth);

      Authorization manDemoAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      manDemoAuth.setGroupId(GROUP_MANAGEMENT);
      manDemoAuth.setResource(USER);
      manDemoAuth.setResourceId(RESOURCE_ID_DEMO);
      manDemoAuth.addPermission(READ);
      authorizationService.saveAuthorization(manDemoAuth);

      Authorization manPeterAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      manPeterAuth.setGroupId(GROUP_MANAGEMENT);
      manPeterAuth.setResource(USER);
      manPeterAuth.setResourceId("peter");
      manPeterAuth.addPermission(READ);
      authorizationService.saveAuthorization(manPeterAuth);

      Authorization accDemoAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      accDemoAuth.setGroupId(GROUP_ACCOUNTING);
      accDemoAuth.setResource(USER);
      accDemoAuth.setResourceId(RESOURCE_ID_DEMO);
      accDemoAuth.addPermission(READ);
      authorizationService.saveAuthorization(accDemoAuth);

      Authorization accMaryAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      accMaryAuth.setGroupId(GROUP_ACCOUNTING);
      accMaryAuth.setResource(USER);
      accMaryAuth.setResourceId("mary");
      accMaryAuth.addPermission(READ);
      authorizationService.saveAuthorization(accMaryAuth);

      Authorization taskMaryAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
      taskMaryAuth.setUserId("mary");
      taskMaryAuth.setResource(TASK);
      taskMaryAuth.setResourceId(ANY);
      taskMaryAuth.addPermission(READ);
      taskMaryAuth.addPermission(UPDATE);
      authorizationService.saveAuthorization(taskMaryAuth);

      // create default filters

      FilterService filterService = engine.getFilterService();

      Map<String, Object> filterProperties = new HashMap<>();
      filterProperties.put("description", "Tasks assigned to me");
      filterProperties.put("priority", -10);
      addVariables(filterProperties);
      TaskService taskService = engine.getTaskService();
      TaskQuery query = taskService.createTaskQuery().taskAssigneeExpression("${currentUser()}");
      Filter myTasksFilter = filterService.newTaskFilter().setName("My Tasks").setProperties(filterProperties).setOwner(
        RESOURCE_ID_DEMO).setQuery(query);
      filterService.saveFilter(myTasksFilter);

      filterProperties.clear();
      filterProperties.put("description", "Tasks assigned to my Groups");
      filterProperties.put("priority", -5);
      addVariables(filterProperties);
      query = taskService.createTaskQuery().taskCandidateGroupInExpression("${currentUserGroups()}").taskUnassigned();
      Filter groupTasksFilter = filterService.newTaskFilter().setName("My Group Tasks").setProperties(filterProperties).setOwner(
        RESOURCE_ID_DEMO).setQuery(query);
      filterService.saveFilter(groupTasksFilter);

      // global read authorizations for these filters

      Authorization globalMyTaskFilterRead = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GLOBAL);
      globalMyTaskFilterRead.setResource(FILTER);
      globalMyTaskFilterRead.setResourceId(myTasksFilter.getId());
      globalMyTaskFilterRead.addPermission(READ);
      authorizationService.saveAuthorization(globalMyTaskFilterRead);

      Authorization globalGroupFilterRead = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GLOBAL);
      globalGroupFilterRead.setResource(FILTER);
      globalGroupFilterRead.setResourceId(groupTasksFilter.getId());
      globalGroupFilterRead.addPermission(READ);
      authorizationService.saveAuthorization(globalGroupFilterRead);

      // management filter

      filterProperties.clear();
      filterProperties.put("description", "Tasks for Group Accounting");
      filterProperties.put("priority", -3);
      addVariables(filterProperties);
      query = taskService.createTaskQuery().taskCandidateGroupIn(Arrays.asList(GROUP_ACCOUNTING)).taskUnassigned();
      Filter candidateGroupTasksFilter = filterService.newTaskFilter().setName("Accounting").setProperties(filterProperties).setOwner(
        RESOURCE_ID_DEMO).setQuery(query);
      filterService.saveFilter(candidateGroupTasksFilter);

      Authorization managementGroupFilterRead = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
      managementGroupFilterRead.setResource(FILTER);
      managementGroupFilterRead.setResourceId(candidateGroupTasksFilter.getId());
      managementGroupFilterRead.addPermission(READ);
      managementGroupFilterRead.setGroupId(GROUP_ACCOUNTING);
      authorizationService.saveAuthorization(managementGroupFilterRead);

      // john's tasks

      filterProperties.clear();
      filterProperties.put("description", "Tasks assigned to John");
      filterProperties.put("priority", -1);
      addVariables(filterProperties);
      query = taskService.createTaskQuery().taskAssignee("john");
      Filter johnsTasksFilter = filterService.newTaskFilter().setName("John's Tasks").setProperties(filterProperties).setOwner(
        RESOURCE_ID_DEMO).setQuery(query);
      filterService.saveFilter(johnsTasksFilter);

      // mary's tasks

      filterProperties.clear();
      filterProperties.put("description", "Tasks assigned to Mary");
      filterProperties.put("priority", -1);
      addVariables(filterProperties);
      query = taskService.createTaskQuery().taskAssignee("mary");
      Filter marysTasksFilter = filterService.newTaskFilter().setName("Mary's Tasks").setProperties(filterProperties).setOwner(
        RESOURCE_ID_DEMO).setQuery(query);
      filterService.saveFilter(marysTasksFilter);

      // peter's tasks

      filterProperties.clear();
      filterProperties.put("description", "Tasks assigned to Peter");
      filterProperties.put("priority", -1);
      addVariables(filterProperties);
      query = taskService.createTaskQuery().taskAssignee("peter");
      Filter petersTasksFilter = filterService.newTaskFilter().setName("Peter's Tasks").setProperties(filterProperties).setOwner(
        RESOURCE_ID_DEMO).setQuery(query);
      filterService.saveFilter(petersTasksFilter);

      // all tasks

      filterProperties.clear();
      filterProperties.put("description", "All Tasks - Not recommended to be used in production :)");
      filterProperties.put("priority", 10);
      addVariables(filterProperties);
      query = taskService.createTaskQuery();
      Filter allTasksFilter = filterService.newTaskFilter().setName("All Tasks").setProperties(filterProperties).setOwner(
        RESOURCE_ID_DEMO).setQuery(query);
      filterService.saveFilter(allTasksFilter);

    }

    protected void addVariables(Map<String, Object> filterProperties) {
      List<Object> variables = new ArrayList<>();

      addVariable(variables, "amount", "Invoice Amount");
      addVariable(variables, "invoiceNumber", "Invoice Number");
      addVariable(variables, "creditor", "Creditor");
      addVariable(variables, "approver", "Approver");

      filterProperties.put("variables", variables);
    }

    protected void addVariable(List<Object> variables, String name, String label) {
      Map<String, String> variable = new HashMap<>();
      variable.put("name", name);
      variable.put("label", label);
      variables.add(variable);
    }
}
