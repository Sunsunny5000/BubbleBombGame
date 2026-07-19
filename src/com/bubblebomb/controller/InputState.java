package com.bubblebomb.controller;

import java.util.HashSet;
import java.util.Set;

/**
 * 保存当前持续按下的按键，以及本次逻辑帧中刚按下的按键。
 */
public final class InputState {
    private final Set<Integer> downKeys = new HashSet<Integer>();
    private final Set<Integer> pressedKeys = new HashSet<Integer>();

    public void press(int keyCode) {
        if (downKeys.add(keyCode)) {
            pressedKeys.add(keyCode);
        }
    }

    public void release(int keyCode) {
        downKeys.remove(keyCode);
    }

    public boolean isDown(int keyCode) {
        return downKeys.contains(keyCode);
    }

    public boolean consumePressed(int keyCode) {
        // 消费后立即移除，避免放置泡泡等单次操作被重复触发。
        return pressedKeys.remove(keyCode);
    }

    public void endTick() {
        pressedKeys.clear();
    }

    public void clear() {
        downKeys.clear();
        pressedKeys.clear();
    }
}
