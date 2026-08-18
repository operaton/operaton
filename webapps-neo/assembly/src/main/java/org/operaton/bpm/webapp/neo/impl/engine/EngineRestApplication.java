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
package org.operaton.bpm.webapp.neo.impl.engine;

import java.util.HashSet;
import java.util.Set;
import jakarta.ws.rs.core.Application;

import org.operaton.bpm.engine.rest.impl.NamedProcessEngineRestServiceImpl;
import org.operaton.bpm.engine.rest.impl.OperatonRestResources;

/**
 * The engine REST API, served from inside the webapp so that it shares the webapp's
 * session and filter chain.
 *
 * <p>The SPA previously talked to the standalone {@code /engine-rest} deployment,
 * which authenticates each request on its own and therefore forced the client to
 * keep credentials around to re-send. Mounted here at
 * {@code {applicationPath}/api/engine/*} the same requests are authenticated by the
 * webapp session instead, and pass through the CSRF and header-security filters on
 * the way.</p>
 *
 * <p>Registers the <em>named</em> engine service, so resources live under
 * {@code /api/engine/engine/{engine}/...}. That is the layout
 * {@code securityFilterRules.json} is written against — it denies
 * {@code /api/engine/.*} wholesale and re-allows only
 * {@code /api/engine/engine/{engine}/.*} through
 * {@code EngineRequestAuthorizer} — so the alternative, mounting the default
 * engine service at the root of this application, would be denied by the rules
 * that protect it.</p>
 */
public class EngineRestApplication extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<>();

    // only provide named process engine access.
    classes.add(NamedProcessEngineRestServiceImpl.class);
    classes.addAll(OperatonRestResources.getConfigurationClasses());

    return classes;
  }
}
