/**
 * Public API of the Operaton engine.<br/><br/>
 * Typical usage of the API starts by the creation of a {@link org.operaton.bpm.engine.ProcessEngineConfiguration}
 * (typically based on a configuration file), from which a {@link org.operaton.bpm.engine.ProcessEngine} can be obtained.<br/><br/>
 * <p>
 * Through the services obtained from such a {@link org.operaton.bpm.engine.ProcessEngine}, BPM and workflow operation
 * can be executed:<br/><br/>
 *
 * <b>{@link org.operaton.bpm.engine.RepositoryService}:</b>
 * Manages {@link org.operaton.bpm.engine.repository.Deployment}s<br/>
 *
 * <b>{@link org.operaton.bpm.engine.RuntimeService}:</b>
 * For starting and searching {@link org.operaton.bpm.engine.runtime.ProcessInstance}s<br/>
 *
 * <b>{@link org.operaton.bpm.engine.TaskService}:</b>
 * Exposes operations to manage human (standalone) {@link org.operaton.bpm.engine.task.Task}s,
 * such as claiming, completing and assigning tasks<br/>
 *
 * <b>{@link org.operaton.bpm.engine.IdentityService}:</b>
 * Used for managing {@link org.operaton.bpm.engine.identity.User}s,
 * {@link org.operaton.bpm.engine.identity.Group}s and the relations between them<br/>
 *
 * <b>{@link org.operaton.bpm.engine.ManagementService}:</b>
 * Exposes engine admin and maintenance operations,
 * which have no relation to the runtime execution of business processes<br/>
 *
 * <b>{@link org.operaton.bpm.engine.HistoryService}:</b>
 * Exposes information about ongoing and past process instances.<br/>
 *
 * <b>{@link org.operaton.bpm.engine.FormService}:</b>
 * Access to form data and rendered forms for starting new process instances and completing tasks.<br/>
 *
 * @since 1.0
 */
@NullMarked
package org.operaton.bpm.engine;

import org.jspecify.annotations.NullMarked;