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
package org.operaton.bpm.dmn.engine.test.asserts;

import org.assertj.core.api.AbstractListAssert;
import org.operaton.bpm.dmn.engine.DmnDecisionRuleResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableResultImpl;

import java.util.ArrayList;

public class DmnDecisionTableResultAssert extends AbstractListAssert<DmnDecisionTableResultAssert, DmnDecisionTableResult, DmnDecisionRuleResult, DmnDecisionRuleResultAssert> {

  public DmnDecisionTableResultAssert(DmnDecisionTableResult decisionTableResult) {
    super(decisionTableResult, DmnDecisionTableResultAssert.class);
  }

  public DmnDecisionRuleResultAssert hasSingleResult() {
    hasSize(1);

    DmnDecisionRuleResult singleResult = actual.getSingleResult();

    return new DmnDecisionRuleResultAssert(singleResult);
  }

  @Override
  protected DmnDecisionRuleResultAssert toAssert(DmnDecisionRuleResult value, String description) {
    info.description(description, "");

    return new DmnDecisionRuleResultAssert(value);
  }

  @Override
  protected DmnDecisionTableResultAssert newAbstractIterableAssert(Iterable<? extends DmnDecisionRuleResult> iterable) {
    DmnDecisionTableResult decisionTableResult = new DmnDecisionTableResultImpl(new ArrayList<>());

    iterable.forEach(decisionTableResult::add);

    return new DmnDecisionTableResultAssert(decisionTableResult);  }
}
