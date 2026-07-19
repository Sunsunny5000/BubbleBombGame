package com.bubblebomb.manager;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.bubblebomb.config.GameConfig;
import com.bubblebomb.controller.InputState;
import com.bubblebomb.element.Box;
import com.bubblebomb.element.Bubble;
import com.bubblebomb.element.Direction;
import com.bubblebomb.element.ElementObj;
import com.bubblebomb.element.Player;
import com.bubblebomb.element.PlayerState;
import com.bubblebomb.element.Terrain;
import com.bubblebomb.element.TerrainType;
import com.bubblebomb.element.Wall;
import com.bubblebomb.element.WaterBlast;
import com.bubblebomb.element.prop.Prop;
import com.bubblebomb.element.prop.PropType;
import com.bubblebomb.game.GameMap;

/**
 * 游戏元素单例管理器，负责地图加载、元素更新、碰撞、爆炸、道具和缩圈。
 */
public final class ElementManager {
    private static final ElementManager INSTANCE = new ElementManager();
    private final Map<ElementType, List<ElementObj>> elements =
            new EnumMap<ElementType, List<ElementObj>>(ElementType.class);
    private final GameConfig config = GameConfig.getInstance();
    private final ResourceManager resources = ResourceManager.getInstance();
    private final Random random = new Random();
    private final List<Point> suddenDeathCells = new ArrayList<Point>();
    private int suddenDeathIndex;
    private Player playerOne;
    private Player playerTwo;

    private ElementManager() {
        for (ElementType type : ElementType.values()) {
            elements.put(type, new ArrayList<ElementObj>());
        }
    }

    public static ElementManager getInstance() {
        return INSTANCE;
    }

    public void resetLevel() {
        resetLevel(GameMap.CLASSIC, 45);
    }

    public void resetLevel(GameMap map, int boxDensity) {
        // 每个新回合先清空旧元素，再按所选地图重新创建场景和玩家。
        clear();
        int tile = config.tileSize();
        List<String> lines = getBaseMap(map);

        int spawn1X = tile;
        int spawn1Y = tile;
        int spawn2X = config.boardWidth() - tile * 2;
        int spawn2Y = config.boardHeight() - tile * 2;

        for (int row = 0; row < lines.size(); row++) {
            String line = lines.get(row);
            for (int column = 0; column < line.length(); column++) {
                char cell = line.charAt(column);
                int x = column * tile;
                int y = row * tile;
                if (cell == '#') {
                    add(new Wall(x, y, tile));
                } else if (cell == 'S') {
                    add(new Terrain(x, y, tile, TerrainType.SPEED));
                } else if (cell == 'M') {
                    add(new Terrain(x, y, tile, TerrainType.MUD));
                } else if (cell == '1') {
                    spawn1X = x + 1;
                    spawn1Y = y + 1;
                } else if (cell == '2') {
                    spawn2X = x + 1;
                    spawn2Y = y + 1;
                }
            }
        }

        addSymmetricBoxes(lines, boxDensity, spawn1X / tile, spawn1Y / tile,
                spawn2X / tile, spawn2Y / tile);

        playerOne = Player.createPlayerOne(spawn1X, spawn1Y);
        playerTwo = Player.createPlayerTwo(spawn2X, spawn2Y);
        add(playerOne);
        add(playerTwo);
    }

    public List<String> getBaseMap(GameMap map) {
        if (map != GameMap.RANDOM && map.path() != null) {
            List<String> lines = resources.readLines(map.path());
            if (!lines.isEmpty()) return lines;
        }
        return createDefaultMap();
    }

