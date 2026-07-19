package com.bubblebomb.element;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import com.bubblebomb.config.GameConfig;
import com.bubblebomb.controller.InputState;
import com.bubblebomb.manager.ElementManager;
import com.bubblebomb.manager.ElementType;
import com.bubblebomb.manager.ResourceManager;

/**
 * 玩家元素，负责输入响应、移动、动画、泡泡能力和各种状态效果。
 */
public final class Player extends ElementObj {
    private final int id;
    private final String imageKey;
    private final int upKey;
    private final int downKey;
    private final int leftKey;
    private final int rightKey;
    private final int bombKey;
    private final int remoteKey;
    private Direction direction = Direction.DOWN;
    private PlayerState state = PlayerState.NORMAL;
    private int animationTick;
    private int animationFrame;
    private int trappedTicks;
    private int maxBubbles = 3;
    private int activeBubbles;
    private int blastRange = 2;
    private double speed = 2.5;
    private boolean shield;
    private boolean remoteEnabled;
    private int invincibleTicks;
    private int reverseTicks;
    private int slowTicks;
    private int waterMineCharges;
    private TerrainType terrainEffect;
    private int terrainEffectTicks;

    public Player(int id, String imageKey, double x, double y,
            int upKey, int downKey, int leftKey, int rightKey, int bombKey, int remoteKey) {
        super(ElementType.PLAYER, x, y, 30, 30);
        this.id = id;
        this.imageKey = imageKey;
        this.upKey = upKey;
        this.downKey = downKey;
        this.leftKey = leftKey;
        this.rightKey = rightKey;
        this.bombKey = bombKey;
        this.remoteKey = remoteKey;
    }

    public static Player createPlayerOne(double x, double y) {
        return new Player(1, "player.red", x, y,
                KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
                KeyEvent.VK_B, KeyEvent.VK_V);
    }

    public static Player createPlayerTwo(double x, double y) {
        return new Player(2, "player.duck", x, y,
                KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                KeyEvent.VK_ENTER, KeyEvent.VK_SLASH);
    }

    @Override
    public void update(ElementManager manager, InputState input) {
        if (invincibleTicks > 0) {
            invincibleTicks--;
        }
        if (reverseTicks > 0) reverseTicks--;
        if (slowTicks > 0) slowTicks--;
        if (state == PlayerState.TRAPPED) {
            trappedTicks--;
            animationTick++;
            if (trappedTicks <= 0) {
                state = PlayerState.DEAD;
                setLive(false);
            }
            return;
        }
        if (state != PlayerState.NORMAL) {
            return;
        }

        int horizontal = (input.isDown(rightKey) ? 1 : 0) - (input.isDown(leftKey) ? 1 : 0);
        int vertical = (input.isDown(downKey) ? 1 : 0) - (input.isDown(upKey) ? 1 : 0);
        if (reverseTicks > 0) {
            horizontal = -horizontal;
            vertical = -vertical;
        }
        updateTerrainEffect(manager);
        double terrainMultiplier = terrainEffect == null ? 1.0 : terrainEffect.speedMultiplier();
        double effectiveSpeed = speed * terrainMultiplier;
        if (slowTicks > 0) effectiveSpeed *= 0.55;
        double dx = horizontal * effectiveSpeed;
        double dy = vertical * effectiveSpeed;
        if (horizontal != 0 && vertical != 0) {
            dx *= 0.7071;
            dy *= 0.7071;
        }

        if (horizontal < 0) direction = Direction.LEFT;
        else if (horizontal > 0) direction = Direction.RIGHT;
        else if (vertical < 0) direction = Direction.UP;
        else if (vertical > 0) direction = Direction.DOWN;

        boolean moved = false;
        if (dx != 0 && manager.canMove(this, getX() + dx, getY())) {
            setX(getX() + dx);
            moved = true;
        }
        if (dy != 0 && manager.canMove(this, getX(), getY() + dy)) {
            setY(getY() + dy);
            moved = true;
        }

        if (moved) {
            animationTick++;
            if (animationTick % 8 == 0) {
                animationFrame = (animationFrame + 1) % 4;
            }
        } else {
            animationFrame = 0;
        }

        if (input.consumePressed(bombKey)) {
            manager.placeBubble(this);
        }
        if (input.consumePressed(remoteKey) && remoteEnabled) {
            manager.triggerOldestBubble(this);
        }
    }

    private void updateTerrainEffect(ElementManager manager) {
        TerrainType currentTerrain = manager.getTerrainType(this);
        if (currentTerrain != null) {
            terrainEffect = currentTerrain;
            int durationMillis = currentTerrain == TerrainType.SPEED
                    ? GameConfig.getInstance().speedLingerMillis()
                    : GameConfig.getInstance().mudLingerMillis();
            terrainEffectTicks = Math.max(1,
                    durationMillis / GameConfig.getInstance().tickMillis());
        } else if (terrainEffectTicks > 0) {
            terrainEffectTicks--;
            if (terrainEffectTicks <= 0) terrainEffect = null;
        }
    }

