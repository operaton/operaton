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
package org.operaton.impl.test.utils.testcontainers;

import java.io.IOException;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;


/**
 * Class for setting up a MSSQLServer database and managing its lifecycle within the test environment. This class is a custom extension of Testcontainers' {@code MSSQLServerContainer
 * }, providing additional functionality for the Operaton project.
 */
public class OperatonMSSQLContainer extends MSSQLServerContainer {

    private static final String DATABASE_NAME = "operaton_test";

    public OperatonMSSQLContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    public OperatonMSSQLContainer(String dockerImageName) {
        super(dockerImageName);
    }

    /**
     * Hook to set up a database with correct transaction isolation after starting the container.
     * We override the parent's database creation to ensure READ_COMMITTED_SNAPSHOT is enabled.
     * Necessary, because the master DB of SQL server has some constraints and cannot be configured to enable {@code READ_COMMITTED_SNAPSHOT}.
     *
     * @param containerResponse Metadata of the started container
     */
    @Override
    protected void containerIsStarted(InspectContainerResponse containerResponse) {
        // Do NOT call super.containerIsStarted() because it creates the database without READ_COMMITTED_SNAPSHOT
        // Instead, we create the database ourselves with the correct settings
        try {
            String dbName = DATABASE_NAME;
            String password = getPassword();
            this.execInContainer("bash", "-c",
                ("echo \"IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = '%s') " +
                "BEGIN " +
                "  CREATE DATABASE %s COLLATE SQL_Latin1_General_CP1_CS_AS; " +
                "END; " +
                "ALTER DATABASE %s SET READ_COMMITTED_SNAPSHOT ON; " +
                "ALTER LOGIN sa WITH DEFAULT_DATABASE = %s\" | " +
                "/opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P %s -i /dev/stdin").formatted(
                    dbName, dbName, dbName, dbName, password));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
