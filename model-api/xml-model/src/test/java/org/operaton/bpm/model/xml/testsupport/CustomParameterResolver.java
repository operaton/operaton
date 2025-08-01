package org.operaton.bpm.model.xml.testsupport;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.engine.execution.BeforeEachMethodAdapter;
import org.junit.jupiter.engine.extension.ExtensionRegistry;

public class CustomParameterResolver implements BeforeEachMethodAdapter, ParameterResolver {

  private ParameterResolver parameterisedTestParameterResolver;

  @Override
  public void invokeBeforeEachMethod(ExtensionContext context, ExtensionRegistry registry)
      throws Throwable {
    Optional<ParameterResolver> resolverOptional = registry.getExtensions(ParameterResolver.class)
        .stream()
        .filter(parameterResolver ->
            parameterResolver.getClass().getName()
                .contains("ParameterizedTestParameterResolver")
        )
        .findFirst();
    if (resolverOptional.isEmpty()) {
      throw new IllegalStateException(
          "ParameterizedTestParameterResolver missed in the registry. Probably it's not a Parameterized Test");
    } else {
      parameterisedTestParameterResolver = resolverOptional.get();
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    if (isExecutedOnAfterOrBeforeMethod(parameterContext)) {
      return getMappedContext(parameterContext, extensionContext).isPresent();
      /*
      return getMappedContext(parameterContext, extensionContext).map(pContext -> {
        return parameterisedTestParameterResolver.supportsParameter(pContext, extensionContext);
      }).orElse(false);

       */
    }
    return false;
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {

    return parameterisedTestParameterResolver.resolveParameter(
        getMappedContext(parameterContext, extensionContext).orElseThrow(), extensionContext);
  }

  private Optional<MappedParameterContext> getMappedContext(ParameterContext parameterContext,
      ExtensionContext extensionContext) {
    if (isExecutedOnAfterOrBeforeMethod(parameterContext)) {
      return Stream.of(parameterContext.getDeclaringExecutable().getParameters())
              .filter(p -> Objects.equals(p.getType(), parameterContext.getParameter().getType()))
              .findFirst()
              .map(p -> new MappedParameterContext(
                      parameterContext.getIndex(),
                      p,
                      Optional.of(parameterContext.getTarget())));
    }
    return Optional.empty();
  }

  private boolean isExecutedOnAfterOrBeforeMethod(ParameterContext parameterContext) {
    return Arrays.stream(parameterContext.getDeclaringExecutable().getDeclaredAnnotations())
        .anyMatch(this::isAfterEachOrBeforeEachAnnotation);
  }

  private boolean isAfterEachOrBeforeEachAnnotation(Annotation annotation) {
    return annotation.annotationType() == BeforeEach.class
        || annotation.annotationType() == AfterEach.class;
  }
}
