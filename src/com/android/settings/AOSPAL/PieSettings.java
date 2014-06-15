package com.android.settings.AOSPAL;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class PieSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String KEY_PIE_SIZE = "pie_size";
    private static final String KEY_PIE_GAP = "pie_gap";
    private static final String KEY_PIE_ANGLE = "pie_angle";

    private static final String KEY_SNAP_BACKGROUND_COLOR = "snap_background_color";
    private static final String KEY_BACKGROUND_COLOR = "background_color";
    private static final String KEY_SELECT_COLOR = "select_color";
    private static final String KEY_OUTLINES_COLOR = "outlines_color";
    private static final String KEY_CHEVRON_COLOR = "chevron_color";
    private static final String KEY_STATUS_COLOR = "status_color";
    private static final String KEY_BATTERY_BACKGROUND_COLOR = "battery_background_color";
    private static final String KEY_BATTERY_JUICE_COLOR = "battery_juice_color";
    private static final String KEY_BATTERY_JUICE_LOW_COLOR = "battery_juice_low_color";
    private static final String KEY_BATTERY_JUICE_CRITICAL_COLOR = "battery_juice_critical_color";

    private ListPreference mPieSize;
    private ListPreference mPieGap;
    private ListPreference mPieAngle;

    private ColorPickerPreference mPieSnapBackgroundColor;
    private ColorPickerPreference mPieBackgroundColor;
    private ColorPickerPreference mPieSelectColor;
    private ColorPickerPreference mPieOutlinesColor;
    private ColorPickerPreference mPieChevronColor;
    private ColorPickerPreference mPieStatusColor;
    private ColorPickerPreference mPieBatteryBackgroundColor;
    private ColorPickerPreference mPieBatteryJuiceColor;
    private ColorPickerPreference mPieBatteryJuiceLowColor;
    private ColorPickerPreference mPieBatteryJuiceCriticalColor;

    // Default colors
    static final int COLOR_SNAP_BACKGROUND = 0xaaffffff;
    static final int COLOR_PIE_BACKGROUND = 0x65000000;
    static final int COLOR_PIE_SELECT = 0xaaffffff;
    static final int COLOR_PIE_OUTLINES = 0x55ffffff;
    static final int COLOR_CHEVRON = 0xaaffffff;
    static final int COLOR_STATUS = 0xaaffffff;
    static final int COLOR_BATTERY_BACKGROUND = 0xaaffffff;
    static final int COLOR_BATTERY_JUICE = 0xaaffffff;
    static final int COLOR_BATTERY_JUICE_LOW = 0xffbb33;
    static final int COLOR_BATTERY_JUICE_CRITICAL = 0xff4444;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pie_settings);
        PreferenceScreen prefs = getPreferenceScreen();
        ContentResolver cr = getActivity().getContentResolver();

        mPieSize = (ListPreference) prefs.findPreference(KEY_PIE_SIZE);
        float pieSize = Settings.System.getFloat(getContentResolver(),
                Settings.System.PIE_SIZE, 1.0f);
        mPieSize.setValue(String.valueOf(pieSize));
        mPieSize.setOnPreferenceChangeListener(this);

        mPieGap = (ListPreference) prefs.findPreference(KEY_PIE_GAP);
        int pieGap = Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_GAP, 1);
        mPieGap.setValue(String.valueOf(pieGap));
        mPieGap.setOnPreferenceChangeListener(this);

        mPieAngle = (ListPreference) prefs.findPreference(KEY_PIE_ANGLE);
        int pieAngle = Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_ANGLE, 12);
        mPieAngle.setValue(String.valueOf(pieAngle));
        mPieAngle.setOnPreferenceChangeListener(this);

        mPieSnapBackgroundColor = (ColorPickerPreference) prefs.findPreference(KEY_SNAP_BACKGROUND_COLOR);
        mPieSnapBackgroundColor.setOnPreferenceChangeListener(this);
        int snapBackgroundColor = Settings.System.getInt(cr, Settings.System.PIE_SNAP_BACKGROUND_COLOR, COLOR_SNAP_BACKGROUND);
        String snapBackgroundHexColor = String.format("#%08x", (0xffffffff & snapBackgroundColor));
        mPieSnapBackgroundColor.setSummary(getResources().getString(
                R.string.pie_snap_background_color_summary) + " (" + snapBackgroundHexColor + ")");
        mPieSnapBackgroundColor.setDefaultValue(snapBackgroundColor);
        mPieSnapBackgroundColor.setNewPreviewColor(snapBackgroundColor);
        mPieSnapBackgroundColor.setAlphaSliderEnabled(true);

        mPieBackgroundColor = (ColorPickerPreference) prefs.findPreference(KEY_BACKGROUND_COLOR);
        mPieBackgroundColor.setOnPreferenceChangeListener(this);
        int backgroundColor = Settings.System.getInt(cr, Settings.System.PIE_BACKGROUND_COLOR, COLOR_PIE_BACKGROUND);
        String backgroundHexColor = String.format("#%08x", (0xffffffff & backgroundColor));
        mPieBackgroundColor.setSummary(getResources().getString(
                R.string.pie_snap_background_color_summary) + " (" + backgroundHexColor + ")");
        mPieBackgroundColor.setDefaultValue(backgroundColor);
        mPieBackgroundColor.setNewPreviewColor(backgroundColor);
        mPieBackgroundColor.setAlphaSliderEnabled(true);

        mPieSelectColor = (ColorPickerPreference) prefs.findPreference(KEY_SELECT_COLOR);
        mPieSelectColor.setOnPreferenceChangeListener(this);
        int selectColor = Settings.System.getInt(cr, Settings.System.PIE_SELECT_COLOR, COLOR_PIE_SELECT);
        String selectHexColor = String.format("#%08x", (0xffffffff & selectColor));
        mPieSelectColor.setSummary(getResources().getString(
                R.string.pie_snap_background_color_summary) + " (" + selectHexColor + ")");
        mPieSelectColor.setDefaultValue(selectColor);
        mPieSelectColor.setNewPreviewColor(selectColor);
        mPieSelectColor.setAlphaSliderEnabled(true);

        mPieOutlinesColor = (ColorPickerPreference) prefs.findPreference(KEY_OUTLINES_COLOR);
        mPieOutlinesColor.setOnPreferenceChangeListener(this);
        int outlinesColor = Settings.System.getInt(cr, Settings.System.PIE_OUTLINES_COLOR, COLOR_PIE_OUTLINES);
        String outlinesHexColor = String.format("#%08x", (0xffffffff & outlinesColor));
        mPieOutlinesColor.setSummary(getResources().getString(
                R.string.pie_snap_background_color_summary) + " (" + outlinesHexColor + ")");
        mPieOutlinesColor.setDefaultValue(outlinesColor);
        mPieOutlinesColor.setNewPreviewColor(outlinesColor);
        mPieOutlinesColor.setAlphaSliderEnabled(true);

        mPieChevronColor = (ColorPickerPreference) prefs.findPreference(KEY_CHEVRON_COLOR);
        mPieChevronColor.setOnPreferenceChangeListener(this);
        int chevronColor = Settings.System.getInt(cr, Settings.System.PIE_CHEVRON_COLOR, COLOR_CHEVRON);
        String chevronHexColor = String.format("#%08x", (0xffffffff & chevronColor));
        mPieChevronColor.setSummary(getResources().getString(
                R.string.pie_snap_background_color_summary) + " (" + chevronHexColor + ")");
        mPieChevronColor.setDefaultValue(chevronColor);
        mPieChevronColor.setNewPreviewColor(chevronColor);
        mPieChevronColor.setAlphaSliderEnabled(true);

        mPieStatusColor = (ColorPickerPreference) prefs.findPreference(KEY_STATUS_COLOR);
        mPieStatusColor.setOnPreferenceChangeListener(this);
        int statusColor = Settings.System.getInt(cr, Settings.System.PIE_STATUS_COLOR, COLOR_STATUS);
        String statusHexColor = String.format("#%08x", (0xffffffff & statusColor));
        mPieStatusColor.setSummary(getResources().getString(
                R.string.pie_snap_background_color_summary) + " (" + statusHexColor + ")");
        mPieStatusColor.setDefaultValue(statusColor);
        mPieStatusColor.setNewPreviewColor(statusColor);
        mPieStatusColor.setAlphaSliderEnabled(true);

        mPieBatteryBackgroundColor = (ColorPickerPreference) prefs.findPreference(KEY_BATTERY_BACKGROUND_COLOR);
        mPieBatteryBackgroundColor.setOnPreferenceChangeListener(this);
        int batteryBackgroundColor = Settings.System.getInt(cr, Settings.System.PIE_BATTERY_BACKGROUND_COLOR, COLOR_BATTERY_BACKGROUND);
        String batteryBackgroundHexColor = String.format("#%08x", (0xffffffff & batteryBackgroundColor));
        mPieBatteryBackgroundColor.setSummary(getResources().getString(
                R.string.pie_snap_background_color_summary) + " (" + batteryBackgroundHexColor + ")");
        mPieBatteryBackgroundColor.setDefaultValue(batteryBackgroundColor);
        mPieBatteryBackgroundColor.setNewPreviewColor(batteryBackgroundColor);
        mPieBatteryBackgroundColor.setAlphaSliderEnabled(true);

        mPieBatteryJuiceColor = (ColorPickerPreference) prefs.findPreference(KEY_BATTERY_JUICE_COLOR);
        mPieBatteryJuiceColor.setOnPreferenceChangeListener(this);
        int batteryJuiceColor = Settings.System.getInt(cr, Settings.System.PIE_BATTERY_JUICE_COLOR, COLOR_BATTERY_JUICE);
        String batteryJuiceHexColor = String.format("#%08x", (0xffffffff & batteryJuiceColor));
        mPieBatteryJuiceColor.setSummary(getResources().getString(
                R.string.pie_snap_background_color_summary) + " (" + batteryJuiceHexColor + ")");
        mPieBatteryJuiceColor.setDefaultValue(batteryJuiceColor);
        mPieBatteryJuiceColor.setNewPreviewColor(batteryJuiceColor);
        mPieBatteryJuiceColor.setAlphaSliderEnabled(true);

        mPieBatteryJuiceLowColor = (ColorPickerPreference) prefs.findPreference(KEY_BATTERY_JUICE_LOW_COLOR);
        mPieBatteryJuiceLowColor.setOnPreferenceChangeListener(this);
        int batteryJuiceLowColor = Settings.System.getInt(cr, Settings.System.PIE_BATTERY_JUICE_LOW_COLOR, COLOR_BATTERY_JUICE_LOW);
        String batteryJuiceLowHexColor = String.format("#%08x", (0xffffffff & batteryJuiceLowColor));
        mPieBatteryJuiceLowColor.setSummary(getResources().getString(
                R.string.pie_snap_background_color_summary) + " (" + batteryJuiceLowHexColor + ")");
        mPieBatteryJuiceLowColor.setDefaultValue(batteryJuiceLowColor);
        mPieBatteryJuiceLowColor.setNewPreviewColor(batteryJuiceLowColor);
        mPieBatteryJuiceLowColor.setAlphaSliderEnabled(true);

        mPieBatteryJuiceCriticalColor = (ColorPickerPreference) prefs.findPreference(KEY_BATTERY_JUICE_CRITICAL_COLOR);
        mPieBatteryJuiceCriticalColor.setOnPreferenceChangeListener(this);
        int batteryJuiceCriticalColor = Settings.System.getInt(cr, Settings.System.PIE_BATTERY_JUICE_CRITICAL_COLOR, COLOR_BATTERY_JUICE_CRITICAL);
        String batteryJuiceCriticalHexColor = String.format("#%08x", (0xffffffff & batteryJuiceCriticalColor));
        mPieBatteryJuiceCriticalColor.setSummary(getResources().getString(
                R.string.pie_snap_background_color_summary) + " (" + batteryJuiceCriticalHexColor + ")");
        mPieBatteryJuiceCriticalColor.setDefaultValue(batteryJuiceCriticalColor);
        mPieBatteryJuiceCriticalColor.setNewPreviewColor(batteryJuiceCriticalColor);
        mPieBatteryJuiceCriticalColor.setAlphaSliderEnabled(true);

        // Enable ActionBar menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.pie_settings_item, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reset:
                showResetDialog();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void showResetDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.pie_settings_reset);
        alertDialog.setMessage(R.string.pie_settings_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetColors();
                resetPieSettings();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object value) {
        if (pref == mPieSize) {
            float pieSize = Float.valueOf((String) value);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.PIE_SIZE, pieSize);
            return true;
        } else if (pref == mPieGap) {
            int pieGap = Integer.valueOf((String) value);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_GAP, pieGap);
            return true;
        } else if (pref == mPieAngle) {
            int pieAngle = Integer.valueOf((String) value);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_ANGLE, pieAngle);
            return true;
        } else if (pref == mPieSnapBackgroundColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_SNAP_BACKGROUND_COLOR, intHex);
            return true;
        } else if (pref == mPieBackgroundColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BACKGROUND_COLOR, intHex);
            return true;
        } else if (pref == mPieSelectColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_SELECT_COLOR, intHex);
            return true;
        } else if (pref == mPieOutlinesColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_OUTLINES_COLOR, intHex);
            return true;
        } else if (pref == mPieChevronColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_CHEVRON_COLOR, intHex);
            return true;
        } else if (pref == mPieStatusColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_STATUS_COLOR, intHex);
            return true;
        } else if (pref == mPieBatteryBackgroundColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BATTERY_BACKGROUND_COLOR, intHex);
            return true;
        } else if (pref == mPieBatteryJuiceColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BATTERY_JUICE_COLOR, intHex);
            return true;
        } else if (pref == mPieBatteryJuiceLowColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BATTERY_JUICE_LOW_COLOR, intHex);
            return true;
        } else if (pref == mPieBatteryJuiceCriticalColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BATTERY_JUICE_CRITICAL_COLOR, intHex);
            return true;
        }
        return false;
    }

    private void resetColors() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_SNAP_BACKGROUND_COLOR, COLOR_SNAP_BACKGROUND);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_BACKGROUND_COLOR, COLOR_PIE_BACKGROUND);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_SELECT_COLOR, COLOR_PIE_SELECT);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_OUTLINES_COLOR, COLOR_PIE_OUTLINES);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_CHEVRON_COLOR, COLOR_CHEVRON);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_STATUS_COLOR, COLOR_STATUS);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_BATTERY_BACKGROUND_COLOR, COLOR_BATTERY_BACKGROUND);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_BATTERY_JUICE_COLOR, COLOR_BATTERY_JUICE);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_BATTERY_JUICE_LOW_COLOR, COLOR_BATTERY_JUICE_LOW);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_BATTERY_JUICE_CRITICAL_COLOR, COLOR_BATTERY_JUICE_CRITICAL);

        mPieSnapBackgroundColor.setNewPreviewColor(COLOR_SNAP_BACKGROUND);
        mPieBackgroundColor.setNewPreviewColor(COLOR_PIE_BACKGROUND);
        mPieSelectColor.setNewPreviewColor(COLOR_PIE_SELECT);
        mPieOutlinesColor.setNewPreviewColor(COLOR_PIE_OUTLINES);
        mPieChevronColor.setNewPreviewColor(COLOR_CHEVRON);
        mPieStatusColor.setNewPreviewColor(COLOR_STATUS);
        mPieBatteryBackgroundColor.setNewPreviewColor(COLOR_BATTERY_BACKGROUND);
        mPieBatteryJuiceColor.setNewPreviewColor(COLOR_BATTERY_JUICE);
        mPieBatteryJuiceLowColor.setNewPreviewColor(COLOR_BATTERY_JUICE_LOW);
        mPieBatteryJuiceCriticalColor.setNewPreviewColor(COLOR_BATTERY_JUICE_CRITICAL);

        ContentResolver cr = getActivity().getContentResolver();

        int snapBackgroundColor = Settings.System.getInt(
                cr, Settings.System.PIE_SNAP_BACKGROUND_COLOR, COLOR_SNAP_BACKGROUND);
        String snapBackgroundHexColor = String.format("#%08x", (0xffffffff & snapBackgroundColor));

        int backgroundColor = Settings.System.getInt(
                cr, Settings.System.PIE_BACKGROUND_COLOR, COLOR_PIE_BACKGROUND);
        String backgroundHexColor = String.format("#%08x", (0xffffffff & backgroundColor));

        int selectColor = Settings.System.getInt(
                cr, Settings.System.PIE_SELECT_COLOR, COLOR_PIE_SELECT);
        String selectHexColor = String.format("#%08x", (0xffffffff & selectColor));

        int outlinesColor = Settings.System.getInt(
                cr, Settings.System.PIE_OUTLINES_COLOR, COLOR_PIE_OUTLINES);
        String outlinesHexColor = String.format("#%08x", (0xffffffff & outlinesColor));

        int statusColor = Settings.System.getInt(cr, Settings.System.PIE_STATUS_COLOR, COLOR_STATUS);
        String statusHexColor = String.format("#%08x", (0xffffffff & statusColor));

        int batteryBackgroundColor = Settings.System.getInt(
                cr, Settings.System.PIE_BATTERY_BACKGROUND_COLOR, COLOR_BATTERY_BACKGROUND);
        String batteryBackgroundHexColor = String.format("#%08x", (0xffffffff & batteryBackgroundColor));

        int batteryJuiceColor = Settings.System.getInt(
                cr, Settings.System.PIE_BATTERY_JUICE_COLOR, COLOR_BATTERY_JUICE);
        String batteryJuiceHexColor = String.format("#%08x", (0xffffffff & batteryJuiceColor));

        int batteryJuiceLowColor = Settings.System.getInt(
                cr, Settings.System.PIE_BATTERY_JUICE_LOW_COLOR, COLOR_BATTERY_JUICE_LOW);
        String batteryJuiceLowHexColor = String.format("#%08x", (0xffffffff & batteryJuiceLowColor));

        int batteryJuiceCriticalColor = Settings.System.getInt(
                cr, Settings.System.PIE_BATTERY_JUICE_CRITICAL_COLOR, COLOR_BATTERY_JUICE_CRITICAL);
        String batteryJuiceCriticalHexColor = String.format("#%08x", (0xffffffff & batteryJuiceCriticalColor));

        int chevronColor = Settings.System.getInt(
                cr, Settings.System.PIE_CHEVRON_COLOR, COLOR_CHEVRON);
        String chevronHexColor = String.format("#%08x", (0xffffffff & chevronColor));

        mPieSnapBackgroundColor.setSummary(getResources().getString(
                R.string.pie_snap_background_color_summary) + " (" + snapBackgroundHexColor + ")");
        mPieBackgroundColor.setSummary(getResources().getString(
                R.string.pie_background_color_summary) + " (" + backgroundHexColor + ")");
        mPieSelectColor.setSummary(getResources().getString(
                R.string.pie_select_color_summary) + " (" + selectHexColor + ")");
        mPieOutlinesColor.setSummary(getResources().getString(
                R.string.pie_outlines_color_summary) + " (" + outlinesHexColor + ")");
        mPieChevronColor.setSummary(getResources().getString(
                R.string.pie_chevron_color_summary) + " (" + chevronHexColor + ")");
        mPieStatusColor.setSummary(getResources().getString(
                R.string.pie_status_color_summary) + " (" + statusHexColor + ")");
        mPieBatteryBackgroundColor.setSummary(getResources().getString(
                R.string.pie_battery_background_color_summary) + " (" + batteryBackgroundHexColor + ")");
        mPieBatteryJuiceColor.setSummary(getResources().getString(
                R.string.pie_battery_juice_color_summary) + " (" + batteryJuiceHexColor + ")");
        mPieBatteryJuiceLowColor.setSummary(getResources().getString(
                R.string.pie_battery_juice_low_color_summary) + " (" + batteryJuiceLowHexColor + ")");
        mPieBatteryJuiceCriticalColor.setSummary(getResources().getString(
                R.string.pie_battery_juice_critical_color_summary) + " (" + batteryJuiceCriticalHexColor + ")");
    }

    private void resetPieSettings() {
        Settings.System.putFloat(getContentResolver(),
                Settings.System.PIE_SIZE, 1.0f);
        Settings.System.putInt(getContentResolver(),
                Settings.System.PIE_GAP, 1);
        Settings.System.putInt(getContentResolver(),
                Settings.System.PIE_ANGLE, 12);

        mPieSize.setValueIndex(2);
        mPieGap.setValueIndex(0);
        mPieAngle.setValueIndex(2);
    }
}
