package com.gmail.necnionch.myplugin.shaderegen.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class AutorunConfig {

    private final Supplier<ConfigurationSection> config;
    private boolean invalidSetting;
    private ZoneId timezone;
    private Frequency frequency;
    private int monthly;
    private DayOfWeek weekly;
    private LocalTime time;

    public AutorunConfig(Supplier<ConfigurationSection> config) {
        this.config = config;
    }

    public void onLoaded(Logger log) {
        invalidSetting = false;

        String tmp = config.get().getString("timezone", "local");
        timezone = ZoneId.systemDefault();
        if (tmp != null && !tmp.equalsIgnoreCase("local")) {
            try {
                timezone = ZoneId.of(tmp);
            } catch (Exception e) {
                log.warning("Invalid autorun.timezone specified: " + tmp);
                invalidSetting = true;
            }
        }

        tmp = Optional.ofNullable(config.get().getString("timing.frequency")).orElse("monthly");
        try {
            frequency = Frequency.valueOf(tmp.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warning("Invalid autorun.timing.frequency specified: " + tmp);
            invalidSetting = true;
        }

        if (Frequency.MONTHLY == frequency) {
            monthly = config.get().getInt("timing.monthly", 1);
            if (monthly < 1 || monthly > 31) {
                log.warning("Invalid autorun.timing.monthly specified: " + monthly);
                invalidSetting = true;
            }

        } else if (Frequency.WEEKLY == frequency) {
            tmp = Optional.ofNullable(config.get().getString("weekly")).orElse("");
            try {
                weekly = DayOfWeek.valueOf(tmp.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                log.warning("Invalid autorun.timing.weekly specified: " + tmp);
                invalidSetting = true;
            }
        }

        tmp = Optional.ofNullable(config.get().getString("time")).orElse("00:00");
        try {
            time = LocalTime.parse(tmp);
        } catch (DateTimeParseException e) {
            log.warning("Invalid autorun.timing.time: " + tmp);
            invalidSetting = true;
        }

        if (invalidSetting && config.get().getBoolean("enabled")) {
            log.severe("Autorun is disabled");
        }
    }

    public boolean isEnabled() {
        return !invalidSetting && config.get().getBoolean("enabled");
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public int getMonthly() {
        return monthly;
    }

    public DayOfWeek getWeekly() {
        return weekly;
    }

    public LocalTime getTime() {
        return time;
    }


    public enum Frequency {
        MONTHLY,
        WEEKLY
    }

}
