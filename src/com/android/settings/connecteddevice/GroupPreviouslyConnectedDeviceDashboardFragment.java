/******************************************************************************
 *  Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/
package com.android.settings.connecteddevice;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.bluetooth.GroupUtils;
import com.android.settings.dashboard.DashboardFragment;
import android.util.Log;
import com.android.settingslib.core.instrumentation.Instrumentable;

import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This fragment contains previously connected Group devices
 */
public class GroupPreviouslyConnectedDeviceDashboardFragment extends DashboardFragment {

    private static final String TAG = "GroupPreviouslyConnectedDeviceDashboardFragment";
    static final String KEY_PREVIOUSLY_CONNECTED_GROUP_DEVICES = "group_saved_device_list";
    private GroupUtils mGroupUtils;

    @Override
    public int getHelpResource() {
        return R.string.group_help_url_previously_connected_devices;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.previously_connected_group_devices;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return Instrumentable.METRICS_CATEGORY_UNKNOWN;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mGroupUtils = new GroupUtils(context);
        use(GroupSavedDeviceController.class).init(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGroupUtils.isHidePCGGroups()) {
            finish();
        }
    }
}
