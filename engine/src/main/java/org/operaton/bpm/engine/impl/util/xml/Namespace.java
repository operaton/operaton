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
package org.operaton.bpm.engine.impl.util.xml;

import org.jspecify.annotations.Nullable;

/**
 * @author Ronny Bräunlich
 *
 */
// TODO Replace by record
public class Namespace {

  private final @Nullable String namespaceUri;
  private final @Nullable String alternativeUri;

  public Namespace(@Nullable String namespaceUri) {
    this(namespaceUri, null);
  }

  /**
   * Creates a namespace with an alternative uri.
   */
  public Namespace(@Nullable String namespaceUri, @Nullable String alternativeUri) {
    this.namespaceUri = namespaceUri;
    this.alternativeUri = alternativeUri;
  }

  /**
   * If a namespace has changed over time it could feel responsible for handling
   * the older one.
   */
  public boolean hasAlternativeUri() {
    return alternativeUri != null;
  }

  public @Nullable String getNamespaceUri() {
    return namespaceUri;
  }

  public @Nullable String getAlternativeUri() {
    return alternativeUri;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + (namespaceUri == null ? 0 : namespaceUri.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Namespace other = (Namespace) obj;
    if (namespaceUri == null) {
      if (other.namespaceUri != null) {
        return false;
      }
    } else if (!namespaceUri.equals(other.namespaceUri)) {
      return false;
    }
    return true;
  }

}
