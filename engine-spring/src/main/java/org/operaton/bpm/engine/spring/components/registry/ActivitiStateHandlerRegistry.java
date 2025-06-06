/*
 * Copyright 2010 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.operaton.bpm.engine.spring.components.registry;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.bpmn.behavior.ReceiveTaskActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


/**
 * this class records and manages all known {@link org.operaton.bpm.engine.annotations.State} - responding
 * beans in the JVM. It <em>should</em> have metadata on all methods, and what
 * those methods expect from a given invocation (ie: which process, which process variables).
 *
 * @author Josh Long
 * @since 1.0
 */
public class ActivitiStateHandlerRegistry extends ReceiveTaskActivityBehavior implements BeanFactoryAware, BeanNameAware, ActivityBehavior, InitializingBean {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private volatile ConcurrentHashMap<String, ActivitiStateHandlerRegistration> registrations = new ConcurrentHashMap<>();

    private ProcessEngine processEngine;

    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public void execute(ActivityExecution execution) throws Exception {
        // nothing to do here
    }

    @Override
    public void signal(ActivityExecution execution, String signalName, Object data) throws Exception {
        leave(execution);
    }

    protected String registrationKey(String stateName, String processName) {
        return (org.operaton.commons.utils.StringUtil.defaultString(processName) +
                ":" + org.operaton.commons.utils.StringUtil.defaultString(stateName)).toLowerCase();
    }

    /**
     * used at runtime to register state handlers as they are registered with the spring context
     *
     * @param registration the {@link org.operaton.bpm.engine.spring.components.registry.ActivitiStateHandlerRegistration}
     */
    public void registerActivitiStateHandler(
            ActivitiStateHandlerRegistration registration) {
        String regKey = registrationKey(registration.getProcessName(),
                registration.getStateName());
        this.registrations.put(regKey, registration);
    }

    /**
     * this is responsible for looking up components in the registry and returning the appropriate handler based
     * on specificity of the {@link org.operaton.bpm.engine.spring.components.registry.ActivitiStateHandlerRegistration}
     *
     * @param processName the process name to look for (optional)
     * @param stateName   the state name to look for (not optional)
     * @return all matching options
     */
    public Collection<ActivitiStateHandlerRegistration> findRegistrationsForProcessAndState(
      String processName, String stateName) {
        Collection<ActivitiStateHandlerRegistration> registrationCollection = new ArrayList<>();
        String regKeyFull = registrationKey(processName, stateName);
        String regKeyWithJustState = registrationKey(null, stateName);

        for (var reg: this.registrations.entrySet()) {
            String k = reg.getKey();
            if (k.contains(regKeyFull)) {
                registrationCollection.add(reg.getValue());
            }
        }

        if (registrationCollection.isEmpty()) {
            for (var reg: this.registrations.entrySet()) {
                String k = reg.getKey();
                if (k.contains(regKeyWithJustState)) {
                    registrationCollection.add(reg.getValue());
                }
            }
        }

        return registrationCollection;
    }

    /**
     * this scours the registry looking for candidate registrations that match a given process name and/ or state name
     *
     * @param processName the name of the process
     * @param stateName   the name of the state
     * @return an unambiguous {@link org.operaton.bpm.engine.spring.components.registry.ActivitiStateHandlerRegistry} or null
     */
    public ActivitiStateHandlerRegistration findRegistrationForProcessAndState(String processName, String stateName) {

        ActivitiStateHandlerRegistration r = null;

        String key = registrationKey(processName, stateName);

        Collection<ActivitiStateHandlerRegistration> rs = this.findRegistrationsForProcessAndState(
                processName, stateName);

        for (ActivitiStateHandlerRegistration sr : rs) {
            String kName = registrationKey(sr.getProcessName(), sr.getStateName());
            if (key.equalsIgnoreCase(kName)) {
                r = sr;
                break;
            }
        }

        for (ActivitiStateHandlerRegistration sr : rs) {
            String kName = registrationKey(null, sr.getStateName());
            if (key.equalsIgnoreCase(kName)) {
                r = sr;
                break;
            }
        }

        if ((r == null) && (!rs.isEmpty())) {
            r = rs.iterator().next();
        }

        return r;
    }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    // no-op
  }

  @Override
  public void setBeanName(String name) {
      // no-op
  }

  @Override
  public void afterPropertiesSet() {
        Assert.notNull(this.processEngine, "the 'processEngine' can't be null");
        logger.info("this bean contains a processEngine reference. " + this.processEngine);
        logger.info("starting " + getClass().getName());
    }
}
