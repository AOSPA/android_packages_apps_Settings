/*
 * Copyright (C) 2020 Paranoid Android
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
 * limitations under the License
 */

package com.android.settings.biometrics.face;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.os.UserManager;

import vendor.pa.biometrics.face.V1_0.IFaceUnlockService;

public class ParanoidFaceSenseConnector implements ServiceConnection {

    private static final String TAG = "ParanoidFaceSenseConnector";

    private Context mContext;
    private IFaceUnlockService mService;
    private static ParanoidFaceSenseConnector sInstance = null;

    private boolean mIsBound = false;
    private boolean mParanoidFaceSenseChecked = false;
    private boolean mParanoidFaceSenseEnabled = false;

    public static synchronized ParanoidFaceSenseConnector getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ParanoidFaceSenseConnector(context);
        }
        return sInstance;
    }

    public ParanoidFaceSenseConnector(Context context) {
        mContext = context;
    }

    public void bind(boolean shouldBind) {
        if (!mIsBound && shouldBind) {
            Intent i = new Intent();
            i.setPackage("com.paranoid.facesense");
            i.setAction("service.remote");
            mContext.bindService(i, this, Context.BIND_AUTO_CREATE);
            return;
        }
        mContext.unbindService(this);
    }

    public boolean isParanoidFaceSenseEnabled() {
        if (mParanoidFaceSenseChecked) {
            return mParanoidFaceSenseEnabled;
        }

        try {
            mContext.getPackageManager().getPackageInfo("com.paranoid.facesense", 0);
            mParanoidFaceSenseEnabled = mContext.getPackageManager().getApplicationInfo("com.paranoid.facesense", 0).enabled;
            mParanoidFaceSenseChecked = true;
        } catch (NameNotFoundException e) {
            Log.d(TAG, "Paranoid FaceSense not found");
        }
        Log.d(TAG, "Paranoid FaceSense enabled state: " + mParanoidFaceSenseEnabled);
        return mParanoidFaceSenseEnabled;
    }

    public boolean hasEnrolledFaceSenseUsers() {
        boolean hasActiveUsers = false;
        if (mIsBound) {
            try {
                if (mService.getActiveUserCount() > 0) {
                    hasActiveUsers = true;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                hasActiveUsers = false;
            }
            return isParanoidFaceSenseEnabled() && hasActiveUsers;
        }
        return isParanoidFaceSenseEnabled() && hasActiveUsers;
    }

    public void removeFaceSenseUsers() {
        if (mIsBound) {
            try {
                if (hasEnrolledFaceSenseUsers()) {
                    mService.removeActiveUser(((UserManager) 
                            mContext.getSystemService("user"))
                            .getSerialNumberForUser(Process.myUserHandle()));
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isFaceDisabledByAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            if (dpm.getPasswordQuality(null) > DevicePolicyManager.PASSWORD_QUALITY_MANAGED) {
                return true;
            }
        } catch (SecurityException e) {
            Log.e(TAG, "isFaceDisabledByAdmin error:", e);
        }

        if ((dpm.getKeyguardDisabledFeatures(null) & DevicePolicyManager.KEYGUARD_DISABLE_FACE) != 0) {
            return true;
        }
        return false;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        mService = IFaceUnlockService.Stub.asInterface(binder);
        mIsBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mIsBound = false;
        mService = null;
    }
}
