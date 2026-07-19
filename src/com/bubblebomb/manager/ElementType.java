package com.bubblebomb.manager;

/**
 * 对游戏元素进行分类，枚举顺序同时决定基础绘制层级。
 */
public enum ElementType {
    TERRAIN,
    WALL,
    BOX,
    PROP,
    BUBBLE,
    WATER_BLAST,
    PLAYER,
    EFFECT
}
