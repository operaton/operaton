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

The packaged Apache Tomcat server is licensed under 
the Apache License v2.0 license.

==================

Contents:

  /
        The root directory contains two start scripts. One for Windows (.bat)
        and one for Linux/Mac (.sh). After executing it, you can access the 
        following web applications:

        webapps: http://localhost:8080/
        rest: http://localhost:8080/engine-rest/

  internal/
        This directory contains the Java application and optional components
        that Operaton Run consists of.

  configuration/
        This directory contains all resources to configure the distro.
        Find a detailed guide on how to use this directory on the following
        documentation pages:
        https://docs.operaton.org/manual/latest/installation/operaton-bpm-run/
        https://docs.operaton.org/manual/latest/user-guide/operaton-bpm-run/

==================

Operaton version: ${project.version}

=================
