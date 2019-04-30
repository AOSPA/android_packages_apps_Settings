/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.location;

import android.content.Context;
import android.text.TextUtils;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.UserHandle;

import android.location.SettingInjectorService;

import androidx.preference.Preference;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import com.android.settings.widget.RestrictedAppPreference;
import com.android.settingslib.location.InjectedSetting;
import com.android.settingslib.location.SettingsInjector;
import com.android.settingslib.widget.apppreference.AppPreference;

/**
 * Adds the preferences specified by the {@link InjectedSetting} objects to a preference group.
 */
public class AppSettingsInjector extends SettingsInjector {

    public AppSettingsInjector(Context context) {
        super(context);
    }

    /**
     * Returns the settings parsed from the attributes of the
     * {@link SettingInjectorService#META_DATA_NAME} tag, or null.
     *
     * Duplicates some code from {@link android.content.pm.RegisteredServicesCache}.
     */
    @Override
    protected InjectedSetting parseServiceInfo(ResolveInfo service, UserHandle userHandle,
            PackageManager pm) throws XmlPullParserException, IOException {
        InjectedSetting res = super.parseServiceInfo(service, userHandle, pm);
        ServiceInfo si = service.serviceInfo;

    	if ((null != res) && (!DimmableIZatIconPreference.showIzat(mContext, si.packageName))) {
        	res = null;
    	}

    	return res;
    }

    @Override
    protected Preference createPreference(Context prefContext, InjectedSetting setting) {
        return TextUtils.isEmpty(setting.userRestriction)
                ? DimmableIZatIconPreference.getAppPreference(prefContext, setting)
                : DimmableIZatIconPreference.getRestrictedAppPreference(prefContext, setting);
    }
}
