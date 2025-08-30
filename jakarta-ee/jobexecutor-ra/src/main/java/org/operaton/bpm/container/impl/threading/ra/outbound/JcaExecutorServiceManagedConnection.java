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
package org.operaton.bpm.container.impl.threading.ra.outbound;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;

import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.operaton.bpm.container.ExecutorService;
import org.operaton.bpm.container.impl.threading.ra.JcaExecutorServiceConnector;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;


/**
 *
 * @author Daniel Meyer
 *
 */
public class JcaExecutorServiceManagedConnection implements ManagedConnection {

  protected PrintWriter logwriter;

  protected JcaExecutorServiceManagedConnectionFactory mcf;
  protected List<ConnectionEventListener> listeners;
  protected JcaExecutorServiceConnectionImpl connection;

  protected ExecutorService delegate;

  public JcaExecutorServiceManagedConnection() {
  }

  public JcaExecutorServiceManagedConnection(JcaExecutorServiceManagedConnectionFactory mcf) {
    this.mcf = mcf;
    this.logwriter = null;
    this.listeners = Collections.synchronizedList(new ArrayList<ConnectionEventListener>(1));
    this.connection = null;
    JcaExecutorServiceConnector ra = (JcaExecutorServiceConnector) mcf.getResourceAdapter();
    delegate = (ExecutorService) ra.getExecutorServiceWrapper().getExecutorService();
  }

  @Override
  public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
    connection = new JcaExecutorServiceConnectionImpl(this, mcf);
    return connection;
  }

  @Override
  public void associateConnection(Object connection) throws ResourceException {
    if (connection == null) {
      throw new ResourceException("Null connection handle");
    }
    if (!(connection instanceof JcaExecutorServiceConnectionImpl)) {
      throw new ResourceException("Wrong connection handle");
    }
    this.connection = (JcaExecutorServiceConnectionImpl) connection;
  }

  @Override
  public void cleanup() throws ResourceException {
    // no-op
  }

  @Override
  public void destroy() throws ResourceException {
    // no-op
  }

  @Override
  public void addConnectionEventListener(ConnectionEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener is null");
    }
    listeners.add(listener);
  }

  @Override
  public void removeConnectionEventListener(ConnectionEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener is null");
    }
    listeners.remove(listener);
  }

  void closeHandle(JcaExecutorServiceConnection handle) {
    ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
    event.setConnectionHandle(handle);
    for (ConnectionEventListener cel : listeners) {
      cel.connectionClosed(event);
    }

  }

  @Override
  public PrintWriter getLogWriter() throws ResourceException {
    return logwriter;
  }

  @Override
  public void setLogWriter(PrintWriter out) throws ResourceException {
    logwriter = out;
  }

  @Override
  public LocalTransaction getLocalTransaction() throws ResourceException {
    throw new NotSupportedException("LocalTransaction not supported");
  }

  @Override
  public XAResource getXAResource() throws ResourceException {
    throw new NotSupportedException("GetXAResource not supported not supported");
  }

  @Override
  public ManagedConnectionMetaData getMetaData() throws ResourceException {
    return null;
  }

  // delegate methods /////////////////////////////////////////

  public boolean schedule(Runnable runnable, boolean isLongRunning) {
    return delegate.schedule(runnable, isLongRunning);
  }

  public Runnable getExecuteJobsRunnable(List<String> jobIds, ProcessEngineImpl processEngine) {
    return delegate.getExecuteJobsRunnable(jobIds, processEngine);
  }

}
