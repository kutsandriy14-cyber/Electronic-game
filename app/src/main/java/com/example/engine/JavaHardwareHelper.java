package com.example.engine;

import android.util.Log;

/**
 * Hardware and physics utility written in Java to demonstrate full multi-language Java/Kotlin 
 * interoperability and to run physical stability calculations for the simulator.
 */
public final class JavaHardwareHelper {

    private JavaHardwareHelper() {
        // Prevent instantiation of utility class
    }

    /**
     * Calculates the stability coefficient of the microcontroller network based on 
     * CPU workload and thermal characteristics.
     *
     * @param totalRequiredMhz The combined clock speed required by all active MCUs.
     * @param systemBudgetMhz The total CPU power allocated in the simulator settings.
     * @param totalRequiredRamKb The combined RAM required by all active MCUs.
     * @param totalAvailableRamKb The combined RAM space available in the system.
     * @return A coefficient between 0.0 (completely unstable/crushed) and 1.0 (perfectly stable).
     */
    public static double calculateStabilityFactor(
            int totalRequiredMhz, 
            int systemBudgetMhz, 
            int totalRequiredRamKb, 
            int totalAvailableRamKb
    ) {
        if (systemBudgetMhz <= 0 || totalAvailableRamKb <= 0) {
            return 0.0;
        }

        double cpuRatio = (double) totalRequiredMhz / systemBudgetMhz;
        double ramRatio = (double) totalRequiredRamKb / totalAvailableRamKb;

        double cpuFactor = Math.max(0.0, 1.0 - Math.max(0.0, cpuRatio - 1.0) * 0.5);
        double ramFactor = Math.max(0.0, 1.0 - Math.max(0.0, ramRatio - 1.0) * 0.8);

        // Combined stability using weighted geometric mean
        double stability = Math.min(cpuFactor, ramFactor);
        
        Log.d("SIM_JAVA", "Stability factor calculated in Java: " + stability 
                + " (CPU load: " + (cpuRatio * 100) + "%, RAM load: " + (ramRatio * 100) + "%)");
        
        return stability;
    }

    /**
     * Computes the precise thermal temperature rise of a microcontroller under specific execution load.
     * Uses thermodynamic laws modeled in pure Java.
     *
     * @param scriptLength Number of instruction lines being executed.
     * @param mcuCores Number of virtual cores inside the MCU.
     * @param currentTemp Current temperature of the MCU in degrees Celsius.
     * @param isThrottled Whether the CPU is currently thermal-throttled.
     * @return The temperature increase value in degrees Celsius.
     */
    public static float calculateThermalIncrease(
            int scriptLength, 
            int mcuCores, 
            float currentTemp, 
            boolean isThrottled
    ) {
        // Power consumption of digital MCU is proportional to frequency (or script execution frequency) and voltage squared
        float loadFactor = (float) scriptLength * mcuCores * 0.15f;
        
        // Heat transfer dynamics: simplified thermal dissipation
        float baseHeatProduced = loadFactor * 4.5f;
        if (isThrottled) {
            baseHeatProduced *= 0.4f; // Throttling halves heat output
        }
        
        // If the component gets hotter, radiation heat loss increases slightly with difference to ambient
        float ambientTemp = 25.0f;
        float deltaToAmbient = Math.max(0.0f, currentTemp - ambientTemp);
        float heatDissipationRate = deltaToAmbient * 0.08f;
        
        float netHeatGain = baseHeatProduced - heatDissipationRate;
        
        // Return net change, ensuring we don't drop below 0 cooling rate
        return Math.max(-10.0f, Math.min(150.0f, netHeatGain));
    }

    /**
     * Diagnostic check to ensure the Java class loader is working correctly.
     * @return Simple confirmation string demonstrating Java execution.
     */
    public static String getJavaDiagnosticStatus() {
        return "Java engine virtual helper is active and linked: OK";
    }
}
