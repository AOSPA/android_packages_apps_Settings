/*
Copyright (c) 2019, The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.android.settings.deviceinfo;

import android.content.Context;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

public class StorageSizePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {
    private static final String LOG_TAG = "StorageSizePreferenceController";
    private final static String KEY_STORAGE_TOTAL_SIZE = "key_storage_total_size";

    public StorageSizePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_STORAGE_TOTAL_SIZE;
    }

    @Override
    public boolean isAvailable() {
        return Utils.isSupportCTPA(mContext);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference ramSizePreference = screen.findPreference(getPreferenceKey());
        if (!isAvailable() ||  null == ramSizePreference || !ramSizePreference.isVisible()) {
            return;
        }
        String ramSize = Utils.getString(mContext, Utils.KEY_RAM_TOTAL_SIZE);
        Log.d(LOG_TAG, "displayPreference: ramSize = " + ramSize);
        if (null != ramSize && !ramSize.isEmpty()) {
            ramSizePreference.setSummary(ramSize);
        } else {
            ramSizePreference.setSummary(mContext.getString(R.string.device_info_default));
        }

        final Preference romSizePreference = createNewPreference(screen.getContext());
        romSizePreference.setOrder(ramSizePreference.getOrder() + 1);
        romSizePreference.setKey(KEY_STORAGE_TOTAL_SIZE + 1);
        screen.addPreference(romSizePreference);
        romSizePreference.setVisible(true);
        romSizePreference.setTitle(mContext.getResources().getString(R.string.rom_total_size));
        String romSize = Utils.getString(mContext, Utils.KEY_ROM_TOTAL_SIZE);
        Log.d(LOG_TAG, "displayPreference: romSize = " + romSize);
        if (null != romSize && !romSize.isEmpty()) {
            romSizePreference.setSummary(romSize);
        } else {
            romSizePreference.setSummary(mContext.getString(R.string.device_info_default));
        }
    }

    private Preference createNewPreference(Context context) {
        return new Preference(context);
    }
}
