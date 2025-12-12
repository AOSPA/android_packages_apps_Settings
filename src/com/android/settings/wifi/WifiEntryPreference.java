/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.wifi;

import static com.android.settingslib.wifi.WifiUtils.getHotspotIconResource;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.R;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.SettingsThemeHelper;
import com.android.settingslib.wifi.WifiUtils;
import com.android.wifitrackerlib.HotspotNetworkEntry;
import com.android.wifitrackerlib.WifiEntry;

/**
 * Preference to display a WifiEntry in a wifi picker.
 */
public class WifiEntryPreference extends RestrictedPreference implements
        WifiEntry.WifiEntryCallback,
        View.OnClickListener {

    // These values must be kept within [WifiEntry.WIFI_LEVEL_MIN, WifiEntry.WIFI_LEVEL_MAX]
    private static final int[] WIFI_CONNECTION_STRENGTH = {
            R.string.accessibility_no_wifi,
            R.string.accessibility_wifi_one_bar,
            R.string.accessibility_wifi_two_bars,
            R.string.accessibility_wifi_three_bars,
            R.string.accessibility_wifi_signal_full
    };

// QTI_BEGIN: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
    private final InternetIconInjector mIconInjector;
// QTI_END: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
    private WifiEntry mWifiEntry;
    private int mLevel = -1;
// QTI_BEGIN: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
    private int mWifiStandard;
// QTI_END: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
    private boolean mShowX; // Shows the Wi-Fi signl icon of Pie+x when it's true.
    private CharSequence mContentDescription;
    private OnButtonClickListener mOnButtonClickListener;
// QTI_BEGIN: 2024-06-10: WLAN: Settings: Adding control to wifi standard display feature.
    private static Boolean sIsWifiStandardDisplaySupported = null;
// QTI_END: 2024-06-10: WLAN: Settings: Adding control to wifi standard display feature.

    public WifiEntryPreference(@NonNull Context context, @NonNull WifiEntry wifiEntry) {
// QTI_BEGIN: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
        this(context, wifiEntry, new InternetIconInjector(context));
// QTI_END: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
    }

    @VisibleForTesting
    WifiEntryPreference(@NonNull Context context, @NonNull WifiEntry wifiEntry,
// QTI_BEGIN: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
            @NonNull InternetIconInjector iconInjector) {
// QTI_END: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
        super(context);

// QTI_BEGIN: 2024-06-10: WLAN: Settings: Adding control to wifi standard display feature.
        if (sIsWifiStandardDisplaySupported == null) {
            sIsWifiStandardDisplaySupported = context.getResources().getBoolean(
                    R.bool.config_show_wifi_standard);
        }
// QTI_END: 2024-06-10: WLAN: Settings: Adding control to wifi standard display feature.
        int layoutResId = SettingsThemeHelper.isExpressiveTheme(getContext())
                ? R.layout.preference_access_point_expressive : R.layout.preference_access_point;
        setLayoutResource(layoutResId);
        mIconInjector = iconInjector;
        setWifiEntry(wifiEntry);
    }

    /**
     * Set updated {@link WifiEntry} to refresh the preference
     *
     * @param wifiEntry An instance of {@link WifiEntry}
     */
    public void setWifiEntry(@NonNull WifiEntry wifiEntry) {
        mWifiEntry = wifiEntry;
        mWifiEntry.setListener(this);
        refresh();
    }

    public WifiEntry getWifiEntry() {
        return mWifiEntry;
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (mWifiEntry.isVerboseSummaryEnabled()) {
            TextView summary = (TextView) view.findViewById(android.R.id.summary);
            if (summary != null) {
                summary.setMaxLines(100);
            }
        }
        final Drawable drawable = getIcon();
        if (drawable != null) {
            drawable.setLevel(mLevel);
        }

        view.itemView.setContentDescription(mContentDescription);

        // Turn off divider
        view.findViewById(com.android.settingslib.widget.preference.twotarget.R.id.two_target_divider)
                .setVisibility(View.INVISIBLE);

        final LinearLayout endIcons = (LinearLayout) view.findViewById(
                com.android.settings.R.id.wifi_end_icons);

        // Enable the icon button when the help string in this WifiEntry is not null.
        final ImageButton imageButton = (ImageButton) view.findViewById(R.id.icon_button);
        if (mWifiEntry.getHelpUriString() != null
                && mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_DISCONNECTED) {
            final Drawable drawablehelp = getDrawable(R.drawable.ic_help);
            drawablehelp.setTintList(
                    Utils.getColorAttr(getContext(), android.R.attr.colorControlNormal));
            ((ImageView) imageButton).setImageDrawable(drawablehelp);
            imageButton.setVisibility(View.VISIBLE);
            imageButton.setOnClickListener(this);
            imageButton.setContentDescription(
                    getContext().getText(R.string.help_label));
        } else if (endIcons != null) {
            updateEndIcons(endIcons);
        }
    }

    @VisibleForTesting
    void updateEndIcons(LinearLayout endIcons) {
        endIcons.removeAllViews();
        // The shared icon should precede the lock icon to match the mocks.
        if (displaySharedIcon()) {
            ImageView sharedIcon =
                    addIcon(endIcons,
                            com.android.settings.R.drawable.ic_group_24dp);
            sharedIcon.setTooltipText(getContext().getString(
                    com.android.settings.R.string.wifi_shared_network_icon_message));
        }
        if ((mWifiEntry.getSecurity() != WifiEntry.SECURITY_NONE)
                && (mWifiEntry.getSecurity() != WifiEntry.SECURITY_OWE)) {
            addIcon(endIcons,
                    com.android.settings.R.drawable.ic_friction_lock_closed);
        }
    }

    private ImageView addIcon(LinearLayout endIcons, @DrawableRes int drawableId) {
        ImageView icon = new ImageView(getContext());
        icon.setImageDrawable(getDrawable(drawableId));
        icon.setImageTintList(Utils.getColorAttr(getContext(),
                android.R.attr.colorControlNormal));
        ((View) icon).setMinimumWidth(
                getContext().getResources().getDimensionPixelSize(
                        com.android.settings.R.dimen.wifi_end_icon_min_width)
        );

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        endIcons.addView(icon, layoutParams);
        return icon;
    }

    private boolean displaySharedIcon() {
        if (!com.android.settings.connectivity.Flags.wifiMultiuser()) {
            return false;
        }

        UserManager userManager = getContext().getSystemService(UserManager.class);
        if (userManager.getUserCount() <= 1) {
            return false;
        }

        return (mWifiEntry.getWifiConfiguration() == null)
                ? false : mWifiEntry.getWifiConfiguration().shared;
    }

    /**
     * Updates the title and summary; may indirectly call notifyChanged().
     */
    public void refresh() {
        setTitle(mWifiEntry.getTitle());
        if (mWifiEntry instanceof HotspotNetworkEntry) {
            updateHotspotIcon(((HotspotNetworkEntry) mWifiEntry).getDeviceType());
        } else {
// QTI_BEGIN: 2024-09-12: WConnect/WLAN_3RDPARTY_GOOGLE: Settings: WiFi icon change for Saved Networks.
                mLevel = mWifiEntry.getLevel();
                mWifiStandard = mWifiEntry.getWifiStandard();
                mShowX = mWifiEntry.shouldShowXLevelIcon();
// QTI_END: 2024-09-12: WConnect/WLAN_3RDPARTY_GOOGLE: Settings: WiFi icon change for Saved Networks.
// QTI_BEGIN: 2024-06-10: WLAN: Settings: Adding control to wifi standard display feature.
                updateIcon(mShowX, mLevel, sIsWifiStandardDisplaySupported ? mWifiStandard : 0);
// QTI_END: 2024-06-10: WLAN: Settings: Adding control to wifi standard display feature.
// QTI_BEGIN: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
                notifyChanged();
// QTI_END: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
        }

// QTI_BEGIN: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
        String summary = mWifiEntry.getSummary(false /* concise */);

        if (mWifiEntry.isPskSaeTransitionMode()) {
           summary = "WPA3(SAE Transition Mode) " + summary;
        } else if (mWifiEntry.isOweTransitionMode()) {
           summary = "WPA3(OWE Transition Mode) " + summary;
        } else if (mWifiEntry.getSecurity() == WifiEntry.SECURITY_SAE) {
           summary = "WPA3(SAE) " + summary;
        } else if (mWifiEntry.getSecurity() == WifiEntry.SECURITY_OWE) {
           summary = "WPA3(OWE) " + summary;
        }

        setSummary(summary);
// QTI_END: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
        mContentDescription = buildContentDescription();
    }

    /**
     * Indicates the state of the WifiEntry has changed and clients may retrieve updates through
     * the WifiEntry getter methods.
     */
    public void onUpdated() {
        // TODO(b/70983952): Fill this method in
        refresh();
    }

    /**
     * Result of the connect request indicated by the WifiEntry.CONNECT_STATUS constants.
     */
    public void onConnectResult(int status) {
        // TODO(b/70983952): Fill this method in
    }

    /**
     * Result of the disconnect request indicated by the WifiEntry.DISCONNECT_STATUS constants.
     */
    public void onDisconnectResult(int status) {
        // TODO(b/70983952): Fill this method in
    }

    /**
     * Result of the forget request indicated by the WifiEntry.FORGET_STATUS constants.
     */
    public void onForgetResult(int status) {
        // TODO(b/70983952): Fill this method in
    }

    /**
     * Result of the sign-in request indecated by the WifiEntry.SIGNIN_STATUS constants
     */
    public void onSignInResult(int status) {
        // TODO(b/70983952): Fill this method in
    }

    protected int getIconColorAttr() {
        final boolean accent = (mWifiEntry.hasInternetAccess()
                && mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED);
        return accent ? android.R.attr.colorAccent : android.R.attr.colorControlNormal;
    }

    private void setIconWithTint(Drawable drawable) {
        if (drawable != null) {
            // Must use Drawable#setTintList() instead of Drawable#setTint() to show the grey
            // icon when the preference is disabled.
            drawable.setTintList(Utils.getColorAttr(getContext(), getIconColorAttr()));
            setIcon(drawable);
        } else {
            setIcon(null);
        }
    }

    @VisibleForTesting
// QTI_BEGIN: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
    void updateIcon(boolean showX, int level, int standard) {
// QTI_END: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
        if (level == -1) {
            setIcon(null);
            return;
        }
// QTI_BEGIN: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
        setIconWithTint(mIconInjector.getIcon(showX, level, standard));
// QTI_END: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
    }

    @VisibleForTesting
    void updateHotspotIcon(int deviceType) {
        setIconWithTint(getContext().getDrawable(getHotspotIconResource(deviceType)));
    }

    /**
     * Helper method to generate content description string.
     */
    @VisibleForTesting
    CharSequence buildContentDescription() {
        final Context context = getContext();

        CharSequence contentDescription = getTitle();
        final CharSequence summary = getSummary();
        if (!TextUtils.isEmpty(summary)) {
            contentDescription = TextUtils.concat(contentDescription, ",", summary);
        }
        int level = mWifiEntry.getLevel();
        if (level >= 0 && level < WIFI_CONNECTION_STRENGTH.length) {
            contentDescription = TextUtils.concat(contentDescription, ",",
                    context.getString(WIFI_CONNECTION_STRENGTH[level]));
        }
        return TextUtils.concat(contentDescription, ",",
                mWifiEntry.getSecurity() == WifiEntry.SECURITY_NONE
                        ? context.getString(R.string.accessibility_wifi_security_type_none)
                        : context.getString(R.string.accessibility_wifi_security_type_secured));
    }

// QTI_BEGIN: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
    static class InternetIconInjector {
        private final Context mContext;

        InternetIconInjector(Context context) {
            mContext = context;
        }

        public Drawable getIcon(boolean showX, int level, int standard) {
            return mContext.getDrawable(WifiUtils.getInternetIconResource(level, showX, standard));
        }
    }

// QTI_END: 2024-04-13: WLAN: Wifi: Use WifiEntryPreferece from Settings implementation.
    /**
     * Set listeners, who want to listen the button client event.
     */
    public void setOnButtonClickListener(OnButtonClickListener listener) {
        mOnButtonClickListener = listener;
        notifyChanged();
    }

    @Override
    protected int getSecondTargetResId() {
        return com.android.settings.R.layout.preference_end_icons_container;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.icon_button) {
            if (mOnButtonClickListener != null) {
                mOnButtonClickListener.onButtonClick(this);
            }
        }
    }

    /**
     * Callback to inform the caller that the icon button is clicked.
     */
    public interface OnButtonClickListener {

        /**
         * Register to listen the button click event.
         */
        void onButtonClick(WifiEntryPreference preference);
    }

    private Drawable getDrawable(@DrawableRes int iconResId) {
        Drawable buttonIcon = null;

        try {
            buttonIcon = getContext().getDrawable(iconResId);
        } catch (Resources.NotFoundException exception) {
            // Do nothing
        }
        return buttonIcon;
    }
}
