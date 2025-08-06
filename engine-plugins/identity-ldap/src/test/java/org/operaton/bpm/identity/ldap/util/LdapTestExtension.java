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
package org.operaton.bpm.identity.ldap.util;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Extension to configure and setup containerized LDAP test instance and populate it with data
 */
public class LdapTestExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String BASE_DN = "dc=operaton,dc=org";
    private static final String ADMIN_PASSWORD = "zePassword";
    private static final String ADMIN_DN = "cn=admin,dc=operaton,dc=org";
    private LdapTestContext ldapTestContextContext;

    private int additionalMumberOfUsers = 0;
    private int additionalMumberOfGroups = 0;
    private int additionalMumberOfRoles = 0;
    private boolean posixContext = false;

    private final GenericContainer<?> ldapContainer = new GenericContainer<>("osixia/openldap:latest")
            .withExposedPorts(389)
            .withEnv("LDAP_ORGANISATION", "Operaton")
            .withEnv("LDAP_DOMAIN", "operaton.org")
            .withEnv("LDAP_BASE_DN", BASE_DN)
            .withEnv("LDAP_ADMIN_PASSWORD",ADMIN_PASSWORD)
            // Activates "sssvlv" for server side sorting
            .withClasspathResourceMapping("ldif/01-enable-sssvlv.ldif",
                                         "/container/service/slapd/assets/config/bootstrap/ldif/custom/01-enable-sssvlv.ldif",
                                 BindMode.READ_ONLY)
            // Provides "ObjectClasses" with attributes and that can be sorted
            // (Basic cn, sn and mail do not have ordering active in this container)
            .withClasspathResourceMapping("ldif/02-enable-custom-attributes.ldif",
                                                  "/container/service/slapd/assets/config/bootstrap/ldif/custom/02-enable-custom-attributes.ldif",
                                          BindMode.READ_ONLY)
            .waitingFor(
                    Wait.forLogMessage(".* slapd starting.*\\n", 1)

            ).withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS));

    @Override
    public void afterAll(ExtensionContext context) {
        ldapContainer.stop();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        ldapContainer.start();
        LdapTestContext ctx;
        if(posixContext) {
            ctx = new LdapTestPosixContextImpl(ldapContainer.getHost(),
                    ldapContainer.getFirstMappedPort(),
                    BASE_DN,
                    ADMIN_PASSWORD);
        } else {
            ctx = new LdapTestContextImpl(ldapContainer.getHost(),
                    ldapContainer.getFirstMappedPort(),
                    BASE_DN,
                    ADMIN_PASSWORD);
        }
        ldapTestContextContext = ctx.withAdditionalUsers(additionalMumberOfUsers)
                .withAdditionalGroups(additionalMumberOfGroups)
                .withAdditionalRoles(additionalMumberOfRoles)
                .initialize();
    }

    /**
     * Injects LDAP instance access data to LdapIdentityProviderPlugin if it is available
     * @param peConfig current {@link ProcessEngineConfigurationImpl} configuration instance
     */
    public void injectLdapUrlIntoProcessEngineConfiguration(ProcessEngineConfigurationImpl peConfig) {
        for(var plugin : peConfig.getProcessEnginePlugins()) {
            if(plugin instanceof LdapIdentityProviderPlugin ldapPlugin) {
                ldapPlugin.setServerUrl( "ldap://" + ldapContainer.getHost() + ":" + ldapContainer.getFirstMappedPort());
                ldapPlugin.setManagerDn(ADMIN_DN);
                ldapPlugin.setManagerPassword(ADMIN_PASSWORD);
                ldapPlugin.setBaseDn(BASE_DN);
                break;
            }
        }
    }

    /**
     * Modifies number of additional users to add to LDAP test instance
     * @param numberOfAdditionalUsers <code>int</code> number of additional users to add
     * @return <code>this</code> itself
     */
    public LdapTestExtension withAdditionalNumberOfUsers(int numberOfAdditionalUsers) {
        this.additionalMumberOfUsers = numberOfAdditionalUsers;
        return this;
    }

    /**
     * Modifies number of additional groups to add to LDAP test instance
     * @param numberOfAdditionalGroups <code>int</code> number of additional groups to add
     * @return <code>this</code> itself
     */
    public LdapTestExtension withAdditionalNumberOfGroups(int numberOfAdditionalGroups) {
        this.additionalMumberOfGroups = numberOfAdditionalGroups;
        return this;
    }

    /**
     * Modifies number of additional roles to add to LDAP test instance
     * @param numberOfAdditionalRoles <code>int</code> number of additional roles to add
     * @return <code>this</code> itself
     */
    public LdapTestExtension withAdditionalNumberOfRoles(int numberOfAdditionalRoles) {
        this.additionalMumberOfRoles = numberOfAdditionalRoles;
        return this;
    }

    /**
     * Modifies if LDAP should be populated with posix groups
     * @return <code>this</code> itself
     */
    public LdapTestExtension withPosixContext() {
        this.posixContext = true;
        return this;
    }

    /**
     * Provides access to currently active {@link LdapTestContext}
     * @return currently active LDAP test context
     */
    public LdapTestContext getLdapTestContext() {
        return ldapTestContextContext;
    }
}
