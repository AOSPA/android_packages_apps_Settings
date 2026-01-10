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

package com.android.settings.vpn2;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

public class AppExclusionViewModel extends ViewModel {
    private static final String TAG = "AppExclusionViewModel";

    private final Application mApplication;
    private final int mUserId;
    private final String mVpnPackage;
    private final ListeningExecutorService mExecutorService;
    private final MutableLiveData<Boolean> mSetAppExclusionEnabledLiveData =
            new MutableLiveData<>();

    private final FutureCallback<Boolean> mAppExclusionListUpdaterCb =
            new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    mSetAppExclusionEnabledLiveData.postValue(result);
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.w(TAG, "set VPN application exclusion failed.");
                }
            };

    final Comparator<String> mComparator;

    MutableLiveData<List<String>> mAppExclusionEnabledLiveData = new MutableLiveData<>();
    MutableLiveData<List<String>> mAppExclusionCandidateLiveData = new MutableLiveData<>();

    /** Constructor for AppExclusionViewModel */
    public AppExclusionViewModel(Application application, int userId, String vpnPackage) {
        this(
                application,
                userId,
                vpnPackage,
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
    }

    @VisibleForTesting
    AppExclusionViewModel(
            Application application,
            int userId,
            String vpnPackage,
            ListeningExecutorService executor) {
        mApplication = application;
        mUserId = userId;
        mVpnPackage = vpnPackage;
        mExecutorService = executor;
        mComparator =
                comparing(
                        (String arg) ->
                                AppExclusionUtils.getApplicationLabel(mApplication, mUserId, arg),
                        String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Returns the {@link LiveData} storing the list of VPN application exclusion candidates, which
     * will be presented in the setting activities.
     */
    public LiveData<List<String>> getAppExclusionCandidateLiveData() {
        return mAppExclusionCandidateLiveData;
    }

    /**
     * Returns the {@link LiveData} storing the list of VPN application exclusion enabled list,
     * which will be presented in the setting activities.
     */
    public LiveData<List<String>> getAppExclusionEnabledLiveData() {
        return mAppExclusionEnabledLiveData;
    }

    /**
     * Returns the {@link LiveData} that indicates the result of setting the VPN app exclusion list.
     * The value is true if the operation was successful.
     */
    public LiveData<Boolean> getSetAppExclusionEnabledLiveData() {
        return mSetAppExclusionEnabledLiveData;
    }

    /**
     * Retrieves the VPN app exclusion enabled and candidate list. The result will be notified via
     * {@code mAppExclusionEnabledLiveData} and {@code mAppExclusionCandidateLiveData}. The
     * candidate list has to filter the enabled apps.
     */
    public void fillAppExclusionLists() {
        Futures.addCallback(
                Futures.submit(this::queryAppExclusionEnabled, mExecutorService),
                new FutureCallback<List<String>>() {
                    @Override
                    public void onSuccess(List<String> exclusionListResult) {
                        mAppExclusionEnabledLiveData.postValue(exclusionListResult);
                        fillAppExclusionCandidate(exclusionListResult);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.w(TAG, "get VPN application exclusion failed.");
                    }
                },
                mExecutorService);
    }

    void fillAppExclusionCandidate(List<String> exclusionListResult) {
        Futures.addCallback(
                Futures.submit(this::queryAppExclusionCandidate, mExecutorService),
                new FutureCallback<List<ApplicationInfo>>() {
                    @Override
                    public void onSuccess(List<ApplicationInfo> candidateListResult) {
                        List<String> candidateList = new ArrayList<>();
                        for (String candidate :
                                AppExclusionUtils.fetchNameListFromInfoList(candidateListResult)) {
                            if (!exclusionListResult.contains(candidate)) {
                                candidateList.add(candidate);
                            }
                        }
                        candidateList.sort(mComparator);
                        mAppExclusionCandidateLiveData.postValue(candidateList);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.d(TAG, "get VPN application exclusion candidates failed.");
                    }
                },
                mExecutorService);
    }

    /**
     * Retrieves the result of setting VPN app exclusion list. Once it is success, {@code
     * mAppExclusionEnabledLiveData} and {@code mAppExclusionCandidateLiveData} should be updated.
     */
    public void addAppToExclusionList(String targetPackageName) {
        appExclusionListUpdater(targetPackageName, /* add= */ true);
    }

    /**
     * Retrieves the result of removing VPN app exclusion from list. Once it is success, {@code
     * mAppExclusionEnabledLiveData} and {@code mAppExclusionCandidateLiveData} should be updated.
     */
    public void removeAppFromExclusionList(String targetPackageName) {
        appExclusionListUpdater(targetPackageName, /* add= */ false);
    }

    private void appExclusionListUpdater(String targetPackageName, boolean add) {
        Futures.addCallback(
                Futures.submit(this::queryAppExclusionEnabled, mExecutorService),
                new FutureCallback<List<String>>() {
                    @Override
                    public void onSuccess(List<String> enableListResult) {
                        List<String> newList = new ArrayList<>();
                        if (enableListResult != null) {
                            newList.addAll(enableListResult);
                        }
                        if (add) {
                            if (newList.contains(targetPackageName)) {
                                return;
                            }
                            newList.add(targetPackageName);
                            newList.sort(mComparator);
                        } else {
                            if (newList.isEmpty()) {
                                return;
                            }
                            newList.remove(targetPackageName);
                        }
                        Futures.addCallback(
                                Futures.submit(
                                        () -> setAppExclusionList(newList), mExecutorService),
                                mAppExclusionListUpdaterCb,
                                mExecutorService);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.d(TAG, "set VPN application exclusion failed.");
                    }
                },
                mExecutorService);
    }

    private List<String> queryAppExclusionEnabled() {
        PackageManager packageManager = mApplication.getPackageManager();
        return AppExclusionUtils.getAppExclusionList(mApplication, mUserId, mVpnPackage).stream()
                .filter(
                        packageName ->
                                PackageUtils.hasLauncherIntent(packageManager, mUserId, packageName)
                                        || PackageUtils.hasCarIntent(
                                                packageManager, mUserId, packageName))
                .collect(toCollection(ArrayList::new));
    }

    @SuppressLint("QueryPermissionsNeeded")
    private List<ApplicationInfo> queryAppExclusionCandidate() {
        PackageManager packageManager = mApplication.getPackageManager();
        return packageManager
                .getInstalledApplicationsAsUser(
                        PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA),
                        mUserId)
                .stream()
                .filter(
                        applicationInfo ->
                                applicationInfo.enabled
                                        && (PackageUtils.hasLauncherIntent(
                                                        packageManager,
                                                        mUserId,
                                                        applicationInfo.packageName)
                                                || PackageUtils.hasCarIntent(
                                                        packageManager,
                                                        mUserId,
                                                        applicationInfo.packageName)))
                .collect(toCollection(ArrayList<ApplicationInfo>::new));
    }

    private boolean setAppExclusionList(List<String> list) {
        return AppExclusionUtils.setAppExclusionList(mApplication, mUserId, mVpnPackage, list);
    }
}
