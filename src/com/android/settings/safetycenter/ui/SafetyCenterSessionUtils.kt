/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.safetycenter.ui

import android.os.Bundle
import java.util.Random

/**
 * A utility object for managing session IDs for Safety Center interaction logging.
 *
 * A session ID is a unique identifier for a user's continuous interaction with the Safety Center
 * UI, from the moment they open it until they navigate away.
 */
object SafetyCenterSessionUtils {
    const val EXTRA_SESSION_ID = "com.android.settings.safetycenter.extra.SESSION_ID"
    const val INVALID_SESSION_ID = 0L

    private val random = Random()

    /**
     * Generates a new, random, positive session ID.
     *
     * This method ensures the generated ID is never equal to [INVALID_SESSION_ID].
     *
     * @return A valid, positive session ID.
     */
    fun generateValidSessionId(): Long {
        // Use a sequence to generate random positive longs until a valid one is found.
        return generateSequence { random.nextLong() and Long.MAX_VALUE }
            .first { it != INVALID_SESSION_ID }
    }

    /**
     * Creates a [Bundle] containing a session ID, useful for fragment arguments.
     *
     * @param sessionId An existing session ID to include in the bundle.
     * @return A [Bundle] containing the session ID under the key [EXTRA_SESSION_ID].
     * @throws IllegalArgumentException if the given sessionId is [INVALID_SESSION_ID].
     */
    fun createSessionArgs(sessionId: Long): Bundle {
        require(sessionId != INVALID_SESSION_ID) {
            "Cannot create session args with an invalid session ID."
        }
        return Bundle().apply { putLong(EXTRA_SESSION_ID, sessionId) }
    }

    /**
     * Retrieves a session ID from the fragment arguments, or generates a new one if not found.
     *
     * If a valid session ID is found in the [fragmentArgs] bundle, it will be returned. Otherwise,
     * a new session ID is generated and returned.
     *
     * @param fragmentArgs The Fragment's arguments bundle, which may contain the session ID.
     * @return A valid session ID.
     */
    fun getOrGenerateSessionId(fragmentArgs: Bundle?): Long {
        val sessionId =
            fragmentArgs?.getLong(EXTRA_SESSION_ID, INVALID_SESSION_ID) ?: INVALID_SESSION_ID

        return if (sessionId != INVALID_SESSION_ID) sessionId else generateValidSessionId()
    }
}
