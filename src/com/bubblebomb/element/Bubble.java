package com.bubblebomb.element;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.bubblebomb.config.GameConfig;
import com.bubblebomb.controller.InputState;
import com.bubblebomb.manager.ElementManager;
import com.bubblebomb.manager.ElementType;
import com.bubblebomb.manager.ResourceManager;

/**
 * 表示玩家放置的泡泡，负责引信、闪烁、推动状态和爆炸触发。
 */
public final class Bubble extends ElementObj {
    private final Player owner;
    private final int blastRange;
    private int fuseTicks;
    private boolean triggered;
    private boolean ownerPassing = true;
    private int pushCooldown;
    private final boolean powered;

    public Bubble(Player owner, int x, int y, int size) {
        super(ElementType.BUBBLE, x, y, size, size);
        this.owner = owner;
        this.powered = owner.consumeWaterMine();
        this.blastRange = owner.getBlastRange() + (powered ? 2 : 0);
        this.fuseTicks = powered
                ? GameConfig.getInstance().bubbleFuseTicks() / 2
                : GameConfig.getInstance().bubbleFuseTicks();
    }

    @Override
    public void update(ElementManager manager, InputState input) {
        if (pushCooldown > 0) pushCooldown--;
        if (ownerPassing && !getBounds().intersects(owner.getBounds())) {
            ownerPassing = false;
        }
        if (--fuseTicks <= 0) {
            triggered = true;
        }
    }

    @Override
    public void draw(Graphics2D g, ResourceManager resources) {
        BufferedImage image = resources.getImage("bubble");
        int frameWidth = Math.max(1, image.getWidth() / 4);
        int frameHeight = Math.min(image.getHeight(), Math.max(1, image.getHeight() / 6));
        int pulse = (fuseTicks / 8) % 4;
        int drawSize = getWidth() + (pulse == 0 ? 2 : 0);
        int offset = (drawSize - getWidth()) / 2;
        g.drawImage(image,
                (int) getX() - offset, (int) getY() - offset,
                (int) getX() - offset + drawSize, (int) getY() - offset + drawSize,
                0, 0, frameWidth, frameHeight, null);
        if (powered) {
            g.setColor(new java.awt.Color(165, 75, 230, 210));
            g.drawOval((int) getX() + 1, (int) getY() + 1, getWidth() - 2, getHeight() - 2);
        }
        if (fuseTicks <= 60 && (fuseTicks / 5) % 2 == 0) {
            g.setColor(new java.awt.Color(255, 255, 255, 220));
            g.drawOval((int) getX() - 2, (int) getY() - 2, getWidth() + 4, getHeight() + 4);
            g.setColor(new java.awt.Color(255, 70, 70, 170));
            g.fillOval((int) getX() + getWidth() / 3, (int) getY() + getHeight() / 3,
                    getWidth() / 3, getHeight() / 3);
        }
    }

    public boolean blocks(Player player) {
        // 放置者在完全离开泡泡前可以穿过，离开后泡泡对所有玩家产生阻挡。
        return player != owner || !ownerPassing;
    }

    public void trigger() { triggered = true; }
    public boolean canBePushed() { return pushCooldown <= 0 && !triggered; }
    public void pushTo(int x, int y) {
        setX(x);
        setY(y);
        ownerPassing = false;
        pushCooldown = 8;
    }
    public boolean isTriggered() { return triggered; }
    public Player getOwner() { return owner; }
    public int getBlastRange() { return blastRange; }
}
