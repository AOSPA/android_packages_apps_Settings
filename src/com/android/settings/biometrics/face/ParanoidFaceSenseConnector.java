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
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.Log;

import vendor.pa.biometrics.face.V1_0.IFaceUnlockService;

public class ParanoidFaceSenseConnector {

    private static final String TAG = "ParanoidFaceSenseConnector";

    private Context mContext;
    private IFaceUnlockService mService;
    private static ParanoidFaceSenseConnector sInstance = null;
    protected final Object mLock = new Object();

    private boolean mIsBind = false;
    private boolean mParanoidFaceSenseChecked = false;
    private boolean mParanoidFaceSenseEnabled = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            synchronized (mLock) {
                Log.d(TAG, "mConnection onServiceConnected");
                mService = IFaceUnlockService.Stub.asInterface(binder);
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (mLock) {
                Log.d(TAG, "mConnection onServiceDisconnected");
                mService = null;
                unbind();
            }
        }
    };

    public static synchronized ParanoidFaceSenseConnector getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ParanoidFaceSenseConnector(context);
        }
        return sInstance;
    }

    public ParanoidFaceSenseConnector(Context context) {
        mContext = context;
    }

    public void bind() {
        synchronized (mLock) {
            Log.d(TAG, "binding: " + mIsBind + ", service: " + mService);
            if (!mIsBind) {
                Intent intent = new Intent();
                intent.setAction("service.remote");
                intent.setPackage("com.paranoid.facesense");
                if (mConnection != null) {
                    if (mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                        mIsBind = true;
                        Log.d(TAG, "FaceSense service bound");
                    }
                }
                Log.d(TAG, "Failed to bind FaceSense service!");
            }
        }
    }

    public void unbind() {
        synchronized (mLock) {
            Log.d(TAG, "unbinding");
            if (!(!mIsBind || mContext == null || mConnection == null)) {
                mIsBind = false;
                mContext.unbindService(mConnection);
                Log.d(TAG, "FaceSense service unbound");
            }
            mService = null;
        }
    }

    public boolean isParanoidFaceSenseEnabled() {
        synchronized (mLock) {
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
    }

    public boolean hasEnrolledFaces() {
        synchronized (mLock) {
            boolean hasActiveUsers = false;
            if (mIsBind && mService != null) {
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
    }

    public void removeEnrolledFaces() {
        synchronized (mLock) {
            if (mIsBind && mService != null) {
                try {
                    if (hasEnrolledFaces()) {
                        mService.removeActiveUser(((UserManager) 
                                mContext.getSystemService("user"))
                                .getSerialNumberForUser(android.os.Process.myUserHandle()));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
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
}
