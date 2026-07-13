package com.example.engine;

import android.content.Context;
import android.util.Log;
import com.example.model.ComponentType;
import com.example.model.GridComponent;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Custom ESM Modding Engine written in pure Java.
 * Compiles/Parses custom written .esm file contents (plain text scripts or packed ZIPs).
 * Controls physical properties of elements and provides a custom syntax parser.
 */
public final class JavaModEngine {

    private static String activeModName = "Hydro-Vortex Core";
    private static String lastParsingErrors = "";
    
    // Dynamic physics properties overridden by mods
    public static boolean vortexActive = true;
    public static int vortexCenterX = 25;
    public static int vortexCenterY = 25;
    public static float vortexStrength = 1.9f;
    public static float vortexRadius = 16.0f;
    
    // Water customization
    public static String waterColorHex = "#0099FF"; // Bright cyan/teal liquid like in the screenshot
    public static float waterFlowChance = 0.95f;
    public static float waterGravityScale = 1.0f;
    
    // Brick customization
    public static String brickColorHex = "#7D8C99"; // Slate gray stone bricks like the screenshot container
    
    private static String currentModScript = 
        "// High-Speed Hydro-Vortex ESM Script\n" +
        "mod \"Teal Vortex\" {\n" +
        "    material WATER {\n" +
        "        color: \"#0099FF\"\n" +
        "        flow_speed: 6\n" +
        "        gravity: 0.95\n" +
        "    }\n" +
        "    material BRICK {\n" +
        "        color: \"#7D8C99\"\n" +
        "    }\n" +
        "    physics {\n" +
        "        vortex_active: true\n" +
        "        vortex_center_x: 25\n" +
        "        vortex_center_y: 20\n" +
        "        vortex_radius: 16\n" +
        "        vortex_strength: 2.2\n" +
        "    }\n" +
        "}\n";

    static {
        // Parse initial scripts
        compileAndApplyModScript(currentModScript);
    }

    public static String getCurrentModScript() {
        return currentModScript;
    }

    public static void setCurrentModScript(String script) {
        currentModScript = script;
    }

    public static String getActiveModName() {
        return activeModName;
    }

    public static String getLastParsingErrors() {
        return lastParsingErrors;
    }

    /**
     * Parse our newly designed custom ESM Mod Syntax.
     * Parses material overrides and custom physical force-fields on-the-fly.
     */
    public static boolean compileAndApplyModScript(String code) {
        if (code == null || code.trim().isEmpty()) {
            lastParsingErrors = "Empty mod script.";
            return false;
        }

        try {
            lastParsingErrors = "";
            String clean = code.replaceAll("//.*", ""); // Strip inline comments
            
            // Extract mod name
            if (clean.contains("mod \"")) {
                int start = clean.indexOf("mod \"") + 5;
                int end = clean.indexOf("\"", start);
                if (end > start) {
                    activeModName = clean.substring(start, end);
                }
            }

            // Simple parser state machine
            String[] tokens = clean.split("\\s+|(?=\\{)|(?<=\\{)|(?=\\})|(?=;)");
            String currentBlock = "";
            String currentMaterial = "";

            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i].trim();
                if (token.isEmpty()) continue;

                if (token.equalsIgnoreCase("material")) {
                    if (i + 1 < tokens.length) {
                        currentMaterial = tokens[i + 1].toUpperCase();
                        currentBlock = "MATERIAL";
                        i++;
                    }
                } else if (token.equalsIgnoreCase("physics")) {
                    currentBlock = "PHYSICS";
                } else if (token.equals("}")) {
                    currentBlock = "";
                } else if (token.contains(":")) {
                    String[] kv = token.split(":", 2);
                    String key = kv[0].trim().toLowerCase();
                    String val = kv.length > 1 ? kv[1].trim().replace("\"", "").replace(";", "") : "";
                    
                    if (val.isEmpty() && i + 1 < tokens.length) {
                        val = tokens[i + 1].trim().replace("\"", "").replace(";", "");
                        i++;
                    }

                    applyKeyVal(currentBlock, currentMaterial, key, val);
                } else if (token.endsWith(":")) {
                    String key = token.substring(0, token.length() - 1).trim().toLowerCase();
                    if (i + 1 < tokens.length) {
                        String val = tokens[i + 1].trim().replace("\"", "").replace(";", "");
                        i++;
                        applyKeyVal(currentBlock, currentMaterial, key, val);
                    }
                }
            }

