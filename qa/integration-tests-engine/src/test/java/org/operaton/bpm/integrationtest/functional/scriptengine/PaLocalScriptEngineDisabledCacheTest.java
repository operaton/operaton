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
package org.operaton.bpm.integrationtest.functional.scriptengine;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.application.AbstractProcessApplication;
import org.operaton.bpm.integrationtest.functional.scriptengine.engine.AbstractScriptEngineFactory;
import org.operaton.bpm.integrationtest.functional.scriptengine.engine.DummyScriptEngineFactory;

/**
 * @author Roman Smirnov
 *
 */
public class PaLocalScriptEngineDisabledCacheTest extends AbstractPaLocalScriptEngineTest {

  @Deployment
  public static WebArchive createProcessApplication() {
    return initWebArchiveDeployment()
      .addClass(AbstractPaLocalScriptEngineTest.class)
      .addClass(AbstractScriptEngineFactory.class)
      .addClass(DummyScriptEngineFactory.class)
      .addAsResource(new StringAsset(DUMMY_SCRIPT_ENGINE_FACTORY_SPI), SCRIPT_ENGINE_FACTORY_PATH)
      .addAsResource(createScriptTaskProcess(SCRIPT_FORMAT, SCRIPT_TEXT), "process.bpmn20.xml");
  }

  @Test
  void shouldNotCacheScriptEngine() {
    AbstractProcessApplication processApplication = (AbstractProcessApplication) getProcessApplication();
    assertThat(processApplication.getScriptEngineForName(SCRIPT_FORMAT, false)).isNotEqualTo(processApplication.getScriptEngineForName(SCRIPT_FORMAT, false));
  }

}
