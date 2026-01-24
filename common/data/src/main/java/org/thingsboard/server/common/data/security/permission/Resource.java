/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.security.permission;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resource types for permission management")
public enum Resource {
    ALL,
    DEVICE,
    ASSET,
    DASHBOARD,
    USER,
    CUSTOMER,
    ALARM,
    RULE_CHAIN,
    ENTITY_VIEW,
    WIDGET_TYPE,
    WIDGETS_BUNDLE,
    TENANT,
    TENANT_PROFILE,
    DEVICE_PROFILE,
    ASSET_PROFILE,
    TB_RESOURCE,
    OTA_PACKAGE,
    EDGE,
    RPC,
    QUEUE,
    NOTIFICATION,
    NOTIFICATION_TARGET,
    NOTIFICATION_TEMPLATE,
    NOTIFICATION_RULE,
    OAUTH2_CLIENT,
    DOMAIN,
    MOBILE_APP,
    ADMIN_SETTINGS,
    AI_MODEL,
    API_KEY,
    ROLE
}
