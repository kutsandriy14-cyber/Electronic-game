package com.example.engine;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Robust Java Modding Console and Central Computational Coordinator.
 * Manages dynamically activated simulation mods and recalculates physics/electrical constants.
 */
public final class JavaModManager {

    private static boolean modSuperconductors = false;
    private static boolean modThermalShield = false;
    private static boolean modOverclockStability = false;
    private static boolean modHighVoltMutator = false;

    // Simulation parameter adjustments dynamically calculated in pure Java
    private static double customResistanceMultiplier = 1.0;
    private static double customThermalDissipationFactor = 1.0;
    private static double maxMcuVoltageAllowed = 12.0;

    private static final List<String> modActivityLog = new ArrayList<>();

    static {
        modActivityLog.add("[Java Modding System]: Initialized on JVM.");
        modActivityLog.add("[Java Modding System]: Custom physics calculator ready.");
    }

    private JavaModManager() {}

    public static boolean isModSuperconductors() {
        return modSuperconductors;
    }

    public static void setModSuperconductors(boolean enabled) {
        modSuperconductors = enabled;
        recalculateConstants();
        logActivity("Superconductors " + (enabled ? "ENABLED" : "DISABLED") + ". Wire electrical resistance reduced to 0.0 ohm.");
    }

    public static boolean isModThermalShield() {
        return modThermalShield;
    }

    public static void setModThermalShield(boolean enabled) {
        modThermalShield = enabled;
        recalculateConstants();
        logActivity("Thermal Shield " + (enabled ? "ENABLED" : "DISABLED") + ". Cooldown rates increased by 400%.");
    }

    public static boolean isModOverclockStability() {
        return modOverclockStability;
    }

    public static void setModOverclockStability(boolean enabled) {
        modOverclockStability = enabled;
        logActivity("Overclock Stability Booster " + (enabled ? "ENABLED" : "DISABLED") + ". CPU overload threshold expanded.");
    }

    public static boolean isModHighVoltMutator() {
        return modHighVoltMutator;
    }

    public static void setModHighVoltMutator(boolean enabled) {
        modHighVoltMutator = enabled;
        recalculateConstants();
        logActivity("Extreme Volt Mutator " + (enabled ? "ENABLED" : "DISABLED") + ". Overload melting threshold set to " + maxMcuVoltageAllowed + "V.");
    }

    private static synchronized void logActivity(String msg) {
        modActivityLog.add("[Java Engine] " + msg);
        if (modActivityLog.size() > 50) {
            modActivityLog.remove(0);
        }
        Log.i("JAVA_MODS", msg);
    }

    public static synchronized List<String> getModLogs() {
        return new ArrayList<>(modActivityLog);
    }

    public static synchronized void clearModLogs() {
        modActivityLog.clear();
        modActivityLog.add("[Java Engine] Log history cleared.");
    }

    /**
     * Heavy recalculation of physical constants. Executes formulas inside Java to configure 
     * simulator behavior.
     */
    private static void recalculateConstants() {
        // Superconductor mod sets total network line resistance close to absolute zero
        if (modSuperconductors) {
            customResistanceMultiplier = 0.0001; 
        } else {
            customResistanceMultiplier = 1.0;
        }

        // Thermal shield mod handles radiation cooling at extreme rates
        if (modThermalShield) {
            customThermalDissipationFactor = 4.5;
        } else {
            customThermalDissipationFactor = 1.0;
        }

        // Volt mutator increases the tolerance of microchips from 5V to 24V
        if (modHighVoltMutator) {
            maxMcuVoltageAllowed = 24.0;
        } else {
            maxMcuVoltageAllowed = 12.0;
        }
    }

    /**
     * Compute wire conductivity factoring in superconducting mods.
     * @param baseConductivity original material coefficient
     * @return modified conductivity computed in Java
     */
    public static float computeModifiedConductivity(float baseConductivity) {
        if (modSuperconductors) {
            return baseConductivity * 100.0f; // 100x conductivity boost
        }
        return baseConductivity;
    }

    /**
     * Compute resistance of a particular component cell.
     * @param baseResistance original resistance in ohms
     * @return resulting resistance
     */
    public static float calculateCircuitResistance(float baseResistance) {
        return (float) (baseResistance * customResistanceMultiplier);
    }

    /**
     * Compute local thermal heat flow rate of a semiconductor junction using thermal parameters.
     */
    public static float calculateSemiconductorThermalGain(float currentTemp, float ambientTemp, float localCurrent) {
        float heatingCoefficient = 0.25f;
        
        // Heat produced is current squared times resistance
        double wireResistance = 1.5 * customResistanceMultiplier;
        double inputHeat = Math.pow(localCurrent, 2) * wireResistance * heatingCoefficient;
        
        double coolingRate = 0.05 * customThermalDissipationFactor;
        double netCooling = (currentTemp - ambientTemp) * coolingRate;
        
        return (float) (inputHeat - netCooling);
    }
    
    /**
     * Quick benchmark algorithm to show execution performance of Java calculations.
     */
    public static double runModSystemBenchmark() {
        long startTime = System.nanoTime();
        double accumulator = 1.1;
        
        // Perform 100,000 matrix multiplication operations in pure Java
        for (int i = 0; i < 100000; i++) {
            accumulator = Math.sin(accumulator) * Math.cos(accumulator * 1.5) + Math.sqrt(accumulator + 0.5);
            if (Double.isNaN(accumulator) || Double.isInfinite(accumulator)) {
                accumulator = 1.1;
            }
        }
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1000000.0; // Return execution time in ms
    }
}
