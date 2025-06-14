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
package org.operaton.bpm.engine.rest.mapper;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.mapper.MultipartFormData.FormPart;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
* <p>Provides a {@link MessageBodyReader} for {@link MultipartFormData}. This allows writing resources which
* consume {@link MediaType#MULTIPART_FORM_DATA} which is parsed into a {@link MultipartFormData} object:</p>
*
* <pre>
* {@literal @}POST
* {@literal @}Consumes(MediaType.MULTIPART_FORM_DATA)
* void handleMultipartPost(MultipartFormData multipartFormData);
* </pre>
*
* <p>The implementation used apache commons fileupload in order to parse the request and populate an instance of
* {@link MultipartFormData}.</p>
*
* @author Daniel Meyer
*/
@Provider
@Consumes(MediaType.MULTIPART_FORM_DATA)
public class MultipartPayloadProvider implements MessageBodyReader<MultipartFormData> {

  public static final String TYPE_NAME = "multipart";
  public static final String SUB_TYPE_NAME = "form-data";

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return TYPE_NAME.equals(mediaType.getType().toLowerCase())
        && SUB_TYPE_NAME.equals(mediaType.getSubtype().toLowerCase());
  }

  public MultipartFormData readFrom(Class<MultipartFormData> type, Type genericType, Annotation[] annotations, MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws WebApplicationException {

    final MultipartFormData multipartFormData = createMultipartFormDataInstance();
    final FileUpload fileUpload = createFileUploadInstance();

    String contentType = httpHeaders.getFirst("content-type");
    RestMultipartRequestContext requestContext = createRequestContext(entityStream, contentType);

    // parse the request (populates the multipartFormData)
    parseRequest(multipartFormData, fileUpload, requestContext);

    return multipartFormData;

  }

  protected FileUpload createFileUploadInstance() {
    return new FileUpload();
  }

  protected MultipartFormData createMultipartFormDataInstance() {
    return new MultipartFormData();
  }

  protected void parseRequest(MultipartFormData multipartFormData, FileUpload fileUpload, RestMultipartRequestContext requestContext) {
    try {
      FileItemIterator itemIterator = fileUpload.getItemIterator(requestContext);
      while (itemIterator.hasNext()) {
        FileItemStream stream = itemIterator.next();
        multipartFormData.addPart(new FormPart(stream));
      }
    } catch (Exception e) {
      throw new RestException(Status.BAD_REQUEST, e, "multipart/form-data cannot be processed");

    }
  }

  protected RestMultipartRequestContext createRequestContext(InputStream entityStream, String contentType) {
    return new RestMultipartRequestContext(entityStream, contentType);
  }

  /**
   * Exposes the REST request to commons fileupload
   *
   */
  static class RestMultipartRequestContext implements RequestContext {

    protected InputStream inputStream;
    protected String contentType;

    public RestMultipartRequestContext(InputStream inputStream, String contentType) {
      this.inputStream = inputStream;
      this.contentType = contentType;
    }

    @Override
    public String getCharacterEncoding() {
      return null;
    }

    @Override
    public String getContentType() {
      return contentType;
    }

    @Override
    public int getContentLength() {
      return -1;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return inputStream;
    }

  }

}
