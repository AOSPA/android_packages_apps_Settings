/*
 * Performance Control - An Android CPU Control application Copyright (C) 2012
 * James Roberts
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.android.settings.omni;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.util.Constants;
import com.android.settings.util.Omni_Helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class BatteryCpuSettings extends Fragment
        implements SeekBar.OnSeekBarChangeListener, Constants {

    private static final String TAG = "BatteryCPUmode";

    private SeekBar mMaxSlider;
    private TextView mMaxSpeedText;
    private String[] mAvailableFrequencies;
    private String mMaxFreqSetting;
    private String mCurMaxSpeed;
    private CurCPUThread mCurCPUThread;
    private boolean mIsTegra3 = false;
    private boolean mIsDynFreq = false;

    private Context context;
    private AlertDialog alertDialog;
    private int mCpuNum = 1;
    private CpuInfoListAdapter mCpuInfoListAdapter;
    private List<String> mCpuInfoListData;
    private LayoutInflater mInflater;

    public class CpuInfoListAdapter extends ArrayAdapter<String> {

        public CpuInfoListAdapter(Context context, int resource, List<String> values) {
            super(context, R.layout.battery_cpu_info_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = mInflater.inflate(R.layout.battery_cpu_info_item, parent, false);
            TextView cpuInfoCore = (TextView) rowView.findViewById(R.id.battery_saver_mode_cpu_info_core);
            TextView cpuInfoFreq = (TextView) rowView.findViewById(R.id.battery_saver_mode_cpu_info_freq);
            cpuInfoCore.setText(getString(R.string.battery_saver_mode_core) + " " + String.valueOf(position) + ": ");
            cpuInfoFreq.setText(mCpuInfoListData.get(position));
            return rowView;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        if (Settings.Global.getInt(context.getContentResolver(),
                     Settings.Global.BATTERY_SAVER_CPU_MODE, 0) == 0) {
            showWarning();
        }
    }

    private void showWarning() {
        alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(R.string.pref_battery_saver_cpu_warning_title);
        alertDialog.setMessage(context.getString(R.string.pref_battery_saver_cpu_warning_summary));
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                 context.getString(R.string.ok), new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                  return;
              }
        });
        alertDialog.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        mInflater = inflater;
        View view = mInflater.inflate(R.layout.battery_cpu_settings, root, false);

        mCpuNum = Omni_Helpers.getNumOfCpus();

        mCpuInfoListData = new ArrayList<String>(mCpuNum);
        for (int i = 0; i < mCpuNum; i++) {
            mCpuInfoListData.add("Core " + String.valueOf(i) + ": ");
        }

        mCpuInfoListAdapter = new CpuInfoListAdapter(
                context, android.R.layout.simple_list_item_1, mCpuInfoListData);

        ListView mCpuInfoList = (ListView) view.findViewById(R.id.battery_saver_mode_cpu_info_list);
        mCpuInfoList.setAdapter(mCpuInfoListAdapter);

        mIsTegra3 = new File(TEGRA_MAX_FREQ_PATH).exists();
        mIsDynFreq = new File(DYN_MAX_FREQ_PATH).exists() && new File(DYN_MIN_FREQ_PATH).exists();
        mAvailableFrequencies = new String[0];

        String availableFrequenciesLine = Omni_Helpers.readOneLine(STEPS_PATH);
        if (availableFrequenciesLine != null) {
            mAvailableFrequencies = availableFrequenciesLine.split(" ");
            Arrays.sort(mAvailableFrequencies, new Comparator<String>() {
                @Override
                public int compare(String object1, String object2) {
                    return Integer.valueOf(object1).compareTo(Integer.valueOf(object2));
                }
            });
        }

        int mFrequenciesNum = mAvailableFrequencies.length - 1;
        String getDefaultCpuValue = null;
        if (new File(DYN_MAX_FREQ_PATH).exists()) {
            getDefaultCpuValue = Omni_Helpers.readOneLine(DYN_MAX_FREQ_PATH);
        } else {
            getDefaultCpuValue = Omni_Helpers.readOneLine(MAX_FREQ_PATH);
        }

        if (mIsTegra3) {
            String curTegraMaxSpeed = Omni_Helpers.readOneLine(TEGRA_MAX_FREQ_PATH);
            int curTegraMax;
            try {
                curTegraMax = Integer.parseInt(curTegraMaxSpeed);
                if (curTegraMax > 0) {
                    getDefaultCpuValue = Integer.toString(curTegraMax);
                }
            } catch (NumberFormatException ignored) {
                // Nothing to do
            }
        }
        String getCpuValue = Settings.Global.getString(context.getContentResolver(),
                     Settings.Global.BATTERY_SAVER_CPU_FREQ);
        if (getCpuValue != null) {
            mCurMaxSpeed = getCpuValue;
        } else {
            mCurMaxSpeed = getDefaultCpuValue;
        }
        Settings.Global.putString(context.getContentResolver(),
                     Settings.Global.BATTERY_SAVER_CPU_FREQ_DEFAULT, getDefaultCpuValue);
        mMaxSlider = (SeekBar) view.findViewById(R.id.battery_saver_mode_max_slider);
        mMaxSlider.setMax(mFrequenciesNum);
        mMaxSpeedText = (TextView) view.findViewById(R.id.battery_saver_mode_max_speed_text);
        mMaxSpeedText.setText(Omni_Helpers.toMHz(mCurMaxSpeed));
        mMaxSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(mCurMaxSpeed));
        mMaxFreqSetting = mCurMaxSpeed;
        mMaxSlider.setOnSeekBarChangeListener(this);

        Switch mCpuEnabled = (Switch) view.findViewById(R.id.battery_saver_cpu_switch);
        mCpuEnabled.setChecked(Settings.Global.getInt(context.getContentResolver(),
                     Settings.Global.BATTERY_SAVER_CPU_MODE, 0) != 0);
        mCpuEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                Settings.Global.putInt(context.getContentResolver(),
                     Settings.Global.BATTERY_SAVER_CPU_MODE, checked ? 1 : 0);
            }
        });

        return view;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (seekBar.getId() == R.id.battery_saver_mode_max_slider) {
                setMaxSpeed(seekBar, progress);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onResume() {
        if (mCurCPUThread == null) {
            mCurCPUThread = new CurCPUThread();
            mCurCPUThread.start();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        Omni_Helpers.updateAppWidget(context);
        super.onPause();

        if (mCurCPUThread != null) {
            if (mCurCPUThread.isAlive()) {
                mCurCPUThread.interrupt();
                try {
                    mCurCPUThread.join();
                } catch (InterruptedException e) {
                }
            }

            mCurCPUThread = null;
        }
    }

    public void setMaxSpeed(SeekBar seekBar, int progress) {
        String current = mAvailableFrequencies[progress];
        mMaxSpeedText.setText(Omni_Helpers.toMHz(current));
        mMaxFreqSetting = current;
        updateSettingsValue(current);
    }

    protected class CurCPUThread extends Thread {
        private boolean mInterrupt = false;

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(500);
                    List<String> freqs = new ArrayList<String>();
                    for (int i = 0; i < mCpuNum; i++) {
                        String cpuFreq = CPU_PATH + String.valueOf(i) + CPU_FREQ_TAIL;
                        String curFreq = "0";
                        if (Omni_Helpers.fileExists(cpuFreq)) {
                            curFreq = Omni_Helpers.readOneLine(cpuFreq);
                        }
                        freqs.add(curFreq);
                    }
                    String[] freqArray = freqs.toArray(new String[freqs.size()]);
                    mCurCPUHandler.sendMessage(mCurCPUHandler.obtainMessage(0, freqArray));
                }
            } catch (InterruptedException e) {
                //return;
            }
        }
    }

    protected Handler mCurCPUHandler = new Handler() {
        public void handleMessage(Message msg) {
            String[] freqArray = (String[]) msg.obj;
            for (int i = 0; i < freqArray.length; i++) {
                // Convert freq in MHz
                try {
                    int freqHz = Integer.parseInt(freqArray[i]);

                    if (freqHz == 0) {
                        mCpuInfoListData.set(i, getString(R.string.battery_saver_mode_core_offline));
                    } else {
                        mCpuInfoListData.set(i, Integer.toString(freqHz / 1000) + " MHz");
                    }
                } catch (Exception e) {
                    // Do nothing
                }
            }
            mCpuInfoListAdapter.notifyDataSetChanged();
        }
    };

    private void updateSettingsValue(String value) {
        Settings.Global.putString(context.getContentResolver(),
                     Settings.Global.BATTERY_SAVER_CPU_FREQ, value);
    }
}

