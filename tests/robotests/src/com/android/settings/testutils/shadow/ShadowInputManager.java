/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.testutils.shadow;

import static org.robolectric.util.ReflectionHelpers.ClassParameter.from;
import static org.robolectric.util.reflector.Reflector.reflector;

import android.annotation.NonNull;
import android.hardware.input.IInputManager;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.InputManagerGlobal;
import android.hardware.input.KeyGlyphMap;
import android.os.Handler;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.MotionEvent;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.reflector.Accessor;
import org.robolectric.util.reflector.ForType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shadow for {@link InputManager} that has accessors for registered {@link
 * InputManager.InputDeviceListener}s.
 */
@Implements(value = InputManager.class, callThroughByDefault = false)
public class ShadowInputManager extends org.robolectric.shadows.ShadowInputManager {

    private final List<InputManager.InputDeviceListener> mInputDeviceListeners = new ArrayList<>();

    private final Map<InputDeviceIdentifier, Map<Integer, Integer>> mButtonRemappings =
            new HashMap<>();
    private final Map<InputDeviceIdentifier, Map<Integer, Integer>> mButtonToAxisRemappings =
            new HashMap<>();
    private final Map<InputDeviceIdentifier, Map<Integer, Integer>> mAxisRemappings =
            new HashMap<>();
    private final Map<Integer, KeyGlyphMap> mGlyphMaps = new HashMap<>();

    @Implementation
    protected static InputManager getInstance() {
        return ReflectionHelpers.callConstructor(
                InputManager.class,
                from(IInputManager.class, null));
    }

    @Implementation
    protected void registerInputDeviceListener(InputManager.InputDeviceListener listener,
            Handler handler) {
        // TODO: Use handler.
        if (!mInputDeviceListeners.contains(listener)) {
            mInputDeviceListeners.add(listener);
        }
    }

    @Implementation
    protected void unregisterInputDeviceListener(InputManager.InputDeviceListener listener) {
        mInputDeviceListeners.remove(listener);
    }

    @Implementation
    public int[] getInputDeviceIds() {
        return reflector(InputManagerGlobalReflector.class, InputManagerGlobal.getInstance())
                .getInputDeviceIds();
    }

    /**
     * @see InputManager#getInputDevice(int)
     */
    @Implementation
    public InputDevice getInputDevice(int id) {
        return reflector(InputManagerGlobalReflector.class, InputManagerGlobal.getInstance())
                .getInputDevice(id);
    }

    /**
     * @see InputManager#getInputDeviceByDescriptor(String)
     */
    @Implementation
    public InputDevice getInputDeviceByDescriptor(String descriptor) {
        return reflector(InputManagerGlobalReflector.class, InputManagerGlobal.getInstance())
                .getInputDeviceByDescriptor(descriptor);
    }

    /**
     * @see InputManager#getControllerButtonRemappings(InputDeviceIdentifier)
     */
    @Implementation
    public Map<Integer, Integer> getControllerButtonRemappings(
            @NonNull InputDeviceIdentifier identifier) {
        return Map.copyOf(mButtonRemappings.getOrDefault(identifier, Map.of()));
    }

    /**
     * @see InputManager#getControllerAxisRemappings(InputDeviceIdentifier)
     */
    @Implementation
    public Map<Integer, Integer> getControllerAxisRemappings(
            @NonNull InputDeviceIdentifier identifier) {
        return Map.copyOf(mAxisRemappings.getOrDefault(identifier, Map.of()));
    }

    /**
     * @see InputManager#remapControllerButton(InputDeviceIdentifier, int, int)
     */
    @Implementation
    public void remapControllerButton(
            @NonNull InputDeviceIdentifier identifier,
            @InputManager.ControllerButton int fromButton,
            int toKeyCode) {
        var map = mButtonRemappings.computeIfAbsent(identifier, unused -> new HashMap<>());

        if (fromButton == toKeyCode) {
            map.remove(fromButton);
        } else {
            map.put(fromButton, toKeyCode);
        }
    }

