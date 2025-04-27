package org.operaton.bpm.integrationtest.deployment.cfg;

import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * Arquillian SPI extension. Provides ability to subscribe to and respond to lifecycle events.
 * Current primary use: ability to start testcontainers before Arquillian containers,
 * so that platform subsystems can access them at startup time.
 */
public class ArquillianLifecycleObserverExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        extensionBuilder.observer(ArquillianEventObserver.class);
    }
}
