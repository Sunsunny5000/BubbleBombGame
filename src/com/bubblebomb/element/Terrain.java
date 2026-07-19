package com.bubblebomb.element;

import java.awt.Color;
import java.awt.Graphics2D;

import com.bubblebomb.manager.ElementType;
import com.bubblebomb.manager.ResourceManager;

/**
 * 表示加速地面或泥地，并负责绘制对应的地形标志。
 */
public final class Terrain extends ElementObj {
    private final TerrainType terrainType;

    public Terrain(int x, int y, int size, TerrainType terrainType) {
        super(ElementType.TERRAIN, x, y, size, size);
        this.terrainType = terrainType;
    }

    @Override
    public void draw(Graphics2D g, ResourceManager resources) {
        int x = (int) getX();
        int y = (int) getY();
        if (terrainType == TerrainType.SPEED) {
            g.setColor(new Color(80, 220, 245, 150));
            g.fillRect(x + 2, y + 2, getWidth() - 4, getHeight() - 4);
            g.setColor(Color.WHITE);
            g.drawString(">>", x + 8, y + 21);
        } else {
            g.setColor(new Color(115, 78, 52, 170));
            g.fillOval(x + 2, y + 5, getWidth() - 4, getHeight() - 10);
            g.setColor(new Color(160, 115, 75));
            g.fillOval(x + 8, y + 10, 6, 5);
            g.fillOval(x + 19, y + 18, 7, 5);
        }
    }

    public TerrainType getTerrainType() { return terrainType; }
}
