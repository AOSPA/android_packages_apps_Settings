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
package com.android.settings.supervision.ipc

import android.os.Bundle
import com.android.settingslib.ipc.ApiDescriptor
import com.android.settingslib.ipc.MessageCodec

class IsSupervisorAccountApi : ApiDescriptor<Unit, Boolean> {
    override val id: Int
        get() = 3

    override val requestCodec: MessageCodec<Unit> =
        object : MessageCodec<Unit> {
            override fun encode(data: Unit) = Bundle()

            override fun decode(data: Bundle) = Unit
        }

    override val responseCodec: MessageCodec<Boolean> =
        object : MessageCodec<Boolean> {
            override fun encode(data: Boolean) =
                Bundle().apply {
                    putBoolean(IS_SUPERVISOR_ACCOUNT, data)
                }

            override fun decode(data: Bundle): Boolean {
              return data.getBoolean(IS_SUPERVISOR_ACCOUNT, false)
            }
        }

    private companion object {
        const val IS_SUPERVISOR_ACCOUNT = "is_supervisor_account"
    }
}