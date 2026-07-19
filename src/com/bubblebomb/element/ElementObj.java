package com.bubblebomb.element;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.bubblebomb.controller.InputState;
import com.bubblebomb.manager.ElementManager;
import com.bubblebomb.manager.ElementType;
import com.bubblebomb.manager.ResourceManager;

/**
 * 所有游戏元素的抽象父类，统一保存位置、尺寸、状态和元素类型。
 */
public abstract class ElementObj {
    private double x;
    private double y;
    private final int width;
    private final int height;
    private boolean live = true;
    private final ElementType type;

    protected ElementObj(ElementType type, double x, double y, int width, int height) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void draw(Graphics2D g, ResourceManager resources);

    /** 每个逻辑帧调用一次，具体元素按需重写。 */
    public void update(ElementManager manager, InputState input) { }

    public Rectangle getBounds() {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
    }

    public boolean intersects(ElementObj other) {
        return getBounds().intersects(other.getBounds());
    }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isLive() { return live; }
    public void setLive(boolean live) { this.live = live; }
    public ElementType getType() { return type; }
}