    private void addSymmetricBoxes(List<String> lines, int density,
            int spawn1Column, int spawn1Row, int spawn2Column, int spawn2Row) {
        // 箱子以地图中心成对生成，并为两个出生点保留安全区域。
        int rows = Math.min(lines.size(), config.boardHeight() / config.tileSize());
        int columns = config.boardWidth() / config.tileSize();
        boolean[][] eligible = new boolean[rows][columns];
        boolean[][] placed = new boolean[rows][columns];
        for (int row = 1; row < rows - 1; row++) {
            String line = lines.get(row);
            for (int column = 1; column < Math.min(columns - 1, line.length()); column++) {
                char cell = line.charAt(column);
                boolean open = cell == '.' || cell == 'B';
                boolean safe1 = Math.abs(column - spawn1Column) + Math.abs(row - spawn1Row) <= 2;
                boolean safe2 = Math.abs(column - spawn2Column) + Math.abs(row - spawn2Row) <= 2;
                eligible[row][column] = open && !safe1 && !safe2;
            }
        }

        int tile = config.tileSize();
        for (int row = 1; row < rows - 1; row++) {
            for (int column = 1; column < columns - 1; column++) {
                int mirrorRow = rows - 1 - row;
                int mirrorColumn = columns - 1 - column;
                int index = row * columns + column;
                int mirrorIndex = mirrorRow * columns + mirrorColumn;
                if (index > mirrorIndex || !eligible[row][column]
                        || !eligible[mirrorRow][mirrorColumn]) {
                    continue;
                }
                if (random.nextInt(100) < Math.max(0, Math.min(85, density))) {
                    placed[row][column] = true;
                    placed[mirrorRow][mirrorColumn] = true;
                    if (!isOpenAreaConnected(lines, placed, rows, columns,
                            spawn1Column, spawn1Row)) {
                        placed[row][column] = false;
                        placed[mirrorRow][mirrorColumn] = false;
                    }
                }
            }
        }
        for (int row = 1; row < rows - 1; row++) {
            for (int column = 1; column < columns - 1; column++) {
                if (placed[row][column]) {
                    add(new Box(column * tile, row * tile, tile));
                }
            }
        }
    }

