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
package org.operaton.bpm.model.bpmn.impl.instance.operaton;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.operaton.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.operaton.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonList;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.UnsupportedModelOperationException;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.impl.util.ModelUtil;
import org.operaton.bpm.model.xml.instance.DomElement;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_ELEMENT_LIST;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.OPERATON_NS;

/**
 * @author Sebastian Menski
 */
public class OperatonListImpl extends BpmnModelElementInstanceImpl implements OperatonList {

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OperatonList.class, OPERATON_ELEMENT_LIST)
      .namespaceUri(OPERATON_NS)
      .instanceProvider(OperatonListImpl::new);

    typeBuilder.build();
  }

  public OperatonListImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends BpmnModelElementInstance> Collection<T> getValues() {

    return new Collection<T>() {

      protected Collection<T> getElements() {
        return ModelUtil.getModelElementCollection(getDomElement().getChildElements(), getModelInstance());
      }

      @Override
      public int size() {
        return getElements().size();
      }

      @Override
      public boolean isEmpty() {
        return getElements().isEmpty();
      }

      @Override
      public boolean contains(Object o) {
        return getElements().contains(o);
      }

      @Override
      public Iterator<T> iterator() {
        return getElements().iterator();
      }

      @Override
      public Object[] toArray() {
        return getElements().toArray();
      }

      public <T1> T1[] toArray(T1[] a) {
        return getElements().toArray(a);
      }

      @Override
      public boolean add(T t) {
        getDomElement().appendChild(t.getDomElement());
        return true;
      }

      @Override
      public boolean remove(Object o) {
        ModelUtil.ensureInstanceOf(o, BpmnModelElementInstance.class);
        return getDomElement().removeChild(((BpmnModelElementInstance) o).getDomElement());
      }

      @Override
      public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
          if (!contains(o)) {
            return false;
          }
        }
        return true;
      }

      public boolean addAll(Collection<? extends T> c) {
        for (T element : c) {
          add(element);
        }
        return true;
      }

      @Override
      public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
          result |= remove(o);
        }
        return result;
      }

      @Override
      public boolean retainAll(Collection<?> c) {
        throw new UnsupportedModelOperationException("retainAll()", "not implemented");
      }

      @Override
      public void clear() {
        DomElement domElement = getDomElement();
        List<DomElement> childElements = domElement.getChildElements();
        for (DomElement childElement : childElements) {
          domElement.removeChild(childElement);
        }
      }
    };
  }

}
