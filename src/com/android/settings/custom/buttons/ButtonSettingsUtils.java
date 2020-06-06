/*
* Copyright (C) 2018 The Pixel Experience Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.settings.custom.buttons;

import android.content.Context;
import android.os.SystemProperties;
import com.android.internal.util.aospa.NavbarUtils;

public class ButtonSettingsUtils {
    public static boolean isAvailable(Context context) {
        return NavbarUtils.canDisable(context)
                && SystemProperties.getBoolean("ro.vendor.settings.ui.hardwarekeys.present", true);
    }
}
