package com.android.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

public class AccountFeatureProviderImpl implements AccountFeatureProvider {

	private static final String ACCOUNT_PROVIDER_GOOGLE = "com.google";
	
    @Override
    public String getAccountType() {
        return ACCOUNT_PROVIDER_GOOGLE;
    }

    @Override
    public Account[] getAccounts(Context context) {
        return AccountManager.get(context).getAccountsByType(ACCOUNT_PROVIDER_GOOGLE);
    }
}