    private boolean isOpenAreaConnected(List<String> lines, boolean[][] boxes,
            int rows, int columns, int startColumn, int startRow) {
        // 使用广度优先搜索，拒绝会把可行走区域分割成孤岛的箱子组合。
        boolean[][] visited = new boolean[rows][columns];
        ArrayDeque<Point> queue = new ArrayDeque<Point>();
        queue.add(new Point(startColumn, startRow));
        visited[startRow][startColumn] = true;
        int visitedCount = 0;
        int openCount = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (isOpenCell(lines, boxes, row, column)) openCount++;
            }
        }
        int[] dx = { 1, -1, 0, 0 };
        int[] dy = { 0, 0, 1, -1 };
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            visitedCount++;
            for (int i = 0; i < 4; i++) {
                int nextColumn = point.x + dx[i];
                int nextRow = point.y + dy[i];
                if (nextRow >= 0 && nextRow < rows && nextColumn >= 0 && nextColumn < columns
                        && !visited[nextRow][nextColumn]
                        && isOpenCell(lines, boxes, nextRow, nextColumn)) {
                    visited[nextRow][nextColumn] = true;
                    queue.addLast(new Point(nextColumn, nextRow));
                }
            }
        }
        return visitedCount == openCount;
    }

    private boolean isOpenCell(List<String> lines, boolean[][] boxes, int row, int column) {
        return column < lines.get(row).length()
                && lines.get(row).charAt(column) != '#'
                && !boxes[row][column];
    }

    private List<String> createDefaultMap() {
        List<String> map = new ArrayList<String>();
        int rows = config.boardHeight() / config.tileSize();
        int columns = config.boardWidth() / config.tileSize();
        for (int row = 0; row < rows; row++) {
            StringBuilder line = new StringBuilder();
            for (int column = 0; column < columns; column++) {
                if (row == 0 || column == 0 || row == rows - 1 || column == columns - 1
                        || (row % 2 == 0 && column % 2 == 0)) {
                    line.append('#');
                } else if (row == 1 && column == 1) {
                    line.append('1');
                } else if (row == rows - 2 && column == columns - 2) {
                    line.append('2');
                } else {
                    line.append('.');
                }
            }
            map.add(line.toString());
        }
        return map;
    }

    public void update(InputState input) {
        // 使用列表快照更新元素，避免更新过程中新增或删除元素造成遍历异常。
        for (ElementType type : ElementType.values()) {
            List<ElementObj> snapshot = new ArrayList<ElementObj>(elements.get(type));
            for (ElementObj element : snapshot) {
                if (element.isLive()) {
                    element.update(this, input);
                }
            }
        }

        processTriggeredBubbles();
        processBlastHits();
        processPropPickup();
        removeDeadElements();
    }

    private void processTriggeredBubbles() {
        // 持续处理被水柱触发的泡泡，直到本帧没有新的连锁爆炸。
        boolean found;
        do {
            found = false;
            List<ElementObj> snapshot = new ArrayList<ElementObj>(elements.get(ElementType.BUBBLE));
            for (ElementObj element : snapshot) {
                Bubble bubble = (Bubble) element;
                if (bubble.isLive() && bubble.isTriggered()) {
                    explode(bubble);
                    found = true;
                }
            }
        } while (found);
    }

    private void explode(Bubble bubble) {
        // 从爆炸中心向四个方向逐格传播，遇墙、箱子或泡泡时按规则停止。
        if (!bubble.isLive()) {
            return;
        }
        bubble.setLive(false);
        bubble.getOwner().bubbleReturned();
        int tile = config.tileSize();
        int originX = (int) bubble.getX();
        int originY = (int) bubble.getY();
        add(new WaterBlast(originX, originY, tile, Direction.CENTER));

        Direction[] directions = { Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT };
        for (Direction direction : directions) {
            for (int step = 1; step <= bubble.getBlastRange(); step++) {
                int x = originX + direction.dx() * tile * step;
                int y = originY + direction.dy() * tile * step;
                if (findAt(ElementType.WALL, x, y) != null) {
                    break;
                }

                add(new WaterBlast(x, y, tile, direction));
                ElementObj box = findAt(ElementType.BOX, x, y);
                if (box != null && box.isLive()) {
                    box.setLive(false);
                    dropProp(x, y);
                    break;
                }

                ElementObj otherBubble = findAt(ElementType.BUBBLE, x, y);
                if (otherBubble instanceof Bubble && otherBubble.isLive()) {
                    ((Bubble) otherBubble).trigger();
                    break;
                }

                ElementObj prop = findAt(ElementType.PROP, x, y);
                if (prop != null) {
                    prop.setLive(false);
                }
            }
        }
    }

    private void dropProp(int x, int y) {
        if (random.nextInt(100) >= 42) {
            return;
        }
        PropType[] types = PropType.values();
        add(new Prop(x, y, config.tileSize(), types[random.nextInt(types.length)]));
    }

    private void processBlastHits() {
        List<ElementObj> blasts = new ArrayList<ElementObj>(elements.get(ElementType.WATER_BLAST));
        for (ElementObj blast : blasts) {
            if (!blast.isLive()) continue;
            for (Player player : getPlayers()) {
                if (player.isLive() && blast.getBounds().intersects(player.getBounds())) {
                    player.hit();
                }
            }
        }
    }

    private void processPropPickup() {
        List<ElementObj> props = new ArrayList<ElementObj>(elements.get(ElementType.PROP));
        for (Player player : getPlayers()) {
            if (!player.isLive() || player.getState() != PlayerState.NORMAL) continue;
            for (ElementObj element : props) {
                if (element.isLive() && player.intersects(element)) {
                    ((Prop) element).applyTo(player);
                }
            }
        }
    }

    public boolean canMove(Player player, double newX, double newY) {
        Rectangle next = player.getCollisionBounds(newX, newY);
        if (next.x < 0 || next.y < 0 || next.x + next.width > config.boardWidth()
                || next.y + next.height > config.boardHeight()) {
            return false;
        }
        for (ElementObj wall : elements.get(ElementType.WALL)) {
            if (wall.isLive() && next.intersects(wall.getBounds())) return false;
        }
        for (ElementObj box : elements.get(ElementType.BOX)) {
            if (box.isLive() && next.intersects(box.getBounds())) return false;
        }
        for (ElementObj element : elements.get(ElementType.BUBBLE)) {
            Bubble bubble = (Bubble) element;
            if (bubble.isLive() && bubble.blocks(player) && next.intersects(bubble.getBounds())) {
                double dx = newX - player.getX();
                double dy = newY - player.getY();
                if (!tryPushBubble(bubble, dx, dy)) return false;
            }
        }
        return true;
    }

    private boolean tryPushBubble(Bubble bubble, double dx, double dy) {
        // 泡泡后方一格为空时，将泡泡沿玩家移动方向推动一格。
        if (!bubble.canBePushed()) return false;
        Direction direction;
        if (Math.abs(dx) >= Math.abs(dy) && dx != 0) {
            direction = dx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else if (dy != 0) {
            direction = dy > 0 ? Direction.DOWN : Direction.UP;
        } else {
            return false;
        }
        int tile = config.tileSize();
        int targetX = (int) bubble.getX() + direction.dx() * tile;
        int targetY = (int) bubble.getY() + direction.dy() * tile;
        if (targetX < 0 || targetY < 0 || targetX >= config.boardWidth()
                || targetY >= config.boardHeight()
                || findAt(ElementType.WALL, targetX, targetY) != null
                || findAt(ElementType.BOX, targetX, targetY) != null
                || findAt(ElementType.BUBBLE, targetX, targetY) != null) {
            return false;
        }
        bubble.pushTo(targetX, targetY);
        return true;
    }

    public double getTerrainSpeedMultiplier(Player player) {
        TerrainType type = getTerrainType(player);
        return type == null ? 1.0 : type.speedMultiplier();
    }

    public TerrainType getTerrainType(Player player) {
        int centerX = (int) player.getX() + player.getWidth() / 2;
        int centerY = (int) player.getY() + player.getHeight() / 2;
        for (ElementObj element : elements.get(ElementType.TERRAIN)) {
            Terrain terrain = (Terrain) element;
            if (terrain.getBounds().contains(centerX, centerY)) {
                return terrain.getTerrainType();
            }
        }
        return null;
    }

    public void placeBubble(Player player) {
        if (!player.canPlaceBubble()) {
            return;
        }
        int tile = config.tileSize();
        int column = (int) ((player.getX() + player.getWidth() / 2.0) / tile);
        int row = (int) ((player.getY() + player.getHeight() / 2.0) / tile);
        int x = column * tile;
        int y = row * tile;
        if (findAt(ElementType.BUBBLE, x, y) != null
                || findAt(ElementType.WALL, x, y) != null
                || findAt(ElementType.BOX, x, y) != null) {
            return;
        }
        add(new Bubble(player, x, y, tile));
        player.bubblePlaced();
    }

    public boolean triggerOldestBubble(Player owner) {
        for (ElementObj element : elements.get(ElementType.BUBBLE)) {
            Bubble bubble = (Bubble) element;
            if (bubble.isLive() && bubble.getOwner() == owner) {
                bubble.trigger();
                return true;
            }
        }
        return false;
    }

    public int getBoxCount() {
        int count = 0;
        for (ElementObj box : elements.get(ElementType.BOX)) {
            if (box.isLive()) count++;
        }
        return count;
    }

    public int getElementCount(ElementType type) {
        int count = 0;
        for (ElementObj element : elements.get(type)) {
            if (element.isLive()) count++;
        }
        return count;
    }

    public void prepareSuddenDeath() {
        // 按由外向内的螺旋顺序预先生成决胜缩圈位置。
        suddenDeathCells.clear();
        suddenDeathIndex = 0;
        int columns = config.boardWidth() / config.tileSize();
        int rows = config.boardHeight() / config.tileSize();
        int left = 1;
        int right = columns - 2;
        int top = 1;
        int bottom = rows - 2;
        while (left <= right && top <= bottom) {
            for (int column = left; column <= right; column++) {
                suddenDeathCells.add(new Point(column, top));
            }
            for (int row = top + 1; row <= bottom; row++) {
                suddenDeathCells.add(new Point(right, row));
            }
            if (bottom > top) {
                for (int column = right - 1; column >= left; column--) {
                    suddenDeathCells.add(new Point(column, bottom));
                }
            }
            if (right > left) {
                for (int row = bottom - 1; row > top; row--) {
                    suddenDeathCells.add(new Point(left, row));
                }
            }
            left++;
            right--;
            top++;
            bottom--;
        }
    }

    public boolean advanceSuddenDeathWall() {
        // 每次同时在中心对称位置生成墙，保证双方受到的缩圈压力一致。
        int tile = config.tileSize();
        int columns = config.boardWidth() / tile;
        int rows = config.boardHeight() / tile;
        while (suddenDeathIndex < suddenDeathCells.size()) {
            Point cell = suddenDeathCells.get(suddenDeathIndex++);
            int x = cell.x * tile;
            int y = cell.y * tile;
            if (findAt(ElementType.WALL, x, y) != null) continue;
            placeSuddenDeathWall(x, y);
            int mirrorX = (columns - 1 - cell.x) * tile;
            int mirrorY = (rows - 1 - cell.y) * tile;
            if ((mirrorX != x || mirrorY != y)
                    && findAt(ElementType.WALL, mirrorX, mirrorY) == null) {
                placeSuddenDeathWall(mirrorX, mirrorY);
            }
            return true;
        }
        return false;
    }

    private void placeSuddenDeathWall(int x, int y) {
        crushCell(x, y);
        add(new Wall(x, y, config.tileSize()));
    }

    private void crushCell(int x, int y) {
        Rectangle cell = new Rectangle(x, y, config.tileSize(), config.tileSize());
        for (Player player : getPlayers()) {
            if (player.isLive() && cell.intersects(player.getBounds())) {
                player.eliminate();
            }
        }
        ElementType[] removable = {
                ElementType.TERRAIN, ElementType.BOX, ElementType.PROP,
                ElementType.WATER_BLAST, ElementType.EFFECT
        };
        for (ElementType type : removable) {
            for (ElementObj element : elements.get(type)) {
                if (element.isLive() && cell.intersects(element.getBounds())) {
                    element.setLive(false);
                }
            }
        }
        for (ElementObj element : elements.get(ElementType.BUBBLE)) {
            Bubble bubble = (Bubble) element;
            if (bubble.isLive() && cell.intersects(bubble.getBounds())) {
                bubble.setLive(false);
                bubble.getOwner().bubbleReturned();
            }
        }
        removeDeadElements();
    }

    private ElementObj findAt(ElementType type, int x, int y) {
        for (ElementObj element : elements.get(type)) {
            if (element.isLive() && (int) element.getX() == x && (int) element.getY() == y) {
                return element;
            }
        }
        return null;
    }

    public void draw(Graphics2D g) {
        for (ElementType type : ElementType.values()) {
            for (ElementObj element : new ArrayList<ElementObj>(elements.get(type))) {
                if (element.isLive()) {
                    element.draw(g, resources);
                }
            }
        }
    }

    private void add(ElementObj element) {
        elements.get(element.getType()).add(element);
    }

    private void removeDeadElements() {
        for (List<ElementObj> list : elements.values()) {
            Iterator<ElementObj> iterator = list.iterator();
            while (iterator.hasNext()) {
                if (!iterator.next().isLive()) {
                    iterator.remove();
                }
            }
        }
    }

    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<Player>();
        if (playerOne != null) players.add(playerOne);
        if (playerTwo != null) players.add(playerTwo);
        return players;
    }

    public String getWinnerText() {
        int winnerId = getWinnerId();
        if (winnerId < 0) return null;
        if (winnerId == 0) return "本回合平局";
        return "玩家 " + winnerId + " 赢得本回合";
    }

    public int getWinnerId() {
        int alive = 0;
        Player survivor = null;
        for (Player player : getPlayers()) {
            if (player.isLive()) {
                alive++;
                survivor = player;
            }
        }
        if (alive >= 2) return -1;
        if (alive == 0) return 0;
        return survivor.getId();
    }

    public String getTimedResult() {
        int winnerId = getTimedWinnerId();
        if (winnerId == 0) return "时间结束：双方平局";
        return "时间结束：玩家 " + winnerId + " 赢得本回合";
    }

    public int getTimedWinnerId() {
        int alive = 0;
        Player survivor = null;
        for (Player player : getPlayers()) {
            if (player.isLive()) {
                alive++;
                survivor = player;
            }
        }
        return alive == 1 ? survivor.getId() : 0;
    }

    public void clear() {
        for (List<ElementObj> list : elements.values()) {
            list.clear();
        }
        playerOne = null;
        playerTwo = null;
        suddenDeathCells.clear();
        suddenDeathIndex = 0;
    }
}
