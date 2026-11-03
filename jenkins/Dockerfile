FROM jenkins/jenkins:lts-jdk17

USER root

# docker-cli + compose plugin (repositorio oficial), Maven, curl, Node 20 e libs do Cypress
RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl gnupg maven \
      libgtk2.0-0 libgtk-3-0 libgbm-dev libnotify-dev libnss3 libxss1 \
      libasound2 libxtst6 xauth xvfb \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
       > /etc/apt/sources.list.d/docker.list \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends docker-ce-cli docker-compose-plugin nodejs \
    && rm -rf /var/lib/apt/lists/*

USER jenkins

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
