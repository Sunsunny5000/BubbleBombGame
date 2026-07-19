package com.bubblebomb.element;

import java.awt.Color;
import java.awt.Graphics2D;

import com.bubblebomb.config.GameConfig;
import com.bubblebomb.controller.InputState;
import com.bubblebomb.manager.ElementManager;
import com.bubblebomb.manager.ElementType;
import com.bubblebomb.manager.ResourceManager;

/**
 * 表示泡泡爆炸后产生的水柱，达到持续时间后自动消失。
 */
public final class WaterBlast extends ElementObj {
    private final Direction direction;
    private int lifeTicks = GameConfig.getInstance().blastLifeTicks();

    public WaterBlast(int x, int y, int size, Direction direction) {
        super(ElementType.WATER_BLAST, x, y, size, size);
        this.direction = direction;
    }

    @Override
    public void update(ElementManager manager, InputState input) {
        if (--lifeTicks <= 0) {
            setLive(false);
        }
    }

    @Override
    public void draw(Graphics2D g, ResourceManager resources) {
        int x = (int) getX();
        int y = (int) getY();
        int size = getWidth();
        float alpha = Math.max(0.25f, lifeTicks / (float) GameConfig.getInstance().blastLifeTicks());
        g.setColor(new Color(80, 220, 255, (int) (210 * alpha)));
        if (direction == Direction.LEFT || direction == Direction.RIGHT) {
            g.fillRoundRect(x, y + size / 4, size, size / 2, size / 3, size / 3);
        } else if (direction == Direction.UP || direction == Direction.DOWN) {
            g.fillRoundRect(x + size / 4, y, size / 2, size, size / 3, size / 3);
        } else {
            g.fillOval(x + 2, y + 2, size - 4, size - 4);
        }
        g.setColor(new Color(240, 255, 255, 210));
        g.fillOval(x + size / 3, y + size / 3, size / 3, size / 3);
    }
}
