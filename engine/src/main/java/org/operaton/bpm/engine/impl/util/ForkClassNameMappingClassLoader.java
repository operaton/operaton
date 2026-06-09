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
package org.operaton.bpm.engine.impl.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

public class ForkClassNameMappingClassLoader extends ClassLoader {

  private static final String CLASS_RESOURCE_SUFFIX = ".class";

  public ForkClassNameMappingClassLoader(ClassLoader parent) {
    super(parent);
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    try {
      return super.loadClass(name);
    } catch (ClassNotFoundException e) {
      String mappedClassName = ClassNameUtil.mapKnownForkClassNameToOperaton(name);
      if (mappedClassName.equals(name)) {
        throw e;
      }

      try {
        return super.loadClass(mappedClassName);
      } catch (ClassNotFoundException mappedException) {
        mappedException.addSuppressed(e);
        throw mappedException;
      }
    }
  }

  @Override
  public URL getResource(String name) {
    URL resource = super.getResource(name);
    if (resource != null) {
      return resource;
    }

    String mappedResourceName = mapKnownForkResourceNameToOperaton(name);
    if (mappedResourceName.equals(name)) {
      return null;
    }
    return super.getResource(mappedResourceName);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    Enumeration<URL> resources = super.getResources(name);
    if (resources.hasMoreElements()) {
      return resources;
    }

    String mappedResourceName = mapKnownForkResourceNameToOperaton(name);
    if (mappedResourceName.equals(name)) {
      return resources;
    }
    return super.getResources(mappedResourceName);
  }

  @Override
  public InputStream getResourceAsStream(String name) {
    InputStream resource = super.getResourceAsStream(name);
    if (resource != null) {
      return resource;
    }

    String mappedResourceName = mapKnownForkResourceNameToOperaton(name);
    if (mappedResourceName.equals(name)) {
      return null;
    }
    return super.getResourceAsStream(mappedResourceName);
  }

  protected String mapKnownForkResourceNameToOperaton(String resourceName) {
    if (resourceName == null || !resourceName.endsWith(CLASS_RESOURCE_SUFFIX)) {
      return resourceName;
    }

    String className = resourceName
        .substring(0, resourceName.length() - CLASS_RESOURCE_SUFFIX.length())
        .replace('/', '.');
    String mappedClassName = ClassNameUtil.mapKnownForkClassNameToOperaton(className);
    if (mappedClassName.equals(className)) {
      return resourceName;
    }
    return mappedClassName.replace('.', '/') + CLASS_RESOURCE_SUFFIX;
  }
}
