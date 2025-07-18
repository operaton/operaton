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
package org.operaton.bpm.engine.rest.hal.cache;

import java.util.Collections;
import java.util.Comparator;

public class HalResourceCacheEntryComparator implements Comparator<HalResourceCacheEntry> {

  public static final Comparator<HalResourceCacheEntry> INSTANCE = new HalResourceCacheEntryComparator();
  public static final Comparator<HalResourceCacheEntry> REVERSE = Collections.reverseOrder(INSTANCE);

  /** Sort cache entries by ascending create time (oldest first) */
  public static Comparator<HalResourceCacheEntry> getInstance() {
    return INSTANCE;
  }

  /** Sort cache entries by descending create time (newest first) */
  public static Comparator<HalResourceCacheEntry> getReverse() {
    return REVERSE;
  }

  @Override
  public int compare(HalResourceCacheEntry entry1, HalResourceCacheEntry entry2) {
    int compareTime = ((Long) entry1.getCreateTime()).compareTo(entry2.getCreateTime());
    if (compareTime != 0) {
      return compareTime;
    }
    else {
      return entry1.getId().compareTo(entry2.getId());
    }
  }

}
