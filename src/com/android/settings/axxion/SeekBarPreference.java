/* Copyright (C) 2013-2014 Dokdo Project - Gwon Hyeok
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
package com.android.settings.axxion;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.android.settings.R;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {

    private String property;

    private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";
    private static final String SETTINGS = "http://schemas.android.com/apk/res/com.android.settings";
    private static final int DEFAULT_VALUE = 50;

    int defaultValue = 60;
    int mSetDefault = -1;
    int mMultiply = -1;
    int mMinimum = -1;
    boolean mDisableText = false;
    boolean mDisablePercentageValue = false;

    private OnPreferenceChangeListener changer;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPreference(context, attrs);
    }

    public void setInitValue(int progress) {
        defaultValue = progress;
        if (bar!=null) {
            bar.setProgress(progress);
            if (!mDisablePercentageValue) {
                monitorBox.setText(progress + "%");
            }
        }
    }

    private String getAttributeStringValue(AttributeSet attrs, String namespace, String name, String defaultValue) {
        String value = attrs.getAttributeValue(namespace, name);
        if(value == null)
            value = defaultValue;

        return value;
    }

    @Override
    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        super.onDependencyChanged(dependency, disableDependent);
        this.setShouldDisableView(true);
        if (mTitle != null)
            mTitle.setEnabled(!disableDependent);
        if (mSeekBar != null)
            mSeekBar.setEnabled(!disableDependent);
    }

    @Override
    protected View onCreateView(ViewGroup parent){
        RelativeLayout layout =  null;
        try {
            LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = (RelativeLayout)mInflater.inflate(R.layout.seek_bar_preference, parent, false);
            mTitle = (TextView) layout.findViewById(android.R.id.title);
        }
        catch(Exception e)
        {
            Log.e(TAG, "Error creating seek bar preference", e);
        }
        return layout;
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        try
        {
            // move our seekbar to the new view we've been given
            ViewParent oldContainer = mSeekBar.getParent();
            ViewGroup newContainer = (ViewGroup) view.findViewById(R.id.seekBarPrefBarContainer);

            if (oldContainer != newContainer) {
                // remove the seekbar from the old view
                if (oldContainer != null) {
                    ((ViewGroup) oldContainer).removeView(mSeekBar);
                }
                // remove the existing seekbar (there may not be one) and add ours
                newContainer.removeAllViews();
                newContainer.addView(mSeekBar, ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
        catch(Exception ex) {
            Log.e(TAG, "Error binding view: " + ex.toString());
        }
        updateView(view);
    }

    public void disablePercentageValue(boolean disable) {
        mDisablePercentageValue = disable;
    }

    public void setProperty(String property) {
        this.property = property;
    }
    
    public void disableText(boolean disable) {
        mDisableText = disable;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

}
