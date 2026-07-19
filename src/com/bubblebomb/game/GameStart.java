package com.bubblebomb.game;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.bubblebomb.manager.ResourceManager;
import com.bubblebomb.show.GameFrame;

/**
 * 游戏正式启动入口，负责初始化Swing环境、资源和主窗口。
 */
public final class GameStart {
    private GameStart() { }

    public static void main(String[] args) {
        // Swing组件统一在事件调度线程中创建和更新。
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            ResourceManager.getInstance().preload();
            new GameFrame().start();
        });
    }
}
