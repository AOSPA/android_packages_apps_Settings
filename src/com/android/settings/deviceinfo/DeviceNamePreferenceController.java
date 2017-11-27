import com.android.settingslib.DeviceInfoUtils;

public class DeviceNamePreferenceController extends PreferenceController {

    private static final String KEY_DEVICE_NAME = "device_name";

    private final Fragment mHost;

    public DeviceNamePreferenceController(Context context, Fragment host) {
        super(context);
        mHost = host;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(KEY_DEVICE_NAME);
        if (pref != null) {
            pref.setSummary(getDeviceName());
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DEVICE_NAME;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_DEVICE_NAME)) {
            return false;
        }
        final HardwareInfoDialogFragment fragment = HardwareInfoDialogFragment.newInstance();
        fragment.show(mHost.getFragmentManager(), HardwareInfoDialogFragment.TAG);
        return true;
    }

    public static String getDeviceName() {
        return Build.NAME + DeviceInfoUtils.getMsvSuffix();
    }
}
