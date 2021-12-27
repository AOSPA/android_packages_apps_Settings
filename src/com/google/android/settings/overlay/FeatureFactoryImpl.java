package com.google.android.settings.overlay;

import android.content.Context;

import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.google.android.settings.fuelgauge.PowerUsageFeatureProviderGoogleImpl;

public final class FeatureFactoryImpl extends com.android.settings.overlay.FeatureFactoryImpl {

    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    @Override
    public PowerUsageFeatureProvider getPowerUsageFeatureProvider(Context context) {
        if (mPowerUsageFeatureProvider == null) {
            mPowerUsageFeatureProvider = new PowerUsageFeatureProviderGoogleImpl(
                    context.getApplicationContext());
        }
        return mPowerUsageFeatureProvider;
    }
}
