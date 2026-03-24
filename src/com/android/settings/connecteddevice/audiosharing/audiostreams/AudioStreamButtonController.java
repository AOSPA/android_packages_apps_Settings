/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.PAUSED;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.STREAMING;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.getLocalSourceState;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.ActionButtonsPreference;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioStreamButtonController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private static final String TAG = "AudioStreamButtonController";
    private static final String KEY = "audio_stream_button";
    private static final int SOURCE_ORIGIN_BT_API =
            SourceOriginForLogging.GET_SOURCE_BLUETOOTH_API.getValue();

    @VisibleForTesting
    final BluetoothLeBroadcastAssistant.Callback mBroadcastAssistantCallback =
            new AudioStreamsBroadcastAssistantCallback() {
                @Override
                public void onSourceRemoved(BluetoothDevice sink, int sourceId, int reason) {
                    super.onSourceRemoved(sink, sourceId, reason);
                    updateButton();
                }

                @Override
                public void onSourceRemoveFailed(BluetoothDevice sink, int sourceId, int reason) {
                    super.onSourceRemoveFailed(sink, sourceId, reason);
                    updateButton();
                    mMetricsFeatureProvider.action(
                            mContext, SettingsEnums.ACTION_AUDIO_STREAM_LEAVE_FAILED);
                }

                @Override
                public void onReceiveStateChanged(
                        BluetoothDevice sink,
                        int sourceId,
                        BluetoothLeBroadcastReceiveState state) {
                    super.onReceiveStateChanged(sink, sourceId, state);
                    var localSourceState = getLocalSourceState(state);
                    boolean shouldUpdateButton =
                            mHysteresisModeFixAvailable
                                    ? (localSourceState == PAUSED || localSourceState == STREAMING)
                                    : localSourceState == STREAMING;
                    if (shouldUpdateButton) {
                        updateButton();
                        // TODO(b/308368124): Verify if this log is too noisy.
                        mMetricsFeatureProvider.action(
                                mContext,
                                localSourceState == PAUSED
                                        ? SettingsEnums.ACTION_AUDIO_STREAM_JOIN_PRESENT_SUCCEED
                                        : SettingsEnums.ACTION_AUDIO_STREAM_JOIN_SUCCEED,
                                SOURCE_ORIGIN_BT_API);
                    }
                }

                @Override
                public void onSourceAddFailed(
                        BluetoothDevice sink, BluetoothLeBroadcastMetadata source, int reason) {
                    super.onSourceAddFailed(sink, source, reason);
                    updateButton();
                    mMetricsFeatureProvider.action(
                            mContext,
                            SettingsEnums.ACTION_AUDIO_STREAM_JOIN_FAILED_OTHER,
                            SOURCE_ORIGIN_BT_API);
                }

                @Override
                public void onSourceLost(int broadcastId) {
                    super.onSourceLost(broadcastId);
                    updateButton();
                }
            };

    private final Executor mExecutor;
    private final AudioStreamsHelper mAudioStreamsHelper;
    private final @Nullable LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final boolean mHysteresisModeFixAvailable;
    private @Nullable ActionButtonsPreference mPreference;
    private int mBroadcastId = -1;
    @VisibleForTesting BluetoothLeBroadcastMetadata mSourceMetadata;

    public AudioStreamButtonController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mExecutor = Executors.newSingleThreadExecutor();
        mAudioStreamsHelper = new AudioStreamsHelper(Utils.getLocalBtManager(context));
        mLeBroadcastAssistant = mAudioStreamsHelper.getLeBroadcastAssistant();
        mHysteresisModeFixAvailable =
                BluetoothUtils.isAudioSharingHysteresisModeFixAvailable(context);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "onStart(): LeBroadcastAssistant is null!");
            return;
        }
        mLeBroadcastAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "onStop(): LeBroadcastAssistant is null!");
            return;
        }
        mLeBroadcastAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
    }

    @Override
    public final void displayPreference(PreferenceScreen screen) {
        mPreference = screen.findPreference(getPreferenceKey());
        var unused = ThreadUtils.postOnBackgroundThread(this::updateButton);
        super.displayPreference(screen);
    }

    private void updateButton() {
        if (mPreference == null) {
            Log.w(TAG, "updateButton(): preference is null!");
            return;
        }

        var connectedDeviceAndState = findConnectedDeviceAndState(mBroadcastId);
        View.OnClickListener onClickListener;
        if (connectedDeviceAndState != null) {
            onClickListener =
                    unused ->
                            ThreadUtils.postOnBackgroundThread(
                                    () -> {
                                        if (mLeBroadcastAssistant != null) {
                                            // Cache source data for rejoining, this metadata will
                                            // have channel selected
                                            mSourceMetadata =
                                                    mLeBroadcastAssistant.getSourceMetadata(
                                                            connectedDeviceAndState.first,
                                                            connectedDeviceAndState.second
                                                                    .getSourceId());
                                        }
                                        mAudioStreamsHelper.removeSource(mBroadcastId);
                                        mMetricsFeatureProvider.action(
                                                mContext,
                                                SettingsEnums
                                                        .ACTION_AUDIO_STREAM_LEAVE_BUTTON_CLICK);
                                        ThreadUtils.postOnMainThread(
                                                () -> {
                                                    if (mPreference != null) {
                                                        mPreference.setButton1Enabled(false);
                                                    }
                                                });
                                    });
            ThreadUtils.postOnMainThread(
                    () -> {
                        if (mPreference != null) {
                            mPreference.setButton1Enabled(true);
                            mPreference
                                    .setButton1Text(R.string.audio_streams_disconnect)
                                    .setButton1Icon(
                                            com.android.settings.R.drawable.ic_settings_close)
                                    .setButton1OnClickListener(onClickListener);
                        }
                    });
        } else {
            onClickListener =
                    unused ->
                            ThreadUtils.postOnBackgroundThread(
                                    () -> {
                                        if (mSourceMetadata != null) {
                                            // As cached metadata is used, we need to make sure
                                            // channels are deselected to get the "original"
                                            // metadata.
                                            var original =
                                                    AudioStreamsHelper.deselectAllChannels(
                                                            mSourceMetadata);
                                            if (original != null) {
                                                mAudioStreamsHelper.addSource(original);

                                                mMetricsFeatureProvider.action(
                                                        mContext,
                                                        SettingsEnums.ACTION_AUDIO_STREAM_JOIN,
                                                        SOURCE_ORIGIN_BT_API);
                                                ThreadUtils.postOnMainThread(
                                                        () -> {
                                                            if (mPreference != null) {
                                                                mPreference.setButton1Enabled(
                                                                        false);
                                                            }
                                                        });
                                            }
                                        } else {
                                            Log.w(
                                                    TAG,
                                                    "updateButton(): SourceMetadata is null, can't"
                                                            + " add source.");
                                        }
                                    });
            ThreadUtils.postOnMainThread(
                    () -> {
                        if (mPreference != null) {
                            mPreference.setButton1Enabled(true);
                            mPreference
                                    .setButton1Text(R.string.audio_streams_connect)
                                    .setButton1Icon(com.android.settings.R.drawable.ic_add_24dp)
                                    .setButton1OnClickListener(onClickListener);
                        }
                    });
        }
    }

    private @Nullable Pair<BluetoothDevice, BluetoothLeBroadcastReceiveState>
            findConnectedDeviceAndState(int broadcastId) {
        Map<BluetoothDevice, List<BluetoothLeBroadcastReceiveState>> sourceByDevice =
                mAudioStreamsHelper.getAllSourcesByDevice();
        for (Map.Entry<BluetoothDevice, List<BluetoothLeBroadcastReceiveState>> entry :
                sourceByDevice.entrySet()) {
            BluetoothDevice device = entry.getKey();
            for (BluetoothLeBroadcastReceiveState state : entry.getValue()) {
                if (state.getBroadcastId() == broadcastId) {
                    return new Pair<>(device, state);
                }
            }
        }
        return null;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    /** Initialize with broadcast id */
    void init(int broadcastId) {
        mBroadcastId = broadcastId;
    }
}
