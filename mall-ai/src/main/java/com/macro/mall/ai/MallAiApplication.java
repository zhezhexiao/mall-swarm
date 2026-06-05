package com.macro.mall.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class MallAiApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(MallAiApplication.class, args);
    }

    /**
     * 从模块根目录 .env 文件加载环境变量到 System Properties。
     * 依次尝试: user.dir → class location → 固定路径。
     */
    private static void loadDotenv() {
        Path envFile = findEnvFile();
        if (envFile == null) {
            System.out.println("[dotenv] No .env file found, skipping.");
            return;
        }
        try {
            int count = 0;
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int idx = trimmed.indexOf('=');
                if (idx <= 0) continue;
                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                System.setProperty(key, value);
                count++;
            }
            System.out.println("[dotenv] Loaded " + count + " vars from " + envFile);
        } catch (Exception e) {
            System.err.println("[dotenv] Failed to load .env: " + e.getMessage());
        }
    }

    private static Path findEnvFile() {
        // 1) user.dir (IDE working directory)
        Path candidate = Paths.get(System.getProperty("user.dir"), ".env");
        if (Files.exists(candidate)) return candidate;

        // 2) user.dir/mall-ai/.env (parent project root)
        candidate = Paths.get(System.getProperty("user.dir"), "mall-ai", ".env");
        if (Files.exists(candidate)) return candidate;

        // 3) class file location → walk up to module root
        try {
            var clazz = MallAiApplication.class;
            var url = clazz.getProtectionDomain().getCodeSource().getLocation();
            Path base = Paths.get(url.toURI());
            // target/classes → walk up to module root
            while (base != null && !Files.exists(base.resolve(".env"))) {
                base = base.getParent();
            }
            if (base != null) {
                candidate = base.resolve(".env");
                if (Files.exists(candidate)) return candidate;
            }
        } catch (Exception ignored) {}

        return null;
    }
}
