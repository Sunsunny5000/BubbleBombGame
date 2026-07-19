package com.bubblebomb.show;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.bubblebomb.controller.GameListener;

/**
 * 游戏主窗口，负责安装游戏面板、键盘监听并显示固定大小窗口。
 */
public final class GameFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final GamePanel gamePanel;

    public GameFrame() {
        super("BubbleBomb 泡泡堂");
        gamePanel = new GamePanel();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(gamePanel);
        gamePanel.addKeyListener(new GameListener(gamePanel));
        pack();
        setLocationRelativeTo(null);
    }

    public void start() {
        setVisible(true);
        SwingUtilities.invokeLater(() -> gamePanel.requestFocusInWindow());
    }
}
