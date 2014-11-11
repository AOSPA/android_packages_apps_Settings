package com.android.settings;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class MainSettings extends TabActivity {

    private TabHost tabHost = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tabs_setting);

        tabHost = getTabHost();

        //Connection tab
        tabHost.addTab(tabHost.newTabSpec(getString(R.string.tab_connection))
            .setContent(new Intent(this, ConnectionSettingsTab.class))
            .setIndicator(prepareTabView(getString(R.string.tab_connection),
                R.drawable.ic_tab_connection)));

        //My device tab
        tabHost.addTab(tabHost.newTabSpec(getString(R.string.tab_device))
            .setContent(new Intent(this, DeviceSettingsTab.class))
            .setIndicator(prepareTabView(getString(R.string.tab_device),
                R.drawable.ic_tab_device)));

        //Accounts tab
        tabHost.addTab(tabHost.newTabSpec(getString(R.string.tab_account))
            .setContent(new Intent(this, AccountSettingsTab.class))
            .setIndicator(prepareTabView(getString(R.string.tab_account),
                R.drawable.ic_tab_account)));

        //More tab
        tabHost.addTab(tabHost.newTabSpec(getString(R.string.tab_more))
            .setContent(new Intent(this, MoreSettingsTab.class))
            .setIndicator(prepareTabView(getString(R.string.tab_more),
                R.drawable.ic_tab_more)));
    }

    private View prepareTabView(String text, int resId) {
        View view = LayoutInflater.from(this).inflate(R.layout.tabs_bg, null);
        ImageView iv = (ImageView) view.findViewById(R.id.tabsImage);
        TextView tv = (TextView) view.findViewById(R.id.tabsText);

        iv.setImageResource(resId);
        tv.setText(text);

        return view;
    }
}
