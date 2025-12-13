package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.operaton.bpm.dmn.engine.DmnDecisionResult;
import org.operaton.bpm.dmn.engine.DmnDecisionRuleResult;
import org.operaton.bpm.dmn.engine.DmnDecisionTableResult;
import org.operaton.bpm.dmn.engine.impl.transform.DmnTransformException;
import org.operaton.bpm.dmn.engine.test.DecisionResource;
import org.operaton.bpm.dmn.engine.test.asserts.DmnEngineTestAssertions;

class OutputOrderHitPolicyHandlerTest extends SortingHitPolicyHandlerTest {

  private static final String DMN_THREE_OUTPUTS_COMPOUND = "org/operaton/bpm/dmn/engine/impl/hitpolicy/OutputOrderHitPolicyHandlerTest.test_threeOutputsCompound_shouldSortOverAllOutputs.dmn";
  private static final String DMN_COMPOUND_OUTPUTS_ALL_OUTPUT_NAMES_SET = "org/operaton/bpm/dmn/engine/impl/hitpolicy/OutputOrderHitPolicyHandlerTest.test_compoundOutputs_shouldHaveAllOutputValuesSet.dmn";
  private static final String DMN_COMPOUND_OUTPUTS_PARTIAL_OUTPUT_VALUES = "org/operaton/bpm/dmn/engine/impl/hitpolicy/OutputOrderHitPolicyHandlerTest.test_compoundOutputs_shouldHavePartialOutputValuesSet.dmn";
  private static final String DMN_COMPOUND_OUTPUTS_NO_OUTPUT_VALUES = "org/operaton/bpm/dmn/engine/impl/hitpolicy/OutputOrderHitPolicyHandlerTest.test_compoundOutputs_shouldHaveNoOutputValuesSet.dmn";
  private static final String DMN_SINGLE_UNNAMED_OUTPUT = "org/operaton/bpm/dmn/engine/impl/hitpolicy/OutputOrderHitPolicyHandlerTest.test_singleUnnamedOutput.dmn";

  @BeforeEach
  void setUp() {
  }

  @AfterEach
  void tearDown() {
  }

  @ParameterizedTest
  @MethodSource("generateThreeOutputsCompoundData")
  @DecisionResource(resource = DMN_THREE_OUTPUTS_COMPOUND)
  void test_threeOutputsCompound_shouldSortOverAllOutputs(Map<String, Object> inputs,
                                                          List<Map<String, Object>> expectedOrder) {
    setInputVariables(inputs);
    DmnDecisionResult decisionResult = evaluateDecision();
    assertResultsMatch(decisionResult, expectedOrder);
  }

  @ParameterizedTest
  @MethodSource("generateInvalidOutputValueData")
  @DecisionResource(resource = DMN_THREE_OUTPUTS_COMPOUND)
  void test_invalidOutputValue_shouldThrowException(Map<String, Object> inputs,
                                                    Class<? extends Exception> expectedException,
                                                    String expectedMessage) {
    setInputVariables(inputs);
    assertExceptionContainsMessage(expectedException, expectedMessage, this::evaluateDecision);
  }

  @Test
  void test_compoundOutputs_shouldHaveAllOutputValuesSet() {
    Exception exception = assertThrows(DmnTransformException.class,
        () -> super.parseDecisionsFromFile(DMN_COMPOUND_OUTPUTS_ALL_OUTPUT_NAMES_SET));
    assertExceptionMessageContains(exception, "does not have an output name");
  }

  @ParameterizedTest
  @MethodSource("generatePartialOutputValuesData")
  @DecisionResource(resource = DMN_COMPOUND_OUTPUTS_PARTIAL_OUTPUT_VALUES)
  void test_compoundOutputs_shouldHavePartialOutputValuesSet(Map<String, Object> inputs,
                                                             List<Map<String, Object>> expectedOrder) {
    setInputVariables(inputs);
    DmnDecisionResult decisionResult = evaluateDecision();
    assertResultsMatch(decisionResult, expectedOrder);
  }

  @ParameterizedTest
  @MethodSource("generateNoOutputValuesData")
  @DecisionResource(resource = DMN_COMPOUND_OUTPUTS_NO_OUTPUT_VALUES)
  void test_compoundOutputs_shouldHaveNoOutputValuesSet(Map<String, Object> inputs,
                                                        List<Map<String, Object>> expectedOrder) {
    setInputVariables(inputs);
    DmnDecisionResult decisionResult = evaluateDecision();
    assertDetailedResultsMatch(decisionResult, expectedOrder);
  }

  @ParameterizedTest
  @MethodSource("generateSingleUnnamedOutputData")
  @DecisionResource(resource = DMN_SINGLE_UNNAMED_OUTPUT)
  void test_singleUnnamedOutput(int input, List<Object> expectedOrder) {
    variables.putValue("Score", input);
    DmnDecisionTableResult decisionResult = evaluateDecisionTable();
    DmnEngineTestAssertions.assertThat(decisionResult).hasSize(expectedOrder.size());
    List<Object> actual = decisionResult.stream().map(DmnDecisionRuleResult::getSingleEntry).toList();
    assertThat(actual).containsExactlyElementsOf(expectedOrder);
  }

  public static Stream<Arguments> generateSingleUnnamedOutputData() {
    return Stream.of(Arguments.of(400, List.of("Low", "Medium", "Medium", "High")),
        Arguments.of(999, List.of("Low", "Low", "Medium", "High", "Unique")));
  }