    @Override
    public Rectangle getBounds() {
        return getCollisionBounds(getX(), getY());
    }

    public Rectangle getCollisionBounds(double x, double y) {
        // 图片显示为30×30，但碰撞框适当缩小，使玩家通过32像素通道和墙角时更加顺畅。
        int inset = 4;
        return new Rectangle(
                (int) Math.round(x) + inset,
                (int) Math.round(y) + inset,
                getWidth() - inset * 2,
                getHeight() - inset * 2);
    }

    @Override
    public void draw(Graphics2D g, ResourceManager resources) {
        if (state == PlayerState.TRAPPED) {
            BufferedImage trap = resources.getImage("bubble.trap");
            int cellW = Math.max(1, trap.getWidth() / 4);
            int cellH = Math.max(1, trap.getHeight() / 4);
            int frame = (animationTick / 8) % 4;
            g.drawImage(trap,
                    (int) getX() - 2, (int) getY() - 2,
                    (int) getX() + getWidth() + 4, (int) getY() + getHeight() + 4,
                    frame * cellW, 0, (frame + 1) * cellW, cellH, null);
            return;
        }

        BufferedImage image = resources.getImage(imageKey);
        int cellW = Math.max(1, image.getWidth() / 4);
        int cellH = Math.max(1, image.getHeight() / 4);
        int sx = animationFrame * cellW;
        int sy = direction.spriteRow() * cellH;
        g.drawImage(image,
                (int) getX() - 1, (int) getY() - 4,
                (int) getX() + getWidth() + 1, (int) getY() + getHeight(),
                sx, sy, sx + cellW, sy + cellH, null);
        if (shield) {
            g.setColor(new Color(255, 230, 80, 180));
            g.drawOval((int) getX() - 2, (int) getY() - 2, getWidth() + 4, getHeight() + 4);
        }
        if (reverseTicks > 0 || slowTicks > 0) {
            g.setColor(reverseTicks > 0 ? new Color(180, 80, 230, 190) : new Color(210, 70, 60, 190));
            g.drawOval((int) getX() + 4, (int) getY() - 7, getWidth() - 8, 7);
        }
    }

    public void hit() {
        if (state != PlayerState.NORMAL || invincibleTicks > 0) {
            return;
        }
        if (shield) {
            shield = false;
            // 无敌时间覆盖整段水柱，避免同一次爆炸先消耗护盾、下一帧又困住玩家。
            invincibleTicks = GameConfig.getInstance().blastLifeTicks() + 5;
            return;
        }
        state = PlayerState.TRAPPED;
        trappedTicks = GameConfig.getInstance().trappedTicks();
        animationTick = 0;
    }

    public boolean canPlaceBubble() {
        return state == PlayerState.NORMAL && activeBubbles < maxBubbles;
    }

    public void bubblePlaced() { activeBubbles++; }
    public void bubbleReturned() { if (activeBubbles > 0) activeBubbles--; }
    public void increaseBubbleCapacity() { maxBubbles = Math.min(8, maxBubbles + 1); }
    public void increaseBlastRange() { blastRange = Math.min(8, blastRange + 1); }
    public void increaseSpeed() { speed = Math.min(4.5, speed + 0.35); }
    public void grantShield() { shield = true; }
    public void grantRemote() { remoteEnabled = true; }
    public void grantWaterMine() { waterMineCharges = Math.min(3, waterMineCharges + 1); }
    public boolean consumeWaterMine() {
        if (waterMineCharges <= 0) return false;
        waterMineCharges--;
        return true;
    }
    public void applyReverse() { reverseTicks = 60 * 8; }
    public void applySlow() { slowTicks = 60 * 8; }
    public void clearDebuffs() { reverseTicks = 0; slowTicks = 0; }
    public void eliminate() {
        state = PlayerState.DEAD;
        setLive(false);
    }

    public int getId() { return id; }
    public PlayerState getState() { return state; }
    public int getBlastRange() { return blastRange; }
    public int getMaxBubbles() { return maxBubbles; }
    public int getActiveBubbles() { return activeBubbles; }
    public double getSpeed() { return speed; }
    public boolean hasRemote() { return remoteEnabled; }
    public boolean hasShield() { return shield; }
    public int getWaterMineCharges() { return waterMineCharges; }
    public int getReverseTicks() { return reverseTicks; }
    public int getSlowTicks() { return slowTicks; }
    public TerrainType getTerrainEffect() { return terrainEffect; }
    public int getTerrainEffectTicks() { return terrainEffectTicks; }
}
