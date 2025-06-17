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
package org.operaton.bpm.webapp.impl.engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.cockpit.Cockpit;
import org.operaton.bpm.cockpit.impl.DefaultCockpitRuntimeDelegate;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.spi.ProcessEngineProvider;
import org.operaton.bpm.webapp.impl.IllegalWebAppConfigurationException;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 *
 * @author nico.rehwaldt
 */
class EnginesFilterTest {

  @Test
  void htmlFilePattern() {

    // given
    Pattern pattern = ProcessEnginesFilter.APP_PREFIX_PATTERN;

    // when
    Matcher matcher1 = pattern.matcher("/app/cockpit/");
    Matcher matcher2 = pattern.matcher("/app/cockpit/engine1/");
    Matcher matcher3 = pattern.matcher("/app/cockpit/engine1/something/asd.html");
    Matcher matcher4 = pattern.matcher("/app/admin/engine1/something/asd.html");
    Matcher matcher5 = pattern.matcher("/app/cockpit/index.html");
    Matcher matcher6 = pattern.matcher("/app/tasklist/spring-engine/");

    // then
    assertThat(matcher1.matches()).isTrue();
    assertThat(matcher1.group(1)).isEqualTo("cockpit");
    assertThat(matcher1.groupCount()).isEqualTo(3);
    assertThat(matcher1.group(2)).isNull();

    assertThat(matcher2.matches()).isTrue();
    assertThat(matcher2.group(1)).isEqualTo("cockpit");
    assertThat(matcher2.group(2)).isEqualTo("engine1");
    assertThat(matcher2.group(3)).isEmpty();

    assertThat(matcher3.matches()).isTrue();
    assertThat(matcher3.group(1)).isEqualTo("cockpit");
    assertThat(matcher3.group(2)).isEqualTo("engine1");
    assertThat(matcher3.group(3)).isEqualTo("something/asd.html");

    assertThat(matcher4.matches()).isTrue();
    assertThat(matcher4.group(1)).isEqualTo("admin");
    assertThat(matcher4.group(2)).isEqualTo("engine1");
    assertThat(matcher4.group(3)).isEqualTo("something/asd.html");

    assertThat(matcher5.matches()).isTrue();
    assertThat(matcher5.group(1)).isEqualTo("cockpit");
    assertThat(matcher5.group(2)).isEqualTo("index.html");
    assertThat(matcher5.group(3)).isEmpty();

    assertThat(matcher6.matches()).isTrue();
    assertThat(matcher6.group(1)).isEqualTo("tasklist");
    assertThat(matcher6.group(2)).isEqualTo("spring-engine");
    assertThat(matcher6.group(3)).isEmpty();
  }

  @Test
  void getDefaultProcessEngine() {

    // see https://app.camunda.com/jira/browse/CAM-2126

    // runtime delegate returns single, non-default-named process engine engine

    Cockpit.setCockpitRuntimeDelegate(new DefaultCockpitRuntimeDelegate() {

      @Override
      protected ProcessEngineProvider loadProcessEngineProvider() {
        return null;
      }

      @Override
      public Set<String> getProcessEngineNames() {
        return Collections.singleton("foo");
      }

      @Override
      public ProcessEngine getDefaultProcessEngine() {
        return null;
      }
    });

    ProcessEnginesFilter processEnginesFilter = new ProcessEnginesFilter();
    String defaultEngineName = processEnginesFilter.getDefaultEngineName();
    assertThat(defaultEngineName).isEqualTo("foo");


    // now it returns 'null'

    Cockpit.setCockpitRuntimeDelegate(new DefaultCockpitRuntimeDelegate() {

      @Override
      protected ProcessEngineProvider loadProcessEngineProvider() {
        return null;
      }

      @Override
      public Set<String> getProcessEngineNames() {
        return Collections.emptySet();
      }

      @Override
      public ProcessEngine getDefaultProcessEngine() {
        return null;
      }
    });

    try {
      defaultEngineName = processEnginesFilter.getDefaultEngineName();
      fail("");
    } catch(IllegalWebAppConfigurationException e) {
      // expected
    }

  }

  @AfterEach
  void cleanup() {
    Cockpit.setCockpitRuntimeDelegate(null);
  }
}
