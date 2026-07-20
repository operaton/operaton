package org.operaton.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.operaton.bpm.engine.IdentityService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Secures the REST API with Keycloak JWT authentication.
 * Extracts the user identity from the JWT and sets it on the Operaton IdentityService.
 */
@Configuration
@EnableWebSecurity
public class RestSecurityConfig {

    private final ApplicationContext applicationContext;

    public RestSecurityConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain restApiSecurity(HttpSecurity http) throws Exception {
        return http
            .securityMatcher(PathPatternRequestMatcher.withDefaults().matcher("/engine-rest/**"))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .addFilterBefore(new IdentityFilter(applicationContext), AuthorizationFilter.class)
            .build();
    }

    /**
     * Extracts the username from the JWT and sets the Operaton authentication context.
     * Looks up IdentityService lazily to avoid circular dependency during startup.
     */
    static class IdentityFilter implements Filter {

        private final ApplicationContext applicationContext;
        private IdentityService identityService;

        IdentityFilter(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        private IdentityService getIdentityService() {
            if (identityService == null) {
                identityService = applicationContext.getBean(IdentityService.class);
            }
            return identityService;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String userId = jwtAuth.getTokenAttributes()
                    .get("preferred_username").toString();

                IdentityService identity = getIdentityService();
                List<String> groupIds = new ArrayList<>();
                identity.createGroupQuery().groupMember(userId).list()
                    .forEach(g -> groupIds.add(g.getId()));

                try {
                    identity.setAuthentication(userId, groupIds);
                    chain.doFilter(request, response);
                } finally {
                    identity.clearAuthentication();
                }
            } else {
                chain.doFilter(request, response);
            }
        }
    }
}
