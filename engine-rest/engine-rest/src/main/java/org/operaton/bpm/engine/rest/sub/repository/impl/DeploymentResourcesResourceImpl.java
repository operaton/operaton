/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.rest.sub.repository.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.Resource;
import org.operaton.bpm.engine.rest.dto.repository.DeploymentResourceDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.repository.DeploymentResourcesResource;
import org.operaton.bpm.engine.rest.util.URLEncodingUtil;

/**
 * @author Sebastian Menski
 */
public class DeploymentResourcesResourceImpl implements DeploymentResourcesResource {

  protected static final Map<String, String> MEDIA_TYPE_MAPPING = new HashMap<>();

  private static final String MEDIA_TYPE_IMAGE_GIF = "image/gif";
  private static final String MEDIA_TYPE_IMAGE_JPEG = "image/jpeg";
  private static final String MEDIA_TYPE_IMAGE_PNG = "image/png";
  private static final String MEDIA_TYPE_IMAGE_TIFF = "image/tiff";

  private static final String MEDIA_TYPE_IMAGE_SVG_XML = "image/svg+xml";

  static {
    MEDIA_TYPE_MAPPING.put("bpmn", MediaType.APPLICATION_XML);
    MEDIA_TYPE_MAPPING.put("cmmn", MediaType.APPLICATION_XML);
    MEDIA_TYPE_MAPPING.put("dmn", MediaType.APPLICATION_XML);
    MEDIA_TYPE_MAPPING.put("json", MediaType.APPLICATION_JSON);
    MEDIA_TYPE_MAPPING.put("xml", MediaType.APPLICATION_XML);

    MEDIA_TYPE_MAPPING.put("gif", MEDIA_TYPE_IMAGE_GIF);
    MEDIA_TYPE_MAPPING.put("jpeg", MEDIA_TYPE_IMAGE_JPEG);
    MEDIA_TYPE_MAPPING.put("jpe", MEDIA_TYPE_IMAGE_JPEG);
    MEDIA_TYPE_MAPPING.put("jpg", MEDIA_TYPE_IMAGE_JPEG);
    MEDIA_TYPE_MAPPING.put("png", MEDIA_TYPE_IMAGE_PNG);
    MEDIA_TYPE_MAPPING.put("svg", MEDIA_TYPE_IMAGE_SVG_XML);
    MEDIA_TYPE_MAPPING.put("tiff", MEDIA_TYPE_IMAGE_TIFF);
    MEDIA_TYPE_MAPPING.put("tif", MEDIA_TYPE_IMAGE_TIFF);

    MEDIA_TYPE_MAPPING.put("groovy", MediaType.TEXT_PLAIN);
    MEDIA_TYPE_MAPPING.put("java", MediaType.TEXT_PLAIN);
    MEDIA_TYPE_MAPPING.put("js", MediaType.TEXT_PLAIN);
    MEDIA_TYPE_MAPPING.put("php", MediaType.TEXT_PLAIN);
    MEDIA_TYPE_MAPPING.put("py", MediaType.TEXT_PLAIN);
    MEDIA_TYPE_MAPPING.put("rb", MediaType.TEXT_PLAIN);

    MEDIA_TYPE_MAPPING.put("html", MediaType.TEXT_HTML);
    MEDIA_TYPE_MAPPING.put("txt", MediaType.TEXT_PLAIN);
  }

  protected final ProcessEngine engine;
  protected final String deploymentId;

  public DeploymentResourcesResourceImpl(ProcessEngine engine, String deploymentId) {
    this.engine = engine;
    this.deploymentId = deploymentId;
  }

  @Override
  public List<DeploymentResourceDto> getDeploymentResources() {
    List<Resource> resources = engine.getRepositoryService().getDeploymentResources(deploymentId);

    List<DeploymentResourceDto> deploymentResources = new ArrayList<>();
    for (Resource resource : resources) {
      deploymentResources.add(DeploymentResourceDto.fromResources(resource));
    }

    if (!deploymentResources.isEmpty()) {
      return deploymentResources;
    }
    else {
      throw new InvalidRequestException(Status.NOT_FOUND,
        "Deployment resources for deployment id '%s' do not exist.".formatted(deploymentId));
    }
  }

  @Override
  public DeploymentResourceDto getDeploymentResource(String resourceId) {
    List<DeploymentResourceDto> deploymentResources = getDeploymentResources();
    for (DeploymentResourceDto deploymentResource : deploymentResources) {
      if (deploymentResource.getId().equals(resourceId)) {
        return deploymentResource;
      }
    }

    throw new InvalidRequestException(Status.NOT_FOUND,
      "Deployment resource with resource id '%s' for deployment id '%s' does not exist.".formatted(resourceId, deploymentId));
  }

  @Override
  public Response getDeploymentResourceData(String resourceId) {
    RepositoryService repositoryService = engine.getRepositoryService();
    InputStream resourceAsStream = repositoryService.getResourceAsStreamById(deploymentId, resourceId);

    if (resourceAsStream == null) {
      throw new InvalidRequestException(Status.NOT_FOUND,
          "Deployment resource '%s' for deployment id '%s' does not exist.".formatted(resourceId, deploymentId));
    }

    DeploymentResourceDto resource = getDeploymentResource(resourceId);
    String name = resource.getName();
    String filename = null;
    String mediaType = null;

    if (name != null) {
      name = name.replace("\\", "/");
      String[] filenameParts = name.split("/");
      if (filenameParts.length > 0) {
        int idx = filenameParts.length-1;
        filename = filenameParts[idx];
      }

      String[] extensionParts = name.split("\\.");
      if (extensionParts.length > 0) {
        int idx = extensionParts.length-1;
        String extension = extensionParts[idx];
        if (extension != null) {
          mediaType = MEDIA_TYPE_MAPPING.get(extension);
        }
      }
    }

    if (filename == null) {
      filename = "data";
    }

    if (mediaType == null) {
      mediaType = MediaType.APPLICATION_OCTET_STREAM;
    }

    return Response
        .ok(resourceAsStream, mediaType)
        .header("Content-Disposition", URLEncodingUtil.buildAttachmentValue(filename))
        .build();
  }
}
