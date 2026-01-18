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
package org.operaton.bpm.engine.impl;

/**
 * Constants for resource file suffixes.
 * @since 1.1
 */
@SuppressWarnings("java:S2386")
public final class ResourceSuffixes {
  private ResourceSuffixes() {
    // prevent instantiation
  }

  public static final String[] BPMN_RESOURCE_SUFFIXES = new String[] { "bpmn20.xml", "bpmn" };
  public static final String[] CMMN_RESOURCE_SUFFIXES = new String[] { "cmmn11.xml", "cmmn10.xml", "cmmn" };
  public static final String[] DMN_RESOURCE_SUFFIXES = new String[] { "dmn11.xml", "dmn" };
  public static final String[] DIAGRAM_RESOURCE_SUFFIXES = new String[] { "png", "jpg", "gif", "svg" };
}
