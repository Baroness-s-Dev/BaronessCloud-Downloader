package ru.baronessdev.lib.cloud.downloader;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Optional;

public final class BaronessCloudDownloader {

    private static boolean alreadyDownloaded = false;
    private static boolean downloadResult = false;

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public synchronized static void call(Runnable callback) {
        if (alreadyDownloaded && downloadResult) {
            callback.run();
        }
        alreadyDownloaded = true;

        File pluginFile = new File("plugins" + File.separator + "BaronessCloud.jar");
        File tempDir = new File("plugins" + File.separator + "BaronessCloud" + File.separator + "temp");
        cleanup(tempDir);
        tempDir.mkdirs();

        if (!pluginFile.exists() && !download(pluginFile)) {
            downloadResult = false;
            return;
        }

        String hash = downloadHash(tempDir);
        if (hash.isEmpty()) {
            downloadResult = false;
            return;
        }

        String currentHash;
        try (InputStream in = new FileInputStream(pluginFile)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }

            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder(2 * bytes.length);
            for (byte b : bytes) {
                sb.append("0123456789ABCDEF".charAt((b & 0xF0) >> 4));
                sb.append("0123456789ABCDEF".charAt((b & 0x0F)));
            }

            currentHash = sb.toString().toUpperCase();
        } catch (Exception e) {
            System.out.println(ChatColor.RED + "Could not calc BaronessCloud hash: " + e.getMessage());
            downloadResult = false;
            return;
        }

        cleanup(tempDir);

        if (!hash.equals(currentHash)) {
            pluginFile.delete();

            Optional.ofNullable(Bukkit.getPluginManager().getPlugin("BaronessCloud")).ifPresent(plugin ->
                    Bukkit.getPluginManager().disablePlugin(plugin));

            if (!download(pluginFile)) {
                downloadResult = false;
                return;
            }
        }

        downloadResult = Bukkit.getPluginManager().isPluginEnabled("BaronessCloud");
        if (downloadResult) callback.run();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void cleanup(File tempDir) {
        if (tempDir.exists()) {
            try {
                MoreFiles.deleteRecursively(tempDir.toPath(), RecursiveDeleteOption.ALLOW_INSECURE);
            } catch (IOException ignored) {
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean download(File pluginFile) {
        try {
            Files.copy(new URL("https://github.com/Baroness-s-Dev/BaronessCloud/releases/latest/download/BaronessCloud.jar").openStream(), pluginFile.toPath());
            return true;
        } catch (Exception e) {
            System.out.println(ChatColor.RED + "Could not download BaronessCloud: " + e.getMessage());
            return false;
        }
    }

    private static String downloadHash(File tempDir) {
        try {
            File hashFile = new File(tempDir.getAbsolutePath() + File.separator + "BaronessCloud.sha256");
            Files.copy(new URL("https://github.com/Baroness-s-Dev/BaronessCloud/releases/latest/download/BaronessCloud.sha256").openStream(),
                    hashFile.toPath());

            BufferedReader br = new BufferedReader(new FileReader(hashFile));
            String hash = br.readLine().toUpperCase();
            br.close();
            return hash;
        } catch (Exception e) {
            System.out.println(ChatColor.RED + "Could not download BaronessCloud hash: " + e.getMessage());
            return "";
        }
    }
}
