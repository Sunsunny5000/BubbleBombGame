package com.bubblebomb.manager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;

/**
 * 统一读取并缓存图片、文本和地图资源，避免游戏过程中重复访问磁盘。
 */
public final class ResourceManager {
    private static final ResourceManager INSTANCE = new ResourceManager();
    private final Properties imagePaths = new Properties();
    private final Map<String, BufferedImage> imageCache = new HashMap<String, BufferedImage>();

    private ResourceManager() {
        loadImagePaths();
    }

    public static ResourceManager getInstance() {
        return INSTANCE;
    }

    private void loadImagePaths() {
        InputStream input = openStream("resources/config/images.properties");
        if (input == null) {
            return;
        }
        try {
            imagePaths.load(input);
        } catch (IOException e) {
            System.err.println("Unable to load image config: " + e.getMessage());
        } finally {
            try { input.close(); } catch (IOException ignored) { }
        }
    }

    public void preload() {
        for (Object key : imagePaths.keySet()) {
            getImage(key.toString());
        }
    }

    public BufferedImage getImage(String key) {
        // 已加载的图片直接从缓存返回。
        BufferedImage cached = imageCache.get(key);
        if (cached != null) {
            return cached;
        }
        String path = imagePaths.getProperty(key, key);
        BufferedImage image = readImage(path);
        if (image == null) {
            image = createPlaceholder(key);
        }
        imageCache.put(key, image);
        return image;
    }

    private BufferedImage readImage(String path) {
        InputStream input = openStream(path);
        if (input == null) {
            return null;
        }
        try {
            return ImageIO.read(input);
        } catch (IOException e) {
            System.err.println("Unable to load image " + path + ": " + e.getMessage());
            return null;
        } finally {
            try { input.close(); } catch (IOException ignored) { }
        }
    }

    public List<String> readLines(String path) {
        InputStream input = openStream(path);
        if (input == null) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.trim().startsWith(";")) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Unable to load text " + path + ": " + e.getMessage());
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) { }
            }
        }
        return lines;
    }

    private InputStream openStream(String path) {
        // 优先读取类路径资源，失败后尝试项目工作目录中的普通文件。
        String normalized = path.replace('\\', '/');
        InputStream input = ResourceManager.class.getClassLoader().getResourceAsStream(normalized);
        if (input != null) {
            return input;
        }
        File file = new File(path);
        if (!file.isFile()) {
            file = new File(System.getProperty("user.dir"), path);
        }
        if (file.isFile()) {
            try {
                return new FileInputStream(file);
            } catch (IOException ignored) { }
        }
        return null;
    }

    private BufferedImage createPlaceholder(String key) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(255, 0, 255, 180));
        g.fillRect(0, 0, 64, 64);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, 63, 63);
        g.drawString("?", 28, 36);
        g.dispose();
        System.err.println("Missing image resource: " + key);
        return image;
    }
}
