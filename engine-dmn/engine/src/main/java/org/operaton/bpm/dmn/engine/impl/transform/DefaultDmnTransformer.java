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
package org.operaton.bpm.dmn.engine.impl.transform;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.dmn.engine.impl.hitpolicy.DefaultHitPolicyHandlerRegistry;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandlerRegistry;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandlerRegistry;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransform;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransformFactory;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransformListener;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnTransformer;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnDataTypeTransformerRegistry;
import org.operaton.bpm.dmn.engine.impl.type.DefaultDataTypeTransformerRegistry;

public class DefaultDmnTransformer implements DmnTransformer {

  protected DmnTransformFactory transformFactory = new DefaultTransformFactory();
  protected List<DmnTransformListener> transformListeners = new ArrayList<DmnTransformListener>();
  protected DmnElementTransformHandlerRegistry elementTransformHandlerRegistry = new DefaultElementTransformHandlerRegistry();
  protected DmnDataTypeTransformerRegistry dataTypeTransformerRegistry = new DefaultDataTypeTransformerRegistry();
  protected DmnHitPolicyHandlerRegistry hitPolicyHandlerRegistry = new DefaultHitPolicyHandlerRegistry();

    /**
  * Returns the DmnTransformFactory used by this class.
  *
  * @return the DmnTransformFactory used by this class
  */
  public DmnTransformFactory getTransformFactory() {
    return transformFactory;
  }

    /**
   * Returns a list of DmnTransformListener objects.
   *
   * @return the list of DmnTransformListener objects
   */
  public List<DmnTransformListener> getTransformListeners() {
    return transformListeners;
  }

    /**
   * Sets the list of transform listeners for the DMN.
   * 
   * @param transformListeners the list of transform listeners to be set
   */
  public void setTransformListeners(List<DmnTransformListener> transformListeners) {
    this.transformListeners = transformListeners;
  }

    /**
   * Sets the list of DmnTransformListeners and returns the DmnTransformer instance.
   *
   * @param transformListeners the list of DmnTransformListeners to set
   * @return the DmnTransformer instance
   */
  public DmnTransformer transformListeners(List<DmnTransformListener> transformListeners) {
    setTransformListeners(transformListeners);
    return this;
  }

    /**
   * Returns the element transform handler registry.
   *
   * @return the element transform handler registry
   */
  public DmnElementTransformHandlerRegistry getElementTransformHandlerRegistry() {
    return elementTransformHandlerRegistry;
  }

    /**
   * Sets the element transform handler registry for the DMN.
   *
   * @param elementTransformHandlerRegistry The handler registry to be set
   */
  public void setElementTransformHandlerRegistry(DmnElementTransformHandlerRegistry elementTransformHandlerRegistry) {
    this.elementTransformHandlerRegistry = elementTransformHandlerRegistry;
  }

    /**
   * Sets the elementTransformHandlerRegistry and returns this DmnTransformer instance.
   * 
   * @param elementTransformHandlerRegistry The DmnElementTransformHandlerRegistry to set
   * @return This DmnTransformer instance
   */
  public DmnTransformer elementTransformHandlerRegistry(DmnElementTransformHandlerRegistry elementTransformHandlerRegistry) {
    setElementTransformHandlerRegistry(elementTransformHandlerRegistry);
    return this;
  }

    /**
   * Returns the DataTypeTransformerRegistry used by this object.
   *
   * @return the DataTypeTransformerRegistry
   */
  public DmnDataTypeTransformerRegistry getDataTypeTransformerRegistry() {
    return dataTypeTransformerRegistry;
  }

    /**
   * Sets the data type transformer registry for the DMN.
   * 
   * @param dataTypeTransformerRegistry the data type transformer registry to be set
   */
  public void setDataTypeTransformerRegistry(DmnDataTypeTransformerRegistry dataTypeTransformerRegistry) {
    this.dataTypeTransformerRegistry = dataTypeTransformerRegistry;
  }

    /**
   * Sets the data type transformer registry and returns the current instance of the DmnTransformer.
   * 
   * @param dataTypeTransformerRegistry the data type transformer registry to set
   * @return the current DmnTransformer instance
   */
  public DmnTransformer dataTypeTransformerRegistry(DmnDataTypeTransformerRegistry dataTypeTransformerRegistry) {
    setDataTypeTransformerRegistry(dataTypeTransformerRegistry);
    return this;
  }

    /**
   * Returns the hit policy handler registry.
   *
   * @return the hit policy handler registry
   */
  public DmnHitPolicyHandlerRegistry getHitPolicyHandlerRegistry() {
    return hitPolicyHandlerRegistry;
  }

    /**
   * Sets the hit policy handler registry with the specified value.
   * 
   * @param hitPolicyHandlerRegistry the new hit policy handler registry
   */
  public void setHitPolicyHandlerRegistry(DmnHitPolicyHandlerRegistry hitPolicyHandlerRegistry) {
    this.hitPolicyHandlerRegistry = hitPolicyHandlerRegistry;
  }

    /**
   * Sets the provided DmnHitPolicyHandlerRegistry and returns the current DmnTransformer instance.
   * 
   * @param hitPolicyHandlerRegistry the DmnHitPolicyHandlerRegistry to be set
   * @return the current DmnTransformer instance
   */
  public DmnTransformer hitPolicyHandlerRegistry(DmnHitPolicyHandlerRegistry hitPolicyHandlerRegistry) {
    setHitPolicyHandlerRegistry(hitPolicyHandlerRegistry);
    return this;
  }

    /**
   * Creates and returns a new DmnTransform object using the provided transformFactory.
   * 
   * @return the newly created DmnTransform object
   */
  public DmnTransform createTransform() {
    return transformFactory.createTransform(this);
  }

}
