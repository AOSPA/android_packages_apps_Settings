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

package com.android.settings.vpn2;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.Future;

/**
 * Base class for a preference that represents an application.
 *
 * <p>It shows the app icon, app label. An action icon will be shown in the widget and the action is
 * defined in the child class.
 */
public abstract class AppExclusionBasePreference extends Preference {
    private static final String TAG = "AppExclusionBasePreference";

    private final String mPackageName;
    private final AppExclusionActionCallback mActionCallback;

    /** Returns the resource ID of the icon for the action button. */
    abstract int getActionIcon();

    /** Returns the content description for the action button. */
    abstract String getActionIconContentDescription();

    /** Returns the {@link AppExclusionActionCallback.Action} to be performed on click. */
    abstract AppExclusionActionCallback.Action getClickAction();

    AppExclusionBasePreference(
            Context context,
            String packageName,
            int userId,
            AppExclusionActionCallback actionCallback) {
        super(context);
        mPackageName = packageName;
        setKey(mPackageName);
        String title = packageName;
        PackageInfo packageInfo = AppExclusionUtils.getPackageInfo(context, userId, packageName);
        if (packageInfo != null) {
            String label =
                    packageInfo.applicationInfo.loadLabel(context.getPackageManager()).toString();
            if (label != null && !label.isEmpty()) {
                title = label;
            }
            loadIcon(packageInfo);
        }
        setTitle(title);
        setWidgetLayoutResource(R.layout.vpn_app_exclusion_preference_widget);
        mActionCallback = actionCallback;
    }

    /**
     * Binds the view holder to the preference data.
     *
     * @param holder The view holder to bind.
     */
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        setOnPreferenceClickListener(
                v -> {
                    if (mActionCallback != null) {
                        mActionCallback.onAppItemClick(mPackageName, getClickAction());
                        return true;
                    }
                    return false;
                });
        ViewCompat.replaceAccessibilityAction(
                holder.itemView,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                getActionIconContentDescription(),
                /* command= */ null);

        final ImageView actionButton = (ImageView) holder.findViewById(R.id.action);
        if (actionButton == null) {
            return;
        }

        int iconRes = getActionIcon();
        if (iconRes != 0) {
            actionButton.setVisibility(View.VISIBLE);
            actionButton.setImageResource(iconRes);
        } else {
            actionButton.setVisibility(View.GONE);
        }
    }

    private void loadIcon(PackageInfo packageInfo) {
        Future<?> unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            // Utils.getBadgedIcon() returns the default icon if the app doesn't
                            // have one.
                            final Drawable icon =
                                    Utils.getBadgedIcon(getContext(), packageInfo.applicationInfo);
                            getContext().getMainThreadHandler().post(() -> setIcon(icon));
                        });
    }
}
