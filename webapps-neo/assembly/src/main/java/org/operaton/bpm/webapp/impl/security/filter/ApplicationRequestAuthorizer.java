/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements.
 * Modifications Copyright the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.webapp.impl.security.filter;

import java.util.Map;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.webapp.impl.security.auth.Authentication;
import org.operaton.bpm.webapp.impl.security.auth.Authentications;
import org.operaton.bpm.webapp.impl.security.auth.UserAuthentication;
import org.operaton.bpm.webapp.impl.util.ProcessEngineUtil;

/**
 * <p>This matcher can be used for restricting access to an app.</p>
 *
 * @author Daniel Meyer
 * @author nico.rehwaldt
 */
public class ApplicationRequestAuthorizer implements RequestAuthorizer {

  @Override
  public Authorization authorize(Map<String, String> parameters) {
    Authentications authentications = Authentications.getCurrent();

    if (authentications == null) {
      // the user is not authenticated
      // grant user anonymous access
      return grantAnnonymous();
    } else {
      String engineName = parameters.get("engine");
      String appName = parameters.get("app");

      Authentication engineAuth = authentications.getAuthenticationForProcessEngine(engineName);
      if (engineAuth == null) {
        // the user is not authenticated
        // grant user anonymous access
        return grantAnnonymous();
      }

      // get process engine
      ProcessEngine processEngine = ProcessEngineUtil.lookupProcessEngine(engineName);
      if (processEngine == null) {
        // the process engine does not exist
        // grant user anonymous access
        return grantAnnonymous();
      }

      // check authorization
      if (engineAuth instanceof UserAuthentication userAuth) {
        if (userAuth.isAuthorizedForApp(appName)) {
          return Authorization.granted(userAuth).forApplication(appName);
        } else {
          return Authorization.denied(userAuth).forApplication(appName);
        }
      }
    }

    // no auth granted
    return Authorization.denied(Authentication.ANONYMOUS);
  }

  private Authorization grantAnnonymous() {
    return Authorization.granted(Authentication.ANONYMOUS);
  }
}
