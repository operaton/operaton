package org.operaton.bpm.engine.test.junit5;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class ProcessEngineExtensionEngineRetentionTest {

    @RegisterExtension
    static ProcessEngineExtension processEngineExtension = ProcessEngineExtension
            .builder()
            .randomEngineName()
            .build();

    @RegisterExtension
    static  ProcessEngineExtension processEngineClosingExtension = ProcessEngineExtension
            .builder()
            .randomEngineName()
            .closeEngineAfterAllTests()
            .build();


    private static String processEngineName;
    private static String processEngineClosingExtensionName;

    private static ProcessEngine firstProcessEngine;
    private static ProcessEngine secondProcessEngine;

    @Nested
    @Order(1)
    class ProcessEngineExtensionFirstTests {

        @Test
        void testProcessEngineExtensionInitialAvailability() {
            processEngineName = processEngineExtension.getProcessEngine().getName();
            processEngineClosingExtensionName = processEngineClosingExtension.getProcessEngine().getName();
            assertNotNull(processEngineExtension.getProcessEngine());
            assertNotNull(processEngineClosingExtension.getProcessEngine());

            firstProcessEngine = processEngineExtension.getProcessEngine();
            secondProcessEngine = processEngineClosingExtension.getProcessEngine();
        }
    }

    @Nested
    @Order(2)
    class ProcessEngineExtensionSecondTests {

        @Test
        void testExtensionsProcessEngineRetention() {
            // Check that same process engine is retained between multiple test classes
            assertThat(processEngineExtension.getProcessEngine())
              .isNotNull()
              .isSameAs(firstProcessEngine);

            // Check that process engine is recreated between multiple test classes
            assertThat(processEngineClosingExtension.getProcessEngine())
              .isNotNull()
              .isNotSameAs(secondProcessEngine);
        }
    }
}
