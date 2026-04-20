/*
 * Copyright 2026 the Operaton contributors.
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
package org.operaton.bpm.spring.boot.starter.webapp;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.spring.boot.starter.property.WebappProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringWebappFrontendHealthContributorTest {

  @Mock
  ResourceLoader resourceLoader;

  @Mock
  WebappProperty webappProperty;

  @Test
  void shouldBeOperationalWhenAllAppsPresent() {
    when(webappProperty.getWebjarClasspath()).thenReturn("/META-INF/resources/webjars/operaton");
    when(webappProperty.getApplicationPath()).thenReturn("/operaton");

    Resource present = mock(Resource.class);
    when(present.exists()).thenReturn(true);
    when(resourceLoader.getResource(anyString())).thenReturn(present);

    var contributor = new SpringWebappFrontendHealthContributor(resourceLoader, webappProperty);
    Map<String, Object> details = contributor.frontendDetails();

    assertThat(details.get("operational")).isEqualTo(true);
    assertThat(details.get("path")).isEqualTo("/operaton");
    @SuppressWarnings("unchecked")
    Map<String, Object> apps = (Map<String, Object>) details.get("apps");
    assertThat(apps.get("cockpit")).isEqualTo(true);
    assertThat(apps.get("tasklist")).isEqualTo(true);
    assertThat(apps.get("admin")).isEqualTo(true);
  }

  @Test
  void shouldNotBeOperationalWhenAnAppIsMissing() {
    when(webappProperty.getWebjarClasspath()).thenReturn("/META-INF/resources/webjars/operaton");
    when(webappProperty.getApplicationPath()).thenReturn("/operaton");

    Resource present = mock(Resource.class);
    when(present.exists()).thenReturn(true);
    Resource absent = mock(Resource.class);
    when(absent.exists()).thenReturn(false);

    // cockpit and tasklist are present, admin is absent
    when(resourceLoader.getResource(startsWith("classpath:/META-INF/resources/webjars/operaton/app/admin"))).thenReturn(absent);
    when(resourceLoader.getResource(startsWith("classpath:/META-INF/resources/webjars/operaton/app/cockpit"))).thenReturn(present);
    when(resourceLoader.getResource(startsWith("classpath:/META-INF/resources/webjars/operaton/app/tasklist"))).thenReturn(present);

    var contributor = new SpringWebappFrontendHealthContributor(resourceLoader, webappProperty);
    Map<String, Object> details = contributor.frontendDetails();

    assertThat(details.get("operational")).isEqualTo(false);
    @SuppressWarnings("unchecked")
    Map<String, Object> apps = (Map<String, Object>) details.get("apps");
    assertThat(apps.get("cockpit")).isEqualTo(true);
    assertThat(apps.get("tasklist")).isEqualTo(true);
    assertThat(apps.get("admin")).isEqualTo(false);
  }
}
