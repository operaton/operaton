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
package org.operaton.spin.impl.util;

import org.operaton.spin.impl.logging.SpinLogger;
import org.operaton.spin.spi.DataFormat;

/**
 * @author Daniel Meyer
 *
 */
public final class SpinReflectUtil {

  private static final SpinLogger LOG = SpinLogger.CORE_LOGGER;

  private SpinReflectUtil() {
  }

  /**
   * Used by dataformats if they need to load a class
   *
   * @param classname the name of the
   * @param dataFormat
   * @return
   */
  public static Class<?> loadClass(String classname, DataFormat<?> dataFormat) {

    // first try context classoader
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if(cl != null) {
      LOG.tryLoadingClass(classname, cl);
      try {
        return cl.loadClass(classname);
      }
      catch(Exception e) {
        // ignore
      }
    }

    // else try the classloader which loaded the dataformat
    cl = dataFormat.getClass().getClassLoader();
    try {
      LOG.tryLoadingClass(classname, cl);
      return cl.loadClass(classname);
    }
    catch (ClassNotFoundException e) {
      throw LOG.classNotFound(classname, e);
    }

  }
}
