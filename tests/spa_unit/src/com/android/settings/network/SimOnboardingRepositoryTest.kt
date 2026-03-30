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

package com.android.settings.network

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimOnboardingRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SimOnboardingRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        repository = SimOnboardingRepository(context)
        runBlocking { repository.clearOnboardingSimSet() }
    }

    @Test
    fun onboardingSimSet_defaultIsEmpty() = runBlocking {
        val savedSet = repository.onboardingSimSet.first()
        assertThat(savedSet).isEmpty()
    }

    @Test
    fun saveOnboardingSimSet_returnsCorrectSet() = runBlocking {
        val simSet = setOf("1", "2")
        repository.saveOnboardingSimSet(simSet)

        val savedSet = repository.onboardingSimSet.first()
        assertThat(savedSet).isEqualTo(simSet)
    }

    @Test
    fun clearOnboardingSimSet_returnsEmptySet() = runBlocking {
        val simSet = setOf("1", "2")
        repository.saveOnboardingSimSet(simSet)
        repository.clearOnboardingSimSet()

        val savedSet = repository.onboardingSimSet.first()
        assertThat(savedSet).isEmpty()
    }

    @Test
    fun saveOnboardingSimSet_overwritesPreviousSet() = runBlocking {
        val firstSet = setOf("1", "2")
        repository.saveOnboardingSimSet(firstSet)
        val secondSet = setOf("3", "4")
        repository.saveOnboardingSimSet(secondSet)

        val savedSet = repository.onboardingSimSet.first()
        assertThat(savedSet).isEqualTo(secondSet)
    }
}
