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
package org.operaton.bpm.engine.impl.test;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.impl.cmmn.behavior.CaseControlRuleImpl;
import org.operaton.bpm.engine.impl.el.FixedValue;


/**
 * @author Tom Baeyens
 */
public class PvmTestCase extends TestCase {
  /**
   * This class isn't used in the Process Engine test suite anymore.
   * However, some Test classes in the following modules still use it:
   *   * operaton-engine-plugin-spin
   *   * operaton-engine-plugin-connect
   *   * operaton-engine-spring
   *   * operaton-identity-ldap
   *
   * <p>
   * It should be removed once those Test classes are migrated to JUnit 4.
   * </p>
   */

  /**
   * Asserts if the provided text is part of some text.
   */
  public void assertTextPresent(String expected, String actual) {
    if ( (actual==null)
         || (actual.indexOf(expected)==-1)
       ) {
      throw new AssertionFailedError("expected presence of [%s], but was [%s]".formatted(expected, actual));
    }
  }

  /**
   * Asserts if the provided text is part of some text, ignoring any uppercase characters
   */
  public void assertTextPresentIgnoreCase(String expected, String actual) {
    assertTextPresent(expected.toLowerCase(), actual.toLowerCase());
  }

  public Object defaultManualActivation() {
    Expression expression = new FixedValue(true);
    return new CaseControlRuleImpl(expression);
  }

}
