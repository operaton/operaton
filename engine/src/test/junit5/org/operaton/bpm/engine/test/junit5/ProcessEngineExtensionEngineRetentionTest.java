package org.operaton.bpm.engine.test.junit5;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The nested classes below share static state across their own separate
 * class-lifecycle boundaries (see {@link #processEngineName}) to simulate
 * behavior across separate test classes within one JVM. Tagged "sequential"
 * so the build always runs it with forkCount=1: with forkCount &gt; 1, Surefire's
 * JUnit Platform provider can dispatch a {@code @Nested} class to a different
 * JVM fork than its enclosing class, running it twice (once as part of the
 * enclosing class's own cascade, once standalone) - see the surefire-plugin
 * config in this module's pom.xml. In this test, the standalone re-run happens
 * in a fresh JVM where this shared state was never set up.
 */
@Tag("sequential")
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

    @Nested
    @Order(1)
    class ProcessEngineExtensionFirstTests {

        @Test
        void testProcessEngineExtensionInitialAvailability() {
            processEngineName = processEngineExtension.getProcessEngine().getName();
            processEngineClosingExtensionName = processEngineClosingExtension.getProcessEngine().getName();
            assertNotNull(processEngineExtension.getProcessEngine());
            assertNotNull(processEngineClosingExtension.getProcessEngine());
        }
    }

    @Nested
    @Order(2)
    class ProcessEngineExtensionSecondTests {

        @Test
        void testExtensionsProcessEngineRetention() {
            // Check that same process engine is retained between multiple test classes
            assertNotNull(processEngineExtension.getProcessEngine());
            assertEquals(processEngineName, processEngineExtension.getProcessEngine().getName());

            // Check that process engine is recreated between multiple test classes
            assertNotNull(processEngineClosingExtension.getProcessEngine());
            assertNotEquals(processEngineClosingExtensionName, processEngineClosingExtension.getProcessEngine().getName());
        }
    }
}
