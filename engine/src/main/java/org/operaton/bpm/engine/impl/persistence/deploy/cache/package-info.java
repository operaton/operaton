/**
 * In-memory cache of deployed process, case, and decision definitions and their parsed models.
 * Provides fast lookup and prevents repeated deserialization of BPMN/CMMN/DMN resources.
 */
@NullMarked package org.operaton.bpm.engine.impl.persistence.deploy.cache;

import org.jspecify.annotations.NullMarked;