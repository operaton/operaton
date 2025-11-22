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
package org.operaton.bpm.run.health;

import org.operaton.bpm.health.HealthResult;
import org.operaton.bpm.health.HealthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health endpoint for Operaton Run.
 * Provides a lightweight /health JSON suitable for load balancers and uptime checks
 *
 * @author <a href="mailto:tomnm77@gmail.com">Tomasz Korcz</a>
 */
@RestController
public class HealthController {

  private final HealthService healthService;

  public HealthController(HealthService healthService) {
    this.healthService = healthService;
  }

  @GetMapping(path = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> body = new LinkedHashMap<>();

    HealthResult result = healthService.check();
    body.put("status", result.status());
    body.put("timestamp", result.timestamp());
    if (result.version() != null) {
      body.put("version", result.version());
    }
    body.put("details", result.details());
    return ResponseEntity.ok()
      .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
      .body(body);
  }
}
