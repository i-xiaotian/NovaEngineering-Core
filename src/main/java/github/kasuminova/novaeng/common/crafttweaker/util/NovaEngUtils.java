package github.kasuminova.novaeng.common.crafttweaker.util;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemDefinition;
import crafttweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

@ZenRegister
@ZenClass("novaeng.NovaEngUtils")
public class NovaEngUtils {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###.##");

    static {
        DECIMAL_FORMAT.setRoundingMode(RoundingMode.HALF_UP);
    }

    @ZenMethod
    public static String formatFloat(float value, int decimalFraction) {
        return formatDouble(value, decimalFraction);
    }

    @ZenMethod
    public static String formatDouble(double value, int decimalFraction) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(decimalFraction);
        return nf.format(value);
    }

    @ZenMethod
    public static String formatDecimal(double value) {
        return DECIMAL_FORMAT.format(value);
    }

    @ZenMethod
    public static String formatNumber(long value) {
        if (value < 1_000L) {
            return String.valueOf(value);
        } else if (value < 1_000_000L) {
            return formatFloat((float) value / 1_000L, 2) + "K";
        } else if (value < 1_000_000_000L) {
            return formatDouble((double) value / 1_000_000L, 2) + "M";
        } else if (value < 1_000_000_000_000L) {
            return formatDouble((double) value / 1_000_000_000L, 2) + "G";
        } else if (value < 1_000_000_000_000_000L) {
            return formatDouble((double) value / 1_000_000_000_000L, 2) + "T";
        } else if (value < 1_000_000_000_000_000_000L) {
            return formatDouble((double) value / 1_000_000_000_000_000L, 2) + "P";
        } else {
            return formatDouble((double) value / 1_000_000_000_000_000_000L, 2) + "E";
        }
    }

    @ZenMethod
    public static String formatNumber(long value, int decimalFraction) {
        if (value < 1_000L) {
            return String.valueOf(value);
        } else if (value < 1_000_000L) {
            return formatFloat((float) value / 1_000L, decimalFraction) + "K";
        } else if (value < 1_000_000_000L) {
            return formatDouble((double) value / 1_000_000L, decimalFraction) + "M";
        } else if (value < 1_000_000_000_000L) {
            return formatDouble((double) value / 1_000_000_000L, decimalFraction) + "G";
        } else if (value < 1_000_000_000_000_000L) {
            return formatDouble((double) value / 1_000_000_000_000L, decimalFraction) + "T";
        } else if (value < 1_000_000_000_000_000_000L) {
            return formatDouble((double) value / 1_000_000_000_000_000L, decimalFraction) + "P";
        } else {
            return formatDouble((double) value / 1_000_000_000_000_000_000L, decimalFraction) + "E";
        }
    }

    @ZenMethod
    public static String formatPercent(double num1, double num2) {
        if (num2 == 0) {
            return "0%";
        }
        return formatDouble((num1 / num2) * 100D, 2) + "%";
    }

    @ZenMethod
    public static String formatFLOPS(double value) {
        if (value < 1000.0F) {
            return formatDouble(value, 1) + "T FloPS";
        }
        return formatDouble(value / 1000.0D, 1) + "P FloPS";
    }

    @ZenMethod
    public static String formatLong2Time(long seconds) {
        if (seconds <= 0) {
            return "0秒";
        }

        long year = seconds / (60 * 60 * 24 * 365);
        seconds %= (60 * 60 * 24 * 365);

        long month = seconds / (60 * 60 * 24 * 30);
        seconds %= (60 * 60 * 24 * 30);

        long day = seconds / (60 * 60 * 24);
        seconds %= (60 * 60 * 24);

        long hour = seconds / (60 * 60);
        seconds %= (60 * 60);

        long minute = seconds / 60;
        seconds %= 60;

        StringBuilder result = new StringBuilder();

        if (year > 0) result.append(year).append("年");
        if (month > 0) result.append(month).append("月");
        if (day > 0) result.append(day).append("天");
        if (hour > 0) result.append(hour).append("小时");
        if (minute > 0) result.append(minute).append("分钟");
        if (seconds > 0) result.append(seconds).append("秒");

        return result.toString();
    }

}