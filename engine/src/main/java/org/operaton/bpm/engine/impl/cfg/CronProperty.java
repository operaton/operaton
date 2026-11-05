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
package org.operaton.bpm.engine.impl.cfg;

import java.util.Locale;

import com.cronutils.model.CronType;

/**
 * Configuration properties for cron expression handling in the process engine.
 */
public class CronProperty {

  public static final String DEFAULT_TYPE = CronType.SPRING53.name();
  public static final boolean DEFAULT_SUPPORT_LEGACY_QUARTZ_SYNTAX = true;

  private String type = DEFAULT_TYPE;
  private boolean supportLegacyQuartzSyntax = DEFAULT_SUPPORT_LEGACY_QUARTZ_SYNTAX;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    if (type == null || type.trim().isEmpty()) {
      return;
    }

    String normalizedType = type.trim().toUpperCase(Locale.ROOT);
    try {
      CronType.valueOf(normalizedType);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid cronType: " + type + ". Valid values are: SPRING53, QUARTZ",
          e);
    }
    this.type = normalizedType;
  }

  public boolean isSupportLegacyQuartzSyntax() {
    return supportLegacyQuartzSyntax;
  }

  public void setSupportLegacyQuartzSyntax(boolean supportLegacyQuartzSyntax) {
    this.supportLegacyQuartzSyntax = supportLegacyQuartzSyntax;
  }

  @Override
  public String toString() {
    return "CronProperty [type=" + type + ", supportLegacyQuartzSyntax=" + supportLegacyQuartzSyntax + "]";
  }
}
