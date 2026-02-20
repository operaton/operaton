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
package org.operaton.bpm.quarkus.engine.extension.deployment.impl;

import jakarta.enterprise.context.Dependent;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.jboss.jandex.DotName;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.ProcessVariables;
import org.operaton.bpm.engine.cdi.annotation.BusinessProcessScoped;
import org.operaton.bpm.engine.cdi.impl.ProcessVariableLocalMap;
import org.operaton.bpm.engine.cdi.impl.ProcessVariableMap;
import org.operaton.bpm.engine.cdi.impl.context.DefaultContextAssociationManager;
import org.operaton.bpm.engine.cdi.impl.context.RequestScopedAssociation;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.quarkus.engine.extension.QuarkusProcessEngineConfiguration;
import org.operaton.bpm.quarkus.engine.extension.impl.InjectableBusinessProcessContext;
import org.operaton.bpm.quarkus.engine.extension.impl.OperatonEngineRecorder;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

public class OperatonEngineProcessor {

  protected static final String FEATURE_NAME = "operaton-platform-engine";

  @BuildStep
  protected FeatureBuildItem feature() {
    return new FeatureBuildItem(FEATURE_NAME);
  }

  @BuildStep
  protected void unremovableBeans(BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer) {
    unremovableBeansProducer.produce(UnremovableBeanBuildItem.beanTypes(
        RequestScopedAssociation.class,
        QuarkusProcessEngineConfiguration.class
    ));
  }

  @BuildStep
  protected ContextConfiguratorBuildItem registerBusinessProcessScoped(ContextRegistrationPhaseBuildItem phase) {
    return new ContextConfiguratorBuildItem(phase.getContext()
        .configure(BusinessProcessScoped.class)
        .normal()
        .contextClass(InjectableBusinessProcessContext.class));
  }

  @BuildStep
  protected void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeansProducer) {
    additionalBeansProducer.produce(
        AdditionalBeanBuildItem.builder()
            .setDefaultScope(DotName.createSimple(Dependent.class.getName()))
            .addBeanClasses(
                DefaultContextAssociationManager.class,
                BusinessProcess.class,
                ProcessVariableLocalMap.class,
                ProcessVariables.class,
                ProcessVariableMap.class
            ).build());
  }

  @BuildStep
  @Record(RUNTIME_INIT)
  protected void cdiConfig(OperatonEngineRecorder recorder, BeanContainerBuildItem beanContainer) {
    recorder.configureProcessEngineCdiBeans(beanContainer.getValue());
  }

  @Consume(AdditionalBeanBuildItem.class)
  @BuildStep
  @Record(RUNTIME_INIT)
  protected void processEngineConfiguration(OperatonEngineRecorder recorder,
                                            BeanContainerBuildItem beanContainerBuildItem,
                                            BuildProducer<ProcessEngineConfigurationBuildItem> configurationProducer) {

    BeanContainer beanContainer = beanContainerBuildItem.getValue();
    recorder.configureProcessEngineCdiBeans(beanContainer);
    RuntimeValue<ProcessEngineConfigurationImpl> processEngineConfiguration =
        recorder.createProcessEngineConfiguration(beanContainer);
    configurationProducer.produce(new ProcessEngineConfigurationBuildItem(processEngineConfiguration));
  }

  @BuildStep
  @Record(RUNTIME_INIT)
  protected void processEngine(OperatonEngineRecorder recorder,
                               ProcessEngineConfigurationBuildItem processEngineConfigurationBuildItem,
                               BuildProducer<ProcessEngineBuildItem> processEngineProducer) {

    RuntimeValue<ProcessEngineConfigurationImpl> processEngineConfiguration =
        processEngineConfigurationBuildItem.getProcessEngineConfiguration();
    processEngineProducer.produce(new ProcessEngineBuildItem(
        recorder.createProcessEngine(processEngineConfiguration)));
  }

  @Consume(ProcessEngineBuildItem.class)
  @BuildStep
  @Record(RUNTIME_INIT)
  protected void deployProcessEngineResources(OperatonEngineRecorder recorder) {
    recorder.fireOperatonEngineStartEvent();
  }

  @BuildStep
  @Record(RUNTIME_INIT)
  protected void shutdown(OperatonEngineRecorder recorder,
                ProcessEngineBuildItem processEngine,
                ShutdownContextBuildItem shutdownContext) {

    recorder.registerShutdownTask(shutdownContext, processEngine.getProcessEngine());
  }
}
