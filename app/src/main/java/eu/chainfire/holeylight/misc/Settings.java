/*
 * Copyright (C) 2019 Jorrit "Chainfire" Jongma
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package eu.chainfire.holeylight.misc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.preference.PreferenceManager;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.animation.SpritePlayer;

@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class Settings implements SharedPreferences.OnSharedPreferenceChangeListener {
    public interface OnSettingsChangedListener {
        void onSettingsChanged();
    }

    private static final int SHIFT_SCREEN_ON = 0;
    private static final int SHIFT_SCREEN_OFF = 1;
    private static final int SHIFT_CHARGING = 0;
    private static final int SHIFT_BATTERY = 2;

    public static final int SCREEN_ON_CHARGING = SHIFT_SCREEN_ON + SHIFT_CHARGING;
    public static final int SCREEN_OFF_CHARGING = SHIFT_SCREEN_OFF + SHIFT_CHARGING;
    public static final int SCREEN_ON_BATTERY = SHIFT_SCREEN_ON + SHIFT_BATTERY;
    public static final int SCREEN_OFF_BATTERY = SHIFT_SCREEN_OFF + SHIFT_BATTERY;

    public static final String[] SCREEN_AND_POWER_STATE = new String[] {
            "screen_on_charging",
            "screen_off_charging",
            "screen_on_battery",
            "screen_off_battery"
    };

    // SCREEN_AND_POWER_STATE indexed
    public static final String[] ANIMATION_STYLE_DEFAULTS = new String[] {
            "swirl",
            "tsp",
            "blink",
            "tsp"
    };

    public static final String[] ANIMATION_STYLE_NAMES = new String[] {
            "swirl",
            "blink",
            "pie",
            "tsp"
    };

    // ANIMATION_STYLE_NAMES indexed
    public static final SpritePlayer.Mode[] ANIMATION_STYLE_VALUES = new SpritePlayer.Mode[] {
            SpritePlayer.Mode.SWIRL,
            SpritePlayer.Mode.BLINK,
            SpritePlayer.Mode.SINGLE,
            SpritePlayer.Mode.TSP
    };

    // ANIMATION_STYLE_NAMES indexed
    public static final int[] ANIMATION_STYLE_TITLES = new int[] {
            R.string.settings_animation_style_swirl_title,
            R.string.settings_animation_style_blink_title,
            R.string.settings_animation_style_single_title,
            R.string.settings_animation_style_tsp_title
    };

    // ANIMATION_STYLE_NAMES indexed
    public static final int[] ANIMATION_STYLE_DESCRIPTIONS = new int[] {
            R.string.settings_animation_style_swirl_description,
            R.string.settings_animation_style_blink_description,
            R.string.settings_animation_style_single_description,
            R.string.settings_animation_style_tsp_description
    };

    public static final String ENABLED_MASTER = "enabled_master";
    private static final boolean ENABLED_MASTER_DEFAULT = true;

    public static final String ENABLED_SCREEN_ON = "enabled_screen_on";
    private static final boolean ENABLED_SCREEN_ON_DEFAULT = true;

    public static final String ENABLED_SCREEN_OFF_CHARGING = "enabled_" + SCREEN_AND_POWER_STATE[SCREEN_OFF_CHARGING];
    private static final boolean ENABLED_SCREEN_OFF_CHARGING_DEFAULT = true;

    public static final String ENABLED_SCREEN_OFF_BATTERY = "enabled_" + SCREEN_AND_POWER_STATE[SCREEN_OFF_BATTERY];
    private static final boolean ENABLED_SCREEN_OFF_BATTERY_DEFAULT = true;

    public static final String ENABLED_LOCKSCREEN = "enabled_lockscreen";
    private static final boolean ENABLED_LOCKSCREEN_DEFAULT = true;

    public static final String SEEN_PICKUP_SCREEN_ON_CHARGING = "seen_pickup_" + SCREEN_AND_POWER_STATE[SCREEN_ON_CHARGING];
    private static final boolean SEEN_PICKUP_SCREEN_ON_CHARGING_DEFAULT = false;

    public static final String SEEN_PICKUP_SCREEN_OFF_CHARGING = "seen_pickup_" + SCREEN_AND_POWER_STATE[SCREEN_OFF_CHARGING];
    private static final boolean SEEN_PICKUP_SCREEN_OFF_CHARGING_DEFAULT = false;

    public static final String SEEN_PICKUP_SCREEN_ON_BATTERY = "seen_pickup_" + SCREEN_AND_POWER_STATE[SCREEN_ON_BATTERY];
    private static final boolean SEEN_PICKUP_SCREEN_ON_BATTERY_DEFAULT = false;

    public static final String SEEN_PICKUP_SCREEN_OFF_BATTERY = "seen_pickup_" + SCREEN_AND_POWER_STATE[SCREEN_OFF_BATTERY];
    private static final boolean SEEN_PICKUP_SCREEN_OFF_BATTERY_DEFAULT = false;

    public static final String SEEN_ON_LOCKSCREEN = "seen_on_lockscreen";
    private static final boolean SEEN_ON_LOCKSCREEN_DEFAULT = false;

    public static final String SEEN_ON_USER_PRESENT = "seen_on_user_present";
    private static final boolean SEEN_ON_USER_PRESENT_DEFAULT = false;

    public static final String ANIMATION_STYLE_FMT = "animation_%s";

    private static final String CUTOUT_AREA_LEFT = "cutout_area_left";
    private static final String CUTOUT_AREA_TOP = "cutout_area_top";
    private static final String CUTOUT_AREA_RIGHT = "cutout_area_right";
    private static final String CUTOUT_AREA_BOTTOM = "cutout_area_bottom";

    private static final String DP_ADD_SCALE_BASE = "dp_add_scale_base_float";
    private static final String DP_ADD_SCALE_HORIZONTAL = "dp_add_scale_horizontal_float";
    private static final String DP_SHIFT_VERTICAL = "dp_shift_vertical_float";
    private static final String DP_SHIFT_HORIZONTAL = "dp_shift_horizontal_float";

    private static final String SPEED_FACTOR = "speed_factor";

    private static final String CHANNEL_COLOR = "CHANNEL_COLOR:";
    private static final String CHANNEL_COLOR_FMT = CHANNEL_COLOR + "%s:%s";

    public static final String HIDE_AOD = "hide_aod";
    private static final boolean HIDE_AOD_DEFAULT = false;

    private static Settings instance;
    public static Settings getInstance(Context context) {
        synchronized (Settings.class) {
            if (instance == null) {
                instance = new Settings(context);
            }
            return instance;
        }
    }

    private final List<OnSettingsChangedListener> listeners = new ArrayList<>();
    private final SharedPreferences prefs;
    private volatile SharedPreferences.Editor editor = null;
    private volatile int ref = 0;

    private Settings(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void finalize() throws Throwable {
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.finalize();
    }

    @Override
    public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (ref == 0) notifyListeners();
    }

    public synchronized void registerOnSettingsChangedListener(OnSettingsChangedListener onSettingsChangedListener) {
        if (!listeners.contains(onSettingsChangedListener)) {
            listeners.add(onSettingsChangedListener);
        }
    }

    public synchronized void unregisterOnSettingsChangedListener(OnSettingsChangedListener onSettingsChangedListener) {
        listeners.remove(onSettingsChangedListener);
    }

    private synchronized void notifyListeners() {
        for (OnSettingsChangedListener listener : listeners) {
            listener.onSettingsChanged();
        }
    }

    @SuppressLint("CommitPrefEdits")
    public synchronized Settings edit() {
        if (editor == null) {
            editor = prefs.edit();
            ref = 0;
        }
        ref++;
        return this;
    }

    public synchronized void save(boolean immediately) {
        ref--;
        if (ref < 0) ref = 0;
        if (ref == 0) {
            ref = 1; // prevent double notify
            try {
                if (immediately) {
                    editor.commit();
                } else {
                    editor.apply();
                }
            } finally {
                ref = 0;
            }
            notifyListeners();
            editor = null;
        }
    }

    public synchronized void cancel() {
        ref = 0;
        editor = null;
    }

    public Rect getCutoutAreaRect() {
        return new Rect(
            prefs.getInt(CUTOUT_AREA_LEFT, -1),
            prefs.getInt(CUTOUT_AREA_TOP, -1),
            prefs.getInt(CUTOUT_AREA_RIGHT, -1),
            prefs.getInt(CUTOUT_AREA_BOTTOM, -1)
        );
    }

    public Settings setCutoutAreaRect(Rect rect) {
        edit();
        try {
            editor.putInt(CUTOUT_AREA_LEFT, rect.left);
            editor.putInt(CUTOUT_AREA_TOP, rect.top);
            editor.putInt(CUTOUT_AREA_RIGHT, rect.right);
            editor.putInt(CUTOUT_AREA_BOTTOM, rect.bottom);
        } finally {
            save(true);
        }
        return this;
    }

    public float getDpAddScaleBase(float defaultValue) {
        return prefs.getFloat(DP_ADD_SCALE_BASE, defaultValue);
    }

    public void setDpAddScaleBase(float value) {
        edit();
        try {
            editor.putFloat(DP_ADD_SCALE_BASE, value);
        } finally {
            save(true);
        }
    }

    public float getDpAddScaleHorizontal(float defaultValue) {
        return prefs.getFloat(DP_ADD_SCALE_HORIZONTAL, defaultValue);
    }

    public void setDpAddScaleHorizontal(float value) {
        edit();
        try {
            editor.putFloat(DP_ADD_SCALE_HORIZONTAL, value);
        } finally {
            save(true);
        }
    }

    public float getDpShiftVertical(float defaultValue) {
        return prefs.getFloat(DP_SHIFT_VERTICAL, defaultValue);
    }

    public void setDpShiftVertical(float value) {
        edit();
        try {
            editor.putFloat(DP_SHIFT_VERTICAL, value);
        } finally {
            save(true);
        }
    }

    public float getDpShiftHorizontal(float defaultValue) {
        return prefs.getFloat(DP_SHIFT_HORIZONTAL, defaultValue);
    }

    public void setDpShiftHorizontal(float value) {
        edit();
        try {
            editor.putFloat(DP_SHIFT_HORIZONTAL, value);
        } finally {
            save(true);
        }
    }

    public float getSpeedFactor() {
        return prefs.getFloat(SPEED_FACTOR, 1.0f);
    }

    public void setSpeedFactor(float value) {
        value = Math.min(Math.max(value, 0.5f), 2.0f);

        edit();
        try {
            editor.putFloat(SPEED_FACTOR, value);
        } finally {
            save(true);
        }
    }

    public boolean isEnabled() {
        return prefs.getBoolean(ENABLED_MASTER, ENABLED_MASTER_DEFAULT);
    }

    public void setEnabled(boolean enabled) {
        edit();
        try {
            editor.putBoolean(ENABLED_MASTER, enabled);
        } finally {
            save(true);
        }
    }

    public boolean isEnabledWhileScreenOn() {
        return isEnabled() && prefs.getBoolean(ENABLED_SCREEN_ON, ENABLED_SCREEN_ON_DEFAULT);
    }

    public boolean isEnabledWhileScreenOffAny() {
        return isEnabledWhileScreenOffCharging() || isEnabledWhileScreenOffBattery();
    }

    public boolean isEnabledWhileScreenOffCharging() {
        return isEnabled() && prefs.getBoolean(ENABLED_SCREEN_OFF_CHARGING, ENABLED_SCREEN_OFF_CHARGING_DEFAULT);
    }

    public boolean isEnabledWhileScreenOffBattery() {
        return isEnabled() && prefs.getBoolean(ENABLED_SCREEN_OFF_BATTERY, ENABLED_SCREEN_OFF_BATTERY_DEFAULT);
    }

    public boolean isEnabledOnLockscreen() {
        return isEnabledWhileScreenOn() && prefs.getBoolean(ENABLED_LOCKSCREEN, ENABLED_LOCKSCREEN_DEFAULT);
    }

    public int getColorForPackageAndChannel(String packageName, String channelName, int defaultValue) {
        return prefs.getInt(String.format(Locale.ENGLISH, CHANNEL_COLOR_FMT, packageName, channelName), defaultValue);
    }

    public void setColorForPackageAndChannel(String packageName, String channelName, int color, boolean fromListener) {
        String key = String.format(Locale.ENGLISH, CHANNEL_COLOR_FMT, packageName, channelName);
        if (!prefs.contains(key) || (prefs.getInt(key, -1) != color)) {
            edit();
            try {
                editor.putInt(key, color);
            } finally {
                save(!fromListener);
            }
        }
    }

    public Map<String, Integer> getPackagesChannelsAndColors() {
        Map<String, Integer> ret = new HashMap<>();
        Map<String, ?> all = prefs.getAll();
        for (String key : all.keySet()) {
            if (key.startsWith(CHANNEL_COLOR)) {
                String pkg = key.substring(CHANNEL_COLOR.length());
                Integer color = prefs.getInt(key, 0);
                ret.put(pkg, color);
            }
        }
        return ret;
    }

    public boolean isSeenPickupScreenOnCharging(boolean effective) {
        return (!effective || isEnabled()) && prefs.getBoolean(SEEN_PICKUP_SCREEN_ON_CHARGING, SEEN_PICKUP_SCREEN_ON_CHARGING_DEFAULT);
    }

    public boolean isSeenPickupScreenOffCharging(boolean effective) {
        return (!effective || isEnabledWhileScreenOffCharging()) && prefs.getBoolean(SEEN_PICKUP_SCREEN_OFF_CHARGING, SEEN_PICKUP_SCREEN_OFF_CHARGING_DEFAULT);
    }

    public boolean isSeenPickupScreenOnBattery(boolean effective) {
        return (!effective || isEnabled()) && prefs.getBoolean(SEEN_PICKUP_SCREEN_ON_BATTERY, SEEN_PICKUP_SCREEN_ON_BATTERY_DEFAULT);
    }

    public boolean isSeenPickupScreenOffBattery(boolean effective) {
        return (!effective || isEnabledWhileScreenOffBattery()) && prefs.getBoolean(SEEN_PICKUP_SCREEN_OFF_BATTERY, SEEN_PICKUP_SCREEN_OFF_BATTERY_DEFAULT);
    }

    public boolean isSeenOnLockscreen(boolean effective) {
        return (!effective || isEnabledWhileScreenOffAny()) && prefs.getBoolean(SEEN_ON_LOCKSCREEN, SEEN_ON_LOCKSCREEN_DEFAULT);
    }

    public boolean isSeenOnUserPresent(boolean effective) {
        return (!effective || isEnabledWhileScreenOffAny()) && prefs.getBoolean(SEEN_ON_USER_PRESENT, SEEN_ON_USER_PRESENT_DEFAULT);
    }

    public SpritePlayer.Mode getAnimationMode(int mode) {
        String key = String.format(Locale.ENGLISH, ANIMATION_STYLE_FMT, SCREEN_AND_POWER_STATE[mode]);
        String value = prefs.getString(key, ANIMATION_STYLE_DEFAULTS[mode]);
        for (int i = 0; i < ANIMATION_STYLE_NAMES.length; i++) {
            if (ANIMATION_STYLE_NAMES[i].equals(value)) {
                return ANIMATION_STYLE_VALUES[i];
            }
        }
        return null;
    }

    public void setAnimationMode(int mode, SpritePlayer.Mode animationMode) {
        String key = String.format(Locale.ENGLISH, ANIMATION_STYLE_FMT, SCREEN_AND_POWER_STATE[mode]);
        for (int i = 0; i < ANIMATION_STYLE_VALUES.length; i++) {
            if (ANIMATION_STYLE_VALUES[i] == animationMode) {
                edit();
                try {
                    editor.putString(key, ANIMATION_STYLE_NAMES[i]);
                } finally {
                    save(true);
                }
            }
        }
    }

    public int getMode(boolean charging, boolean screenOn) {
        return (charging ? SHIFT_CHARGING : SHIFT_BATTERY) + (screenOn ? SHIFT_SCREEN_ON : SHIFT_SCREEN_OFF);
    }

    public void resetTuning() {
        edit();
        try {
            for (String key : new String[] {
                    DP_ADD_SCALE_BASE,
                    DP_ADD_SCALE_HORIZONTAL,
                    DP_SHIFT_VERTICAL,
                    DP_SHIFT_HORIZONTAL,
                    SPEED_FACTOR
            }) {
                if (prefs.contains(key)) {
                    editor.remove(key);
                }
            }
        } finally {
            save(true);
        }
    }

    public boolean isHideAOD() {
        return prefs.getBoolean(HIDE_AOD, HIDE_AOD_DEFAULT);
    }
}
