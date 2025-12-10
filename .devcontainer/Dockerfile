# Dev container for FactoryFone Admin Portal
# Includes Node.js 22, Java 25, Python 3, and pnpm

FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

# Install Node.js 22
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g pnpm

# Install dependencies for SDKMAN and other tools
RUN apt-get update && apt-get install -y \
    curl \
    zip \
    unzip \
    python3 \
    python3-pip \
    python3-venv \
    python3-full \
    sqlite3 \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install SDKMAN and Java 25 as vscode user
USER vscode
ENV SDKMAN_DIR="/home/vscode/.sdkman"
RUN curl -s "https://get.sdkman.io" | bash \
    && bash -c "source $SDKMAN_DIR/bin/sdkman-init.sh && sdk install java 25-open"

# Set up Java environment
ENV JAVA_HOME="/home/vscode/.sdkman/candidates/java/current"
ENV PATH="${JAVA_HOME}/bin:${PATH}"
RUN echo 'export SDKMAN_DIR="$HOME/.sdkman"' >> ~/.bashrc \
    && echo '[[ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]] && source "$SDKMAN_DIR/bin/sdkman-init.sh"' >> ~/.bashrc

# Create gradle directory with correct permissions
RUN mkdir -p /home/vscode/.gradle

# Switch back to root for remaining setup
USER root

# Create Python virtual environment and install faker
RUN python3 -m venv /opt/venv \
    && /opt/venv/bin/pip install --no-cache-dir faker \
    && echo 'export PATH="/opt/venv/bin:$PATH"' >> /etc/bash.bashrc \
    && ln -sf /opt/venv/bin/python3 /usr/local/bin/python3 \
    && ln -sf /opt/venv/bin/python /usr/local/bin/python
ENV PATH="/opt/venv/bin:${PATH}"

# Set working directory
WORKDIR /workspaces
