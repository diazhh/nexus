#
# Copyright © 2016-2026 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# ═══════════════════════════════════════════════════
# NEXUS Simulators - Multi-stage Docker Build
# ═══════════════════════════════════════════════════

# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# Copy workspace configuration
COPY package*.json ./
COPY tsconfig.json ./

# Copy all simulator packages
COPY shared/ ./shared/
COPY rv-simulator/ ./rv-simulator/
COPY ct-simulator/ ./ct-simulator/
COPY dr-simulator/ ./dr-simulator/

# Install dependencies (including dev dependencies for build)
RUN npm ci

# Build all packages
RUN npm run build -w shared
RUN npm run build -w rv-simulator
RUN npm run build -w ct-simulator
RUN npm run build -w dr-simulator

# Stage 2: Production Runtime
FROM node:20-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Copy workspace configuration
COPY tsconfig.json ./

# Install only production dependencies
RUN npm ci --production

# Copy shared module
COPY --from=builder /app/shared/dist ./shared/dist
COPY --from=builder /app/shared/package.json ./shared/

# Copy RV simulator
COPY --from=builder /app/rv-simulator/dist ./rv-simulator/dist
COPY --from=builder /app/rv-simulator/package.json ./rv-simulator/

# Copy CT simulator
COPY --from=builder /app/ct-simulator/dist ./ct-simulator/dist
COPY --from=builder /app/ct-simulator/package.json ./ct-simulator/

# Copy DR simulator
COPY --from=builder /app/dr-simulator/dist ./dr-simulator/dist
COPY --from=builder /app/dr-simulator/package.json ./dr-simulator/

# Create directory for configs
RUN mkdir -p /app/config

# Set default simulator (can be overridden)
ENV SIMULATOR=rv-simulator
ENV CONFIG_FILE=/app/config/config.yaml

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD node -e "require('net').connect({port:1883}, () => process.exit(0)).on('error', () => process.exit(1))" || exit 1

# Run the specified simulator
CMD node ${SIMULATOR}/dist/index.js

# Labels
LABEL org.opencontainers.image.title="NEXUS Telemetry Simulators"
LABEL org.opencontainers.image.description="IoT telemetry simulators for petroleum operations"
LABEL org.opencontainers.image.vendor="ThingsBoard"
LABEL org.opencontainers.image.version="1.0.0"
