/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settingslib.metadata.PreferenceScreenBindingKeyProviderKt.EXTRA_BINDING_SCREEN_ARGS;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.accessibility.a11yactivity.ui.A11yActivityScreen;
import com.android.settings.accessibility.extensions.ComponentNameBundleUtils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.metadata.CatalystFlagProviderFactory;
import com.android.settingslib.metadata.ValidatedKeyParameters;

import java.util.Map;
import java.util.Objects;


/** Fragment for providing open activity button. */
public class LaunchAccessibilityActivityPreferenceFragment extends DashboardFragment {

    private static final String TAG = "LaunchAccessibilityActivityPreferenceFragment";

    @Override
    public int getMetricsCategory() {
        // This could be called before the fragment is attached to an activity.
        return FeatureFactory.getFeatureFactory()
                .getAccessibilityPageIdFeatureProvider().getCategory(getFeatureComponentName());
    }

    @Override
    protected int getPreferenceScreenResId() {
        return 0;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Nullable
    @Override
    public String getPreferenceScreenBindingKey(@NonNull Context context) {
        return A11yActivityScreen.KEY;
    }

    @Nullable
    @Override
    @Deprecated(since = "This method will be removed once the catalyst framework stops passing the "
            + "arguments as a bundle. Use getPreferenceScreenBindingKeyParameters instead.")
    public Bundle getPreferenceScreenBindingArgs(@NonNull Context context) {
        if (com.android.settings.flags.Flags.catalystUseStringBundle() ||
                CatalystFlagProviderFactory.INSTANCE.catalystUseKeyParameters()) {
            Bundle arguments = new Bundle();
            arguments.putString(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    getFeatureComponentName().flattenToString()
            );
            return arguments;
        } else {
            return getFragmentArguments();
        }
    }

    @Nullable
    @Override
    public ValidatedKeyParameters getPreferenceScreenBindingKeyParameters(
            @NonNull Context context
    ) {
        return A11yActivityScreen.Companion.getParametersSchema().prepare(
                Map.of(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    getFeatureComponentName().flattenToString()
                )
        );
    }

    @NonNull
    private ComponentName getFeatureComponentName() {
        Bundle arguments = getFragmentArguments();
        return Objects.requireNonNull(
                ComponentNameBundleUtils.getComponentName(
                        arguments,
                        AccessibilitySettings.EXTRA_COMPONENT_NAME
                )
        );
    }

    /**
     * Retrieves the fragment arguments.
     *
     * <p>If the arguments contain PreferenceScreenBindingKeyProviderKt#EXTRA_BINDING_SCREEN_ARGS
     * the nested bundle associated with that key will be returned. Otherwise, the original
     * arguments are returned.
     *
     * @return The fragment arguments, or the nested arguments if
     * PreferenceScreenBindingKeyProviderKt#EXTRA_BINDING_SCREEN_ARGS is present.
     * @throws NullPointerException if the initial arguments are null or if the nested argument are
     *                              null
     */
    @NonNull
    private Bundle getFragmentArguments() {
        Bundle arguments = getArguments();
        Objects.requireNonNull(arguments);
        if (arguments.containsKey(EXTRA_BINDING_SCREEN_ARGS)) {
            arguments = Objects.requireNonNull(arguments.getBundle(EXTRA_BINDING_SCREEN_ARGS));
        }
        return arguments;
    }
}
