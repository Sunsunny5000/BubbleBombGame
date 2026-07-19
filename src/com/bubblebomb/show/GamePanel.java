package com.bubblebomb.show;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.Timer;

import com.bubblebomb.config.GameConfig;
import com.bubblebomb.controller.InputState;
import com.bubblebomb.element.Player;
import com.bubblebomb.game.GameState;
import com.bubblebomb.game.GameMap;
import com.bubblebomb.manager.ElementManager;
import com.bubblebomb.manager.ResourceManager;

/**
 * 游戏主面板，负责状态机、逻辑刷新、界面绘制、回合计分和决胜倒计时。
 */
public final class GamePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final GameConfig config = GameConfig.getInstance();
    private final ResourceManager resources = ResourceManager.getInstance();
    private final ElementManager elements = ElementManager.getInstance();
    private final InputState input = new InputState();
    private final Timer timer;
    private GameState state = GameState.TITLE;
    private String winnerText = "";
    private final GameMap[] maps = GameMap.values();
    private final int[] boxDensities = { 25, 45, 65 };
    private final String[] densityNames = { "低", "中", "高" };
    private int selectedMapIndex;
    private int densityIndex = 1;
    private boolean showdownActive;
    private int showdownTicks;
    private int shrinkAccumulator;
    private final int[] scores = new int[2];
    private int roundNumber = 1;
    private String roundResult = "";

    public GamePanel() {
        setPreferredSize(new Dimension(config.windowWidth(), config.windowHeight()));
        setBackground(new Color(10, 35, 75));
        setFocusable(true);
        timer = new Timer(config.tickMillis(), e -> tick());
        timer.setCoalesce(true);
        timer.start();
    }

    private void tick() {
        // 只有运行状态更新游戏世界，暂停和结算状态仅执行重绘。
        if (state == GameState.RUNNING) {
            elements.update(input);
            String result = elements.getWinnerText();
            if (result != null) {
                finishRound(elements.getWinnerId(), result);
            } else {
                updateShowdown();
            }
        }
        input.endTick();
        repaint();
    }

    private void updateShowdown() {
        // 箱子清空后启动两分钟决胜，并在最后一分钟逐步缩小安全区域。
        if (!showdownActive && elements.getBoxCount() == 0) {
            showdownActive = true;
            showdownTicks = Math.max(1, config.showdownSeconds() * 1000 / config.tickMillis());
            shrinkAccumulator = 0;
            elements.prepareSuddenDeath();
        }
        if (!showdownActive) return;

        showdownTicks--;
        int oneMinuteTicks = Math.max(1,
                config.shrinkStartSeconds() * 1000 / config.tickMillis());
        if (showdownTicks <= oneMinuteTicks) {
            shrinkAccumulator++;
            int wallInterval = Math.max(1, config.wallIntervalMillis() / config.tickMillis());
            if (shrinkAccumulator >= wallInterval) {
                shrinkAccumulator = 0;
                elements.advanceSuddenDeathWall();
            }
        }
        if (showdownTicks <= 0) {
            finishRound(elements.getTimedWinnerId(), elements.getTimedResult());
        }
    }

    private void finishRound(int winnerId, String result) {
        // 平局不加分，任一玩家达到目标分数后结束整场比赛。
        roundResult = result;
        if (winnerId == 1 || winnerId == 2) {
            scores[winnerId - 1]++;
        }
        if (winnerId > 0 && scores[winnerId - 1] >= config.matchWins()) {
            winnerText = "玩家 " + winnerId + " 赢得比赛";
            state = GameState.GAME_OVER;
        } else {
            state = GameState.ROUND_OVER;
        }
        input.clear();
    }

    public void onKeyPressed(int keyCode) {
        // 同一个按键根据当前界面状态执行不同操作。
        if (state == GameState.TITLE) {
            if (keyCode == KeyEvent.VK_ENTER) startMatch();
            else if (keyCode == KeyEvent.VK_H) state = GameState.HELP;
            else if (keyCode == KeyEvent.VK_I) state = GameState.ITEM_HELP;
            else if (keyCode == KeyEvent.VK_LEFT) {
                selectedMapIndex = (selectedMapIndex + maps.length - 1) % maps.length;
            } else if (keyCode == KeyEvent.VK_RIGHT) {
                selectedMapIndex = (selectedMapIndex + 1) % maps.length;
            } else if (keyCode == KeyEvent.VK_UP) {
                densityIndex = (densityIndex + 1) % boxDensities.length;
            } else if (keyCode == KeyEvent.VK_DOWN) {
                densityIndex = (densityIndex + boxDensities.length - 1) % boxDensities.length;
            }
            return;
        }
        if (state == GameState.HELP || state == GameState.ITEM_HELP) {
            if (keyCode == KeyEvent.VK_ESCAPE) state = GameState.TITLE;
            else if (keyCode == KeyEvent.VK_TAB) {
                state = state == GameState.HELP ? GameState.ITEM_HELP : GameState.HELP;
            }
            return;
        }
        if (state == GameState.GAME_OVER) {
            if (keyCode == KeyEvent.VK_R || keyCode == KeyEvent.VK_ENTER) startMatch();
            else if (keyCode == KeyEvent.VK_ESCAPE) returnToTitle();
            return;
        }
        if (state == GameState.ROUND_OVER) {
            if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_N) {
                roundNumber++;
                startRound();
            } else if (keyCode == KeyEvent.VK_ESCAPE) {
                returnToTitle();
            }
            return;
        }
        if (state == GameState.PAUSED) {
            if (keyCode == KeyEvent.VK_P || keyCode == KeyEvent.VK_ENTER) {
                state = GameState.RUNNING;
            } else if (keyCode == KeyEvent.VK_R) {
                startRound();
            } else if (keyCode == KeyEvent.VK_ESCAPE) {
                returnToTitle();
            }
            return;
        }
        if (state == GameState.RUNNING) {
            if (keyCode == KeyEvent.VK_P) {
                input.clear();
                state = GameState.PAUSED;
            } else if (keyCode == KeyEvent.VK_ESCAPE) {
                returnToTitle();
            } else {
                input.press(keyCode);
            }
        }
    }

    public void onKeyReleased(int keyCode) {
        input.release(keyCode);
    }

    private void startMatch() {
        // 新比赛会清空双方比分并从第一回合开始。
        scores[0] = 0;
        scores[1] = 0;
        roundNumber = 1;
        startRound();
    }

    private void startRound() {
        // 新回合保留比赛比分，但重新创建地图、玩家和全部游戏元素。
        input.clear();
        winnerText = "";
        roundResult = "";
        showdownActive = false;
        showdownTicks = 0;
        shrinkAccumulator = 0;
        elements.resetLevel(maps[selectedMapIndex], boxDensities[densityIndex]);
        state = GameState.RUNNING;
        requestFocusInWindow();
    }

    private void returnToTitle() {
        input.clear();
        elements.clear();
        showdownActive = false;
        scores[0] = 0;
        scores[1] = 0;
        roundNumber = 1;
        state = GameState.TITLE;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        // 根据状态机绘制标题、说明、游戏、暂停或结算画面。
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (state == GameState.TITLE) drawTitle(g);
        else if (state == GameState.HELP) drawGuide(g, "game.guide", "游戏说明");
        else if (state == GameState.ITEM_HELP) drawGuide(g, "item.guide", "道具说明");
        else if (state == GameState.RUNNING) drawGame(g);
        else if (state == GameState.PAUSED) {
            drawGame(g);
            drawPause(g);
        }
        else if (state == GameState.ROUND_OVER) {
            drawGame(g);
            drawRoundOver(g);
        }
        else if (state == GameState.GAME_OVER) drawGameOver(g);
        g.dispose();
    }

    private void drawTitle(Graphics2D g) {
        drawFullImage(g, resources.getImage("title"));
        g.setColor(new Color(0, 20, 60, 175));
        g.fillRoundRect(45, 345, 710, 225, 24, 24);
        drawMapPreview(g, maps[selectedMapIndex], 70, 375, 260, 165);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        g.drawString("地图：" + maps[selectedMapIndex].displayName(), 360, 390);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(195, 235, 255));
        g.drawString(maps[selectedMapIndex].description(), 360, 417);
        g.drawString("← → 选择地图", 360, 445);
        g.drawString("↑ ↓ 箱子密度：" + densityNames[densityIndex]
                + "（" + boxDensities[densityIndex] + "%）", 360, 470);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 21));
        g.drawString("Enter 开始游戏", 360, 505);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(190, 230, 255));
        g.drawString("H 游戏说明    I 道具说明", 360, 532);
        g.drawString("P1: WASD/B/V   P2: 方向键/Enter/斜杠", 360, 554);
    }

    private void drawMapPreview(Graphics2D g, GameMap map, int x, int y, int width, int height) {
        // 缩略图展示固定结构和箱子密度示意，实际箱子在开局时重新生成。
        java.util.List<String> lines = elements.getBaseMap(map);
        int rows = Math.max(1, lines.size());
        int columns = config.boardWidth() / config.tileSize();
        double cellW = width / (double) columns;
        double cellH = height / (double) rows;
        g.setColor(new Color(35, 115, 75));
        g.fillRect(x, y, width, height);
        for (int row = 0; row < lines.size(); row++) {
            String line = lines.get(row);
            for (int column = 0; column < Math.min(columns, line.length()); column++) {
                char cell = line.charAt(column);
                int px = x + (int) Math.floor(column * cellW);
                int py = y + (int) Math.floor(row * cellH);
                int pw = Math.max(1, (int) Math.ceil(cellW));
                int ph = Math.max(1, (int) Math.ceil(cellH));
                if (cell == '#') g.setColor(new Color(35, 65, 125));
                else if (cell == 'S') g.setColor(new Color(60, 220, 240));
                else if (cell == 'M') g.setColor(new Color(130, 85, 55));
                else if (cell == '1') g.setColor(new Color(250, 80, 70));
                else if (cell == '2') g.setColor(new Color(255, 220, 70));
                else {
                    int hash = Math.abs((row * 37 + column * 17 + selectedMapIndex * 13) % 100);
                    g.setColor(hash < boxDensities[densityIndex]
                            ? new Color(195, 125, 55) : new Color(75, 165, 85));
                }
                g.fillRect(px, py, pw, ph);
            }
        }
        g.setColor(new Color(190, 230, 255));
        g.drawRect(x, y, width, height);
    }

    private void drawGuide(Graphics2D g, String imageKey, String title) {
        drawFullImage(g, resources.getImage(imageKey));
        g.setColor(new Color(0, 20, 55, 150));
        g.fillRect(0, 0, getWidth(), 48);
        drawCentered(g, title + "    Tab：切换说明    Esc：返回", 32, 18, Color.WHITE);
    }

    private void drawGame(Graphics2D g) {
        // 先绘制顶部信息和地图底色，再交给元素管理器按层级绘制元素。
        int boardTop = config.boardTop();
        int tile = config.tileSize();
        g.setColor(new Color(8, 38, 84));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(220, 245, 255));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("BubbleBomb", 16, 37);
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString("第" + roundNumber + "回合  " + scores[0] + " : " + scores[1], 175, 36);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        if (showdownActive) {
            int seconds = Math.max(0, showdownTicks * config.tickMillis() / 1000);
            String time = String.format("决胜 %02d:%02d", seconds / 60, seconds % 60);
            g.setColor(seconds <= config.warningSeconds() && (showdownTicks / 15) % 2 == 0
                    ? new Color(255, 80, 70) : new Color(235, 250, 255));
            g.setFont(new Font("SansSerif", Font.BOLD,
                    seconds <= config.warningSeconds() ? 22 : 17));
            g.drawString(time, 330, 37);
            if (seconds <= config.warningSeconds()) {
                g.drawString("警告：安全区域正在缩小！", 445, 37);
            }
        } else {
            g.drawString("剩余箱子：" + elements.getBoxCount(), 330, 36);
        }
        g.setColor(new Color(220, 245, 255));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("P 暂停", 720, 36);

        Graphics2D board = (Graphics2D) g.create();
        board.translate(0, boardTop);
        for (int y = 0; y < config.boardHeight(); y += tile) {
            for (int x = 0; x < config.boardWidth(); x += tile) {
                boolean light = ((x / tile) + (y / tile)) % 2 == 0;
                board.setColor(light ? new Color(100, 190, 105) : new Color(88, 176, 96));
                board.fillRect(x, y, tile, tile);
                board.setColor(new Color(255, 255, 255, 22));
                board.drawRect(x, y, tile, tile);
            }
        }
        elements.draw(board);
        board.dispose();

        g.setColor(new Color(115, 205, 255));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(0, boardTop, config.boardWidth(), config.boardHeight());
        drawStatusPanel(g);
    }

    private void drawStatusPanel(Graphics2D g) {
        int x = config.boardWidth();
        int top = config.boardTop();
        BufferedImage panel = resources.getImage("status.panel");
        g.drawImage(panel, x, top, getWidth(), top + config.boardHeight(),
                0, 0, panel.getWidth(), panel.getHeight(), null);
        g.setColor(new Color(0, 25, 70, 185));
        g.fillRoundRect(x + 8, top + 8, getWidth() - x - 16, 220, 16, 16);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        int y = top + 36;
        for (Player player : elements.getPlayers()) {
            g.setColor(player.getId() == 1 ? new Color(255, 125, 115) : new Color(255, 220, 95));
            g.drawString("玩家 " + player.getId(), x + 20, y);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.drawString("状态：" + stateText(player), x + 20, y + 22);
            g.drawString("泡泡：" + player.getActiveBubbles() + "/" + player.getMaxBubbles(), x + 20, y + 42);
            g.drawString("水柱：" + player.getBlastRange(), x + 20, y + 62);
            g.drawString("效果：" + effectText(player), x + 20, y + 80);
            y += 112;
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
        }
        g.setColor(new Color(225, 245, 255));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("P1  WASD / B放置 / V遥控", x + 14, top + 300);
        g.drawString("P2  方向键 / Enter / 斜杠", x + 14, top + 322);
    }

    private String stateText(Player player) {
        switch (player.getState()) {
            case TRAPPED: return "被困";
            case DEAD: return "出局";
            default: return "正常";
        }
    }

    private String effectText(Player player) {
        if (player.getReverseTicks() > 0) return "方向反转";
        if (player.getSlowTicks() > 0) return "减速";
        if (player.getTerrainEffect() != null) {
            double seconds = player.getTerrainEffectTicks() * config.tickMillis() / 1000.0;
            String name = player.getTerrainEffect() == com.bubblebomb.element.TerrainType.SPEED
                    ? "加速" : "泥地减速";
            return name + String.format(" %.1fs", seconds);
        }
        if (player.hasShield()) return "护盾";
        if (player.getWaterMineCharges() > 0) return "强化水雷×" + player.getWaterMineCharges();
        if (player.hasRemote()) return "遥控器";
        return "无";
    }

    private void drawGameOver(Graphics2D g) {
        drawFullImage(g, resources.getImage("game.over"));
        g.setColor(new Color(0, 20, 60, 175));
        g.fillRoundRect(150, 370, 500, 180, 28, 28);
        drawCentered(g, winnerText, 420, 34, new Color(255, 240, 95));
        drawCentered(g, "最终比分  " + scores[0] + " : " + scores[1], 462, 26, Color.WHITE);
        drawCentered(g, "R 或 Enter：开始新比赛", 505, 20, Color.WHITE);
        drawCentered(g, "Esc：返回标题", 535, 16, new Color(200, 235, 255));
    }

    private void drawPause(Graphics2D g) {
        g.setColor(new Color(0, 15, 45, 190));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(15, 65, 125, 235));
        g.fillRoundRect(190, 180, 420, 245, 28, 28);
        drawCentered(g, "游戏已暂停", 240, 38, new Color(255, 240, 100));
        drawCentered(g, "P 或 Enter：继续游戏", 300, 21, Color.WHITE);
        drawCentered(g, "R：重新开始当前回合", 340, 19, Color.WHITE);
        drawCentered(g, "Esc：放弃比赛并返回标题", 380, 18, new Color(195, 230, 255));
    }

    private void drawRoundOver(Graphics2D g) {
        g.setColor(new Color(0, 15, 45, 185));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(15, 65, 125, 238));
        g.fillRoundRect(175, 165, 450, 275, 28, 28);
        drawCentered(g, "第 " + roundNumber + " 回合结束", 220, 30, new Color(255, 240, 100));
        drawCentered(g, roundResult, 270, 25, Color.WHITE);
        drawCentered(g, "当前比分  " + scores[0] + " : " + scores[1], 320, 30,
                new Color(150, 225, 255));
        drawCentered(g, "先获得 " + config.matchWins() + " 分者赢得比赛", 355, 17,
                new Color(205, 235, 255));
        drawCentered(g, "Enter 或 N：下一回合", 397, 20, Color.WHITE);
        drawCentered(g, "Esc：返回标题", 425, 15, new Color(195, 225, 250));
    }

    private void drawFullImage(Graphics2D g, BufferedImage image) {
        g.drawImage(image, 0, 0, getWidth(), getHeight(), 0, 0, image.getWidth(), image.getHeight(), null);
    }

    private void drawCentered(Graphics2D g, String text, int baseline, int size, Color color) {
        g.setFont(new Font("SansSerif", Font.BOLD, size));
        g.setColor(color);
        int x = (getWidth() - g.getFontMetrics().stringWidth(text)) / 2;
        g.drawString(text, x, baseline);
    }
}
