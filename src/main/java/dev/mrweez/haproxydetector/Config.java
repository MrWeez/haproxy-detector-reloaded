package dev.mrweez.haproxydetector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

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
                    config.logSuccessfulProxy = (boolean) data.getOrDefault("log-successful-proxy", true);
                    config.logInvalidProxy = (boolean) data.getOrDefault("log-invalid-proxy", true);
                    config.logToSeparateFile = (boolean) data.getOrDefault("log-to-separate-file", false);
                }
            }
        } else {
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

    public void save(Path path) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("log-successful-proxy", logSuccessfulProxy);
        data.put("log-invalid-proxy", logInvalidProxy);
        data.put("log-to-separate-file", logToSeparateFile);

        Yaml yaml = new Yaml();
        try (OutputStream out = Files.newOutputStream(path)) {
            out.write("# HAProxyDetector Reloaded Configuration\n".getBytes());
            out.write("# Whether to log successful proxy connections (IP restoration messages)\n".getBytes());
            yaml.dump(data, new java.io.OutputStreamWriter(out));
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
