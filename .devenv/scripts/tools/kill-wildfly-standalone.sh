# This script kills a running Wildfly server process.
# The script is used when the execution of the integration tests for the Wildfly distro
# has been manually interrupted. In that case a Wildfly server might still be running.
#
# The situation is reported by this error message:
# org.jboss.arquillian.container.spi.client.container.LifecycleException: The port 39990 is already in use. It means that either the server might be already running or there is another process using port 39990.

# Extract the PID
PID_TO_KILL=$(ps -ef | grep java | grep '[s]tandalone' | awk '{print $2}')

# Check if a PID was found and then kill the process
if [ -n "$PID_TO_KILL" ]; then
  echo "Found Wildfly server PID: $PID_TO_KILL. Terminating..."
  kill -9 $PID_TO_KILL
else
  echo "Wildfly process not found."
fi
