package org.operaton.spin;

import org.junit.jupiter.api.Test;
import org.operaton.spin.impl.SpinFactoryImpl;

import static org.assertj.core.api.Assertions.assertThat;

class SpinFactoryTest {
    @Test
    void factoryInstanceLoadsSuccessfully() {
        SpinFactory factory = SpinFactory.INSTANCE;
        assertThat(factory).isInstanceOf(SpinFactoryImpl.class);
    }
}
