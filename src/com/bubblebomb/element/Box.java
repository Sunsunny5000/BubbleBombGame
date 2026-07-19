package com.bubblebomb.element;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.bubblebomb.manager.ElementType;
import com.bubblebomb.manager.ResourceManager;

/**
 * 表示能够被水柱破坏并可能掉落道具的箱子。
 */
public final class Box extends ElementObj {
    public Box(int x, int y, int size) {
        super(ElementType.BOX, x, y, size, size);
    }

    @Override
    public void draw(Graphics2D g, ResourceManager resources) {
        BufferedImage image = resources.getImage("obstacles");
        int cellW = Math.max(1, image.getWidth() / 4);
        int cellH = Math.max(1, image.getHeight() / 4);
        int sx = Math.min(image.getWidth() - cellW, cellW * 3);
        g.drawImage(image,
                (int) getX(), (int) getY(),
                (int) getX() + getWidth(), (int) getY() + getHeight(),
                sx, 0, sx + cellW, cellH, null);
    }
}
