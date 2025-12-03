package org.operaton.bpm.engine.test.junit5;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class ProcessEngineExtensionEngineRetentionTest2 {
    static class ProcessEngineExtensionVerifier implements BeforeAllCallback, AfterAllCallback {
        private final ProcessEngineExtension processEngineExtension;

        public ProcessEngineExtensionVerifier(ProcessEngineExtension processEngineExtension) {
            this.processEngineExtension = processEngineExtension;
        }

        @Override
        public void beforeAll(ExtensionContext context) throws Exception {
            assertThat(processEngineExtension.getProcessEngine()).isNotNull();
        }

        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            assertThat(processEngineExtension.getProcessEngine()).isNotNull();
        }
    }

    abstract class AbstractTest {

        static ProcessEngine myProcessEngine = ProcessEngineConfiguration
          .createStandaloneInMemProcessEngineConfiguration()
          .setHistory("full")
          .setJdbcUrl("jdbc:h2:mem:operaton;DB_CLOSE_DELAY=1000")
          .buildProcessEngine();

        @RegisterExtension
        static final ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder()
          .useProcessEngine(myProcessEngine)
          .build();

        @RegisterExtension
        static final ProcessEngineExtensionVerifier verifier =
          new ProcessEngineExtensionVerifier(processEngineExtension);

    }

    @Nested
    @Order(1)
    class TestA extends AbstractTest {
        @Test
        void testDeployment() {
            assertThat(processEngineExtension.getProcessEngine())
              .isNotNull()
              .isSameAs(AbstractTest.myProcessEngine);
        }
    }
    @Nested
    @Order(2)
    class TestB extends AbstractTest {
        @Test
        void testDeployment() {
            assertThat(processEngineExtension.getProcessEngine())
              .isNotNull()
              .isSameAs(AbstractTest.myProcessEngine);
        }
    }
}
