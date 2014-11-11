package com.android.settings.crystalroms.cpustats;

import android.annotation.SuppressLint;
import android.os.SystemClock;

import java.io.*;
import java.util.*;

@SuppressLint("UseSparseArrays")
public class CPUStateMonitor implements Constants {

    private List<CpuState> mStates = new ArrayList<CpuState>();
    private Map<Integer, Long> mOffsets = new HashMap<Integer, Long>();

    @SuppressWarnings("serial")
    public class CPUStateMonitorException extends Exception {
        public CPUStateMonitorException(String s) {
            super(s);
        }
    }

    @SuppressLint({"UseValueOf", "UseValueOf"})
    public class CpuState implements Comparable<CpuState> {
        public CpuState(int a, long b) {
            freq = a;
            duration = b;
        }

        public int freq = 0;
        public long duration = 0;

        public int compareTo(CpuState state) {
            Integer a = new Integer(freq);
            Integer b = new Integer(state.freq);
            return a.compareTo(b);
        }
    }

    public List<CpuState> getStates() {
        List<CpuState> states = new ArrayList<CpuState>();

        for (CpuState state : mStates) {
            long duration = state.duration;
            if (mOffsets.containsKey(state.freq)) {
                long offset = mOffsets.get(state.freq);
                if (offset <= duration) {
                    duration -= offset;
                } else {
                    mOffsets.clear();
                    return getStates();
                }
            }
            states.add(new CpuState(state.freq, duration));
        }
        return states;
    }

    public long getTotalStateTime() {
        long sum = 0;
        long offset = 0;

        for (CpuState state : mStates) {
            sum += state.duration;
        }

        for (Map.Entry<Integer, Long> entry : mOffsets.entrySet()) {
            offset += entry.getValue();
        }
        return sum - offset;
    }

    public Map<Integer, Long> getOffsets() {
        return mOffsets;
    }

    public void setOffsets(Map<Integer, Long> offsets) {
        mOffsets = offsets;
    }

    public void setOffsets() throws CPUStateMonitorException {
        mOffsets.clear();
        updateStates();

        for (CpuState state : mStates) {
            mOffsets.put(state.freq, state.duration);
        }
    }

    public void removeOffsets() {
        mOffsets.clear();
    }

    public List<CpuState> updateStates() throws CPUStateMonitorException {
        try {
            InputStream is = new FileInputStream(TIME_IN_STATE_PATH);
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);
            mStates.clear();
            readInStates(br);
            is.close();
        } catch (IOException e) {
            throw new CPUStateMonitorException(
                    "Problem opening time-in-states file");
        }

        long sleepTime = (SystemClock.elapsedRealtime() - SystemClock
                .uptimeMillis()) / 10;
        mStates.add(new CpuState(0, sleepTime));

        Collections.sort(mStates, Collections.reverseOrder());

        return mStates;
    }

    private void readInStates(BufferedReader br)
            throws CPUStateMonitorException {
        try {
            String line;
            while ((line = br.readLine()) != null) {
                String[] nums = line.split(" ");
                mStates.add(new CpuState(Integer.parseInt(nums[0]), Long
                        .parseLong(nums[1])));
            }
        } catch (IOException e) {
            throw new CPUStateMonitorException(
                    "Problem processing time-in-states file");
        }
    }
}
