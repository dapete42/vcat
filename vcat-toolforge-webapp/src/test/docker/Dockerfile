ARG image
FROM ${image}

SHELL ["/bin/bash", "-c"]

ARG apt_packages
# install required packages for SDKMAN! and those defined in Aptfile
RUN apt-get update \
 && apt-get install -y curl unzip zip ${apt_packages} \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# install Java using SDKMAN!
ARG java_version
RUN curl -s "https://get.sdkman.io" | bash \
 && source /root/.sdkman/bin/sdkman-init.sh \
 && sdk install java ${java_version}-tem \
# links java binary so it is available
 && ln -s /root/.sdkman/candidates/java/current/bin/java /usr/local/bin

# copy JAR to the image
COPY target/vcat-toolforge-runner.jar /

# create $TOOL_DATA_DIR
ENV TOOL_DATA_DIR=/tool-data-dir/
RUN mkdir -p ${TOOL_DATA_DIR}

CMD ["/usr/local/bin/java", "-jar", "/vcat-toolforge-runner.jar"]