    /**
     * @see InputManager#remapControllerButtonToAxis(InputDeviceIdentifier, int, int)
     */
    @Implementation
    public void remapControllerButtonToAxis(
            @NonNull InputDeviceIdentifier identifier,
            @InputManager.ControllerButton int fromButton,
            @MotionEvent.Axis int toAxis) {
        var map = mButtonToAxisRemappings.computeIfAbsent(identifier, unused -> new HashMap<>());

        map.put(fromButton, toAxis);
    }

    /**
     * Returns the button to axis remappings.
     */
    public Map<Integer, Integer> getControllerButtonToAxisRemappings(
            @NonNull InputDeviceIdentifier identifier) {
        return Map.copyOf(mButtonToAxisRemappings.getOrDefault(identifier, Map.of()));
    }

    /**
     * @see InputManager#remapControllerAxis(InputDeviceIdentifier, int, int)
     */
    @Implementation
    public void remapControllerAxis(
            @NonNull InputDeviceIdentifier identifier,
            @MotionEvent.Axis int fromAxis,
            @MotionEvent.Axis int toAxis) {
        var map = mAxisRemappings.computeIfAbsent(identifier, unused -> new HashMap<>());

        if (fromAxis == toAxis) {
            map.remove(fromAxis);
        } else {
            map.put(fromAxis, toAxis);
        }
    }

    /**
     * @see InputManager#clearAllControllerButtonRemappings(InputDeviceIdentifier)
     */
    @Implementation
    public void clearAllControllerButtonRemappings(@NonNull InputDeviceIdentifier identifier) {
        mButtonRemappings.remove(identifier);
    }

    /**
     * @see InputManager#clearAllControllerButtonToAxisRemappings(InputDeviceIdentifier)
     */
    @Implementation
    public void clearAllControllerButtonToAxisRemappings(
            @NonNull InputDeviceIdentifier identifier) {
        mButtonToAxisRemappings.remove(identifier);
    }

    /**
     * @see InputManager#clearAllControllerAxisRemappings(InputDeviceIdentifier)
     */
    @Implementation
    public void clearAllControllerAxisRemappings(@NonNull InputDeviceIdentifier identifier) {
        mAxisRemappings.remove(identifier);
    }

    /**
     * @see InputManager#removeControllerButtonToAxisRemapping(InputDeviceIdentifier, int)
     */
    @Implementation
    public void removeControllerButtonToAxisRemapping(
            @NonNull InputDeviceIdentifier identifier,
            @InputManager.ControllerButton int fromButton) {
        var map = mButtonToAxisRemappings.getOrDefault(identifier, new HashMap<>());
        map.remove(fromButton);
    }

    /**
     * @see InputManager#getKeyGlyphMap(int)
     */
    @Implementation
    public KeyGlyphMap getKeyGlyphMap(int deviceId) {
        return mGlyphMaps.get(deviceId);
    }

    /** Sets {@link KeyGlyphMap} for {@code deviceId} */
    public void setKeyGlyphMap(int deviceId, KeyGlyphMap map) {
        mGlyphMaps.put(deviceId, map);
    }

    @Override
    public void addInputDevice(InputDevice device) {
        super.addInputDevice(device);
        mInputDeviceListeners.forEach(listener -> listener.onInputDeviceAdded(device.getId()));
    }

    /** Remove the input device with given id triggering the listeners. */
    public void removeInputDevice(int deviceId) {
        reflector(InputManagerGlobalReflector.class, InputManagerGlobal.getInstance())
                .getInputDevices()
                .delete(deviceId);
        mInputDeviceListeners.forEach(listener -> listener.onInputDeviceRemoved(deviceId));
    }

    @ForType(InputManagerGlobal.class)
    interface InputManagerGlobalReflector {
        int[] getInputDeviceIds();

        InputDevice getInputDevice(int id);

        InputDevice getInputDeviceByDescriptor(String descriptor);

        @Accessor("mInputDevices")
        SparseArray<InputDevice> getInputDevices();
    }
}
