/*
 * Copyright (C) 2026 The Android Open Source Project
 * ...
 */
package com.android.settings.spa.app.catalyst

import android.app.AppOpsManager
import android.app.Application
import android.apphibernation.AppHibernationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.os.UserHandle
import android.permission.PermissionControllerManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.DeviceConfig
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow.extract
import org.robolectric.shadows.ShadowContextImpl

@RunWith(AndroidJUnit4::class)
class AppInfoScreenApiFirstTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var context: Context
    private lateinit var tester: ApiTester

    private val mockAppOpsManager = mock<AppOpsManager>()
    private val mockAppHibernationManager = mock<AppHibernationManager>()
    private val mockPermissionControllerManager = mock<PermissionControllerManager>()

    private val validPackageName = "com.test.package"
    private val validUid = 12345

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val shadowContext = extract<ShadowContextImpl>(app.baseContext)
        shadowContext.setSystemService(Context.APP_OPS_SERVICE, mockAppOpsManager)
        shadowContext.setSystemService("app_hibernation", mockAppHibernationManager)
        shadowContext.setSystemService("permission_controller", mockPermissionControllerManager)

        context =
            object : ContextWrapper(app) {
                override fun createContextAsUser(user: UserHandle, flags: Int): Context {
                    return this
                }
            }

        tester = ApiTester(AppInfoScreenApiFirst(), context)

        val shadowPackageManager = shadowOf(context.packageManager)
        val packageInfo =
            PackageInfo().apply {
                packageName = validPackageName
                applicationInfo =
                    ApplicationInfo().apply {
                        packageName = validPackageName
                        uid = validUid
                        targetSdkVersion = Build.VERSION_CODES.S
                    }
            }
        shadowPackageManager.installPackage(packageInfo)

        DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_APP_HIBERNATION,
            "app_hibernation_enabled",
            "true",
            false,
        )

        tester.initializeScreenParameters(
            Parameters(AppInfoScreenApiFirst.PARAM_PACKAGE to validPackageName)
        )
        setupHibernationEligibility(PermissionControllerManager.HIBERNATION_ELIGIBILITY_ELIGIBLE)
    }

    @After
    fun cleanUp() {
        DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_APP_HIBERNATION,
            "app_hibernation_enabled",
            "true",
            false,
        )
    }

    private fun setupHibernationEligibility(eligibility: Int) {
        doAnswer { invocation ->
                val callback = invocation.getArgument<java.util.function.IntConsumer>(2)
                callback.accept(eligibility)
                null
            }
            .`when`(mockPermissionControllerManager)
            .getHibernationEligibility(anyString(), any(Executor::class.java), any())
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun get_featureNotEnabled_throwsCustomPrecondition() {
        DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_APP_HIBERNATION,
            "app_hibernation_enabled",
            "false",
            false,
        )

        assertFailsWith<FailedPreconditionException> { tester.get<Boolean>("unused_apps_switch") }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun get_appIsArchived_throwsCustomPrecondition() {
        val shadowPackageManager = shadowOf(context.packageManager)
        val packageInfo =
            PackageInfo().apply {
                packageName = validPackageName
                applicationInfo =
                    ApplicationInfo().apply {
                        packageName = validPackageName
                        uid = validUid
                        targetSdkVersion = Build.VERSION_CODES.S
                        isArchived = true
                    }
            }
        shadowPackageManager.installPackage(packageInfo)

        assertFailsWith<FailedPreconditionException> { tester.get<Boolean>("unused_apps_switch") }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun get_systemExemptApp_returnsFalse() {
        setupHibernationEligibility(
            PermissionControllerManager.HIBERNATION_ELIGIBILITY_EXEMPT_BY_SYSTEM
        )
        val result = tester.get<Boolean>("unused_apps_switch")
        assertThat(result).isFalse()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun get_modeAllowed_returnsTrue() {
        `when`(
                mockAppOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED,
                    validUid,
                    validPackageName,
                )
            )
            .thenReturn(AppOpsManager.MODE_ALLOWED)

        val result = tester.get<Boolean>("unused_apps_switch")
        assertThat(result).isTrue()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun get_modeDefault_targetPreS_returnsFalse() {
        val shadowPackageManager = shadowOf(context.packageManager)
        val packageInfo =
            PackageInfo().apply {
                packageName = validPackageName
                applicationInfo =
                    ApplicationInfo().apply {
                        packageName = validPackageName
                        uid = validUid
                        targetSdkVersion = Build.VERSION_CODES.Q
                    }
            }
        shadowPackageManager.installPackage(packageInfo)

        `when`(
                mockAppOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED,
                    validUid,
                    validPackageName,
                )
            )
            .thenReturn(AppOpsManager.MODE_DEFAULT)

        val result = tester.get<Boolean>("unused_apps_switch")
        assertThat(result).isFalse()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun get_modeDefault_targetS_returnsTrue() {
        context.packageManager.getApplicationInfo(validPackageName, 0).targetSdkVersion =
            Build.VERSION_CODES.S
        `when`(
                mockAppOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED,
                    validUid,
                    validPackageName,
                )
            )
            .thenReturn(AppOpsManager.MODE_DEFAULT)

        val result = tester.get<Boolean>("unused_apps_switch")
        assertThat(result).isTrue()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun set_systemExemptApp_throwsCustomPrecondition() {
        setupHibernationEligibility(
            PermissionControllerManager.HIBERNATION_ELIGIBILITY_EXEMPT_BY_SYSTEM
        )

        assertFailsWith<FailedPreconditionException> { tester.set("unused_apps_switch", true) }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun set_false_updatesAppOps_and_unhibernates() {
        tester.set("unused_apps_switch", false)

        verify(mockAppOpsManager)
            .setUidMode(
                AppOpsManager.OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED,
                validUid,
                AppOpsManager.MODE_IGNORED,
            )
        verify(mockAppHibernationManager).setHibernatingForUser(validPackageName, false)
        verify(mockAppHibernationManager).setHibernatingGlobally(validPackageName, false)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun set_true_updatesAppOps_only() {
        tester.set("unused_apps_switch", true)

        verify(mockAppOpsManager)
            .setUidMode(
                AppOpsManager.OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED,
                validUid,
                AppOpsManager.MODE_ALLOWED,
            )
        verify(mockAppHibernationManager, org.mockito.Mockito.never())
            .setHibernatingForUser(anyString(), org.mockito.Mockito.anyBoolean())
    }
}
