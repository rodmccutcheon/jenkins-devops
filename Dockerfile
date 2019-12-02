FROM jenkins/jenkins:2.206

USER root

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y sudo libltdl-dev \
    && rm -rf /var/lib/apt/lists/*

USER jenkins

ENV JENKINS_CONFIG_PATH /var/jenkins_home/init.groovy.d/config

# Disable the setup wizard - we're going to configure Jenkins programmatically
ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false

# Install plugins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

# Copy configuration scripts that will be executed by groovy
COPY scripts/*.groovy /usr/share/jenkins/ref/init.groovy.d/

# Jenkins home directory is a volume, so configuration and build history
# can be persisted and survive image upgrades
VOLUME /var/jenkins_home
