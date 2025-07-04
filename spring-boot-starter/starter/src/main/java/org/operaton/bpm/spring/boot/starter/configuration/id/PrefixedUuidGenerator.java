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
package org.operaton.bpm.spring.boot.starter.configuration.id;

import org.apache.commons.lang3.StringUtils;
import org.operaton.bpm.engine.impl.cfg.IdGenerator;
import org.operaton.bpm.engine.impl.persistence.StrongUuidGenerator;

import static java.util.Objects.requireNonNull;

public class PrefixedUuidGenerator implements IdGenerator {

  private final StrongUuidGenerator strongUuidGenerator = new StrongUuidGenerator();

  private final String prefix;

  public PrefixedUuidGenerator(final String applicationName) {
    this.prefix = requireNonNull(StringUtils.trimToNull(applicationName), "prefix must not be null or blank! set the spring.application.name property!");
  }

  @Override
  public String getNextId() {
    return String.join("-", prefix, strongUuidGenerator.getNextId());
  }
}
