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
package org.operaton.bpm.integrationtest.jboss;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>Ensures subsystem boots in domain mode</p>
 *
 * @author Daniel Meyer
 * @author Christian Lipphardt
 *
 */
@ExtendWith(ArquillianExtension.class)
public class TestManagedDomain_JBOSS {

  @Deployment @TargetsContainer("test-domain")
  public static WebArchive create1() {
      return ShrinkWrap.create(WebArchive.class, "test.war");
  }

  @Test
  void shouldBeAbleToLookupDefaultProcessEngine() {

    assertThatCode(() -> InitialContext.doLookup("java:global/operaton-bpm-platform/process-engine/default"))
      .as("Expected to lookup default process engine")
      .doesNotThrowAnyException();

    assertThatThrownBy(() -> InitialContext.doLookup("java:global/operaton-bpm-platform/process-engine/someNonExistingEngine"))
      .as("Expected exception when looking up someNonExistingEngine")
      .isInstanceOf(NamingException.class);
  }

}
