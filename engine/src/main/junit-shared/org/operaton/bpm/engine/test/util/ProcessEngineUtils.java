/*
 * Copyright 2025 the Operaton contributors.
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
package org.operaton.bpm.engine.test.util;

import java.util.Random;

import org.operaton.bpm.engine.ProcessEngines;

public class ProcessEngineUtils {
    private static final Random RANDOM = new Random();

    /**
     * Returns a random name for a ProcessEngine that is not yet used.
     * This name might be used for a new ProcessEngine without explicitly name it.
     */
    public static String newRandomProcessEngineName() {
        String result = "processEngine-rnd" + RANDOM.nextLong();
        return ProcessEngines.isRegisteredProcessEngine(result) ? newRandomProcessEngineName() : result;
    }

  private ProcessEngineUtils() {
  }
}
