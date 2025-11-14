@echo off

IF [%~1]==[--version] GOTO Version

SET "BASEDIR=%~dp0"
SET "CATALINA_HOME=%BASEDIR%\server\apache-tomcat-${version.tomcat}"
SET "EXECUTABLE=%BASEDIR%server\apache-tomcat-${version.tomcat}\bin\startup.bat"

echo "starting Operaton ${project.version} on Apache Tomcat ${version.tomcat}"

start %EXECUTABLE%

ping -n 5 localhost > NULL
start http://localhost:8080/operaton-welcome/index.html

GOTO Done

:Version
ECHO Operaton: ${project.version} (Tomcat ${version.tomcat})

:Done
