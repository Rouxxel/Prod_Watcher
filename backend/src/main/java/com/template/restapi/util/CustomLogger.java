/**
 * #############################################################################
 * ### Custom logger file
 * ###
 * ### @file CustomLogger.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Thread-safe custom logger. Writes to both the console and a timestamped
 * log file, with daily rotation and configurable level filtering.
 *
 * Use CustomLogger.info(...) / debug(...) / etc. everywhere instead of
 * System.out.println. It is initialized once in Application.main() from
 * config_file.json's "logging" section.
 *
 * No external dependencies (pure JDK). Adapted from java_various_utils/CustomLogger.java.
 */
package com.template.restapi.util;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class CustomLogger {

    // Custom level mapping
    public enum LogLevel {
        DEBUG(0),
        INFO(1),
        WARNING(2),
        ERROR(3),
        CRITICAL(4);

        final int rank;
        LogLevel(int rank) { this.rank = rank; }
    }

    // Internal state for log file management
    private static final Object LOCK = new Object();
    private static Writer fileWriter;
    private static boolean isInitialized;
    private static String currentLogDate;
    private static String currentLogFile;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter LINE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Default configuration values - overridden in setup() from config_file.json
    public static String logDirectory = "logs";
    public static String logFileName = "x_bcknd";
    public static LogLevel minimumLogLevel = LogLevel.DEBUG;

    private CustomLogger() { }

    public static void setup() {
        setup(null, null, LogLevel.DEBUG);
    }

    public static void setup(String logDir, String logFile, LogLevel minLogLevel) {
        synchronized (LOCK) {
            try {
                if (logDir != null && !logDir.isEmpty()) logDirectory = logDir;
                if (logFile != null && !logFile.isEmpty()) logFileName = logFile;
                minimumLogLevel = minLogLevel;

                Files.createDirectories(Paths.get(logDirectory));

                LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
                currentLogDate = nowUtc.format(DATE_FMT);
                String timestamp = nowUtc.format(STAMP_FMT);
                currentLogFile = Paths.get(logDirectory, logFileName + "_" + timestamp + ".log").toString();

                openLogFile(currentLogFile);
                isInitialized = true;

                info("Logger initialized");
                warning("Current working directory: " + System.getProperty("user.dir")
                        + ", Logs written to '" + logDirectory + "'");
            } catch (Exception ex) {
                System.err.println("Failed to initialize logger: " + ex.getMessage());
                isInitialized = false;
            }
        }
    }

    private static void openLogFile(String filePath) {
        try {
            if (filePath == null || filePath.trim().isEmpty())
                throw new IllegalArgumentException("Invalid file path");

            Path baseDir = Paths.get(logDirectory).toAbsolutePath().normalize();
            Files.createDirectories(baseDir);

            Path fullPath = Paths.get(filePath).toAbsolutePath().normalize();

            if (!fullPath.startsWith(baseDir))
                throw new IllegalArgumentException("Path traversal detected");

            if (fileWriter != null) {
                try { fileWriter.close(); } catch (IOException ignored) { }
            }

            fileWriter = Files.newBufferedWriter(fullPath, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            System.err.println("Failed to open log file '" + filePath + "': " + ex.getMessage());
            fileWriter = null;
        }
    }

    private static void checkRotation() {
        // Daily log rotation when the UTC date changes
        String todayDate = LocalDate.now(ZoneOffset.UTC).format(DATE_FMT);
        if (!todayDate.equals(currentLogDate)) {
            String timestamp = LocalDateTime.now(ZoneOffset.UTC).format(STAMP_FMT);
            String newLogFile = Paths.get(logDirectory, logFileName + "_" + timestamp + ".log").toString();
            openLogFile(newLogFile);
            currentLogDate = todayDate;
            currentLogFile = newLogFile;
        }
    }

    private static void write(LogLevel level, String message) {
        if (level.rank < minimumLogLevel.rank)
            return;

        if (!isInitialized) {
            System.err.println("WARNING: Logger not initialized. Call setup() before logging. Message: " + message);
            return;
        }

        try {
            synchronized (LOCK) {
                checkRotation();

                String ts = LocalDateTime.now(ZoneOffset.UTC).format(LINE_FMT);
                String line = "[" + ts + "] [" + level.name() + "] " + message;

                System.out.println(line);

                if (fileWriter != null) {
                    fileWriter.write(line);
                    fileWriter.write(System.lineSeparator());
                    fileWriter.flush();
                }
            }
        } catch (Exception ex) {
            System.err.println("Error writing to log: " + ex.getMessage());
            System.err.println("Original message: " + message);
        }
    }

    public static void debug(String message)    { write(LogLevel.DEBUG, message); }
    public static void info(String message)      { write(LogLevel.INFO, message); }
    public static void warning(String message)   { write(LogLevel.WARNING, message); }
    public static void error(String message)     { write(LogLevel.ERROR, message); }
    public static void critical(String message)  { write(LogLevel.CRITICAL, message); }

    // Properly closes and flushes the file writer. Call before application shutdown.
    public static void shutdown() {
        synchronized (LOCK) {
            try {
                if (fileWriter != null) {
                    info("Logger shutting down");
                    fileWriter.flush();
                    fileWriter.close();
                    fileWriter = null;
                }
                isInitialized = false;
            } catch (Exception ex) {
                System.err.println("Error during logger shutdown: " + ex.getMessage());
            }
        }
    }
}
