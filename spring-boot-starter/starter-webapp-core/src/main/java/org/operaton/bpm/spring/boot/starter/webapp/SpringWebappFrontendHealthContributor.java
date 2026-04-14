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
package org.operaton.bpm.spring.boot.starter.webapp;

import org.operaton.bpm.engine.health.FrontendHealthContributor;
import org.operaton.bpm.spring.boot.starter.property.WebappProperty;
import org.springframework.core.io.ResourceLoader;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Webapps contributor that reports whether the packaged webapp resources are available.
 *
 * @author <a href="mailto:tomnm77@gmail.com">Tomasz Korcz</a>
 * @since 2.1
 */
class SpringWebappFrontendHealthContributor implements FrontendHealthContributor {

  private final ResourceLoader resourceLoader;
  private final WebappProperty webappProperty;

  SpringWebappFrontendHealthContributor(ResourceLoader resourceLoader, WebappProperty webappProperty) {
    this.resourceLoader = resourceLoader;
    this.webappProperty = webappProperty;
  }

  @Override
  public Map<String, Object> frontendDetails() {
    String webjarClasspath = webappProperty != null
            ? webappProperty.getWebjarClasspath()
            : "/META-INF/resources/webjars/operaton";
    String applicationPath = webappProperty != null
            ? webappProperty.getApplicationPath()
            : "/operaton";

    String basePath = webjarClasspath.endsWith("/") ? webjarClasspath : webjarClasspath + "/";

    boolean cockpit = isAppPresent(basePath, "cockpit");
    boolean tasklist = isAppPresent(basePath, "tasklist");
    boolean admin = isAppPresent(basePath, "admin");

    boolean operational = cockpit && tasklist && admin;

    Map<String, Object> apps = new LinkedHashMap<>();
    apps.put("cockpit", cockpit);
    apps.put("tasklist", tasklist);
    apps.put("admin", admin);

    Map<String, Object> details = new LinkedHashMap<>();
    details.put("operational", operational);
    details.put("path", applicationPath);
    details.put("apps", apps);

    return details;
  }

  private boolean isAppPresent(String basePath, String appName) {
    String path = "classpath:" + basePath + "app/" + appName + "/index.html";
    return resourceLoader.getResource(path).exists();
  }
}