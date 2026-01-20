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
package org.operaton.bpm.qa.performance.engine.query;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.query.Query;
import org.operaton.bpm.qa.performance.engine.junit.AuthorizationPerformanceTestCase;
import org.operaton.bpm.qa.performance.engine.junit.PerfTestProcessEngine;

import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.DEPLOYMENT;
import static java.util.Collections.emptyList;

/**
 * @author Daniel Meyer
 *
 */
@SuppressWarnings("rawtypes")
public class RepositoryAuthorizationQueryPerformanceTest extends AuthorizationPerformanceTestCase {
  public String name;
  public Query query;
  public Resource resource;
  public Permission[] permissions;
  public Authentication authentication;

  static final List<Object[]> queryResourcesAndPermissions;

  static final List<Authentication> authentications;

  static {
    ProcessEngine processEngine = PerfTestProcessEngine.getInstance();
    RepositoryService repositoryService = processEngine.getRepositoryService();

    queryResourcesAndPermissions = List.of(
        new Object[] {
            "DeploymentQuery",
            repositoryService.createDeploymentQuery(),
            DEPLOYMENT,
            new Permission[] { READ }
        },
        new Object[] {
            "ProcessDefinitionQuery",
            repositoryService.createProcessDefinitionQuery(),
            DEPLOYMENT,
            new Permission[] { READ }
        }
    );

    authentications = List.of(
        new Authentication(null, emptyList()){
          @Override
          public String toString() {
            return "without authentication";
          }
        },
        new Authentication("test", emptyList()){
          @Override
          public String toString() {
            return "with authenticated user without groups";
          }
        },
        new Authentication("test", List.of("g0", "g1")) {
          @Override
          public String toString() {
            return "with authenticated user and 2 groups";
          }
        },
        new Authentication("test", List.of("g0", "g1", "g2", "g3", "g4", "g5", "g6", "g7", "g8", "g9")) {
          @Override
          public String toString() {
            return "with authenticated user and 10 groups";
          }
        }
    );

  }

  public static Iterable<Object[]> params() {
    final ArrayList<Object[]> params = new ArrayList<>();

    for (Object[] queryResourcesAndPermission : queryResourcesAndPermissions) {
      for (Authentication authentication : authentications) {
        Object[] array = new Object[queryResourcesAndPermission.length + 1];
        System.arraycopy(queryResourcesAndPermission, 0, array, 0, queryResourcesAndPermission.length);
        array[queryResourcesAndPermission.length] = authentication;
        params.add(array);
      }
    }

    return params;
  }

  @BeforeEach
  void createAuthorizations() {
    AuthorizationService authorizationService = engine.getAuthorizationService();
    List<Authorization> auths = authorizationService.createAuthorizationQuery().list();
    for (Authorization authorization : auths) {
      authorizationService.deleteAuthorization(authorization.getId());
    }

    userGrant("test", resource, permissions);
    for (int i = 0; i < 5; i++) {
      groupGrant("g"+i, resource, permissions);
    }
    engine.getProcessEngineConfiguration().setAuthorizationEnabled(true);
  }

  @MethodSource("params")
  @ParameterizedTest(name = "{0} - {4}")
  void queryList(String name, Query query, Resource resource, Permission[] permissions, Authentication authentication) {
    initRepositoryAuthorizationQueryPerformanceTest(name, query, resource, permissions, authentication);
    performanceTest().step(context -> {
      try {
        engine.getIdentityService().setAuthentication(authentication);
        query.listPage(0, 15);
      } finally {
        engine.getIdentityService().clearAuthentication();
      }
    }).run();
  }

  @MethodSource("params")
  @ParameterizedTest(name = "{0} - {4}")
  void queryCount(String name, Query query, Resource resource, Permission[] permissions, Authentication authentication) {
    initRepositoryAuthorizationQueryPerformanceTest(name, query, resource, permissions, authentication);
    performanceTest().step(context -> {
      try {
        engine.getIdentityService().setAuthentication(authentication);
        query.count();
      } finally {
        engine.getIdentityService().clearAuthentication();
      }
    }).run();
  }

  public void initRepositoryAuthorizationQueryPerformanceTest(String name, Query query, Resource resource, Permission[] permissions, Authentication authentication) {
    this.name = name;
    this.query = query;
    this.resource = resource;
    this.permissions = permissions;
    this.authentication = authentication;
  }

}
