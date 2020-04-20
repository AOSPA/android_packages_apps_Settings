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
 * limitations under the License.
 */

package com.android.settings.deviceinfo.aboutphone;

import android.accounts.Account;
import android.annotation.IdRes;
import android.annotation.Nullable;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.ActionBarShadowController;
import com.android.settingslib.widget.LayoutPreference;

import java.net.URISyntaxException;
import java.util.List;

public class DeviceInfoHeaderController implements LifecycleObserver {

    private static final String TAG = "DeviceInfoHeaderController";

    @VisibleForTesting
    static final Intent INTENT_GET_ACCOUNT_DATA =
            new Intent("android.content.action.SETTINGS_ACCOUNT_DATA");

    private static final String METHOD_GET_ACCOUNT_AVATAR = "getAccountAvatar";
    private static final String KEY_AVATAR_BITMAP = "account_avatar";
    private static final String KEY_ACCOUNT_NAME = "account_name";
    private static final String EXTRA_ACCOUNT_NAME = "extra.accountName";

    private final ActivityManager mActivityManager;
    private final Context mContext;
    private final Activity mActivity;
    private final Fragment mFragment;
    private final View mHeader;
    private Lifecycle mLifecycle;
    private RecyclerView mRecyclerView;

    private MutableLiveData<Bitmap> mAvatarImage;
    private Drawable mAvatar;
    private ImageView mAvatarView;
    private CharSequence mLabel;
    private CharSequence mSummary;

    private Observer mObserver = new Observer<Bitmap>() {
        @Override
        public void onChanged(@Nullable Bitmap bitmap) {
            if (bitmap != null) {
                Drawable avatar = new BitmapDrawable(mContext.getResources(), bitmap);
                mAvatar = avatar.getConstantState().newDrawable(mContext.getResources());
                update();
            }
        }

    };

    public DeviceInfoHeaderController(Activity activity, Fragment fragment, View header, RecyclerView recyclerView, Lifecycle lifecycle) {
        mActivity = activity;
        mContext = activity.getApplicationContext();
        mActivityManager = mContext.getSystemService(ActivityManager.class);
        mFragment = fragment;
        mRecyclerView = recyclerView;
        mLifecycle = lifecycle;
        if (header != null) {
            mHeader = header;
        } else {
            mHeader = LayoutInflater.from(fragment.getContext())
                    .inflate(R.layout.settings_entity_header, null /* root */);
        }

        mAvatarImage = new MutableLiveData<>();
        mAvatarImage.observeForever(mObserver);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (!mContext.getResources().getBoolean(R.bool.config_show_avatar_in_homepage)) {
            addMultiUserInfo();
            Log.d(TAG, "Feature disabled by config. Skipping");
            return;
        }
        if (mActivityManager.isLowRamDevice()) {
            addMultiUserInfo();
            Log.d(TAG, "Feature disabled on low ram device. Skipping");
            return;
        }
        if (hasAccount()) {
            loadAccount();
        } else {
            addMultiUserInfo();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mAvatarImage.removeObserver(mObserver);
    }

    private void addMultiUserInfo() {
        final UserManager um = (UserManager) mActivity.getSystemService(
                Context.USER_SERVICE);
        final UserInfo info = Utils.getExistingUser(um, android.os.Process.myUserHandle());
        mLabel = info.name;
        Drawable avatar = com.android.settingslib.Utils.getUserIcon(mActivity, um, info);
        mAvatar = avatar.getConstantState().newDrawable(mContext.getResources());
        update();
    }

    @VisibleForTesting
    boolean hasAccount() {
        final Account accounts[] = FeatureFactory.getFactory(
                mContext).getAccountFeatureProvider().getAccounts(mContext);
        return (accounts != null) && (accounts.length > 0);
    }

    private void loadAccount() {
        final String authority = queryProviderAuthority();
        if (TextUtils.isEmpty(authority)) {
            addMultiUserInfo();
            return;
        }

        ThreadUtils.postOnBackgroundThread(() -> {
            final Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(authority)
                    .build();
            final Bundle bundle = mContext.getContentResolver().call(uri,
                    METHOD_GET_ACCOUNT_AVATAR, null /* arg */, null /* extras */);
            final Bitmap bitmap = bundle.getParcelable(KEY_AVATAR_BITMAP);
            mLabel = bundle.getString(KEY_ACCOUNT_NAME, "" /* defaultValue */);
            mAvatarImage.postValue(bitmap);
        });
    }

    @VisibleForTesting
    String queryProviderAuthority() {
        final List<ResolveInfo> providers =
                mContext.getPackageManager().queryIntentContentProviders(INTENT_GET_ACCOUNT_DATA,
                        PackageManager.MATCH_SYSTEM_ONLY);
        if (providers.size() == 1) {
            return providers.get(0).providerInfo.authority;
        } else {
            Log.w(TAG, "The size of the provider is " + providers.size());
            return null;
        }
    }

    public View update() {
        styleActionBar(mActivity);
        ImageView avatarView = mHeader.findViewById(R.id.entity_header_icon);
        if (avatarView != null) {
            avatarView.setImageDrawable(mAvatar);
            avatarView.setOnClickListener(v -> {
                Intent intent;
                try {
                    final String uri = mContext.getResources().getString(
                            R.string.config_account_intent_uri);
                    intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
                } catch (URISyntaxException e) {
                    Log.w(TAG, "Error parsing avatar mixin intent, skipping", e);
                    return;
                }

                if (!TextUtils.isEmpty(mLabel)) {
                    intent.putExtra(EXTRA_ACCOUNT_NAME, mLabel);
                }

                final List<ResolveInfo> matchedIntents =
                        mContext.getPackageManager().queryIntentActivities(intent,
                                PackageManager.MATCH_SYSTEM_ONLY);
                if (matchedIntents.isEmpty()) {
                    Log.w(TAG, "Cannot find any matching action VIEW_ACCOUNT intent.");
                    return;
                }

                // Here may have two different UI while start the activity.
                // It will display adding account UI when device has no any account.
                // It will display account information page when intent added the specified account.
                mActivity.startActivity(intent);
            });
        }
        setText(R.id.entity_header_title, mLabel);
        setText(R.id.entity_header_summary, mSummary);
        return mHeader;
    }

    /**
     * Styles the action bar (elevation, scrolling behaviors, color, etc).
     * <p/>
     * This method must be called after {@link Fragment#onCreate(Bundle)}.
     */
    public DeviceInfoHeaderController styleActionBar(Activity activity) {
        if (activity == null) {
            Log.w(TAG, "No activity, cannot style actionbar.");
            return this;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (actionBar == null) {
            Log.w(TAG, "No actionbar, cannot style actionbar.");
            return this;
        }
        actionBar.setBackgroundDrawable(
                new ColorDrawable(
                        Utils.getColorAttrDefaultColor(activity, android.R.attr.colorPrimaryDark)));
        actionBar.setElevation(0);
        if (mRecyclerView != null && mLifecycle != null) {
            ActionBarShadowController.attachToView(mActivity, mLifecycle, mRecyclerView);
        }

        return this;
    }

    private void setText(@IdRes int id, CharSequence text) {
        TextView textView = mHeader.findViewById(id);
        if (textView != null) {
            textView.setText(text);
            textView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
        }
    }
}
