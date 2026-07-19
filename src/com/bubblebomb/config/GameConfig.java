package com.bubblebomb.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 读取并提供窗口、地图、泡泡和比赛规则等游戏配置。
 */
public final class GameConfig {
    private static final GameConfig INSTANCE = new GameConfig();
    private final Properties values = new Properties();

    private GameConfig() {
        load("resources/config/game.properties");
    }

    public static GameConfig getInstance() {
        return INSTANCE;
    }

    private void load(String path) {
        // 优先从类路径读取，便于以后打包；失败时再读取项目目录中的文件。
        InputStream input = null;
        try {
            input = GameConfig.class.getClassLoader().getResourceAsStream(path);
            if (input == null) {
                File file = new File(path);
                if (file.isFile()) {
                    input = new FileInputStream(file);
                }
            }
            if (input != null) {
                values.load(input);
            }
        } catch (IOException e) {
            System.err.println("Unable to load game config: " + e.getMessage());
        } finally {
            if (input != null) {
                try { input.close(); } catch (IOException ignored) { }
            }
        }
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(values.getProperty(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int windowWidth() { return getInt("window.width", 800); }
    public int windowHeight() { return getInt("window.height", 600); }
    public int boardWidth() { return getInt("board.width", 640); }
    public int boardHeight() { return getInt("board.height", 480); }
    public int boardTop() { return getInt("board.top", 60); }
    public int tileSize() { return getInt("tile.size", 32); }
    public int tickMillis() { return getInt("game.tickMillis", 16); }
    public int bubbleFuseTicks() { return getInt("bubble.fuseTicks", 180); }
    public int blastLifeTicks() { return getInt("blast.lifeTicks", 30); }
    public int trappedTicks() { return getInt("player.trappedTicks", 90); }
    public int showdownSeconds() { return getInt("showdown.seconds", 120); }
    public int shrinkStartSeconds() { return getInt("showdown.shrinkStartSeconds", 60); }
    public int warningSeconds() { return getInt("showdown.warningSeconds", 30); }
    public int wallIntervalMillis() { return getInt("showdown.wallIntervalMillis", 500); }
    public int matchWins() { return getInt("match.wins", 3); }
    public int speedLingerMillis() { return getInt("terrain.speedLingerMillis", 2000); }
    public int mudLingerMillis() { return getInt("terrain.mudLingerMillis", 1000); }
}
