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
package org.operaton.bpm.spring.boot.starter.webapp.filter;

import org.operaton.bpm.spring.boot.starter.webapp.filter.LazyDelegateFilter.InitHook;

import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LazyInitRegistrationTest {

  @Mock
  private LazyDelegateFilter<ResourceLoaderDependingFilter> lazyDelegateFilterMock;

  @Mock
  private ApplicationContext applicationContextMock;

  @Mock
  private InitHook<ResourceLoaderDependingFilter> initHookMock;

  @BeforeEach
  void init() {
    LazyInitRegistration.applicationContext = null;
    LazyInitRegistration.REGISTRATION.clear();
  }

  @Test
  void registerTest() {
    LazyInitRegistration.register(lazyDelegateFilterMock);
    assertThat(LazyInitRegistration.getRegistrations()).contains(lazyDelegateFilterMock);
  }

  @Test
  void getInitHookWithoutApplicationContextTest() {
    assertThat(LazyInitRegistration.getInitHook()).isNull();
  }

  @Test
  void getInitHookWithoutResourceLoaderDependingInitHook() {
    LazyInitRegistration.applicationContext = applicationContextMock;
    when(applicationContextMock.containsBean(LazyInitRegistration.RESOURCE_LOADER_DEPENDING_INIT_HOOK)).thenReturn(false);
    assertThat(LazyInitRegistration.getInitHook()).isNull();
  }

  @Test
  void getInitHookWithResourceLoaderDependingInitHook() {
    LazyInitRegistration.applicationContext = applicationContextMock;
    when(applicationContextMock.containsBean(LazyInitRegistration.RESOURCE_LOADER_DEPENDING_INIT_HOOK)).thenReturn(true);
    when(applicationContextMock.getBean(LazyInitRegistration.RESOURCE_LOADER_DEPENDING_INIT_HOOK, InitHook.class)).thenReturn(initHookMock);

    assertThat(LazyInitRegistration.getInitHook()).isEqualTo(initHookMock);
  }

  @Test
  void isRegisteredTest() {
    assertThat(LazyInitRegistration.isRegistered(lazyDelegateFilterMock)).isFalse();
    LazyInitRegistration.register(lazyDelegateFilterMock);
    assertThat(LazyInitRegistration.isRegistered(lazyDelegateFilterMock)).isTrue();
  }

  @SuppressWarnings("unchecked")
  @Test
  void lazyInitWithoutApplicationContext() {
    assertThat(LazyInitRegistration.lazyInit(lazyDelegateFilterMock)).isFalse();
    verify(lazyDelegateFilterMock, times(0)).setInitHook(Mockito.any(InitHook.class));
    verify(lazyDelegateFilterMock, times(0)).lazyInit();
  }

  @SuppressWarnings("unchecked")
  @Test
  void lazyInitWithoutRegistration() {
    LazyInitRegistration.applicationContext = applicationContextMock;
    assertThat(LazyInitRegistration.lazyInit(lazyDelegateFilterMock)).isFalse();
    verify(lazyDelegateFilterMock, times(0)).setInitHook(Mockito.any(InitHook.class));
    verify(lazyDelegateFilterMock, times(0)).lazyInit();
  }

  @Test
  void lazyInitWithRegistration() {
    LazyInitRegistration.applicationContext = applicationContextMock;
    LazyInitRegistration.register(lazyDelegateFilterMock);
    when(applicationContextMock.containsBean(LazyInitRegistration.RESOURCE_LOADER_DEPENDING_INIT_HOOK)).thenReturn(true);
    when(applicationContextMock.getBean(LazyInitRegistration.RESOURCE_LOADER_DEPENDING_INIT_HOOK, InitHook.class)).thenReturn(initHookMock);

    assertThat(LazyInitRegistration.lazyInit(lazyDelegateFilterMock)).isTrue();
    verify(lazyDelegateFilterMock, times(1)).setInitHook(initHookMock);
    verify(lazyDelegateFilterMock, times(1)).lazyInit();
    assertThat(LazyInitRegistration.getRegistrations()).doesNotContain(lazyDelegateFilterMock);
  }

  @Test
  void lazyInitWithoutInitHook() {
    LazyInitRegistration.applicationContext = applicationContextMock;
    LazyInitRegistration.register(lazyDelegateFilterMock);
    when(applicationContextMock.containsBean(LazyInitRegistration.RESOURCE_LOADER_DEPENDING_INIT_HOOK)).thenReturn(false);

    assertThat(LazyInitRegistration.lazyInit(lazyDelegateFilterMock)).isTrue();
    verify(lazyDelegateFilterMock, times(1)).setInitHook(null);
    verify(lazyDelegateFilterMock, times(1)).lazyInit();
    assertThat(LazyInitRegistration.getRegistrations()).doesNotContain(lazyDelegateFilterMock);
  }

  @Test
  void getRegistrationsTest() {
    var registrations = LazyInitRegistration.getRegistrations();

    assertThatThrownBy(() -> registrations.add(lazyDelegateFilterMock))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void setApplicationContextTest() {
    try (MockedStatic<LazyInitRegistration> theMock = Mockito.mockStatic(LazyInitRegistration.class)) {
      LazyInitRegistration.register(lazyDelegateFilterMock);
      Set<LazyDelegateFilter<? extends Filter>> registrations = new HashSet<>();
      registrations.add(lazyDelegateFilterMock);
      theMock.when(LazyInitRegistration::getRegistrations).thenReturn(registrations);

      new LazyInitRegistration().setApplicationContext(applicationContextMock);

      assertThat(applicationContextMock).isEqualTo(LazyInitRegistration.applicationContext);
      theMock.verify(() -> LazyInitRegistration.lazyInit(lazyDelegateFilterMock));
    }
  }
}
