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

import jakarta.servlet.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class LazyDelegateFilterTest {

  @Mock
  private Filter filterMock;

  @Mock
  private FilterConfig filterConfigMock;

  @Mock
  private InitHook<Filter> initHookMock;

  @Test
  void initTest() throws Exception {
    try (MockedStatic<LazyInitRegistration> theMock = Mockito.mockStatic(LazyInitRegistration.class)) {
      LazyDelegateFilter<Filter> delegateFilter = new LazyDelegateFilter<>(filterMock.getClass());
      delegateFilter.delegate = filterMock;
      delegateFilter.init(filterConfigMock);
      assertThat(delegateFilter.filterConfig).isSameAs(filterConfigMock);
      verify(filterMock, times(0)).init(Mockito.any(FilterConfig.class));
      theMock.verify(() -> LazyInitRegistration.lazyInit(delegateFilter));
    }
  }

  @Test
  void lazyInitTest() throws Exception {
    try (MockedStatic<LazyInitRegistration> theMock = Mockito.mockStatic(LazyInitRegistration.class)) {
      LazyDelegateFilter<Filter> delegateFilter = spy(new LazyDelegateFilter<>(filterMock.getClass()));
      delegateFilter.init(filterConfigMock);
      doReturn(filterMock).when(delegateFilter).createNewFilterInstance();
      delegateFilter.lazyInit();
      verify(filterMock).init(filterConfigMock);
    }
  }

  @Test
  void lazyInitWithHookTest() throws Exception {
    try (MockedStatic<LazyInitRegistration> theMock = Mockito.mockStatic(LazyInitRegistration.class)) {
      LazyDelegateFilter<Filter> delegateFilter = spy(new LazyDelegateFilter<>(filterMock.getClass()));
      delegateFilter.setInitHook(initHookMock);
      delegateFilter.init(filterConfigMock);
      doReturn(filterMock).when(delegateFilter).createNewFilterInstance();
      delegateFilter.lazyInit();
      InOrder order = inOrder(filterMock, initHookMock);
      order.verify(initHookMock).init(filterMock);
      order.verify(filterMock).init(filterConfigMock);
    }
  }

  @Test
  void doFilterTest() throws Exception {
    try (MockedStatic<LazyInitRegistration> theMock = Mockito.mockStatic(LazyInitRegistration.class)) {
      LazyDelegateFilter<Filter> delegateFilter = new LazyDelegateFilter<>(filterMock.getClass());
      delegateFilter.delegate = filterMock;
      ServletRequest request = mock(ServletRequest.class);
      ServletResponse response = mock(ServletResponse.class);
      FilterChain chain = mock(FilterChain.class);
      delegateFilter.doFilter(request, response, chain);
      verify(filterMock).doFilter(request, response, chain);
    }
  }

  @Test
  void destroyTest() {
    try (MockedStatic<LazyInitRegistration> theMock = Mockito.mockStatic(LazyInitRegistration.class)) {
      LazyDelegateFilter<Filter> delegateFilter = new LazyDelegateFilter<>(filterMock.getClass());
      delegateFilter.delegate = filterMock;
      delegateFilter.destroy();
      verify(filterMock).destroy();
    }
  }

  @Test
  void destroyUninitializedDelegateTest() {
    try (MockedStatic<LazyInitRegistration> theMock = Mockito.mockStatic(LazyInitRegistration.class)) {
      LazyDelegateFilter<Filter> delegateFilter = new LazyDelegateFilter<>(filterMock.getClass());
      delegateFilter.destroy();
      verify(filterMock, never()).destroy();
    }
  }

  @Test
  void lazyInitRegistrationTest() {
    try (MockedStatic<LazyInitRegistration> theMock = Mockito.mockStatic(LazyInitRegistration.class)) {
      LazyDelegateFilter<Filter> delegateFilter = new LazyDelegateFilter<>(filterMock.getClass());
      theMock.verify(() -> LazyInitRegistration.register(delegateFilter));
    }
  }

}
