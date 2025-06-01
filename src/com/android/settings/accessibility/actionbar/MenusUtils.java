/*
 * Copyright (C) 2025 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.settings.accessibility.actionbar;

/**
 * Utility class for managing menu-related identifiers and potentially other menu-specific logic.
 */
public final class MenusUtils {

    /**
     * Represents the unique identifiers for various menu items in the application. Each menu item
     * is associated with an integer value.
     */
    public enum MenuId {

        /**
         * Identifier for the feedback menu option.
         */
        FEEDBACK(10),

        /**
         * Identifier for the send survey menu option.
         */
        SEND_SURVEY(20),

        /**
         * Identifier for the disability support menu option.
         */
        DISABILITY_SUPPORT(30);

        private final int mValue;

        /**
         * Constructs a {@code MenuId} enum constant with the specified integer value.
         *
         * @param value The unique integer value for this menu ID.
         */
        MenuId(int value) {
            this.mValue = value;
        }

        /**
         * Returns the integer value associated with this menu ID.
         *
         * @return The integer value of this menu ID.
         */
        public int getValue() {
            return mValue;
        }
    }

    private MenusUtils() {}
}
