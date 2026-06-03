/**
 * External task API for decoupled service task execution.
 * Workers fetch and lock tasks via {@link org.operaton.bpm.engine.externaltask.ExternalTaskQueryBuilder}, execute work externally, then report completion or failure.
 */
package org.operaton.bpm.engine.externaltask;
