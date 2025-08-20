This is a distribution of

       Operaton v${project.version}

visit
      https://docs.operaton.org/

Operaton is a Java-based framework licensed under the
Apache License 2.0 license.

License information can be found in the LICENSE file.

Operaton includes libraries developed by third
parties. For license and attribution notices for these libraries,
please refer to the documentation that accompanies this distribution
(see the LICENSE_BOOK-${project.version} file).

The packaged Wildfly Application Server is licensed under
the LGPL license.

==================

Contents:

  lib/
        This directory contains the java libraries for application 
        development.

  modules/
        This directory contains additional modules for Wildfly Application
        Server. You can use these modules to patch a vanilla distribution
        of Wildfly Application Server.

  server/
        This directory contains a preconfigured distribution 
        of Wildfly Application Server with Operaton readily
        installed.

        run the
          server/wildfly-${version.wildfly}/bin/standalone.{bat/sh}
        script to start up the the server.

        After starting the server, you can access the 
        following web applications:

        http://localhost:8080/operaton
        http://localhost:8080/engine-rest

    sql/
        This directory contains the create and upgrade sql script
        for the different databases.
        The engine create script contain the engine and history tables.

        Execute the current upgrade script to make the database compatible
        with the newest Operaton release.

==================

Operaton version: ${project.version}
Wildfly Application Server version: ${version.wildfly}

=================
