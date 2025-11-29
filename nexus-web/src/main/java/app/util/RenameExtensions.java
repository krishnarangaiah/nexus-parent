package app.util;

import java.io.File;
import java.util.Map;

public class RenameExtensions {

    private static final Map<String, String> EXT_MAP = Map.ofEntries(
            Map.entry(".java", ".jav_"),
            Map.entry(".groovy", ".gro_"),
            Map.entry(".html", ".htm_"),
            Map.entry(".class", ".cla_"),
            Map.entry(".jar", ".ja_"),
            Map.entry(".js", ".j_"),
            Map.entry(".css", ".cs_"),
            Map.entry(".xml", ".xm_"),
            Map.entry(".json", ".jsn_"),
            Map.entry(".properties", ".pro_"),
            Map.entry(".txt", ".tx_"),
            Map.entry(".log", ".lo_"),
            Map.entry(".md", ".m_"),
            Map.entry(".yaml", ".ya_"),
            Map.entry(".yml", ".yl_")
    );

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java RenameExtensions <directory> <change|restore>");
            System.exit(1);
        }

        File root = new File(args[0]);
        String mode = args[1].toLowerCase();

        if (!root.exists() || !root.isDirectory()) {
            System.err.println("❌ Directory not found: " + root.getAbsolutePath());
            System.exit(1);
        }

        if (!mode.equals("change") && !mode.equals("restore")) {
            System.err.println("❌ Mode must be 'change' or 'restore'");
            System.exit(1);
        }

        renameFilesRecursively(root, mode);
        System.out.println("\n✅ " + mode.substring(0, 1).toUpperCase() + mode.substring(1) + " complete!");
    }

    private static void renameFilesRecursively(File dir, String mode) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                renameFilesRecursively(file, mode);
                continue;
            }

            EXT_MAP.forEach((original, safe) -> {
                if (mode.equals("change") && file.getName().endsWith(original)) {
                    renameFile(file, original, safe);
                } else if (mode.equals("restore") && file.getName().endsWith(safe)) {
                    renameFile(file, safe, original);
                }
            });
        }
    }

    private static void renameFile(File file, String fromExt, String toExt) {
        String name = file.getName();
        String newName = name.substring(0, name.length() - fromExt.length()) + toExt;
        File newFile = new File(file.getParent(), newName);
        if (file.renameTo(newFile)) {
            System.out.println("Renamed: " + name + " → " + newName);
        } else {
            System.err.println("❌ Failed to rename: " + name);
        }
    }
}
