package com.google.android.settings.overlay;

import android.content.Context;

import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.applications.GameSettingsFeatureProvider;
import com.google.android.settings.fuelgauge.PowerUsageFeatureProviderGoogleImpl;
import com.google.android.settings.games.GameSettingsFeatureProviderGoogleImpl;

public final class FeatureFactoryImpl extends com.android.settings.overlay.FeatureFactoryImpl {

    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private GameSettingsFeatureProvider mGameSettingsFeatureProvider;

    @Override
    public PowerUsageFeatureProvider getPowerUsageFeatureProvider(Context context) {
        if (mPowerUsageFeatureProvider == null) {
            mPowerUsageFeatureProvider = new PowerUsageFeatureProviderGoogleImpl(
                    context.getApplicationContext());
        }
        return mPowerUsageFeatureProvider;
    }

    @Override
    public GameSettingsFeatureProvider getGameSettingsFeatureProvider() {
        if (mGameSettingsFeatureProvider == null) {
            mGameSettingsFeatureProvider = new GameSettingsFeatureProviderGoogleImpl();
        }
        return mGameSettingsFeatureProvider;
    }
}
