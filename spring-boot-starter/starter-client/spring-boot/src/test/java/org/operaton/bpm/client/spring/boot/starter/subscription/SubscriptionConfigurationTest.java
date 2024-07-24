/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.client.spring.boot.starter.subscription;

import org.operaton.bpm.client.spring.boot.starter.ParsePropertiesHelper;
import org.operaton.bpm.client.spring.impl.subscription.SubscriptionConfiguration;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@TestPropertySource(properties = {
    "operaton.bpm.client.subscriptions.topic-one.auto-open=false",
    "operaton.bpm.client.subscriptions.topic-one.lock-duration=555",
    "operaton.bpm.client.subscriptions.topic-one.variable-names=var-one,var-two",
    "operaton.bpm.client.subscriptions.topic-one.local-variables=true",
    "operaton.bpm.client.subscriptions.topic-one.business-key=business-key",
    "operaton.bpm.client.subscriptions.topic-one.process-definition-id=definition-id",
    "operaton.bpm.client.subscriptions.topic-one.process-definition-id-in=id-one,id-two",
    "operaton.bpm.client.subscriptions.topic-one.process-definition-key=key",
    "operaton.bpm.client.subscriptions.topic-one.process-definition-key-in=key-one,key-two",
    "operaton.bpm.client.subscriptions.topic-one.process-definition-version-tag=version-tag",
    "operaton.bpm.client.subscriptions.topic-one.process-variables.var-name-foo=var-val-foo",
    "operaton.bpm.client.subscriptions.topic-one.process-variables.var-name-bar=var-val-bar",
    "operaton.bpm.client.subscriptions.topic-one.without-tenant-id=true",
    "operaton.bpm.client.subscriptions.topic-one.tenant-id-in=tenant-id-one,tenant-id-two",
    "operaton.bpm.client.subscriptions.topic-one.include-extension-properties=true",

    "operaton.bpm.client.subscriptions.topic-two.lock-duration=6666",
})
public class SubscriptionConfigurationTest extends ParsePropertiesHelper {

  @Test
  public void shouldCheckSubscriptionOneProperties() {
    SubscriptionConfiguration subscriptionOne = subscriptions.get("topic-one");

    assertThat(subscriptionOne.getAutoOpen()).isFalse();
    assertThat(subscriptionOne.getLockDuration()).isEqualTo(555);
    assertThat(subscriptionOne.getVariableNames()).contains("var-one", "var-two");
    assertThat(subscriptionOne.getLocalVariables()).isTrue();
    assertThat(subscriptionOne.getBusinessKey()).isEqualTo("business-key");
    assertThat(subscriptionOne.getProcessDefinitionId()).isEqualTo("definition-id");
    assertThat(subscriptionOne.getProcessDefinitionIdIn()).contains("id-one", "id-two");
    assertThat(subscriptionOne.getProcessDefinitionKey()).isEqualTo("key");
    assertThat(subscriptionOne.getProcessDefinitionKeyIn()).contains("key-one", "key-two");
    assertThat(subscriptionOne.getProcessDefinitionVersionTag()).isEqualTo("version-tag");
    assertThat(subscriptionOne.getProcessVariables())
        .contains(entry("var-name-foo", "var-val-foo"), entry("var-name-bar", "var-val-bar"));
    assertThat(subscriptionOne.getWithoutTenantId()).isTrue();
    assertThat(subscriptionOne.getTenantIdIn()).contains("tenant-id-one", "tenant-id-two");
    assertThat(subscriptionOne.getIncludeExtensionProperties()).isTrue();
  }

  @Test
  public void shouldCheckSubscriptionTwoProperties() {
    SubscriptionConfiguration subscriptionTwo = subscriptions.get("topic-two");

    assertThat(subscriptionTwo.getAutoOpen()).isNull();
    assertThat(subscriptionTwo.getLockDuration()).isEqualTo(6666);
    assertThat(subscriptionTwo.getVariableNames()).isNull();
    assertThat(subscriptionTwo.getLocalVariables()).isNull();
    assertThat(subscriptionTwo.getBusinessKey()).isNull();
    assertThat(subscriptionTwo.getProcessDefinitionId()).isNull();
    assertThat(subscriptionTwo.getProcessDefinitionIdIn()).isNull();
    assertThat(subscriptionTwo.getProcessDefinitionKey()).isNull();
    assertThat(subscriptionTwo.getProcessDefinitionKeyIn()).isNull();
    assertThat(subscriptionTwo.getProcessDefinitionVersionTag()).isNull();
    assertThat(subscriptionTwo.getProcessVariables()).isEmpty();
    assertThat(subscriptionTwo.getWithoutTenantId()).isNull();
    assertThat(subscriptionTwo.getTenantIdIn()).isNull();
    assertThat(subscriptionTwo.getIncludeExtensionProperties()).isNull();
  }

}
