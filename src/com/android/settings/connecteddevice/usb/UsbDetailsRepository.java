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

package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.os.SystemProperties;

/** A repository for USB details related functionality. */
public class UsbDetailsRepository {
    private static final String TRANSCODE_MTP_SYS_PROP_KEY = "sys.fuse.transcode_mtp";

    private final UsbBackend mUsbBackend;

    public UsbDetailsRepository(Context appContext) {
        mUsbBackend = new UsbBackend(appContext);
    }

    /** Gets what the current USB functions are. */
    public long getCurrentFunction() {
        return mUsbBackend.getCurrentFunctions();
    }

    /** Gets the data role of the current USB port. */
    public int getDataRole() {
        return mUsbBackend.getDataRole();
    }

    /** Checks if current USB functions roles are supported. */
    public boolean isSupportAllRoles() {
        return mUsbBackend.areAllRolesSupported();
    }

    /** Sets the power role of the current USB port. */
    public void setPowerRole(int role) {
        mUsbBackend.setPowerRole(role);
    }

    /** Gets the power role of the current USB port. */
    public int getPowerRole() {
        return mUsbBackend.getPowerRole();
    }

    /** Gets current state of MTP transcoding for transferring the file. */
    public boolean isMtpTranscodeEnabled() {
        return SystemProperties.getBoolean(TRANSCODE_MTP_SYS_PROP_KEY, false);
    }

    /** Sets the state of MTP transcoding for transferring the file . */
    public void setMtpTranscodeState(boolean value) {
        SystemProperties.set(TRANSCODE_MTP_SYS_PROP_KEY, String.valueOf(value));
    }
}
