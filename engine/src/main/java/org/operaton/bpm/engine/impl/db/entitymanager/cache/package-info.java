/**
 * First-level entity cache (identity map) for the current command execution.
 * Tracks loaded entities, detects changes, and coordinates dirty-checking during flush
 * via {@link org.operaton.bpm.engine.impl.db.entitymanager.cache.DbEntityCache}.
 */
package org.operaton.bpm.engine.impl.db.entitymanager.cache;
