package com.example.engine;

import com.example.model.ComponentType;
import com.example.model.GridComponent;
import java.util.Random;

/**
 * High-Speed 2D Cellular Physics and Fluid Dynamics Simulator written in pure Java.
 * Executes fluid current fields, material gravities, and heat coefficients dynamically.
 * Coordinates with JavaModEngine custom scripts.
 */
public final class JavaPhysicsEvaluator {

    private static final Random rng = new Random();

    private JavaPhysicsEvaluator() {}

    /**
     * Main physical step evaluator running the physics, water vortex currents, and powder gravity.
     * Overrides and custom physical formulas are governed by active ESM mods.
     */
    public static void evaluateSimulationTick(
            GridComponent[][] grid, 
            int width, 
            int height, 
            boolean[][] moved,
            double randVal
    ) {
        // 1. Apply ESM Mod Vortex force fields onto all active Fluids (Water, Lava, Oil, etc.)
        if (JavaModEngine.vortexActive) {
            applyVortexForces(grid, width, height, moved);
        }

        // 2. Perform falling sand and fluid flow calculation scans
        for (int y = height - 1; y >= 0; y--) {
            boolean goRight = rng.nextBoolean();
            int startX = goRight ? 0 : width - 1;
            int endX = goRight ? width - 1 : 0;
            int stepX = goRight ? 1 : -1;

            int x = startX;
            while (true) {
                if (!moved[x][y]) {
                    GridComponent comp = grid[x][y];
                    if (comp != null && isMobileParticle(comp)) {
                        evaluateSingleCellMovement(grid, width, height, x, y, comp, moved);
                    }
                }
                if (x == endX) break;
                x += stepX;
            }
        }
    }

    private static boolean isMobileParticle(GridComponent comp) {
        ComponentType type = comp.getType();
        if (type == ComponentType.WATER || 
            type == ComponentType.LAVA || 
            type == ComponentType.OIL || 
            type == ComponentType.ACID || 
            type == ComponentType.SLIME || 
            type == ComponentType.GASOLINE || 
            type == ComponentType.LIQUID_NITROGEN || 
            type == ComponentType.SAND || 
            type == ComponentType.DIRT || 
            type == ComponentType.MAGIC_DUST) {
            return true;
        }
        return false;
    }

    private static boolean isFluid(ComponentType type) {
        return type == ComponentType.WATER || 
               type == ComponentType.LAVA || 
               type == ComponentType.OIL || 
               type == ComponentType.ACID || 
               type == ComponentType.SLIME || 
               type == ComponentType.GASOLINE || 
               type == ComponentType.LIQUID_NITROGEN;
    }

    /**
     * Swirling hydrodynamic vortex calculations run in pure Java on-the-fly.
     * Simulates fluid currents forming circles and waves matching the sketched flow lines!
     */
    private static void applyVortexForces(GridComponent[][] grid, int width, int height, boolean[][] moved) {
        int cx = JavaModEngine.vortexCenterX;
        int cy = JavaModEngine.vortexCenterY;
        float radius = JavaModEngine.vortexRadius;
        float strength = JavaModEngine.vortexStrength;

        // Apply circular vectors to fluids
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                GridComponent comp = grid[x][y];
                if (comp != null && isFluid(comp.getType()) && !moved[x][y]) {
                    double dx = x - cx;
                    double dy = y - cy;
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist < radius && dist > 1.2) {
                        // Calculate tangent velocity vector for spiral currents
                        double vx = -dy / dist * strength;
                        double vy = dx / dist * strength;

                        // Target coordinate offset
                        int tx = x + (vx > 0.4 ? 1 : (vx < -0.4 ? -1 : 0));
                        int ty = y + (vy > 0.4 ? 1 : (vy < -0.4 ? -1 : 0));

                        if (tx >= 0 && tx < width && ty >= 0 && ty < height) {
                            if (grid[tx][ty].getType() == ComponentType.EMPTY && !moved[tx][ty]) {
                                grid[tx][ty] = comp;
                                grid[x][y] = new GridComponent(ComponentType.EMPTY);
                                moved[tx][ty] = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void evaluateSingleCellMovement(
            GridComponent[][] grid, 
            int width, 
            int height, 
            int x, 
            int y, 
            GridComponent comp, 
            boolean[][] moved
    ) {
        ComponentType type = comp.getType();
        boolean isLiquid = isFluid(type);
        boolean isPowder = type == ComponentType.SAND || type == ComponentType.DIRT || type == ComponentType.MAGIC_DUST;

        int dirY = 1; // standard gravity downward
        double gravityScale = (type == ComponentType.WATER) ? JavaModEngine.waterGravityScale : 1.0;
        
        // Randomly check if gravity affects it on this frame depending on scale
        if (rng.nextDouble() > gravityScale) {
            return;
        }

        int targetY = y + dirY;
        if (targetY >= 0 && targetY < height) {
            ComponentType belowType = grid[x][targetY].getType();

            // Cell directly below is empty -> falls down
            if (belowType == ComponentType.EMPTY) {
                grid[x][targetY] = comp;
                grid[x][y] = new GridComponent(ComponentType.EMPTY);
                moved[x][targetY] = true;
                return;
            }

            // Powder sinking into liquids
            if (isPowder && isFluid(belowType)) {
                GridComponent temp = grid[x][targetY];
                grid[x][targetY] = comp;
                grid[x][y] = temp;
                moved[x][targetY] = true;
                moved[x][y] = true;
                return;
            }

            // Diagonal downward roll off and slip flow
            double flowChance = isLiquid ? JavaModEngine.waterFlowChance : 0.45;
            if (rng.nextDouble() < flowChance) {
                boolean leftFirst = rng.nextBoolean();
                int side1 = leftFirst ? -1 : 1;
                int side2 = leftFirst ? 1 : -1;

                if (x + side1 >= 0 && x + side1 < width && grid[x + side1][targetY].getType() == ComponentType.EMPTY) {
                    grid[x + side1][targetY] = comp;
                    grid[x][y] = new GridComponent(ComponentType.EMPTY);
                    moved[x + side1][targetY] = true;
                    return;
                } else if (x + side2 >= 0 && x + side2 < width && grid[x + side2][targetY].getType() == ComponentType.EMPTY) {
                    grid[x + side2][targetY] = comp;
                    grid[x][y] = new GridComponent(ComponentType.EMPTY);
                    moved[x + side2][targetY] = true;
                    return;
                }
            }

            // Horizontal liquid dispersion
            if (isLiquid && rng.nextDouble() < JavaModEngine.waterFlowChance) {
                boolean leftFirst = rng.nextBoolean();
                int side1 = leftFirst ? -1 : 1;
                int side2 = leftFirst ? 1 : -1;

                if (x + side1 >= 0 && x + side1 < width && grid[x + side1][y].getType() == ComponentType.EMPTY) {
                    grid[x + side1][y] = comp;
                    grid[x][y] = new GridComponent(ComponentType.EMPTY);
                    moved[x + side1][y] = true;
                    return;
                } else if (x + side2 >= 0 && x + side2 < width && grid[x + side2][y].getType() == ComponentType.EMPTY) {
                    grid[x + side2][y] = comp;
                    grid[x][y] = new GridComponent(ComponentType.EMPTY);
                    moved[x + side2][y] = true;
                    return;
                }
            }
        }
    }
}
