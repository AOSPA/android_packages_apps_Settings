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

package com.android.settings.wifi.repository

import android.content.Context
import android.content.pm.PackageManager
import android.net.NetworkPolicyManager
import android.os.Process
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Repository for managing application data usage policies and restrictions. */
open class DataUsageRepository(context: Context) {

    protected val appContext: Context = context.applicationContext
    protected val packageManager: PackageManager = appContext.packageManager
    protected val networkPolicyManager: NetworkPolicyManager? =
        appContext.getSystemService(NetworkPolicyManager::class.java)

    /**
     * Gets the UID for the given package name.
     *
     * @param packageName The name of the package.
     * @return The UID of the package, or [Process.INVALID_UID] if not found.
     */
    open suspend fun getPackageUid(packageName: String): Int =
        withContext(Dispatchers.IO) {
            try {
                packageManager.getPackageUid(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "Package not found: $packageName")
                Process.INVALID_UID
            }
        }

    /**
     * Returns whether background data is restricted for the package name.
     *
     * @param packageName The name of the package.
     * @return True if background data is restricted, false otherwise.
     */
    open suspend fun isPolicyReject(packageName: String): Boolean =
        isPolicyReject(getPackageUid(packageName))

    /**
     * Returns whether background data is restricted for the UID.
     *
     * @param uid The UID of the package.
     * @return True if background data is restricted, false otherwise.
     */
    open fun isPolicyReject(uid: Int): Boolean = (getPolicy(uid) and POLICY_REJECT) != 0

    /**
     * Updates the background data restricted policy for the package name.
     *
     * @param packageName The name of the package.
     * @param reject True to restrict background data, false to allow it.
     */
    open suspend fun setPolicyReject(packageName: String, reject: Boolean) =
        setPolicyReject(getPackageUid(packageName), reject)

    /**
     * Updates the background data restricted policy for the UID.
     *
     * @param uid The UID of the package.
     * @param reject True to restrict background data, false to allow it.
     */
    open suspend fun setPolicyReject(uid: Int, reject: Boolean) {
        val policy = if (reject) POLICY_REJECT else NetworkPolicyManager.POLICY_NONE
        setPolicy(uid, policy)
    }

    /**
     * Checks if unrestricted mobile data usage can be configured for the package name.
     *
     * @param packageName The name of the package.
     * @return True if available, false otherwise.
     */
    open suspend fun isPolicyAllowAvailable(packageName: String): Boolean =
        isPolicyAllowAvailable(getPackageUid(packageName))

    /**
     * Checks if unrestricted mobile data usage can be configured for the UID.
     *
     * @param uid The UID of the package.
     * @return True if available, false otherwise.
     */
    open fun isPolicyAllowAvailable(uid: Int): Boolean = !isPolicyReject(uid)

    /**
     * Returns whether the package is allowed to use unrestricted mobile data for the package name.
     *
     * @param packageName The name of the package.
     * @return True if unrestricted data is allowed, false otherwise.
     */
    open suspend fun isPolicyAllow(packageName: String): Boolean =
        isPolicyAllow(getPackageUid(packageName))

    /**
     * Returns whether the UID is allowed to use unrestricted mobile data for the UID.
     *
     * @param uid The UID of the package.
     * @return True if unrestricted data is allowed, false otherwise.
     */
    open fun isPolicyAllow(uid: Int): Boolean {
        if (!isPolicyAllowAvailable(uid)) return false
        return (getPolicy(uid) and POLICY_ALLOW) != 0
    }

    /**
     * Updates the unrestricted mobile data usage policy for the package name.
     *
     * @param packageName The name of the package.
     * @param allow True to allow unrestricted usage, false to disable.
     */
    open suspend fun setPolicyAllow(packageName: String, allow: Boolean) =
        setPolicyAllow(getPackageUid(packageName), allow)

    /**
     * Updates the unrestricted mobile data usage policy for the UID.
     *
     * @param uid The UID of the package.
     * @param allow True to allow unrestricted usage, false to disable.
     */
    open suspend fun setPolicyAllow(uid: Int, allow: Boolean) {
        if (!isPolicyAllowAvailable(uid)) {
            Log.w(TAG, "setPolicyAllow: Feature unavailable")
            return
        }
        val policy = if (allow) POLICY_ALLOW else NetworkPolicyManager.POLICY_NONE
        setPolicy(uid, policy)
    }

    /**
     * Internal helper to retrieve UID policy.
     *
     * @param uid The UID of the package.
     * @return The policy bitmask for the UID.
     */
    open fun getPolicy(uid: Int): Int {
        if (uid == Process.INVALID_UID) return NetworkPolicyManager.POLICY_NONE
        val policy = networkPolicyManager?.getUidPolicy(uid) ?: NetworkPolicyManager.POLICY_NONE
        Log.d(TAG, "getPolicy: $policy")
        return policy
    }

    /**
     * Internal helper to update UID policy using setUidPolicy.
     *
     * @param uid The target application UID.
     * @param policy The finalized policy value.
     */
    open suspend fun setPolicy(uid: Int, policy: Int) =
        withContext(Dispatchers.IO) {
            if (uid == Process.INVALID_UID) return@withContext
            Log.d(TAG, "setPolicy: $policy")
            networkPolicyManager?.setUidPolicy(uid, policy)
        }

    companion object {
        private const val TAG = "DataUsageRepository"
        private const val POLICY_REJECT = NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND
        private const val POLICY_ALLOW = NetworkPolicyManager.POLICY_ALLOW_METERED_BACKGROUND
    }
}
