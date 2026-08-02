FROM node:20

ARG TZ
ENV TZ="$TZ"

ARG CLAUDE_CODE_VERSION=latest

# Install basic development tools and iptables/ipset
RUN apt-get update && apt-get install -y --no-install-recommends \
  less \
  git \
  procps \
  sudo \
  fzf \
  zsh \
  man-db \
  unzip \
  gnupg2 \
  gh \
  iptables \
  ipset \
  iproute2 \
  dnsutils \
  aggregate \
  jq \
  nano \
  vim \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install Eclipse Temurin JDK 25 and Maven
RUN wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /usr/share/keyrings/adoptium.gpg && \
  echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print $2}' /etc/os-release) main" > /etc/apt/sources.list.d/adoptium.list && \
  apt-get update && apt-get install -y --no-install-recommends temurin-25-jdk maven && \
  ln -s /usr/lib/jvm/temurin-25-jdk-* /opt/java && \
  apt-get clean && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /etc/zsh && \
      echo "export JAVA_HOME=/opt/java" >> /etc/zsh/zshenv && \
      echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /etc/zsh/zshenv && \
      echo "export JAVA_HOME=/opt/java" > /etc/profile.d/java-home.sh && \
      echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /etc/profile.d/java-home.sh && \
      chmod +x /etc/profile.d/java-home.sh && \
      echo "export JAVA_HOME=/opt/java" >> /etc/bash.bashrc && \
      echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /etc/bash.bashrc

ENV JAVA_HOME=/opt/java
ENV PATH=$JAVA_HOME/bin:$PATH

# Ensure default node user has access to /usr/local/share
RUN mkdir -p /usr/local/share/npm-global && \
  chown -R node:node /usr/local/share

ARG USERNAME=node

# Persist bash history.
RUN SNIPPET="export PROMPT_COMMAND='history -a' && export HISTFILE=/commandhistory/.bash_history" \
  && mkdir /commandhistory \
  && touch /commandhistory/.bash_history \
  && chown -R $USERNAME /commandhistory

# Set `DEVCONTAINER` environment variable to help with orientation
ENV DEVCONTAINER=true

# Create workspace, config and maven repository directories and set permissions
RUN mkdir -p /workspace /home/node/.claude /home/node/.m2 && \
  chown -R node:node /workspace /home/node/.claude /home/node/.m2

WORKDIR /workspace

ARG GIT_DELTA_VERSION=0.18.2
RUN ARCH=$(dpkg --print-architecture) && \
  wget "https://github.com/dandavison/delta/releases/download/${GIT_DELTA_VERSION}/git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb" && \
  sudo dpkg -i "git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb" && \
  rm "git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb"

# Set up non-root user
USER node

# Install global packages
ENV NPM_CONFIG_PREFIX=/usr/local/share/npm-global
ENV PATH=$PATH:/usr/local/share/npm-global/bin

# Set the default shell to zsh rather than sh
ENV SHELL=/bin/zsh

# Set the default editor and visual
ENV EDITOR=nano
ENV VISUAL=nano

# Install Claude
RUN npm install -g @anthropic-ai/claude-code@${CLAUDE_CODE_VERSION}


# Copy and set up firewall script
COPY init-firewall.sh /usr/local/bin/
USER root
RUN chmod +x /usr/local/bin/init-firewall.sh && \
  echo "node ALL=(root) NOPASSWD: /usr/local/bin/init-firewall.sh" > /etc/sudoers.d/node-firewall && \
  chmod 0440 /etc/sudoers.d/node-firewall
USER node