            Log.i("ESM_ENGINE", "Successfully applied mod: " + activeModName);
            return true;
        } catch (Exception e) {
            lastParsingErrors = "Parsing Error: " + e.getMessage();
            Log.e("ESM_ENGINE", "Failed to parse script", e);
            return false;
        }
    }

    private static void applyKeyVal(String block, String material, String key, String val) {
        if ("MATERIAL".equals(block)) {
            if ("WATER".equals(material)) {
                if ("color".equals(key)) {
                    waterColorHex = val;
                } else if ("flow_speed".equals(key)) {
                    try {
                        waterFlowChance = Math.min(1.0f, Integer.parseInt(val) / 6.0f);
                    } catch (Exception e) {}
                } else if ("gravity".equals(key)) {
                    try {
                        waterGravityScale = Float.parseFloat(val);
                    } catch (Exception e) {}
                }
            } else if ("BRICK".equals(material)) {
                if ("color".equals(key)) {
                    brickColorHex = val;
                }
            }
        } else if ("PHYSICS".equals(block)) {
            if ("vortex_active".equals(key)) {
                vortexActive = Boolean.parseBoolean(val);
            } else if ("vortex_center_x".equals(key)) {
                try {
                    vortexCenterX = Integer.parseInt(val);
                } catch (Exception e) {}
            } else if ("vortex_center_y".equals(key)) {
                try {
                    vortexCenterY = Integer.parseInt(val);
                } catch (Exception e) {}
            } else if ("vortex_radius".equals(key)) {
                try {
                    vortexRadius = Float.parseFloat(val);
                } catch (Exception e) {}
            } else if ("vortex_strength".equals(key)) {
                try {
                    vortexStrength = Float.parseFloat(val);
                } catch (Exception e) {}
            }
        }
    }

    /**
     * Packages the current ESM script inside a ZIP container and writes to disk as <name>.esm
     */
    public static File packAndSaveEsm(Context context, String modFileName) {
        File dir = context.getExternalFilesDir("mods");
        if (dir == null) {
            dir = context.getCacheDir();
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String safeName = modFileName.trim().replaceAll("[^a-zA-Z0-True0-9_-]", "");
        if (safeName.isEmpty()) safeName = "user_mod";
        File esmFile = new File(dir, safeName + ".esm");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(esmFile))) {
            ZipEntry entry = new ZipEntry("mod.esms");
            zos.putNextEntry(entry);
            byte[] data = currentModScript.getBytes("UTF-8");
            zos.write(data, 0, data.length);
            zos.closeEntry();
            Log.i("ESM_ENGINE", "Mod successfully packaged to: " + esmFile.getAbsolutePath());
            return esmFile;
        } catch (Exception e) {
            Log.e("ESM_ENGINE", "Error exporting mod to ESM", e);
            return null;
        }
    }

    /**
     * Unpacks an .esm ZIP container and parses its inner scripts.
     */
    public static boolean unpackAndLoadEsm(Context context, File esmFile) {
        if (!esmFile.exists()) return false;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(esmFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equalsIgnoreCase("mod.esms")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    String script = baos.toString("UTF-8");
                    currentModScript = script;
                    boolean compiled = compileAndApplyModScript(script);
                    zis.closeEntry();
                    return compiled;
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            Log.e("ESM_ENGINE", "Failed to load/unpack ESM mod file", e);
        }
        return false;
    }
}
