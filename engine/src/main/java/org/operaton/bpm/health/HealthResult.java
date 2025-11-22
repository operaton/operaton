/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.health;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable value object describing Operaton health.
 *
 * @author <a href="mailto:tomnm77@gmail.com">Tomasz Korcz</a>
 */
public record HealthResult(String status, String timestamp, String version, Map<String, Object> details) {

    public HealthResult(String status, String timestamp, String version, Map<String, Object> details) {
        this.status = status;
        this.timestamp = timestamp != null ? timestamp : Instant.now().toString();
        this.version = version;
        this.details = details != null ? Collections.unmodifiableMap(new LinkedHashMap<>(details)) : Collections.emptyMap();
    }
}
