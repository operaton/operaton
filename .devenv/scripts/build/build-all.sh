pushd $(pwd)
cd $(git rev-parse --show-toplevel) || exit 1
./mvnw -Pdistro,distro-run,distro-tomcat,distro-wildfly,distro-webjar,distro-starter,distro-serverless,h2-in-memory -fae clean install
popd