  public static Stream<Arguments> generateThreeOutputsCompoundData() {
    return Stream.of(Arguments.of(Map.of("RiskCategory", "Medium", "IsExistingCustomer", false, "CustomerScore", 620),
            List.of(Map.of("Action", "ManualReviewed", "PriorityLevel", "High", "ReviewUrgency", "Urgent"),
                Map.of("Action", "ManualReviewed", "PriorityLevel", "Medium", "ReviewUrgency", "Standard"))),
        Arguments.of(Map.of("RiskCategory", "Low", "IsExistingCustomer", true, "CustomerScore", 480),
            List.of(Map.of("Action", "ManualReviewed", "PriorityLevel", "Low", "ReviewUrgency", "Urgent"))),
        Arguments.of(Map.of("RiskCategory", "High", "IsExistingCustomer", false, "CustomerScore", 350),
            List.of(Map.of("Action", "Declined", "PriorityLevel", "High", "ReviewUrgency", "Urgent"))),
        Arguments.of(Map.of("RiskCategory", "Medium", "IsExistingCustomer", false, "CustomerScore", 510),
            List.of(Map.of("Action", "ManualReviewed", "PriorityLevel", "Medium", "ReviewUrgency", "Standard"))));
  }

  public static Stream<Arguments> generateInvalidOutputValueData() {
    return Stream.of(Arguments.of(Map.of("RiskCategory", "None", "IsExistingCustomer", true, "CustomerScore", 100),
            DmnHitPolicyException.class, "not found in allowed output values for output"),
        Arguments.of(Map.of("RiskCategory", "None", "IsExistingCustomer", true, "CustomerScore", 1000),
            DmnHitPolicyException.class, "DMN-03007 Hit policy 'OUTPUT ORDER' requires at least one matching rule"));
  }

  public static Stream<Arguments> generatePartialOutputValuesData() {
    return Stream.of(Arguments.of(Map.of("RiskCategory", "Medium", "IsExistingCustomer", false, "CustomerScore", 620),
            List.of(Map.of("Action", "ManualReviewed", "PriorityLevel", "High", "ReviewUrgency", "Urgent"),
                Map.of("Action", "ManualReviewed", "PriorityLevel", "Medium", "ReviewUrgency", "Standard"),
                Map.of("Action", "ManualReviewed", "PriorityLevel", "Medium", "ReviewUrgency", "Standard"),
                Map.of("Action", "ManualReviewed", "PriorityLevel", "Low", "ReviewUrgency", "Standard"))),
        Arguments.of(Map.of("RiskCategory", "Low", "IsExistingCustomer", true, "CustomerScore", 480),
            List.of(Map.of("Action", "ManualReviewed", "PriorityLevel", "Low", "ReviewUrgency", "Urgent"))),
        Arguments.of(Map.of("RiskCategory", "High", "IsExistingCustomer", false, "CustomerScore", 350),
            List.of(Map.of("Action", "Declined", "PriorityLevel", "High", "ReviewUrgency", "Urgent"))),
        Arguments.of(Map.of("RiskCategory", "Medium", "IsExistingCustomer", false, "CustomerScore", 510),
            List.of(Map.of("Action", "ManualReviewed", "PriorityLevel", "Medium", "ReviewUrgency", "Standard"),
                Map.of("Action", "ManualReviewed", "PriorityLevel", "Medium", "ReviewUrgency", "Standard"),
                Map.of("Action", "ManualReviewed", "PriorityLevel", "Low", "ReviewUrgency", "Standard"))));
  }

  public static Stream<Arguments> generateNoOutputValuesData() {
    return Stream.of(Arguments.of(Map.of("RiskCategory", "Medium", "IsExistingCustomer", false, "CustomerScore", 620),
            List.of(Map.of("Action", "ManualReviewed", "PriorityLevel", "Low", "ReviewUrgency", "Standard"),
                Map.of("Action", "ManualReviewed", "PriorityLevel", "Medium", "ReviewUrgency", "Standard"),
                Map.of("Action", "ManualReviewed", "PriorityLevel", "High", "ReviewUrgency", "Urgent"),
                Map.of("Action", "ManualReviewed", "PriorityLevel", "Medium", "ReviewUrgency", "Standard"))),
        Arguments.of(Map.of("RiskCategory", "Low", "IsExistingCustomer", true, "CustomerScore", 480),
            List.of(Map.of("Action", "ManualReviewed", "PriorityLevel", "Low", "ReviewUrgency", "Urgent"))),
        Arguments.of(Map.of("RiskCategory", "High", "IsExistingCustomer", false, "CustomerScore", 350),
            List.of(Map.of("Action", "Declined", "PriorityLevel", "High", "ReviewUrgency", "Urgent"))),
        Arguments.of(Map.of("RiskCategory", "Medium", "IsExistingCustomer", false, "CustomerScore", 510),
            List.of(Map.of("Action", "ManualReviewed", "PriorityLevel", "Low", "ReviewUrgency", "Standard"),
                Map.of("Action", "ManualReviewed", "PriorityLevel", "Medium", "ReviewUrgency", "Standard"),
                Map.of("Action", "ManualReviewed", "PriorityLevel", "Medium", "ReviewUrgency", "Standard"))));
  }
}
