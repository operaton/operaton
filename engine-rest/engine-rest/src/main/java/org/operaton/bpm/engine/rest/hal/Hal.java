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
package org.operaton.bpm.engine.rest.hal;

import java.util.HashMap;
import java.util.Map;
import jakarta.ws.rs.core.MediaType;

import org.operaton.bpm.engine.rest.CaseDefinitionRestService;
import org.operaton.bpm.engine.rest.GroupRestService;
import org.operaton.bpm.engine.rest.IdentityRestService;
import org.operaton.bpm.engine.rest.ProcessDefinitionRestService;
import org.operaton.bpm.engine.rest.UserRestService;
import org.operaton.bpm.engine.rest.cache.Cache;
import org.operaton.bpm.engine.rest.hal.casedefinition.HalCaseDefinitionResolver;
import org.operaton.bpm.engine.rest.hal.group.HalGroupResolver;
import org.operaton.bpm.engine.rest.hal.identitylink.HalIdentityLinkResolver;
import org.operaton.bpm.engine.rest.hal.processdefinition.HalProcessDefinitionResolver;
import org.operaton.bpm.engine.rest.hal.user.HalUserResolver;

/**
 * @author Daniel Meyer
 *
 */
public class Hal {

  public static final String APPLICATION_HAL_JSON = "application/hal+json";
  public static final MediaType APPLICATION_HAL_JSON_TYPE = new MediaType("application", "hal+json");

  private static final Hal instance = new Hal();

  private final Map<Class<?>, HalLinkResolver> halLinkResolvers = new HashMap<>();
  private final Map<Class<?>, Cache> halRelationCaches = new HashMap<>();

  public Hal() {
    // register the built-in resolvers
    halLinkResolvers.put(UserRestService.class, new HalUserResolver());
    halLinkResolvers.put(GroupRestService.class, new HalGroupResolver());
    halLinkResolvers.put(ProcessDefinitionRestService.class, new HalProcessDefinitionResolver());
    halLinkResolvers.put(CaseDefinitionRestService.class, new HalCaseDefinitionResolver());
    halLinkResolvers.put(IdentityRestService.class, new HalIdentityLinkResolver());
  }

  public static Hal getInstance() {
    return instance;
  }

  public HalLinker createLinker(HalResource<?> resource) {
    return new HalLinker(this, resource);
  }

  public HalLinkResolver getLinkResolver(Class<?> resourceClass) {
    return halLinkResolvers.get(resourceClass);
  }

  public void registerHalRelationCache(Class<?> entityClass, Cache cache) {
    halRelationCaches.put(entityClass, cache);
  }

  public Cache getHalRelationCache(Class<?> resourceClass) {
    return halRelationCaches.get(resourceClass);
  }

  public void destroyHalRelationCaches() {
    for (Cache cache : halRelationCaches.values()) {
      cache.destroy();
    }
    halRelationCaches.clear();
  }

}
