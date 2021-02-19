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

package com.android.settings.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/** A preference which provide Group action buttons (connect, disconnect,
 * refresh, cacnelRefresh, forget
 **/

public class GroupOptionsPreference extends Preference {

    private static final String TAG = "GroupOptionsPreference";
    private final ButtonInfo mBtnAddSrcGroup = new ButtonInfo();
    private final ButtonInfo mBtnConnect = new ButtonInfo();
    private final ButtonInfo mBtnForget = new ButtonInfo();
    private final ButtonInfo mBtnDisconnect = new ButtonInfo();
    private final ButtonInfo mBtnRefresh = new ButtonInfo();
    private final ButtonInfo mBtnCancelRefresh = new ButtonInfo();
    private final TextViewInfo mTvGroupId = new TextViewInfo();
    private final TextViewInfo mTvStatus = new TextViewInfo();
    private final ProgressInfo mProgressScan = new ProgressInfo();

    public GroupOptionsPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public GroupOptionsPreference(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public GroupOptionsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.bluetooth_group_options);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(true);
        holder.setDividerAllowedBelow(true);
        mBtnAddSrcGroup.mButton = (Button) holder.findViewById(R.id.id_btn_group_add_source);
        mBtnConnect.mButton = (Button) holder.findViewById(R.id.id_btn_connect);
        mBtnForget.mButton = (Button) holder.findViewById(R.id.id_btn_forget);
        mBtnDisconnect.mButton = (Button) holder.findViewById(R.id.id_btn_disconnect);
        mBtnRefresh.mButton = (Button) holder.findViewById(R.id.id_btn_refresh);
        mBtnCancelRefresh.mButton = (Button) holder.findViewById(R.id.id_btn_refresh_cancel);
        mTvGroupId.mTextView = (TextView)holder.findViewById(R.id.id_tv_groupid);
        mTvStatus.mTextView =(TextView)holder.findViewById(R.id.id_tv_status);
        mProgressScan.mProgress = (ProgressBar)holder.findViewById(R.id.id_progress_group_scan);
        mBtnAddSrcGroup.setUpButton();
        mBtnConnect.setUpButton();
        mBtnForget.setUpButton();
        mBtnDisconnect.setUpButton();
        mBtnRefresh.setUpButton();
        mBtnCancelRefresh.setUpButton();
        mTvGroupId.setUpTextView();
        mTvStatus.setUpTextView();
        mProgressScan.setUpProgress();
    }

    public GroupOptionsPreference setAddSourceGroupButtonVisible(boolean isVisible) {
        if (isVisible != mBtnAddSrcGroup.mIsVisible) {
            mBtnAddSrcGroup.mIsVisible = isVisible;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setAddSourceGroupButtonText(@StringRes int textResId) {
        final String newText = getContext().getString(textResId);
        if (!TextUtils.equals(newText, mBtnAddSrcGroup.mText)) {
            mBtnAddSrcGroup.mText = newText;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setAddSourceGroupButtonEnabled(boolean isEnabled) {
        if (isEnabled != mBtnAddSrcGroup.mIsEnabled) {
            mBtnAddSrcGroup.mIsEnabled = isEnabled;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setAddSourceGroupButtonOnClickListener(
        View.OnClickListener listener) {
        if (listener != mBtnAddSrcGroup.mListener) {
            mBtnAddSrcGroup.mListener = listener;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setConnectButtonVisible(boolean isVisible) {
        if (isVisible != mBtnConnect.mIsVisible) {
            mBtnConnect.mIsVisible = isVisible;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setConnectButtonText(@StringRes int textResId) {
        final String newText = getContext().getString(textResId);
        if (!TextUtils.equals(newText, mBtnConnect.mText)) {
            mBtnConnect.mText = newText;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setConnectButtonEnabled(boolean isEnabled) {
        if (isEnabled != mBtnConnect.mIsEnabled) {
            mBtnConnect.mIsEnabled = isEnabled;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setConnectButtonOnClickListener(
            View.OnClickListener listener) {
        if (listener != mBtnConnect.mListener) {
            mBtnConnect.mListener = listener;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setForgettButtonVisible(boolean isVisible) {
        if (isVisible != mBtnForget.mIsVisible) {
            mBtnForget.mIsVisible = isVisible;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setForgetButtonText(@StringRes int textResId) {
        final String newText = getContext().getString(textResId);
        if (!TextUtils.equals(newText, mBtnForget.mText)) {
            mBtnForget.mText = newText;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setForgetButtonEnabled(boolean isEnabled) {
        if (isEnabled != mBtnForget.mIsEnabled) {
            mBtnForget.mIsEnabled = isEnabled;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setForgetButtonOnClickListener(
        View.OnClickListener listener) {
        if (listener != mBtnForget.mListener) {
            mBtnForget.mListener = listener;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setDisconnectButtonVisible(boolean isVisible) {
        if (isVisible != mBtnDisconnect.mIsVisible) {
            mBtnDisconnect.mIsVisible = isVisible;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setDisconnectButtonText(@StringRes int textResId) {
        final String newText = getContext().getString(textResId);
        if (!TextUtils.equals(newText, mBtnDisconnect.mText)) {
            mBtnDisconnect.mText = newText;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setDisconnectButtonEnabled(boolean isEnabled) {
        if (isEnabled != mBtnDisconnect.mIsEnabled) {
            mBtnDisconnect.mIsEnabled = isEnabled;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setDisconnectButtonOnClickListener(
            View.OnClickListener listener) {
        if (listener != mBtnDisconnect.mListener) {
            mBtnDisconnect.mListener = listener;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setRefreshButtonVisible(boolean isVisible) {
        if (isVisible != mBtnRefresh.mIsVisible) {
            mBtnRefresh.mIsVisible = isVisible;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setRefreshButtonText(@StringRes int textResId) {
        final String newText = getContext().getString(textResId);
        if (!TextUtils.equals(newText, mBtnRefresh.mText)) {
            mBtnRefresh.mText = newText;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setRefreshButtonEnabled(boolean isEnabled) {
        if (isEnabled != mBtnRefresh.mIsEnabled) {
            mBtnRefresh.mIsEnabled = isEnabled;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setRefreshButtonOnClickListener(
            View.OnClickListener listener) {
        if (listener != mBtnRefresh.mListener) {
            mBtnRefresh.mListener = listener;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setCancelRefreshButtonText(@StringRes int textResId) {
        final String newText = getContext().getString(textResId);
        if (!TextUtils.equals(newText, mBtnCancelRefresh.mText)) {
            mBtnCancelRefresh.mText = newText;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setCancelRefreshButtonVisible(boolean isVisible) {
        if (isVisible != mBtnCancelRefresh.mIsVisible) {
            mBtnCancelRefresh.mIsVisible = isVisible;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setCancelRefreshButtonEnabled(boolean isEnabled) {
        if (isEnabled != mBtnCancelRefresh.mIsEnabled) {
            mBtnCancelRefresh.mIsEnabled = isEnabled;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setCancelRefreshButtonOnClickListener(
            View.OnClickListener listener) {
        if (listener != mBtnCancelRefresh.mListener) {
            mBtnCancelRefresh.mListener = listener;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setTvSetIdVisible(boolean isVisible) {
        if (isVisible != mTvGroupId.mIsVisible) {
            mTvGroupId.mIsVisible = isVisible;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setTextViewText(String newText) {
        if (!TextUtils.equals(newText, mTvGroupId.mText)) {
            mTvGroupId.mText = newText;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setTvStatusVisible(boolean isVisible) {
        if (isVisible != mTvStatus.mIsVisible) {
            mTvStatus.mIsVisible = isVisible;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setTexStatusText(@StringRes int textResId) {
        final String newText = getContext().getString(textResId);
        if (!TextUtils.equals(newText, mTvStatus.mText)) {
            mTvStatus.mText = newText;
            notifyChanged();
        }
        return this;
    }

    public GroupOptionsPreference setProgressScanVisible(boolean isVisible) {
        if (isVisible != mProgressScan.mIsVisible) {
            mProgressScan.mIsVisible = isVisible;
            notifyChanged();
        }
        return this;
    }

    static class ButtonInfo {
        private Button mButton;
        private CharSequence mText;
        private View.OnClickListener mListener;
        private boolean mIsEnabled = true;
        private boolean mIsVisible = true;

        void setUpButton() {
            mButton.setText(mText);
            mButton.setOnClickListener(mListener);
            mButton.setEnabled(mIsEnabled);
            mButton.setTypeface(null, Typeface.BOLD);
            if (shouldBeVisible()) {
                mButton.setVisibility(View.VISIBLE);
            } else {
                mButton.setVisibility(View.GONE);
            }
        }

        private boolean shouldBeVisible() {
            return mIsVisible && (!TextUtils.isEmpty(mText));
        }
    }

    static class TextViewInfo {
        private TextView mTextView;
        private CharSequence mText;
        private boolean mIsVisible = true;

        void setUpTextView() {
            mTextView.setText(mText);
            mTextView.setTypeface(null, Typeface.BOLD);
            if (shouldBeVisible()) {
                mTextView.setVisibility(View.VISIBLE);
            } else {
                mTextView.setVisibility(View.INVISIBLE);
            }
        }

        private boolean shouldBeVisible() {
            return mIsVisible && (!TextUtils.isEmpty(mText));
        }
    }

    static class ProgressInfo {
        private ProgressBar mProgress;
        private boolean mIsVisible = true;
        void setUpProgress() {
            if (shouldBeVisible()) {
                mProgress.setVisibility(View.VISIBLE);
            } else {
                mProgress.setVisibility(View.INVISIBLE);
            }
        }

        private boolean shouldBeVisible() {
            return mIsVisible;
        }
    }
}
