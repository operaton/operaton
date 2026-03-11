/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.qa.rolling.update;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/**
 * Provides one invocation per engine tag for {@link RollingUpdateTest}-annotated methods.
 */
public class RollingUpdateTestInvocationContextProvider implements TestTemplateInvocationContextProvider {

  @Override
  public boolean supportsTestTemplate(ExtensionContext context) {
    return context.getTestMethod()
        .map(method -> method.isAnnotationPresent(RollingUpdateTest.class))
        .orElse(false);
  }

  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
    return AbstractRollingUpdateTestCase.engineTags()
        .map(RollingUpdateInvocationContext::new);
  }

  protected static class RollingUpdateInvocationContext implements TestTemplateInvocationContext {

    private final String tag;

    protected RollingUpdateInvocationContext(String tag) {
      this.tag = tag;
    }

    @Override
    public String getDisplayName(int invocationIndex) {
      return tag + " engine";
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
      return Arrays.<Extension>asList(new RollingUpdateBeforeEachCallback(tag));
    }
  }

  protected static class RollingUpdateBeforeEachCallback implements BeforeEachCallback {

    private final String tag;

    protected RollingUpdateBeforeEachCallback(String tag) {
      this.tag = tag;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
      Object testInstance = context.getRequiredTestInstance();
      if (testInstance instanceof AbstractRollingUpdateTestCase) {
        ((AbstractRollingUpdateTestCase) testInstance).setEngineTag(tag);
      }
    }
  }
}
