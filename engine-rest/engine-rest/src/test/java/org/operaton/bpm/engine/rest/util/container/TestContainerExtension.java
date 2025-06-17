package org.operaton.bpm.engine.rest.util.container;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension counterpart of the {@code TestContainerRule}.
 * It looks up the {@link ContainerSpecifics} implementation via
 * {@link ServiceLoader} and delegates the lifecycle callbacks to
 * the container specific extension.
 */
public class TestContainerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final Logger LOGGER = Logger.getLogger(TestContainerExtension.class.getSimpleName());

  protected ContainerSpecifics containerSpecifics;
  protected Extension delegate;

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    lookUpContainerSpecifics();
    delegate = containerSpecifics.getExtension(context.getRequiredTestClass());
    if (delegate instanceof BeforeAllCallback) {
      ((BeforeAllCallback) delegate).beforeAll(context);
    }
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (delegate instanceof AfterAllCallback) {
      ((AfterAllCallback) delegate).afterAll(context);
    }
  }

  protected void lookUpContainerSpecifics() {
    if (this.containerSpecifics == null) {
      ServiceLoader<ContainerSpecifics> serviceLoader = ServiceLoader.load(ContainerSpecifics.class);
      Iterator<ContainerSpecifics> it = serviceLoader.iterator();

      if (it.hasNext()) {
        this.containerSpecifics = it.next();

        if (it.hasNext()) {
          LOGGER.warning("There is more than one test runtime container implementation present on the classpath. "
              + "Using " + this.containerSpecifics.getClass().getName());
        }
      } else {
        throw new RuntimeException(
            "Could not find container provider SPI that implements " + ContainerSpecifics.class.getName());
      }
    }
  }
}
