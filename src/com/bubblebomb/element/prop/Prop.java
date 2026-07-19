package com.bubblebomb.element.prop;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.bubblebomb.element.ElementObj;
import com.bubblebomb.element.Player;
import com.bubblebomb.manager.ElementType;
import com.bubblebomb.manager.ResourceManager;

/**
 * 地图上的可拾取道具，根据道具类型向玩家施加对应效果。
 */
public final class Prop extends ElementObj {
    private final PropType propType;

    public Prop(int x, int y, int size, PropType propType) {
        super(ElementType.PROP, x, y, size, size);
        this.propType = propType;
    }

    public void applyTo(Player player) {
        // 道具拾取后立即生效，并将自身标记为失效等待管理器清理。
        switch (propType) {
            case BUBBLE_UP: player.increaseBubbleCapacity(); break;
            case RANGE_UP: player.increaseBlastRange(); break;
            case ANTIDOTE: player.clearDebuffs(); break;
            case REMOTE: player.grantRemote(); break;
            case WATER_MINE: player.grantWaterMine(); break;
            case DEMON_RED: player.applySlow(); break;
            case DEMON_PURPLE: player.applyReverse(); break;
            case GOLD_CARD: player.increaseSpeed(); break;
            case DIAMOND_CARD: player.grantShield(); break;
            default: break;
        }
        setLive(false);
    }

    @Override
    public void draw(Graphics2D g, ResourceManager resources) {
        BufferedImage image = resources.getImage(propType.imageKey());
        int frameWidth = Math.min(32, image.getWidth());
        int frameHeight = Math.min(32, image.getHeight());
        g.drawImage(image,
                (int) getX(), (int) getY(),
                (int) getX() + getWidth(), (int) getY() + getHeight(),
                0, 0, frameWidth, frameHeight, null);
    }

    public PropType getPropType() { return propType; }
}
