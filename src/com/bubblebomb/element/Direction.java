package com.bubblebomb.element;

/**
 * 定义移动和水柱传播方向，同时记录方向增量与精灵图行号。
 */
public enum Direction {
    DOWN(0, 1, 0),
    LEFT(-1, 0, 1),
    RIGHT(1, 0, 2),
    UP(0, -1, 3),
    CENTER(0, 0, 0);

    private final int dx;
    private final int dy;
    private final int spriteRow;

    Direction(int dx, int dy, int spriteRow) {
        this.dx = dx;
        this.dy = dy;
        this.spriteRow = spriteRow;
    }

    public int dx() { return dx; }
    public int dy() { return dy; }
    public int spriteRow() { return spriteRow; }
}
