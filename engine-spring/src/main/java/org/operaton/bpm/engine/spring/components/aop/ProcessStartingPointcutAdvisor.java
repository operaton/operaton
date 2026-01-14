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
package org.operaton.bpm.engine.spring.components.aop;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.spring.annotations.StartProcess;
import org.operaton.bpm.engine.spring.components.aop.util.MetaAnnotationMatchingPointcut;

/**
 * AOP advice for methods annotated with (by default) {@link StartProcess}.
 * <p>
 * Advised methods start a process after the method executes.
 * <p>
 * Advised methods can declare a return
 * type of {@link org.operaton.bpm.engine.runtime.ProcessInstance} and then subsequently
 * return null. The real return ProcessInstance value will be given by the aspect.
 *
 * @author Josh Long
 * @since 5.3
 */
public class ProcessStartingPointcutAdvisor implements PointcutAdvisor {


    /**
     * annotations that shall be scanned
     */
    private final Set<Class<? extends Annotation>> annotations = new HashSet<>(Arrays.asList(StartProcess.class));

    /**
     * the {@link org.aopalliance.intercept.MethodInterceptor} that handles launching the business process.
     */
    protected MethodInterceptor advice;

    /**
     * matches any method containing the {@link StartProcess} annotation.
     */
    protected Pointcut pointcut;

    /**
     * the injected reference to the {@link org.operaton.bpm.engine.ProcessEngine}
     */
    protected ProcessEngine processEngine;

    public ProcessStartingPointcutAdvisor(ProcessEngine pe) {
        this.processEngine = pe;
        this.pointcut = buildPointcut();
        this.advice = buildAdvise();

    }

    protected MethodInterceptor buildAdvise() {
        return new ProcessStartingMethodInterceptor(this.processEngine);
    }

  @Override
  public Pointcut getPointcut() {
        return pointcut;
    }

  @Override
  public Advice getAdvice() {
        return advice;
    }

  @Override
  public boolean isPerInstance() {
        return true;
    }

    private Pointcut buildPointcut() {
        ComposablePointcut result = null;
        for (Class<? extends Annotation> publisherAnnotationType : this.annotations) {
            Pointcut mpc = new MetaAnnotationMatchingPointcut(null, publisherAnnotationType);
            if (result == null) {
                result = new ComposablePointcut(mpc);
            } else {
                result.union(mpc);
            }
        }
        return result;
    }


}

