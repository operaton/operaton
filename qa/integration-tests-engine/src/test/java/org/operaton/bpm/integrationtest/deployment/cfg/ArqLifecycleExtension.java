package org.operaton.bpm.integrationtest.deployment.cfg;

import org.jboss.arquillian.core.spi.LoadableExtension;

public class ArqLifecycleExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        extensionBuilder.observer(LifecycleExecutor.class);
    }
}
