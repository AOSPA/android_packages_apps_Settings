/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage.bugreport;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.fuelgauge.BatteryUsageHistoricalLogEntry.Action;
import com.android.settings.testutils.shadow.ShadowThreadUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowThreadUtils.class})
public final class BatteryUsageLogUtilsTest {

    private StringWriter mTestStringWriter;
    private PrintWriter mTestPrintWriter;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mTestStringWriter = new StringWriter();
        mTestPrintWriter = new PrintWriter(mTestStringWriter);
        BatteryUsageLogUtils.getSharedPreferences(mContext).edit().clear().commit();
    }

    @After
    public void tearDown() {
        ShadowThreadUtils.reset();
    }

    @Test
    public void printHistoricalLog_withDefaultLogs() {
        final String expectedInformation = "nothing to dump";
        // Environment checking.
        assertThat(mTestStringWriter.toString().contains(expectedInformation)).isFalse();

        BatteryUsageLogUtils.printHistoricalLog(mContext, mTestPrintWriter);
        assertThat(mTestStringWriter.toString()).contains(expectedInformation);
    }

    @Test
    public void writeLog_multipleLogs_withCorrectCounts() {
        final int expectedCount = 10;
        final List<String> expectedTokens = new ArrayList<>();
        for (int i = 0; i < expectedCount; i++) {
            writeLogWrapper(mContext, Action.SCHEDULE_JOB, " " + i, expectedTokens);
        }
        writeLogWrapper(mContext, Action.EXECUTE_JOB, "execute", expectedTokens);

        assertActionCount(Action.EXECUTE_JOB.name(), 0);
        BatteryUsageLogUtils.printHistoricalLog(mContext, mTestPrintWriter);

        assertInOrder(expectedTokens);
        assertActionCount(Action.SCHEDULE_JOB.name(), expectedCount);
        assertActionCount(Action.EXECUTE_JOB.name(), 1);
    }

    @Test
    public void writeLog_overMaxEntriesLogs_withCorrectCounts() {
        final List<String> expectedTokens = new ArrayList<>();
        writeLogWrapper(mContext, Action.SCHEDULE_JOB, "0", expectedTokens);
        writeLogWrapper(mContext, Action.SCHEDULE_JOB, "1", expectedTokens);
        for (int i = 0; i < BatteryUsageLogUtils.MAX_ENTRIES * 2; i++) {
            writeLogWrapper(mContext, Action.EXECUTE_JOB, "" + i, expectedTokens);
        }

        assertActionCount(Action.EXECUTE_JOB.name(), 0);
        BatteryUsageLogUtils.printHistoricalLog(mContext, mTestPrintWriter);

        final String dumpResults = mTestStringWriter.toString();
        assertThat(dumpResults.contains(Action.SCHEDULE_JOB.name())).isFalse();
        assertInOrder(expectedTokens);
        assertActionCount(Action.EXECUTE_JOB.name(), BatteryUsageLogUtils.MAX_ENTRIES);
    }

    private void assertActionCount(String token, int count) {
        final String dumpResults = mTestStringWriter.toString();
        assertThat(dumpResults.split(token).length).isEqualTo(count + 1);
    }

    private void assertInOrder(List<String> tokens) {
        final String dumpResults = mTestStringWriter.toString();
        int lastIndex = 0;
        for (String token : tokens) {
            int foundIndex = dumpResults.indexOf(token, lastIndex);
            assertThat(foundIndex).isNotEqualTo(-1);
            lastIndex = foundIndex + token.length();
        }
    }

    private void writeLogWrapper(Context context, Action action, String actionDescription,
                                 final List<String> expectedTokens) {
        BatteryUsageLogUtils.writeLog(context, action, actionDescription);
        if (expectedTokens != null) {
            if (expectedTokens.size() >= BatteryUsageLogUtils.MAX_ENTRIES) {
                expectedTokens.removeFirst();
            }
            expectedTokens.add(action.name() + " " + actionDescription);
        }
    }
}
