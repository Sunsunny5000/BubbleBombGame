package com.bubblebomb.element.prop;

/**
 * 定义全部道具类型、对应图片键和界面显示名称。
 */
public enum PropType {
    BUBBLE_UP("bubble.item", "泡泡+1"),
    RANGE_UP("potion.blue", "水柱+1"),
    ANTIDOTE("antidote", "解除异常"),
    REMOTE("remote", "遥控引爆"),
    WATER_MINE("water.mine", "强化水雷"),
    DEMON_RED("demon.red", "减速诅咒"),
    DEMON_PURPLE("demon.purple", "方向反转"),
    GOLD_CARD("card.gold", "速度提升"),
    DIAMOND_CARD("card.diamond", "一次护盾");

    private final String imageKey;
    private final String displayName;

    PropType(String imageKey, String displayName) {
        this.imageKey = imageKey;
        this.displayName = displayName;
    }

    public String imageKey() { return imageKey; }
    public String displayName() { return displayName; }
}
