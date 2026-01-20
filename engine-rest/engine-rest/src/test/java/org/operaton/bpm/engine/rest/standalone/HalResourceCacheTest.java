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
package org.operaton.bpm.engine.rest.standalone;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.identity.UserQuery;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.rest.AbstractRestServiceTest;
import org.operaton.bpm.engine.rest.IdentityRestService;
import org.operaton.bpm.engine.rest.UserRestService;
import org.operaton.bpm.engine.rest.cache.Cache;
import org.operaton.bpm.engine.rest.hal.Hal;
import org.operaton.bpm.engine.rest.hal.HalLinkResolver;
import org.operaton.bpm.engine.rest.hal.HalResource;
import org.operaton.bpm.engine.rest.hal.cache.DefaultHalResourceCache;
import org.operaton.bpm.engine.rest.hal.cache.HalRelationCacheBootstrap;
import org.operaton.bpm.engine.rest.hal.cache.HalRelationCacheConfiguration;
import org.operaton.bpm.engine.rest.hal.cache.HalRelationCacheConfigurationException;
import org.operaton.bpm.engine.rest.hal.identitylink.HalIdentityLink;
import org.operaton.bpm.engine.rest.hal.user.HalUser;
import org.operaton.bpm.engine.task.IdentityLink;

import static org.operaton.bpm.engine.rest.hal.cache.HalRelationCacheConfiguration.CONFIG_CACHES;
import static org.operaton.bpm.engine.rest.hal.cache.HalRelationCacheConfiguration.CONFIG_CACHE_IMPLEMENTATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HalResourceCacheTest extends AbstractRestServiceTest {

  protected DefaultHalResourceCache cache;
  protected HalRelationCacheBootstrap contextListener;

  @BeforeEach
  void createCache() {
    cache = new DefaultHalResourceCache(100, 100);
    contextListener = new HalRelationCacheBootstrap();
  }

  @AfterEach
  void destroy() {
    contextListener.contextDestroyed(null);
  }

  @Test
  void testResourceRetrieval() {
    cache.put("hello", "world");

    assertThat(cache.get(null)).isNull();
    assertThat(cache.get("unknown")).isNull();
    assertThat(cache.get("hello")).isEqualTo("world");
  }

  @Test
  void testCacheCapacity() {
    assertThat(cache.size()).isZero();

    cache.put("a", "a");
    cache.put("b", "b");
    cache.put("c", "c");
    assertThat(cache.size()).isEqualTo(3);

    forwardTime(100);

    for (int i = 0; i < 2 * cache.getCapacity(); i++) {
      cache.put("id" + i, i);
    }
    assertThat(cache.size()).isLessThanOrEqualTo(cache.getCapacity());

    // old entries should be removed
    assertThat(cache.get("a")).isNull();
    assertThat(cache.get("b")).isNull();
    assertThat(cache.get("c")).isNull();
  }

  @Test
  void testEntryExpiration() {
    cache.put("hello", "world");

    assertThat(cache.get("hello")).isEqualTo("world");
    assertThat(cache.size()).isEqualTo(1);

    forwardTime(cache.getSecondsToLive() + 1);

    assertThat(cache.get("hello")).isNull();
    assertThat(cache.size()).isZero();
  }

  @Test
  void testInvalidConfigurationFormat() {
    assertThatThrownBy(() -> contextListener.configureCaches("<!xml>"))
      .isInstanceOf(HalRelationCacheConfigurationException.class)
      .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void testUnknownCacheImplementationClass() {
    assertThatThrownBy(() -> contextListener.configureCaches("{\"" + CONFIG_CACHE_IMPLEMENTATION +"\": \"org.operaton.bpm.UnknownCache\" }"))
      .isInstanceOf(HalRelationCacheConfigurationException.class)
      .hasCauseInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void testCacheImplementationNotImplementingCache() {
    String contextParameter = "{\"" + CONFIG_CACHE_IMPLEMENTATION + "\": \"" + getClass().getName() + "\" }";
    assertThatThrownBy(() -> contextListener.configureCaches(contextParameter))
      .isInstanceOf(HalRelationCacheConfigurationException.class)
      .hasMessageContaining(Cache.class.getName());
  }

  @Test
  void testCacheCreation() {
    String contextParameter = "{" +
        "\"" + CONFIG_CACHE_IMPLEMENTATION + "\": \"" + DefaultHalResourceCache.class.getName() + "\"," +
        "\"" + CONFIG_CACHES + "\": {" +
          "\"" + HalUser.class.getName() + "\": {" +
            "\"capacity\": 123, \"secondsToLive\": 123" +
          "}" +
        "}" +
      "}";

    contextListener.configureCaches(contextParameter);

    Cache userCache = Hal.getInstance().getHalRelationCache(HalUser.class);
    assertThat(userCache).isNotNull();
    assertThat(((DefaultHalResourceCache) userCache).getCapacity()).isEqualTo(123);
    assertThat(((DefaultHalResourceCache) userCache).getSecondsToLive()).isEqualTo(123);
  }

  @Test
  void testCacheInvalidParameterName() {
    HalRelationCacheConfiguration configuration = new HalRelationCacheConfiguration();
    configuration.setCacheImplementationClass(DefaultHalResourceCache.class);
    configuration.addCacheConfiguration(HalUser.class, Collections.singletonMap("unknown", "property"));

    assertThatThrownBy(() -> contextListener.configureCaches(configuration))
      .isInstanceOf(HalRelationCacheConfigurationException.class)
      .hasMessageContaining("setter");
  }

  @Test
  void testEntityCaching() {
    String[] userIds = new String[]{"test"};
    // mock user and query
    User user = mock(User.class);
    when(user.getId()).thenReturn(userIds[0]);
    when(user.getFirstName()).thenReturn("kermit");
    UserQuery userQuery = mock(UserQuery.class);
    when(userQuery.userIdIn(Mockito.any())).thenReturn(userQuery);
    when(userQuery.listPage(anyInt(), anyInt())).thenReturn(List.of(user));
    when(processEngine.getIdentityService().createUserQuery()).thenReturn(userQuery);

    // configure cache
    HalRelationCacheConfiguration configuration = new HalRelationCacheConfiguration();
    configuration.setCacheImplementationClass(DefaultHalResourceCache.class);
    Map<String, Object> halUserConfig = new HashMap<>();
    halUserConfig.put("capacity", 100);
    halUserConfig.put("secondsToLive", 10000);
    configuration.addCacheConfiguration(HalUser.class, halUserConfig);

    contextListener.configureCaches(configuration);

    // cache exists and is empty
    DefaultHalResourceCache userCache = (DefaultHalResourceCache) Hal.getInstance().getHalRelationCache(HalUser.class);
    assertThat(userCache).isNotNull();
    assertThat(userCache.size()).isZero();

    // get link resolver and resolve user
    HalLinkResolver linkResolver = Hal.getInstance().getLinkResolver(UserRestService.class);
    List<HalResource<?>> halUsers = linkResolver.resolveLinks(userIds, processEngine);

    assertThat(halUsers)
            // mocked user was resolved
            .isNotNull()
            .hasSize(1);
    HalUser halUser = (HalUser) halUsers.get(0);
    assertThat(halUser.getFirstName()).isEqualTo("kermit");

    // cache contains user
    assertThat(userCache.size()).isEqualTo(1);

    // change user mock
    when(user.getFirstName()).thenReturn("fritz");

    // resolve users again
    halUsers = linkResolver.resolveLinks(userIds, processEngine);

    assertThat(halUsers)
            // cached mocked user was resolved with old name
            .isNotNull()
            .hasSize(1);
    halUser = (HalUser) halUsers.get(0);
    assertThat(halUser.getFirstName()).isEqualTo("kermit");

    forwardTime(userCache.getSecondsToLive() * 3);

    // resolve users again
    halUsers = linkResolver.resolveLinks(userIds, processEngine);

    assertThat(halUsers)
            // new mocked user was resolved with old name
            .isNotNull()
            .hasSize(1);
    halUser = (HalUser) halUsers.get(0);
    assertThat(halUser.getFirstName()).isEqualTo("fritz");
  }

  @Test
  void testIdentityLinkCaching() {
    String[] taskIds = new String[]{"test"};
    // mock identityLinks and query
    IdentityLink link1 = mock(IdentityLink.class);
    when(link1.getTaskId()).thenReturn(taskIds[0]);
    IdentityLink link2 = mock(IdentityLink.class);
    when(link2.getTaskId()).thenReturn(taskIds[0]);
    when(processEngine.getTaskService().getIdentityLinksForTask(anyString())).thenReturn(List.of(link1, link2));

    // configure cache
    HalRelationCacheConfiguration configuration = new HalRelationCacheConfiguration();
    configuration.setCacheImplementationClass(DefaultHalResourceCache.class);
    Map<String, Object> halIdentityLinkConfig = new HashMap<>();
    halIdentityLinkConfig.put("capacity", 100);
    halIdentityLinkConfig.put("secondsToLive", 10000);
    configuration.addCacheConfiguration(HalIdentityLink.class, halIdentityLinkConfig);

    contextListener.configureCaches(configuration);

    // cache exists and is empty
    DefaultHalResourceCache identityLinkCache = (DefaultHalResourceCache) Hal.getInstance().getHalRelationCache(HalIdentityLink.class);
    assertThat(identityLinkCache).isNotNull();
    assertThat(identityLinkCache.size()).isZero();

    // get link resolver and resolve identity link
    HalLinkResolver linkResolver = Hal.getInstance().getLinkResolver(IdentityRestService.class);
    List<HalResource<?>> halIdentityLinks = linkResolver.resolveLinks(taskIds, processEngine);

    assertThat(halIdentityLinks).hasSize(2);
    assertThat(identityLinkCache.size()).isEqualTo(1);

    assertThat(identityLinkCache.get(taskIds[0])).isEqualTo(halIdentityLinks);
  }

  protected void forwardTime(long seconds) {
    Date later = new Date(ClockUtil.getCurrentTime().getTime() + seconds * 1000);
    ClockUtil.setCurrentTime(later);
  }

}
