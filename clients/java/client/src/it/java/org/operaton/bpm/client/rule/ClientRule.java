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
package org.operaton.bpm.client.rule;

import java.util.Properties;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.operaton.bpm.client.ExternalTaskClient;
import org.operaton.bpm.client.ExternalTaskClientBuilder;
import org.operaton.bpm.client.util.PropertyUtil;

import static org.operaton.bpm.client.util.PropertyUtil.DEFAULT_PROPERTIES_PATH;
import static org.operaton.bpm.client.util.PropertyUtil.OPERATON_ENGINE_NAME;
import static org.operaton.bpm.client.util.PropertyUtil.OPERATON_ENGINE_REST;
import static org.operaton.bpm.client.util.TestUtil.waitUntil;

public class ClientRule implements BeforeEachCallback, AfterEachCallback {

  public static final long LOCK_DURATION = 1000 * 60 * 5;

  protected ExternalTaskClientBuilder builder;
  protected ExternalTaskClient client;

  public ClientRule() {
    this(PropertyUtil.loadProperties(DEFAULT_PROPERTIES_PATH));
  }

  public ClientRule(Properties properties) {
    this(() -> {
      String endpoint = properties.getProperty(OPERATON_ENGINE_REST);
      String engine = properties.getProperty(OPERATON_ENGINE_NAME);
      return ExternalTaskClient.create()
              .baseUrl(endpoint + engine)
              .lockDuration(LOCK_DURATION);
    });
  }

  public ClientRule(Supplier<ExternalTaskClientBuilder> builderSupplier) {
    this.builder = builderSupplier.get();
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    before();
  }

  public void before() {
    builder.disableAutoFetching();
    client = builder.build();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    after();
  }

  public void after() {
    client.stop();
    client = null;
  }

  public ExternalTaskClient client() {
    return client;
  }

  public void waitForFetchAndLockUntil(BooleanSupplier condition) {
    client.start();

    try {
      waitUntil(condition);
    }
    finally {
      client.stop();
    }
  }
}
