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
package org.operaton.bpm.qa.rolling.update;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.qa.upgrade.UpgradeTestExtension;

/**
 * The abstract rolling update test case, which should be used as base class from all
 * rolling update test cases. Provides access to the shared {@link UpgradeTestExtension}
 * and exposes the engine tags that tests should exercise.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public abstract class AbstractRollingUpdateTestCase {

  @RegisterExtension
  public static UpgradeTestExtension rule = new UpgradeTestExtension();

  private static String currentTag;

  static Stream<String> engineTags() {
    return Stream.of(RollingUpdateConstants.OLD_ENGINE_TAG, RollingUpdateConstants.NEW_ENGINE_TAG);
  }

  void setEngineTag(String tag) {
    currentTag = tag;
    rule.setTag(tag);
  }

  protected String getEngineTag() {
    return currentTag;
  }
}
