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
 * 
 */
package org.operaton.bpm.engine.test.junit5;

import org.junit.jupiter.api.Test;

/**
 * This test is checking that a ProcessEngineExtension annotated to the superclass of a test is injecting the
 * services into the superclass fields.
 */
class ProcessEngineExtensionSubTypeInjectionTest extends ProcessEngineExtensionServiceInjectionTest {
    @Test
    @Override
    void extensionInjectsServiceInstances() {
        super.extensionInjectsServiceInstances();
    }
}
