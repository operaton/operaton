package org.operaton.bpm.engine.rest.security.auth.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.operaton.bpm.engine.rest.security.auth.impl.HttpBasicAuthenticationProvider.BASIC_AUTH_HEADER_PREFIX;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.security.auth.AuthenticationResult;

class HttpBasicAuthenticationProviderTest {

  private static final String USER_ID = "user";

  private static final String PASSWORD = "password";

  private static final String CREDENTIALS =
      Base64.getEncoder().encodeToString(String.format("%s:%s", USER_ID, PASSWORD).getBytes());

  private static final String AUTHORIZATION_HEADER = BASIC_AUTH_HEADER_PREFIX + CREDENTIALS;

  @Test
  void testExtractAuthenticatedUserNoAuthHeader() {
    HttpBasicAuthenticationProvider provider = new HttpBasicAuthenticationProvider();
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    ProcessEngine engine = Mockito.mock(ProcessEngine.class);
    Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
    AuthenticationResult result = provider.extractAuthenticatedUser(request, engine);
    assertFalse(result.isAuthenticated());
    assertNull(result.getAuthenticatedUser());
  }

  @Test
  void testExtractAuthenticatedUserHeaderNotBasic() {
    HttpBasicAuthenticationProvider provider = new HttpBasicAuthenticationProvider();
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    ProcessEngine engine = Mockito.mock(ProcessEngine.class);
    Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("NOTBASIC user:password");
    AuthenticationResult result = provider.extractAuthenticatedUser(request, engine);
    assertFalse(result.isAuthenticated());
    assertNull(result.getAuthenticatedUser());
  }

  @Test
  void testExtractAuthenticatedUserNoCredentials() {
    HttpBasicAuthenticationProvider provider = new HttpBasicAuthenticationProvider();
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    ProcessEngine engine = Mockito.mock(ProcessEngine.class);
    Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_AUTH_HEADER_PREFIX);
    AuthenticationResult result = provider.extractAuthenticatedUser(request, engine);
    assertFalse(result.isAuthenticated());
    assertNull(result.getAuthenticatedUser());
  }

  @Test
  void testExtractAuthenticatedUserValidCredentials() {
    HttpBasicAuthenticationProvider provider = new HttpBasicAuthenticationProvider();
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    ProcessEngine engine = Mockito.mock(ProcessEngine.class);
    IdentityService identityService = Mockito.mock(IdentityService.class);
    Mockito.when(identityService.checkPassword(USER_ID, PASSWORD)).thenReturn(true);
    Mockito.when(engine.getIdentityService()).thenReturn(identityService);
    Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(AUTHORIZATION_HEADER);
    AuthenticationResult result = provider.extractAuthenticatedUser(request, engine);
    assertTrue(result.isAuthenticated());
    assertEquals(USER_ID, result.getAuthenticatedUser());
  }

  @Test
  void testExtractAuthenticatedUserInvalidCredentials() {
    HttpBasicAuthenticationProvider provider = new HttpBasicAuthenticationProvider();
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    ProcessEngine engine = Mockito.mock(ProcessEngine.class);
    IdentityService identityService = Mockito.mock(IdentityService.class);
    Mockito.when(identityService.checkPassword(USER_ID, PASSWORD)).thenReturn(false);
    Mockito.when(engine.getIdentityService()).thenReturn(identityService);
    Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(AUTHORIZATION_HEADER);
    AuthenticationResult result = provider.extractAuthenticatedUser(request, engine);
    assertFalse(result.isAuthenticated());
    assertEquals(USER_ID, result.getAuthenticatedUser());
  }

  @Test
  void testExtractAuthenticatedUserInvalidBase64() {
    HttpBasicAuthenticationProvider provider = new HttpBasicAuthenticationProvider();
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    ProcessEngine engine = Mockito.mock(ProcessEngine.class);
    Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_AUTH_HEADER_PREFIX + "!!!invalidbase64!!!");
    AuthenticationResult result = provider.extractAuthenticatedUser(request, engine);
    assertFalse(result.isAuthenticated());
    assertNull(result.getAuthenticatedUser());
  }

  @Test
  void testAugmentResponseByAuthenticationChallengeSetsHeader() {
    HttpBasicAuthenticationProvider provider = new HttpBasicAuthenticationProvider();
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    ProcessEngine engine = Mockito.mock(ProcessEngine.class);
    Mockito.when(engine.getName()).thenReturn("testEngine");
    provider.augmentResponseByAuthenticationChallenge(response, engine);
    Mockito.verify(response).setHeader(HttpHeaders.WWW_AUTHENTICATE,
        BASIC_AUTH_HEADER_PREFIX + "realm=\"testEngine\"");
  }
}