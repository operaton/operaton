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
package org.operaton.bpm.engine.cdi.impl.el;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.enterprise.inject.spi.BeanManager;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.cdi.impl.util.BeanManagerLookup;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;


/**
 * Resolver wrapping an instance of jakarta.el.ELResolver obtained from the
 * {@link BeanManager}. Allows the process engine to resolve Cdi-Beans.
 *
 * @author Daniel Meyer
 */
public class CdiResolver extends ELResolver {

  private static final Logger LOG = Logger.getLogger(CdiResolver.class.getName());

  protected BeanManager getBeanManager() {
    try {
      return BeanManagerLookup.getBeanManager();
    } catch (ProcessEngineException e) {
      LOG.log(Level.FINE, "BeanManager not available, CDI resolution will be skipped", e);
      return null;
    }
  }

  protected ELResolver getWrappedResolver() {
    BeanManager beanManager = getBeanManager();
    if (beanManager == null) {
      return null;
    }
    return beanManager.getELResolver();
  }

  @Override
  public Class< ? > getCommonPropertyType(ELContext context, Object base) {
    ELResolver resolver = getWrappedResolver();
    return resolver != null ? resolver.getCommonPropertyType(context, base) : null;
  }

  @Override
  public Class< ? > getType(ELContext context, Object base, Object property) {
    ELResolver resolver = getWrappedResolver();
    return resolver != null ? resolver.getType(context, base, property) : null;
  }

  @Override
  public Object getValue(ELContext context, Object base, Object property) {
    //we need to resolve a bean only for the first "member" of expression, e.g. bean.property1.property2
    if (base == null) {
      BeanManager beanManager = getBeanManager();
      if (beanManager == null) {
        return null;
      }
      Object result = ProgrammaticBeanLookup.lookup(property.toString(), beanManager);
      if (result != null) {
        context.setPropertyResolved(true);
      }
      return result;
    } else {
      return null;
    }
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property) {
    ELResolver resolver = getWrappedResolver();
    return resolver != null ? resolver.isReadOnly(context, base, property) : true;
  }

  @Override
  public void setValue(ELContext context, Object base, Object property, Object value) {
    ELResolver resolver = getWrappedResolver();
    if (resolver != null) {
      resolver.setValue(context, base, property, value);
    }
  }

  @Override
  public Object invoke(ELContext context, Object base, Object method, java.lang.Class< ? >[] paramTypes, Object[] params) {
    ELResolver resolver = getWrappedResolver();
    return resolver != null ? resolver.invoke(context, base, method, paramTypes, params) : null;
  }

}
