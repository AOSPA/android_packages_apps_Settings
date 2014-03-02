package com.android.settings.crystalroms.cpustats;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import com.android.settings.util.CMDProcessor;

import java.io.*;

public class Helpers implements Constants {

    private static String mVoltagePath;

    /**
     * Checks device for SuperUser permission
     *
     * @return If SU was granted or denied
     */
    public static boolean checkSu() {
        if (!new File("/system/bin/su").exists()
                && !new File("/system/xbin/su").exists()) {
            Log.e(TAG, "su does not exist!!!");
            return false; // tell caller to bail...
        }

        try {
            if ((new CMDProcessor().su.runWaitFor("ls /data/app-private"))
                    .success()) {
                Log.i(TAG, " SU exists and we have permission");
                return true;
            } else {
                Log.i(TAG, " SU exists but we dont have permission");
                return false;
            }
        } catch (final NullPointerException e) {
            Log.e(TAG, e.getLocalizedMessage().toString());
            return false;
        }
    }

    /**
     * Checks to see if Busybox is installed in "/system/"
     *
     * @return If busybox exists
     */
    public static boolean checkBusybox() {
        if (!new File("/system/bin/busybox").exists()
                && !new File("/system/xbin/busybox").exists()) {
            Log.e(TAG, "Busybox not in xbin or bin!");
            return false;
        }

        try {
            if (!new CMDProcessor().su.runWaitFor("busybox mount").success()) {
                Log.e(TAG, " Busybox is there but it is borked! ");
                return false;
            }
        } catch (final NullPointerException e) {
            Log.e(TAG, e.getLocalizedMessage().toString());
            return false;
        }
        return true;
    }

    /**
     * Return mount points
     *
     * @param path
     * @return line if present
     */
    public static String[] getMounts(final String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    "/proc/mounts"), 256);
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.contains(path)) {
                    return line.split(" ");
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "/proc/mounts does not exist");
        } catch (IOException e) {
            Log.d(TAG, "Error reading /proc/mounts");
        }
        return null;
    }

    /**
     * Get mounts
     *
     * @param mount
     * @return success or failure
     */
    public static boolean getMount(final String mount) {
        final CMDProcessor cmd = new CMDProcessor();
        final String mounts[] = getMounts("/system");
        if (mounts != null && mounts.length >= 3) {
            final String device = mounts[0];
            final String path = mounts[1];
            final String point = mounts[2];
            if (cmd.su.runWaitFor(
                    "mount -o " + mount + ",remount -t " + point + " " + device
                            + " " + path).success()) {
                return true;
            }
        }
        return (cmd.su.runWaitFor("busybox mount -o remount," + mount
                + " /system").success());
    }

    /**
     * Read one line from file
     *
     * @param fname
     * @return line
     */
    public static String readOneLine(String fname) {
        BufferedReader br;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(fname), 512);
            try {
                line = br.readLine();
            } finally {
                br.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "IO Exception when reading sys file", e);
            // attempt to do magic!
            return readFileViaShell(fname, true);
        }
        return line;
    }

    /**
     * Read file via shell
     *
     * @param filePath
     * @param useSu
     * @return file output
     */
    public static String readFileViaShell(String filePath, boolean useSu) {
        CMDProcessor.CommandResult cr = null;
        if (useSu) {
            cr = new CMDProcessor().su.runWaitFor("cat " + filePath);
        } else {
            cr = new CMDProcessor().sh.runWaitFor("cat " + filePath);
        }
        if (cr.success())
            return cr.stdout;
        return null;
    }

    /**
     * Write one line to a file
     *
     * @param fname
     * @param value
     * @return if line was written
     */
    public static boolean writeOneLine(String fname, String value) {
        try {
            FileWriter fw = new FileWriter(fname);
            try {
                fw.write(value);
            } finally {
                fw.close();
            }
        } catch (IOException e) {
            String Error = "Error writing to " + fname + ". Exception: ";
            Log.e(TAG, Error, e);
            return false;
        }
        return true;
    }

    /**
     * Gets available schedulers from file
     *
     * @return available schedulers
     */
    public static String[] getAvailableIOSchedulers() {
        String[] schedulers = null;
        String[] aux = readStringArray(IO_SCHEDULER_PATH);
        if (aux != null) {
            schedulers = new String[aux.length];
            for (int i = 0; i < aux.length; i++) {
                if (aux[i].charAt(0) == '[') {
                    schedulers[i] = aux[i].substring(1, aux[i].length() - 1);
                } else {
                    schedulers[i] = aux[i];
                }
            }
        }
        return schedulers;
    }

    /**
     * Reads string array from file
     *
     * @param fname
     * @return string array
     */
    private static String[] readStringArray(String fname) {
        String line = readOneLine(fname);
        if (line != null) {
            return line.split(" ");
        }
        return null;
    }

    /**
     * Get current IO Scheduler
     *
     * @return current io scheduler
     */
    public static String getIOScheduler() {
        String scheduler = null;
        String[] schedulers = readStringArray(IO_SCHEDULER_PATH);
        if (schedulers != null) {
            for (String s : schedulers) {
                if (s.charAt(0) == '[') {
                    scheduler = s.substring(1, s.length() - 1);
                    break;
                }
            }
        }
        return scheduler;
    }

    /**
     * Get total number of cpus
     *
     * @return total number of cpus
     */
    public static int getNumOfCpus() {
        int numOfCpu = 1;
        String numOfCpus = Helpers.readOneLine(NUM_OF_CPUS_PATH);
        String[] cpuCount = numOfCpus.split("-");
        if (cpuCount.length > 1) {
            try {
                int cpuStart = Integer.parseInt(cpuCount[0]);
                int cpuEnd = Integer.parseInt(cpuCount[1]);

                numOfCpu = cpuEnd - cpuStart + 1;

                if (numOfCpu < 0)
                    numOfCpu = 1;
            } catch (NumberFormatException ex) {
                numOfCpu = 1;
            }
        }
        return numOfCpu;
    }

    /**
     * Convert to MHz and append a tag
     *
     * @param mhzString
     * @return tagged and converted String
     */
    public static String toMHz(String mhzString) {
        return new StringBuilder().append(Integer.valueOf(mhzString) / 1000)
                .append(" MHz").toString();
    }

    /**
     * Restart the activity smoothly
     *
     * @param activity
     */
    public static void restartPC(final Activity activity) {
        if (activity == null)
            return;
        final int enter_anim = android.R.anim.fade_in;
        final int exit_anim = android.R.anim.fade_out;
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.finish();
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.startActivity(activity.getIntent());
    }

    /**
     * Helper to create a bitmap to set as imageview or bg
     *
     * @param bgcolor
     * @return bitmap
     */
    public static Bitmap getBackground(int bgcolor) {
        try {
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = Bitmap.createBitmap(2, 2, config);

            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(bgcolor);

            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
}
