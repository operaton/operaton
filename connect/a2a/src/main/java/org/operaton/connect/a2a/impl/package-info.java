/*
 * Copyright 2026 the Operaton contributors.
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
/**
 * Internal implementation of the A2A connector.
 *
 * <p>
 * All use of the A2A Java SDK is confined to {@link org.operaton.connect.a2a.impl.SdkA2aAgent} and
 * {@link org.operaton.connect.a2a.impl.TimeoutAwareA2AHttpClient}, behind the
 * {@link org.operaton.connect.a2a.impl.A2aAgent} interface, so that a protocol or SDK version bump touches
 * as few files as possible.
 * </p>
 */
package org.operaton.connect.a2a.impl;
