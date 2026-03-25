/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.android.settings.supervision

/**
 * Interface for providing supervisor account status.
 *
 * This interface defines a method for retrieving supervisor account status.
 * Implementations of this interface are responsible for fetching and returning a boolean value
 * indicating whether the current user is a supervisor account.
 */
interface IsSupervisorAccountProvider {
    /**
     * @return Whether the current user is a supervisor account
     */
    suspend fun getIsSupervisorAccount(): Boolean
}