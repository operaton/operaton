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
