package org.operaton.bpm.model.dmn.instance;


import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.dmn.Dmn;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.bpm.model.dmn.OperatonExtensionsTest;
import org.operaton.bpm.model.dmn.impl.instance.DecisionImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.CAMUNDA_NS;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.OPERATON_ATTRIBUTE_HISTORY_TIME_TO_LIVE;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.OPERATON_ATTRIBUTE_VERSION_TAG;

/**
 * Test to check the interoperability when changing elements and attributes with
 * the {@link org.operaton.bpm.model.dmn.impl.DmnModelConstants#CAMUNDA_NS}. In contrast to
 * {@link org.operaton.bpm.model.dmn.OperatonExtensionsTest} this test uses directly the get*Ns() methods to
 * check the expected value.
 *
 * @author Tim Zöller
 */
class CompatibilityTest {

    @Test
    void modifyingAttributeWithCamundaNsKeepsIt() {
        DmnModelInstance modelInstance = Dmn.readModelFromStream(OperatonExtensionsTest.class.getResourceAsStream("OperatonExtensionsCompatibilityTest.dmn"));
        DecisionImpl decision = modelInstance.getModelElementById("decision");

        String historyTimeToLive = "120";
        String versionTag = "v1.0.0";

        decision.setOperatonHistoryTimeToLiveString(historyTimeToLive);
        decision.setVersionTag(versionTag);

        assertThat(decision.getAttributeValueNs(CAMUNDA_NS, OPERATON_ATTRIBUTE_HISTORY_TIME_TO_LIVE)).isEqualTo(historyTimeToLive);
        assertThat(decision.getAttributeValueNs(CAMUNDA_NS, OPERATON_ATTRIBUTE_VERSION_TAG)).isEqualTo(versionTag);

    }
}
