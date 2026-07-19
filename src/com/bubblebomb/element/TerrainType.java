package com.bubblebomb.element;

/**
 * 定义特殊地形类型及其对玩家速度的倍率。
 */
public enum TerrainType {
    SPEED(1.35),
    MUD(0.65);

    private final double speedMultiplier;

    TerrainType(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public double speedMultiplier() { return speedMultiplier; }
}
