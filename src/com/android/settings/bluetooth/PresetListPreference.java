/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.bluetooth;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.CustomListPreference;
import com.android.settings.R;

/**
 * A preference of hearing device preset list which will refresh the selected item in the dialog
 * when value is changed.
 */
public class PresetListPreference extends CustomListPreference {

    @Nullable
    private PresetArrayAdapter mAdapter;

    public PresetListPreference(@NonNull Context context) {
        super(context, null);
    }

    @Override
    public void setValue(@Nullable String value) {
        super.setValue(value);
        if (mAdapter != null) {
            mAdapter.setSelectedIndex(getSelectedValueIndex());
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        mAdapter = new PresetArrayAdapter(builder.getContext(), getEntries(),
                getSelectedValueIndex());
        builder.setAdapter(mAdapter, listener);
    }

    @VisibleForTesting
    void setAdapter(PresetArrayAdapter adapter) {
        mAdapter = adapter;
    }

    private int getSelectedValueIndex() {
        final String selectedValue = getValue();
        return (selectedValue == null) ? -1 : findIndexOfValue(selectedValue);
    }

    public static class PresetArrayAdapter extends ArrayAdapter<CharSequence> {
        private int mSelectedIndex;

        public PresetArrayAdapter(@NonNull Context context, @NonNull CharSequence[] objects,
                int selectedIndex) {
            super(context, R.layout.preset_dialog_singlechoice, R.id.text1, objects);
            mSelectedIndex = selectedIndex;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View root = super.getView(position, convertView, parent);
            CheckedTextView text = root.findViewById(R.id.text1);
            if (text != null && mSelectedIndex != -1) {
                text.setChecked(position == mSelectedIndex);
            }
            return root;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        /**
         * Updates the selected index.
         */
        public void setSelectedIndex(int index) {
            mSelectedIndex = index;
            notifyDataSetChanged();
        }
    }
}
