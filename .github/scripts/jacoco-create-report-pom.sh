#!/usr/bin/env bash
TARGET_FILE=$1
if [ -z "$TARGET_FILE" ]; then
  echo "Usage: $0 <target-file>"
  exit 1
fi

VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout | tail -n 1)

cat <<EOF > $TARGET_FILE
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.operaton.bpm</groupId>
  <artifactId>jacoco-aggregate-report</artifactId>
  <version>${VERSION}</version>
  <dependencies>
EOF

find model-api -type d | while read -r dir; do
  if [[ -d "$dir/src/test/java" || -d "$dir/target/generated-test-sources/java" ]]; then
    echo "Processing $dir"

    GROUP_ID=$(xq -e /project/groupId $dir/pom.xml)
    if [ -z "$GROUP_ID" ]; then
      GROUP_ID=$(xq -e /project/parent/groupId $dir/pom.xml)
    fi
    ARTIFACT_ID=$(xq -e /project/artifactId $dir/pom.xml)

    cat <<EOF >> $TARGET_FILE
    <dependency>
      <groupId>${GROUP_ID}</groupId>
      <artifactId>${ARTIFACT_ID}</artifactId>
      <version>${VERSION}</version>
    </dependency>
EOF
  fi
done

cat <<EOF >> $TARGET_FILE
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.8</version>
        <executions>
          <execution>
            <id>report-aggregate</id>
            <phase>verify</phase>
            <goals>
              <goal>report-aggregate</goal>
            </goals>
              <configuration>
                <dataFileIncludes>
                    <dataFileInclude>**/jacoco.exec</dataFileInclude>
                </dataFileIncludes>
                <outputDirectory>\${project.reporting.outputDirectory}/jacoco-aggregate</outputDirectory>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
EOF
