/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.customization.model.clock;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

import com.android.customization.model.CustomizationManager.OptionsFetchedListener;
import com.android.customization.model.ResourcesApkProvider;
import com.android.customization.model.clock.Clockface.Builder;
import com.android.wallpaper.R;

import java.util.ArrayList;
import java.util.List;

public class ResourcesApkClockProvider extends ResourcesApkProvider implements ClockProvider {

    private static final String TAG = "ResourcesApkClockProvider";

    private static final String CLOCKS_ARRAY = "clocks";
    private static final String TITLE_PREFIX = "clock_title_";
    private static final String ID_PREFIX = "clock_id_";
    private static final String PREVIEW_PREFIX = "clock_preview_";
    private static final String THUMBNAIL_PREFIX = "clock_thumbnail_";

    private List<Clockface> mClocks;

    public ResourcesApkClockProvider(Context context){
        super(context, context.getString(R.string.clocks_stub_package));
    }

    @Override
    public void fetch(OptionsFetchedListener<Clockface> callback, boolean reload) {
        if (mClocks == null || reload) {
            mClocks = new ArrayList<>();
            loadAll();
        }

        if(callback != null) {
            callback.onOptionsLoaded(mClocks);
        }
    }

    private void loadAll() {
        String[] clockNames = getItemsFromStub(CLOCKS_ARRAY);

        for (String clockName : clockNames) {
            try {
                Builder builder = new Builder();

                builder.setTitle(getItemStringFromStub(TITLE_PREFIX, clockName))
                        .setId(getItemStringFromStub(ID_PREFIX, clockName))
                        .setPreview(getItemDrawableFromStub(PREVIEW_PREFIX, clockName))
                        .setThumbnail(getItemDrawableFromStub(THUMBNAIL_PREFIX, clockName));

                mClocks.add(builder.build());
            } catch (NotFoundException e) {
                Log.i(TAG, "Resource not found, skipping clock", e);
            }
        }
    }
}
