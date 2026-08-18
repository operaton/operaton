/*
 * Copyright 2026 the Operaton contributors.
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
package org.operaton.bpm.webapp.neo.impl.web;

import java.util.HashSet;
import java.util.Set;
import jakarta.ws.rs.core.Application;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.operaton.bpm.engine.rest.exception.ExceptionHandler;
import org.operaton.bpm.engine.rest.exception.RestExceptionHandler;
import org.operaton.bpm.engine.rest.mapper.JacksonConfigurator;
import org.operaton.bpm.webapp.neo.impl.security.auth.UserAuthenticationResource;

/**
 * The administrative API the SPA needs in order to authenticate.
 *
 * <p>Mounted at {@code {applicationPath}/api/admin/*}, which puts
 * {@link UserAuthenticationResource} at
 * {@code {applicationPath}/api/admin/auth/user/{engine}/login/{app}} and
 * {@code .../logout} — the same shape the legacy webapp exposes, so the login and
 * logout calls mean the same thing in both.</p>
 *
 * <p>Much narrower than the legacy admin application on purpose: webapps-neo has no
 * admin plugin SPI and no setup resource, so authentication is all that lives here.
 * Everything else the SPA needs comes from {@link EngineRestApplication}.</p>
 */
public class AdminApplication extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<>();

    classes.add(JacksonConfigurator.class);
    classes.add(JacksonJsonProvider.class);
    classes.add(RestExceptionHandler.class);
    classes.add(ExceptionHandler.class);

    classes.add(UserAuthenticationResource.class);

    return classes;
  }
}
