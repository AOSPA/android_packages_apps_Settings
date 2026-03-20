# Migration Data Lists

This directory contains JSON files cataloging the status of migrated preferences and screens.

## File Descriptions:

*   **`golden_list_preferences.json`**:
    This file lists all preferences that have undergone the initial migration process. While the basic migration work is complete, the enforcement of write permissions (i.e., whether a preference is truly writable or read-only as intended) has NOT yet been validated for all entries in this file.

*   **`golden_list_preferences_validated.json`**:
    This file contains a subset of the preferences listed in `golden_list_preferences.json`. The preferences in this list have been fully validated to ensure that their write permits are correctly enforced. This means:
        *   Preferences marked as writable can be successfully modified.
        *   Preferences marked as read-only cannot be modified.

*   **`golden_list_screens.json`**:
    This file lists all screens that have been confirmed as successfully migrated. All screens are inherently read-only, and their presence in this list means their migrated state has been verified.
