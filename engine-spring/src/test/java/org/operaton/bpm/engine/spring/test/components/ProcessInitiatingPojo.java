/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.spring.test.components;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.spring.annotations.BusinessKey;
import org.operaton.bpm.engine.spring.annotations.ProcessVariable;
import org.operaton.bpm.engine.spring.annotations.StartProcess;

import java.util.logging.Logger;

/**
 * simple class that demonstrates the annotations to implicitly handle annotation-driven process management
 *
 * @author Josh Long
 * @since 5.3
 */
@SuppressWarnings("unused")
public class ProcessInitiatingPojo {

    private final Logger log = Logger.getLogger(getClass().getName());

    private int methodState = 0;

    public void reset() {
        this.methodState = 0;
    }

    public void setCustomer(ScopedCustomer customer) {
        this.customer = customer;
    }

    private ScopedCustomer customer;

    public void logScopedCustomer(ProcessInstance processInstance) {
        System.out.println("ProcessInstance ID:" + processInstance.getId() + "; Name: " + this.customer.getName());
    }

    @StartProcess(processKey = "b")
    public void startProcess(@ProcessVariable("customerId") long customerId) {
        log.info("starting 'b' with customerId # " + customerId);
        this.methodState += 1;
        log.info("up'd the method state");
    }

    public int getMethodState() {
        return methodState;
    }

    @StartProcess(processKey = "waiter", returnProcessInstanceId = true)
    public String startProcessA(@ProcessVariable("customerId") long cId) {
        return null;
    }

    @StartProcess(processKey = "waiter")
    public ProcessInstance enrollCustomer(@BusinessKey String key, @ProcessVariable("customerId") long customerId) {
        return null;
    }

    @StartProcess(processKey = "component-waiter")
    public void startScopedProcess(@ProcessVariable("customerId") long customerId) {
        log.info(" start scoped 'component-waiter' process.");
    }


}
