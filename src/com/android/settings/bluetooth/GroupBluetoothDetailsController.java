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

package com.android.settings.bluetooth;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import android.content.Context;

public abstract class GroupBluetoothDetailsController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver,
        OnStop, OnStart, BluetoothCallback {

    protected final Context mContext;
    protected final PreferenceFragmentCompat mFragment;
    protected LocalBluetoothManager mLocalManager;


    public GroupBluetoothDetailsController(Context context, PreferenceFragmentCompat fragment,
                int groupId, Lifecycle lifecycle) {
        super(context);
        mContext = context;
        mFragment = fragment;
        lifecycle.addObserver(this);
        mLocalManager = Utils.getLocalBtManager(mContext);
    }

    @Override
    public void onStart() {
        mLocalManager.getEventManager().registerCallback(this);
        loadDevices();
        refresh();
    }

    @Override
    public void onStop() {
        mLocalManager.getEventManager().unregisterCallback(this);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public final void displayPreference(PreferenceScreen screen) {
        init(screen);
        super.displayPreference(screen);
    }

    /**
     * This is a method to do one-time initialization when the screen is first created, such as
     * adding preferences.
     * @param screen the screen where this controller's preferences should be added
     */
    protected abstract void init(PreferenceScreen screen);

    /**
     * This method is called when something about the bluetooth device has changed, and this object
     * should update the preferences it manages based on the new state.
     */
    protected abstract void refresh();

    protected abstract void loadDevices();
}
