/*
 * Copyright 2026 The Android Open Source Project
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

package com.android.settings.wifi.calling

import android.content.Context
import android.telephony.SubscriptionManager
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.metadata.preferencesapi.types.DirectFiniteOptionsType

/** A subscription ID. */
// This is only open to allow the companion object to be created. Do not subclass.
open class SubscriptionId(
    private val includeActive: Boolean = true,
    private val includeInactive: Boolean = false,
) : DirectFiniteOptionsType<Int> {
    init {
        require(includeActive && !includeInactive) {
            "SubscriptionId currently only supports active subscriptions."
        }
    }

    override fun getParametersSchema() = KeyParametersSchema {
        parameter("includeActive", "Whether to include active subscriptions.", type = AnyBoolean)
        parameter(
            "includeInactive",
            "Whether to include inactive subscriptions.",
            type = AnyBoolean,
        )
    }

    override fun getParameters() =
        getParametersSchema()
            .prepare(
                buildMap {
                    includeActive.let { put("includeActive", if (it) "true" else "false") }
                    includeInactive.let { put("includeInactive", if (it) "true" else "false") }
                }
            )

    override fun getType(): Class<Int> = Int::class.java

    override fun getDescription(context: Context): String = "An ID of a network subscription"

    override fun getKey(): String = "SubscriptionId:${includeActive}:${includeInactive}"

    override suspend fun getOptions(context: Context): List<Pair<Int, String>> {
        return try {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)

            subscriptionManager.activeSubscriptionInfoList
                ?.map { it.subscriptionId to it.displayName.toString() }
                ?.toList() ?: emptyList()
        } catch (e: UnsupportedOperationException) {
            // Do not support telephony subscriptions
            emptyList()
        }
    }

    companion object : SubscriptionId()
}
