package dev.mrweez.haproxydetector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class Config {
    private boolean logSuccessfulProxy = true;
    private boolean logInvalidProxy = true;
    private boolean logToSeparateFile = false;
    
    private static Config instance;
    private Logger separateLogger;

    public static Config getInstance() {
        return instance;
    }

    public static Config load(Path path) throws IOException {
        Config config = new Config();
        if (Files.exists(path)) {
            Yaml yaml = new Yaml();
            try (InputStream in = Files.newInputStream(path)) {
                Map<String, Object> data = yaml.load(in);
                if (data != null) {
                    config.logSuccessfulProxy = getBoolean(data, "log-successful-proxy", true);
                    config.logInvalidProxy = getBoolean(data, "log-invalid-proxy", true);
                    config.logToSeparateFile = getBoolean(data, "log-to-separate-file", false);
                }
            }
        } else {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            config.save(path);
        }
        
        if (config.logToSeparateFile) {
            Path logPath = path.getParent().resolve("proxy_connections.log");
            config.separateLogger = Logger.getLogger("HAProxyDetectorSeparate");
            config.separateLogger.setUseParentHandlers(false);
            FileHandler fh = new FileHandler(logPath.toString(), true);
            fh.setFormatter(new SimpleFormatter());
            config.separateLogger.addHandler(fh);
        }
        
        instance = config;
        return config;
    }

    private static boolean getBoolean(Map<String, Object> data, String key, boolean defaultValue) {
        Object val = data.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return defaultValue;
    }

    public void save(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("log-successful-proxy", logSuccessfulProxy);
        data.put("log-invalid-proxy", logInvalidProxy);
        data.put("log-to-separate-file", logToSeparateFile);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);

        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8)) {
            writer.write("# HAProxyDetector Reloaded Configuration\n");
            writer.write("# log-successful-proxy: Whether to log successful proxy connections (IP restoration messages)\n");
            writer.write("# log-invalid-proxy: Whether to log connections from proxies not in the whitelist\n");
            writer.write("# log-to-separate-file: Whether to log proxy information to proxy_connections.log instead of console\n");
            yaml.dump(data, writer);
        }
    }

    public void log(Logger mainLogger, Level level, String message) {
        if (separateLogger != null) {
            separateLogger.log(level, message);
        } else if (mainLogger != null) {
            mainLogger.log(level, message);
        }
    }

    public boolean isLogSuccessfulProxy() {
        return logSuccessfulProxy;
    }

    public boolean isLogInvalidProxy() {
        return logInvalidProxy;
    }

    public boolean isLogToSeparateFile() {
        return logToSeparateFile;
    }
}
