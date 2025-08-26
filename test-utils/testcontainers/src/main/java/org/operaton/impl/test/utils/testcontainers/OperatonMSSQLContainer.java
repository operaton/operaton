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
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;


/**
 * Class for setting up a MSSQLServer database and managing its lifecycle within the test environment. This class is a custom extension of Testcontainers' {@code MSSQLServerContainer
 * }, providing additional functionality for the Operaton project.
 */
public class OperatonMSSQLContainer<SELF extends MSSQLServerContainer<SELF>> extends MSSQLServerContainer<SELF> {

    public OperatonMSSQLContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    public OperatonMSSQLContainer(String dockerImageName) {
        super(dockerImageName);
    }

    /**
     * Hook to set up a database with correct transaction isolation after starting the container and setting it as a default.
     * Necessary, because the master DB of SQL server has some constraints and cannot be configured to enable {@code READ_COMMITTED_SNAPSHOT}.
     * Cannot be put into `OperatonMSSQLContainerProvider`, as this has no influence over the container after it was started
     *
     * @param containerResponse Metadata of the started container
     */
    @Override
    protected void containerIsStarted(InspectContainerResponse containerResponse) {
        super.containerIsStarted(containerResponse);
        try {
            this.execInContainer("bash", "-c", "echo \"create database operaton_test collate SQL_Latin1_General_CP1_CS_AS; alter database operaton_test set READ_COMMITTED_SNAPSHOT ON; alter login sa WITH DEFAULT_DATABASE = operaton_test\" | /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P A_Str0ng_Required_Password -i /dev/stdin");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
