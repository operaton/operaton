/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.commons.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @since 1.1
 */
class ServiceLoaderUtilTest {
  interface TestService {
    String getName();
  }
  interface TestService2 {
    String getName();
  }

  public static class TestServiceImpl implements TestService {
    @Override
    public String getName() {
      return "TestServiceImpl";
    }
  }
  public static class TestServiceImpl2 implements TestService2 {
    @Override
    public String getName() {
      return "TestServiceImpl";
    }
  }

  @Test
  void loadSingleService_succeeds_whenImplementationIsRegisteredInMetaInf() {
    TestService service = ServiceLoaderUtil.loadSingleService(TestService.class);
    assertThat(service.getName()).isEqualTo("TestServiceImpl");
  }

  @Test
  void loadSingleService_throwsException_whenNoImplementationRegistered() {
    assertThatThrownBy(() -> ServiceLoaderUtil.loadSingleService(TestService2.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No TestService2 found");
  }
}
