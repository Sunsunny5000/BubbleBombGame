package com.bubblebomb.controller;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import com.bubblebomb.show.GamePanel;

/**
 * 接收键盘按下和松开事件，并将事件转交给游戏面板处理。
 */
public final class GameListener extends KeyAdapter {
    private final GamePanel panel;

    public GameListener(GamePanel panel) {
        this.panel = panel;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        panel.onKeyPressed(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        panel.onKeyReleased(e.getKeyCode());
    }
}
