package com.bubblebomb.game;

/**
 * 定义可选择的地图、地图文件位置和地图特点说明。
 */
public enum GameMap {
    CLASSIC("经典对称", "resources/maps/level_01.map", "固定石柱，路线均衡"),
    GARDEN("开放花园", "resources/maps/level_02.map", "开阔区域和加速地面"),
    MAZE("泥地迷宫", "resources/maps/level_03.map", "狭窄通道和减速泥地"),
    RANDOM("随机地图", null, "对称生成，每局箱子不同");

    private final String displayName;
    private final String path;
    private final String description;

    GameMap(String displayName, String path, String description) {
        this.displayName = displayName;
        this.path = path;
        this.description = description;
    }

    public String displayName() { return displayName; }
    public String path() { return path; }
    public String description() { return description; }
}
