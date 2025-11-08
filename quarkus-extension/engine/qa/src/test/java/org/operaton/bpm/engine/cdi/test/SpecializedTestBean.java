package org.operaton.bpm.engine.cdi.test;

import jakarta.enterprise.inject.Alternative;
import org.operaton.bpm.engine.cdi.test.impl.util.ProgrammaticBeanLookupTest;

@Alternative
public class SpecializedTestBean extends ProgrammaticBeanLookupTest.TestBean {
}
