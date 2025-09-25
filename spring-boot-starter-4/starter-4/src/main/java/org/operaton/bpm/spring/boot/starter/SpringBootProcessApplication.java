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
package org.operaton.bpm.spring.boot.starter;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;

import org.operaton.bpm.application.PostDeploy;
import org.operaton.bpm.application.PreUndeploy;
import org.operaton.bpm.container.RuntimeContainerDelegate;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.spring.application.SpringProcessApplication;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonDeploymentConfiguration;
import org.operaton.bpm.spring.boot.starter.event.PostDeployEvent;
import org.operaton.bpm.spring.boot.starter.event.PreUndeployEvent;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

import static org.operaton.bpm.application.ProcessApplicationInfo.PROP_SERVLET_CONTEXT_PATH;
import static org.operaton.bpm.spring.boot.starter.util.GetProcessApplicationNameFromAnnotation.processApplicationNameFromAnnotation;
import static org.operaton.bpm.spring.boot.starter.util.SpringBootProcessEngineLogger.LOG;

@Configuration
public class SpringBootProcessApplication extends SpringProcessApplication {

  @Bean
  public static OperatonDeploymentConfiguration deploymentConfiguration() {
    return new OperatonDeploymentConfiguration() {
      @Override
      public Set<Resource> getDeploymentResources() {
        return Collections.emptySet();
      }

      @Override
      public void preInit(ProcessEngineConfigurationImpl configuration) {
        LOG.skipAutoDeployment();
      }

      @Override
      public String toString() {
        return "disableDeploymentResourcePattern";
      }
    };
  }

  @Value("${spring.application.name:null}")
  protected Optional<String> springApplicationName;

  protected String contextPath = "/";

  protected OperatonBpmProperties operatonBpmProperties;

  protected ProcessEngine processEngine;

  protected ApplicationEventPublisher eventPublisher;

  @Autowired
  public SpringBootProcessApplication(
    OperatonBpmProperties operatonBpmProperties,
    ProcessEngine processEngine,
    ApplicationEventPublisher eventPublisher){
    this.operatonBpmProperties = operatonBpmProperties;
    this.processEngine = processEngine;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void afterPropertiesSet() {
    processApplicationNameFromAnnotation(applicationContext)
      .apply(springApplicationName)
      .ifPresent(this::setBeanName);

    if (Boolean.TRUE.equals(operatonBpmProperties.getGenerateUniqueProcessApplicationName())) {
      setBeanName(OperatonBpmProperties.getUniqueName(OperatonBpmProperties.UNIQUE_APPLICATION_NAME_PREFIX));
    }

    String processEngineName = processEngine.getName();
    setDefaultDeployToEngineName(processEngineName);

    RuntimeContainerDelegate.INSTANCE.get().registerProcessEngine(processEngine);

    properties.put(PROP_SERVLET_CONTEXT_PATH, contextPath);
    super.afterPropertiesSet();
  }

  @Override
  public void destroy() {
    super.destroy();
    RuntimeContainerDelegate.INSTANCE.get().unregisterProcessEngine(processEngine);
  }

  @PostDeploy
  public void onPostDeploy(ProcessEngine processEngine) {
    eventPublisher.publishEvent(new PostDeployEvent(processEngine));
  }

  @PreUndeploy
  public void onPreUndeploy(ProcessEngine processEngine) {
    eventPublisher.publishEvent(new PreUndeployEvent(processEngine));
  }

  @ConditionalOnWebApplication
  @Configuration
  class WebApplicationConfiguration implements ServletContextAware {

    @Override
    public void setServletContext(ServletContext servletContext) {
      if (StringUtils.hasText(servletContext.getContextPath())) {
        contextPath = servletContext.getContextPath();
      }
    }
  }
}
