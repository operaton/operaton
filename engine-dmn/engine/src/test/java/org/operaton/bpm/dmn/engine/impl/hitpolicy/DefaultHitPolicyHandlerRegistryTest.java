package org.operaton.bpm.dmn.engine.impl.hitpolicy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.dmn.engine.impl.spi.hitpolicy.DmnHitPolicyHandler;
import org.operaton.bpm.model.dmn.HitPolicy;

class DefaultHitPolicyHandlerRegistryTest {

  @Nested
  @DisplayName("getHandler with preview features disabled or no configuration")
  class PreviewFeaturesDisabled {

    @Test
    @DisplayName("returns null for PRIORITY and OUTPUT_ORDER when configuration is null")
    void shouldReturnNullForPriorityAndOutputOrderWhenConfigurationIsNull() {
      DefaultHitPolicyHandlerRegistry registry = new DefaultHitPolicyHandlerRegistry(null);

      DmnHitPolicyHandler priorityHandler = registry.getHandler(HitPolicy.PRIORITY, null);
      DmnHitPolicyHandler outputOrderHandler = registry.getHandler(HitPolicy.OUTPUT_ORDER, null);

      assertThat(priorityHandler).as("priority handler").isNull();
      assertThat(outputOrderHandler).as("output order handler").isNull();
    }

    @Test
    @DisplayName("returns null for PRIORITY and OUTPUT_ORDER when preview features are disabled")
    void shouldReturnNullForPriorityAndOutputOrderWhenPreviewFeaturesDisabled() {
      DefaultDmnEngineConfiguration configuration = new DefaultDmnEngineConfiguration();
      configuration.setPreviewFeaturesEnabled(false);
      DefaultHitPolicyHandlerRegistry registry = new DefaultHitPolicyHandlerRegistry(configuration);

      DmnHitPolicyHandler priorityHandler = registry.getHandler(HitPolicy.PRIORITY, null);
      DmnHitPolicyHandler outputOrderHandler = registry.getHandler(HitPolicy.OUTPUT_ORDER, null);

      assertThat(priorityHandler).as("priority handler").isNull();
      assertThat(outputOrderHandler).as("output order handler").isNull();
    }
  }

  @Nested
  @DisplayName("getHandler with preview features enabled")
  class PreviewFeaturesEnabled {

    @Test
    @DisplayName("returns handlers for PRIORITY and OUTPUT_ORDER when preview features are enabled")
    void shouldReturnHandlersForPriorityAndOutputOrderWhenPreviewFeaturesEnabled() {
      DefaultDmnEngineConfiguration configuration = new DefaultDmnEngineConfiguration();
      configuration.setPreviewFeaturesEnabled(true);
      DefaultHitPolicyHandlerRegistry registry = new DefaultHitPolicyHandlerRegistry(configuration);

      DmnHitPolicyHandler priorityHandler = registry.getHandler(HitPolicy.PRIORITY, null);
      DmnHitPolicyHandler outputOrderHandler = registry.getHandler(HitPolicy.OUTPUT_ORDER, null);

      assertThat(priorityHandler)
          .as("priority handler should be available when preview features are enabled")
          .isNotNull();
      assertThat(outputOrderHandler)
          .as("output order handler should be available when preview features are enabled")
          .isNotNull();
    }

    @Test
    @DisplayName("continues to return handlers for non-preview hit policies regardless of preview flag")
    void shouldAlwaysReturnHandlersForNonPreviewHitPolicies() {
      DefaultDmnEngineConfiguration configuration = new DefaultDmnEngineConfiguration();
      configuration.setPreviewFeaturesEnabled(false);
      DefaultHitPolicyHandlerRegistry registry = new DefaultHitPolicyHandlerRegistry(configuration);

      // UNIQUE is a standard, non-preview hit policy
      DmnHitPolicyHandler uniqueHandlerWhenDisabled = registry.getHandler(HitPolicy.UNIQUE, null);
      assertThat(uniqueHandlerWhenDisabled)
          .as("unique handler should always be available regardless of preview feature flag")
          .isNotNull();

      configuration.setPreviewFeaturesEnabled(true);
      DmnHitPolicyHandler uniqueHandlerWhenEnabled = registry.getHandler(HitPolicy.UNIQUE, null);
      assertThat(uniqueHandlerWhenEnabled)
          .as("unique handler should remain available when preview features are enabled")
          .isNotNull();
    }
  }
}
