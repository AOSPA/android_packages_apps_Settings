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
package com.android.customization.model.grid;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.customization.model.CustomizationManager;

import java.util.List;

/**
 * {@link CustomizationManager} for interfacing with the launcher to handle {@link GridOption}s.
 */
public class GridOptionsManager implements CustomizationManager<GridOption> {

    private final LauncherGridOptionsProvider mProvider;

    public GridOptionsManager(LauncherGridOptionsProvider provider) {
        mProvider = provider;
    }

    @Override
    public boolean isAvailable() {
        return mProvider.areGridsAvailable();
    }

    @Override
    public void apply(GridOption option, Callback callback) {
        int updated = mProvider.applyGrid(option.name);
        if (updated == 1) {
            callback.onSuccess();
        } else {
            callback.onError(null);
        }
    }

    @Override
    public void fetchOptions(OptionsFetchedListener<GridOption> callback) {
        new FetchTask(mProvider, callback).execute();
    }

    private static class FetchTask extends AsyncTask<Void, Void, List<GridOption>> {
        private final LauncherGridOptionsProvider mProvider;
        @Nullable private final OptionsFetchedListener<GridOption> mCallback;

        private FetchTask(@NonNull LauncherGridOptionsProvider provider,
                @Nullable OptionsFetchedListener<GridOption> callback) {
            mCallback = callback;
            mProvider = provider;
        }

        @Override
        protected List<GridOption> doInBackground(Void[] params) {
            return mProvider.fetch(false);
        }

        @Override
        protected void onPostExecute(List<GridOption> gridOptions) {
            if (mCallback != null) {
                mCallback.onOptionsLoaded(gridOptions);
            }
        }
    }
}
