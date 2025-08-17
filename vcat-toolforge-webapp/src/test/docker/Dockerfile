ARG image
ARG java_version

FROM eclipse-temurin:${java_version}-jdk AS jdk

FROM ${image}

SHELL ["/bin/bash", "-c"]

# create $TOOL_DATA_DIR
ENV TOOL_DATA_DIR=/tmp/tool-data-dir/
RUN mkdir -p ${TOOL_DATA_DIR}

# install packages defined in Aptfile
ARG apt_packages
RUN apt-get update \
 && apt-get install -y ${apt_packages} \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# copy Java from JDK docker image
COPY --from=jdk /opt/java/openjdk /opt/java/openjdk

# copy JAR to the image
COPY target/vcat-toolforge-runner.jar /

CMD ["/opt/java/openjdk/bin/java", "-jar", "/vcat-toolforge-runner.jar"]
