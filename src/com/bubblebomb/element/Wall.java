package com.bubblebomb.element;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

import com.bubblebomb.manager.ElementType;
import com.bubblebomb.manager.ResourceManager;

/**
 * 表示不可破坏的固定墙，同时阻挡玩家、泡泡和水柱。
 */
public final class Wall extends ElementObj {
    public Wall(int x, int y, int size) {
        super(ElementType.WALL, x, y, size, size);
    }

    @Override
    public void draw(Graphics2D g, ResourceManager resources) {
        int x = (int) getX();
        int y = (int) getY();
        g.setPaint(new GradientPaint(x, y, new Color(52, 95, 170),
                x + getWidth(), y + getHeight(), new Color(16, 42, 99)));
        g.fillRoundRect(x + 1, y + 1, getWidth() - 2, getHeight() - 2, 8, 8);
        g.setColor(new Color(130, 190, 255));
        g.drawRoundRect(x + 2, y + 2, getWidth() - 5, getHeight() - 5, 7, 7);
    }
}
