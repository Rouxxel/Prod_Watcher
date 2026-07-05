/**
 * #############################################################################
 * ### Logs / build-artifact deleter
 * ###
 * ### @file LogsDeleter.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Dev utility that recursively deletes throwaway folders (logs, build output,
 * etc.) confined to the project root.
 *
 * HOW IT WORKS
 *   1. Climbs up from the working directory until it finds a folder named
 *      ROOT_FOLDER (case-insensitive) -- this keeps cleanup inside the project.
 *   2. From that root, walks downward and deletes any directory whose name
 *      matches FOLDERS_TO_REMOVE (deepest first).
 *
 * IMPORTANT: set ROOT_FOLDER to YOUR project's root folder name before running.
 *
 * No external dependencies (pure JDK). Adapted from java_various_utils/LogsDeleter.java.
 */
package com.template.restapi.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LogsDeleter {

    // SET THIS to your project's root folder name (case-insensitive).
    public static final String ROOT_FOLDER = "java-rest-api-template";
    public static final String[] FOLDERS_TO_REMOVE =
            { "logs", "build", "out", "bin", ".gradle", "dist" };

    private LogsDeleter() { }

    public static Path findProjectRootByName(String startPath, String targetName) {
        Path current = Paths.get(startPath).toAbsolutePath().normalize();
        if (Files.isRegularFile(current)) {
            current = current.getParent();
        }

        String targetLower = targetName.trim().toLowerCase();
        while (current != null) {
            Path name = current.getFileName();
            if (name != null && name.toString().toLowerCase().equals(targetLower)) {
                return current;
            }
            Path parent = current.getParent();
            if (parent == null || parent.equals(current)) {
                break;
            }
            current = parent;
        }

        throw new RuntimeException("Could not find any parent directory named '"
                + targetName + "' starting from '"
                + Paths.get(startPath).toAbsolutePath().normalize() + "'.");
    }

    public static void removeFolders(Path rootDir, String[] foldersToRemove) {
        if (!Files.exists(rootDir)) {
            throw new RuntimeException("The specified root directory does not exist: " + rootDir);
        }

        List<String> targets = (foldersToRemove == null || foldersToRemove.length == 0)
                ? Arrays.asList("logs", "build")
                : Arrays.asList(foldersToRemove);

        List<Path> matches;
        try (Stream<Path> walk = Files.walk(rootDir)) {
            matches = walk
                    .filter(Files::isDirectory)
                    .filter(p -> {
                        Path name = p.getFileName();
                        return name != null && targets.stream()
                                .anyMatch(t -> t.equalsIgnoreCase(name.toString()));
                    })
                    .sorted(Comparator.comparingInt(LogsDeleter::depth).reversed())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println("Failed to scan " + rootDir + ": " + e.getMessage());
            return;
        }

        for (Path dir : matches) {
            try {
                deleteRecursively(dir);
                System.out.println("Deleted: " + dir);
            } catch (IOException e) {
                System.out.println("Failed to delete " + dir + ": " + e.getMessage());
            }
        }
    }

    private static int depth(Path p) {
        return p.getNameCount();
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) throw (IOException) e.getCause();
            throw e;
        }
    }

    public static void main(String[] args) {
        if (ROOT_FOLDER == null || ROOT_FOLDER.isEmpty()) {
            System.out.println("Error: ROOT_FOLDER is not set! Open this file and specify "
                    + "your project's root folder name in the 'ROOT_FOLDER' constant.");
            return;
        }

        String startPath = System.getProperty("user.dir");
        Path actualRoot = findProjectRootByName(startPath, ROOT_FOLDER);
        System.out.println("Project root found at: " + actualRoot);
        removeFolders(actualRoot, FOLDERS_TO_REMOVE);
    }
}
