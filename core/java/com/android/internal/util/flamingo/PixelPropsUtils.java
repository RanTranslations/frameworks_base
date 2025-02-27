/*
 * Copyright (C) 2020 The Pixel Experience Project
 * Copyright (C) 2022 FlamingoOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.flamingo;

import static java.util.Map.entry;

import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

/*
 * @hide
 */
public final class PixelPropsUtils {

    private static final String TAG = "PixelPropsUtils";
    private static final boolean DEBUG = false;

    private static volatile boolean sIsGms = false;
    public static final String PACKAGE_GMS = "com.google.android.gms";

    private static final Map<String, Object> commonProps = Map.ofEntries(
        entry("BRAND", "google"),
        entry("MANUFACTURER", "Google"),
        entry("IS_DEBUGGABLE", false),
        entry("IS_ENG", false),
        entry("IS_USERDEBUG", false),
        entry("IS_USER", true),
        entry("TYPE", "user")
    );

    private static final Map<String, String> cheetahProps = Map.ofEntries(
        entry("DEVICE", "cheetah"),
        entry("PRODUCT", "cheetah"),
        entry("MODEL", "Pixel 7 Pro"),
        entry("FINGERPRINT", "google/cheetah/cheetah:13/TD1A.220804.031/9071314:user/release-keys")
    );

    private static final Map<String, String> marlinProps = Map.ofEntries(
        entry("DEVICE", "marlin"),
        entry("PRODUCT", "marlin"),
        entry("MODEL", "Pixel XL"),
        entry("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys")
    );

    private static final Set<String> packagesToChange = Set.of(
        "com.google.android.apps.customization.pixel",
        "com.google.android.apps.fitness",
        "com.google.android.apps.gcs",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.apps.messaging",
        "com.google.android.apps.safetyhub",
        "com.google.android.apps.tachyon",
        "com.google.android.apps.turbo",
        "com.google.android.apps.turboadapter",
        "com.google.android.apps.wallpaper",
        "com.google.android.apps.wallpaper.pixel",
        "com.google.android.apps.wellbeing",
        "com.google.android.as",
        "com.google.android.configupdater",
        "com.google.android.dialer",
        "com.google.android.ext.services",
        "com.google.android.gms",
        "com.google.android.gms.location.history",
        "com.google.android.googlequicksearchbox",
        "com.google.android.gsf",
        "com.google.android.inputmethod.latin",
        "com.google.android.soundpicker",
        "com.google.intelligence.sense",
        "com.google.pixel.dynamicwallpapers",
        "com.google.pixel.livewallpaper"
    );

    private static final Set<String> packagesToChangePixelXL = Set.of(
        "com.google.android.apps.photos", // unlimited photos
        "com.samsung.accessory.berrymgr", // Sammy apps for wearables to fix crash 
        "com.samsung.accessory.fridaymgr",
        "com.samsung.accessory.neobeanmgr",
        "com.samsung.android.app.watchmanager",
        "com.samsung.android.geargplugin",
        "com.samsung.android.gearnplugin",
        "com.samsung.android.modenplugin",
        "com.samsung.android.neatplugin",
        "com.samsung.android.waterplugin"
    );

    public static void setProps(String packageName) {
        if (packageName == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Package = " + packageName);
        }
        if (packagesToChange.contains(packageName)) {
            commonProps.forEach(PixelPropsUtils::setPropValue);
            cheetahProps.forEach((key, value) -> {
                if (key.equals("MODEL") && packageName.equals(PACKAGE_GMS)) {
                    sIsGms = true;
                } else {
                    setPropValue(key, value);
                }
            });
        } else if (packagesToChangePixelXL.contains(packageName)) {
            commonProps.forEach(PixelPropsUtils::setPropValue);
            marlinProps.forEach(PixelPropsUtils::setPropValue);
        }
        // Set proper indexing fingerprint
        if (packageName.equals("com.google.android.settings.intelligence")) {
            setPropValue("FINGERPRINT", Build.DATE);
        }
    }

    private static void setPropValue(String key, Object value) {
        try {
            if (DEBUG) {
                Log.d(TAG, "Setting prop " + key + " to " + value);
            }
            final Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static boolean isCallerSafetyNet() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet
        if (sIsGms && isCallerSafetyNet()) {
            throw new UnsupportedOperationException();
        }
    }
}
