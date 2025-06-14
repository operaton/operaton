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
package org.operaton.bpm.welcome;

/**
 * The welcome application. Provides access to the welcome core services.
 *
 * @author Daniel Meyer
 *
 */
public class Welcome {

  private Welcome() {
  }

  /**
   * The {@link WelcomeRuntimeDelegate} is an delegate that will be
   * initialized by bootstrapping operaton welcome with an specific
   * instance
   */
  protected static WelcomeRuntimeDelegate welcomeRuntimeDelegate;

  /**
   * Returns an instance of {@link WelcomeRuntimeDelegate}
   *
   * @return
   */
  public static WelcomeRuntimeDelegate getRuntimeDelegate() {
    return welcomeRuntimeDelegate;
  }

  /**
   * A setter to set the {@link WelcomeRuntimeDelegate}.
   * @param cockpitRuntimeDelegate
   */
  public static void setRuntimeDelegate(WelcomeRuntimeDelegate welcomeRuntimeDelegate) {
    Welcome.welcomeRuntimeDelegate = welcomeRuntimeDelegate;
  }
}
