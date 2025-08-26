package org.operaton.bpm.engine.impl.jobexecutor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandInvocationContext;
import org.operaton.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler.AsyncContinuationConfiguration;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.TransitionImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.operation.PvmAtomicOperation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncContinuationJobHandlerTest {
    @Mock
    ExecutionEntity execution;

    @Mock
    CommandContext commandContext;

    @Mock
    ActivityImpl activity;

    @Mock
    TransitionImpl transition;

    @Mock
    CommandInvocationContext commandInvocationContext;

    @InjectMocks
    AsyncContinuationJobHandler handler;

    @Test
    void newConfiguration_shouldParseConfigurationWithTransitionId() {
        // given
        String configString = "opName$trans123";

        // when
        AsyncContinuationConfiguration config = handler.newConfiguration(configString);

        // then
        assertThat(config.getAtomicOperation()).isEqualTo("opName");
        assertThat(config.getTransitionId()).isEqualTo("trans123");
    }

    @Test
    void newConfiguration_shouldParseConfigurationWithoutTransitionId() {
        // given
        String configString = "opName";

        // when
        AsyncContinuationConfiguration config = handler.newConfiguration(configString);

        // then
        assertThat(config.getAtomicOperation()).isEqualTo("opName");
        assertThat(config.getTransitionId()).isNull();
    }

    @Test
    void toCanonicalString_shouldSerializeConfigurationWithTransitionId() {
        // given
        AsyncContinuationConfiguration config = new AsyncContinuationConfiguration();
        config.setAtomicOperation("opName");
        config.setTransitionId("trans123");

        // when
        String canonical = config.toCanonicalString();

        // then
        assertThat(canonical).isEqualTo("opName$trans123");
    }

    @Test
    void toCanonicalString_shouldSerializeConfigurationWithoutTransitionId() {
        // given
        AsyncContinuationConfiguration config = new AsyncContinuationConfiguration();
        config.setAtomicOperation("opName");

        // when
        String canonical = config.toCanonicalString();

        // then
        assertThat(canonical).isEqualTo("opName");
    }

    @Test
    void findMatchingAtomicOperation_shouldReturnDefaultOperationIfNull() {
        // given
        String operationName = null;

        // when
        PvmAtomicOperation op = handler.findMatchingAtomicOperation(operationName);

        // then
        assertThat(op).isEqualTo(PvmAtomicOperation.TRANSITION_CREATE_SCOPE);
    }

    @Test
    void findMatchingAtomicOperation_shouldReturnNullForUnknownOperation() {
        // given
        String operationName = "unknownOp";

        // when
        PvmAtomicOperation op = handler.findMatchingAtomicOperation(operationName);

        // then
        assertThat(op).isNull();
    }

    @Test
    void tokenizeJobConfiguration_shouldReturnOperationAndTransitionId() {
        // given
        String config = "operation$transition";

        // when
        String[] result = handler.tokenizeJobConfiguration(config);

        // then
        assertThat(result).containsExactly("operation", "transition");
    }

    @Test
    void tokenizeJobConfiguration_shouldReturnOnlyOperationIfNoTransitionId() {
        // given
        String config = "operation";

        // when
        String[] result = handler.tokenizeJobConfiguration(config);

        // then
        assertThat(result).containsExactly("operation", null);
    }

    @Test
    void tokenizeJobConfiguration_shouldReturnArrayWithNullsIfInputIsNull() {
        // given
        String config = null;

        // when
        String[] result = handler.tokenizeJobConfiguration(config);

        // then
        assertThat(result).containsExactly(null, null);
    }


    @Test
    void tokenizeJobConfiguration_shouldThrowOnInvalidTokenizeJobConfiguration() {
        // given
        String invalid = "a$b$c";

        // when / then
        assertThatThrownBy(() -> handler.tokenizeJobConfiguration(invalid))
                .isInstanceOf(ProcessEngineException.class)
                .hasMessageContaining("Illegal async continuation job handler configuration");
    }

    @Test
    void execute_shouldPerformOperationWithTransition() {
        // given
        String configString = "transition-create-scope$t1";
        AsyncContinuationConfiguration config = handler.newConfiguration(configString);

        when(execution.getActivity()).thenReturn(activity);
        when(activity.findOutgoingTransition("t1")).thenReturn(transition);

        // static mocking für Context.getCommandInvocationContext()
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(Context::getCommandInvocationContext).thenReturn(commandInvocationContext);

            // when
            handler.execute(config, execution, commandContext, "tenant");

            // then
            verify(execution).setTransition(transition);
            verify(commandInvocationContext).performOperation(PvmAtomicOperation.TRANSITION_CREATE_SCOPE, execution);
        }
    }

    @Test
    void execute_shouldPerformOperationWithoutTransition() {
        // given
        String configString = "transition-create-scope";
        AsyncContinuationConfiguration config = handler.newConfiguration(configString);

        when(execution.getActivity()).thenReturn(activity);
        when(activity.isAsyncBefore()).thenReturn(false);

        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(Context::getCommandInvocationContext).thenReturn(commandInvocationContext);

            // when
            handler.execute(config, execution, commandContext, "tenant");

            // then
            verify(execution, never()).setTransition(any());
            verify(commandInvocationContext).performOperation(PvmAtomicOperation.TRANSITION_CREATE_SCOPE, execution);
        }
    }

    @Test
    void execute_shouldThrowIfOperationNotSupported() {
        // given
        String configString = "unknownOp";
        AsyncContinuationConfiguration config = handler.newConfiguration(configString);
        when(execution.getActivity()).thenReturn(activity);
        when(activity.isAsyncBefore()).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> handler.execute(config, execution, commandContext, "tenant"))
                .isInstanceOf(ProcessEngineException.class)
                .hasMessageContaining("Cannot process job with configuration");
    }

    @ParameterizedTest
    @CsvSource({
            // operationName, expectedSupported
            "activity-start-create-scope,true",
            "activity-end,true",
            "process-start,true",
            "transition-notify-listener-take,true",
            "transition-create-scope,true",
            "UNSUPPORTED_OP,false"
    })
    void isSupported_shouldReturnExpectedResult(String operationName, boolean expectedSupported) {
        // given
        PvmAtomicOperation op = mock(PvmAtomicOperation.class);
        when(op.getCanonicalName()).thenReturn(operationName);

        // when
        boolean supported = handler.isSupported(op);

        // then
        assertThat(supported).isEqualTo(expectedSupported);
    }
}