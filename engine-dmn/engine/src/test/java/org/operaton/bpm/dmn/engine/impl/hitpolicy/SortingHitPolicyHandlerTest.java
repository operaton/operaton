package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.test.DmnEngineTest;

public abstract class SortingHitPolicyHandlerTest extends DmnEngineTest {

  protected void setInputVariables(Map<String, Object> inputs) {
    inputs.forEach((key, value) -> variables.putValue(key, value));
  }

  protected void assertResultsMatch(DmnDecisionResult decisionResult, List<Map<String, Object>> expectedOrder) {
    assertThat(decisionResult).hasSize(expectedOrder.size());
    List<Map<String, Object>> results = decisionResult.getResultList();
    for (int i = 0; i < expectedOrder.size(); i++) {
      assertEquals(expectedOrder.get(i), results.get(i),
          createPositionErrorMessage(i, expectedOrder.get(i), results.get(i)));
    }
  }

  protected void assertDetailedResultsMatch(DmnDecisionResult decisionResult, List<Map<String, Object>> expectedOrder) {
    assertThat(decisionResult).hasSize(expectedOrder.size());
    List<Map<String, Object>> results = decisionResult.getResultList();
    for (int i = 0; i < expectedOrder.size(); i++) {
      Map<String, Object> expectedResult = expectedOrder.get(i);
      Map<String, Object> actualResult = results.get(i);
      final int position = i;

      expectedResult.forEach((key, expectedValue) -> {
        assertTrue(actualResult.containsKey(key),
            String.format("Output '%s' is missing from result at position %d", key, position));
        assertEquals(expectedValue, actualResult.get(key),
            String.format("Output '%s' at position %d does not match. Expected: %s, Actual: %s", key, position,
                expectedValue, actualResult.get(key)));
      });

      assertEquals(expectedResult.size(), actualResult.size(),
          String.format("Result at position %d has different number of outputs. Expected: %d, Actual: %d", position,
              expectedResult.size(), actualResult.size()));
    }
  }

  protected void assertExceptionContainsMessage(Class<? extends Exception> expectedException,
                                                String expectedMessage,
                                                Runnable action) {
    Exception exception = assertThrows(expectedException, action::run);
    assertExceptionMessageContains(exception, expectedMessage);
  }

  protected void assertExceptionMessageContains(Exception exception, String expectedMessage) {
    assertTrue(exception.getMessage().contains(expectedMessage),
        String.format("Exception message should contain '%s'. Actual message: %s", expectedMessage,
            exception.getMessage()));
  }

  private String createPositionErrorMessage(int position, Map<String, Object> expected, Map<String, Object> actual) {
    return String.format("Result at position %d does not match. Expected: %s, Actual: %s", position, expected, actual);
  }
}
