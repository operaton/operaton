/*
 * Copyright 2026 the Operaton contributors.
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
package org.operaton.bpm.engine.impl.bpmn.behavior;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.apache.commons.mail2.core.EmailException;
import org.apache.commons.mail2.jakarta.Email;
import org.apache.commons.mail2.jakarta.HtmlEmail;
import org.apache.commons.mail2.jakarta.SimpleEmail;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.delegate.Expression;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.stream.Stream;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;

class MailActivityBehaviorTest {

  private enum RecipientType { TO, CC, BCC }

  @Test
  void execute_configuresAndSendsEmail() throws Exception {
    // given
    MailActivityBehavior behavior = Mockito.spy(new MailActivityBehavior());
    Email email = mock(Email.class);
    ExecutionEntity execution = mock(ExecutionEntity.class);

    Expression toExpr = mock(Expression.class);
    Expression fromExpr = mock(Expression.class);
    Expression ccExpr = mock(Expression.class);
    Expression bccExpr = mock(Expression.class);
    Expression subjectExpr = mock(Expression.class);
    Expression textExpr = mock(Expression.class);
    Expression htmlExpr = mock(Expression.class);
    Expression charsetExpr = mock(Expression.class);

    behavior.to = toExpr;
    behavior.from = fromExpr;
    behavior.cc = ccExpr;
    behavior.bcc = bccExpr;
    behavior.subject = subjectExpr;
    behavior.text = textExpr;
    behavior.html = htmlExpr;
    behavior.charset = charsetExpr;

    when(toExpr.getValue(execution)).thenReturn("a@x.com, b@x.com");
    when(fromExpr.getValue(execution)).thenReturn("sender@example.com");
    when(ccExpr.getValue(execution)).thenReturn("c@x.com");
    when(bccExpr.getValue(execution)).thenReturn("d@x.com");
    when(subjectExpr.getValue(execution)).thenReturn("the subject");
    when(textExpr.getValue(execution)).thenReturn("plain text");
    when(htmlExpr.getValue(execution)).thenReturn("<b>html</b>");
    when(charsetExpr.getValue(execution)).thenReturn("UTF-8");

    doReturn(email).when(behavior).createEmail("plain text", "<b>html</b>");

    ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
    when(config.getMailServerHost()).thenReturn("smtp.example.com");
    when(config.getMailServerPort()).thenReturn(2525);
    when(config.getMailServerUseTLS()).thenReturn(true);
    when(config.getMailServerUsername()).thenReturn("user");
    when(config.getMailServerPassword()).thenReturn("pass");

    try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
      ctx.when(Context::getProcessEngineConfiguration).thenReturn(config);

      // when
      behavior.execute(execution);

      // then
      verify(email).addTo("a@x.com");
      verify(email).addTo("b@x.com");
      verify(email).addCc("c@x.com");
      verify(email).addBcc("d@x.com");
      verify(email).setFrom("sender@example.com");
      verify(email).setSubject("the subject");
      verify(email).setHostName("smtp.example.com");
      verify(email).setSmtpPort(2525);
      verify(email).setStartTLSEnabled(true);
      verify(email).setAuthentication("user", "pass");
      verify(email).setCharset("UTF-8");
      verify(email).send();
    }
  }

  @Test
  void execute_sendThrowsEmailException_wrapped() throws Exception {
    // given
    MailActivityBehavior behavior = Mockito.spy(new MailActivityBehavior());
    Email email = mock(Email.class);
    ExecutionEntity execution = mock(ExecutionEntity.class);

    Expression toExpr = mock(Expression.class);
    Expression textExpr = mock(Expression.class);

    behavior.to = toExpr;
    behavior.text = textExpr;

    when(toExpr.getValue(execution)).thenReturn("a@x.com");
    when(textExpr.getValue(execution)).thenReturn("plain text");

    doReturn(email).when(behavior).createEmail("plain text", null);

    ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
    when(config.getMailServerHost()).thenReturn("smtp.example.com");
    when(config.getMailServerPort()).thenReturn(2525);
    when(config.getMailServerUseTLS()).thenReturn(false);

    try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
      ctx.when(Context::getProcessEngineConfiguration).thenReturn(config);

      doThrow(new EmailException("boom")).when(email).send();

      // when / then
      assertThatThrownBy(() -> behavior.execute(execution))
        .isInstanceOf(RuntimeException.class)
        .hasCauseInstanceOf(EmailException.class);
    }
  }

  static Stream<RecipientType> recipientTypes() {
    return Stream.of(RecipientType.TO, RecipientType.CC, RecipientType.BCC);
  }

  @ParameterizedTest
  @MethodSource("recipientTypes")
  void recipients_splitsAndTrimsAndAddsAllRecipients(RecipientType type) throws Exception {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();
    Email email = mock(Email.class);

    // when
    switch (type) {
      case TO:
        behavior.addTo(email, " alice@example.com ,bob@example.com,carol@example.com ");
        break;
      case CC:
        behavior.addCc(email, " alice@example.com ,bob@example.com,carol@example.com ");
        break;
      case BCC:
        behavior.addBcc(email, " alice@example.com ,bob@example.com,carol@example.com ");
        break;
    }

    // then
    switch (type) {
      case TO:
        verify(email).addTo("alice@example.com");
        verify(email).addTo("bob@example.com");
        verify(email).addTo("carol@example.com");
        break;
      case CC:
        verify(email).addCc("alice@example.com");
        verify(email).addCc("bob@example.com");
        verify(email).addCc("carol@example.com");
        break;
      case BCC:
        verify(email).addBcc("alice@example.com");
        verify(email).addBcc("bob@example.com");
        verify(email).addBcc("carol@example.com");
        break;
    }
    verifyNoMoreInteractions(email);
  }

  @ParameterizedTest
  @MethodSource("recipientTypes")
  void recipients_nullBehavior_respected(RecipientType type) {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();
    Email email = mock(Email.class);

    // when / then
    switch (type) {
      case TO:
        // when / then - addTo should throw on null recipients
        assertThatThrownBy(() -> behavior.addTo(email, null))
          .isInstanceOf(RuntimeException.class);
        break;
      case CC:
        // when
        behavior.addCc(email, null);
        // then - should not throw and should not interact with the email
        verifyNoMoreInteractions(email);
        break;
      case BCC:
        // when
        behavior.addBcc(email, null);
        // then - should not throw and should not interact with the email
        verifyNoMoreInteractions(email);
        break;
    }
  }

  @ParameterizedTest
  @MethodSource("recipientTypes")
  void recipients_emailThrowsEmailException_wrapped(RecipientType type) throws Exception {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();
    Email email = mock(Email.class);

    // configure mock to throw for the 'bad@example.com' recipient
    switch (type) {
      case TO:
        doThrow(new EmailException("boom")).when(email).addTo("bad@example.com");
        break;
      case CC:
        doThrow(new EmailException("boom")).when(email).addCc("bad@example.com");
        break;
      case BCC:
        doThrow(new EmailException("boom")).when(email).addBcc("bad@example.com");
        break;
    }

    // when / then
    switch (type) {
      case TO:
        assertThatThrownBy(() -> behavior.addTo(email, " bad@example.com "))
          .isInstanceOf(RuntimeException.class)
          .hasCauseInstanceOf(EmailException.class);
        break;
      case CC:
        assertThatThrownBy(() -> behavior.addCc(email, " bad@example.com "))
          .isInstanceOf(RuntimeException.class)
          .hasCauseInstanceOf(EmailException.class);
        break;
      case BCC:
        assertThatThrownBy(() -> behavior.addBcc(email, " bad@example.com "))
          .isInstanceOf(RuntimeException.class)
          .hasCauseInstanceOf(EmailException.class);
        break;
    }
  }

  @Test
  void setFrom_withExplicitFrom_callsEmailSetFrom() throws Exception {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();
    Email email = mock(Email.class);

    // when
    behavior.setFrom(email, "explicit@example.com");

    // then
    verify(email).setFrom("explicit@example.com");
    verifyNoMoreInteractions(email);
  }

  @Test
  void setFrom_withNullUsesDefaultFromFromProcessEngineConfig() throws Exception {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();
    Email email = mock(Email.class);

    ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
    when(config.getMailServerDefaultFrom()).thenReturn("default@example.com");

    try (MockedStatic<Context> mocked = mockStatic(Context.class)) {
      mocked.when(Context::getProcessEngineConfiguration).thenReturn(config);

      // when
      behavior.setFrom(email, null);

      // then
      verify(email).setFrom("default@example.com");
      verifyNoMoreInteractions(email);
    }
  }

  @Test
  void setFrom_emailThrowsEmailException_wrapped() throws Exception {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();
    Email email = mock(Email.class);

    doThrow(new EmailException("boom")).when(email).setFrom("bad@example.com");

    // when / then
    assertThatThrownBy(() -> behavior.setFrom(email, "bad@example.com"))
      .isInstanceOf(RuntimeException.class)
      .hasCauseInstanceOf(EmailException.class);
  }

  // --- Neue Tests ---

  @Test
  void createEmail_prefersHtmlWhenHtmlPresent() {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();

    // when
    Email email = behavior.createEmail("plain text", "<b>html</b>");

    // then
    assertThat(email).isInstanceOf(HtmlEmail.class);
  }

  @Test
  void createEmail_returnsTextOnlyWhenOnlyTextPresent() {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();

    // when
    Email email = behavior.createEmail("plain text", null);

    // then
    assertThat(email).isInstanceOf(SimpleEmail.class);
  }

  @Test
  void createEmail_withNoContent_throws() {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();

    // when / then
    assertThatThrownBy(() -> behavior.createEmail(null, null))
      .isInstanceOf(RuntimeException.class);
  }

  @Test
  void createHtmlEmail_returnsHtmlEmail() {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();

    // when
    Email email = behavior.createHtmlEmail("fallback text", "<p>html</p>");

    // then
    assertThat(email).isInstanceOf(HtmlEmail.class);
  }

  @Test
  void createTextOnlyEmail_returnsSimpleEmail() {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();

    // when
    Email email = behavior.createTextOnlyEmail("just text");

    // then
    assertThat(email).isInstanceOf(SimpleEmail.class);
  }

  @Test
  void setSubject_withExplicitSubject_setsIt() {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();
    Email email = mock(Email.class);

    // when
    behavior.setSubject(email, "My subject");

    // then
    verify(email).setSubject("My subject");
  }

  @Test
  void setSubject_withNull_setsEmptyString() {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();
    Email email = mock(Email.class);

    // when
    behavior.setSubject(email, null);

    // then
    verify(email).setSubject("");
  }

  @Test
  void setMailServerProperties_setsAllPropertiesFromProcessEngineConfig() {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();
    Email email = mock(Email.class);

    ProcessEngineConfigurationImpl config = mock(ProcessEngineConfigurationImpl.class);
    when(config.getMailServerHost()).thenReturn("smtp.example.com");
    when(config.getMailServerPort()).thenReturn(2525);
    when(config.getMailServerUseTLS()).thenReturn(true);
    when(config.getMailServerUsername()).thenReturn("user");
    when(config.getMailServerPassword()).thenReturn("pass");

    try (MockedStatic<Context> mocked = mockStatic(Context.class)) {
      mocked.when(Context::getProcessEngineConfiguration).thenReturn(config);

      // when
      behavior.setMailServerProperties(email);

      // then
      verify(email).setHostName("smtp.example.com");
      verify(email).setSmtpPort(2525);
      verify(email).setStartTLSEnabled(true);
      verify(email).setAuthentication("user", "pass");
    }
  }

  @Test
  void setCharset_whenCharsetExpressionNull_doesNotSetCharset() {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();
    Email email = mock(Email.class);
    behavior.charset = null; // explicit for clarity

    // when
    behavior.setCharset(email, "UTF-8");

    // then
    verifyNoMoreInteractions(email);
  }

  @Test
  void setCharset_whenCharsetExpressionPresent_setsCharset() {
    // given
    MailActivityBehavior behavior = new MailActivityBehavior();
    Email email = mock(Email.class);
    behavior.charset = mock(Expression.class); // non-null triggers charset setting

    // when
    behavior.setCharset(email, "UTF-8");

    // then
    verify(email).setCharset("UTF-8");
  }

}
