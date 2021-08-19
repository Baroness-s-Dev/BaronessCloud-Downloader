package ru.baronessdev.lib.cloud.downloader;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;

public final class BaronessCloudDownloader extends JavaPlugin {

    private static boolean alreadyDownloaded = false;
    private static boolean downloadResult = false;

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public synchronized static boolean call() {
        if (alreadyDownloaded) return downloadResult;
        alreadyDownloaded = true;

        Plugin baronessCloudPlugin = Bukkit.getPluginManager().getPlugin("BaronessCloud");
        if (baronessCloudPlugin != null) {
            Bukkit.getPluginManager().disablePlugin(baronessCloudPlugin);
        }

        File pluginFile = new File("plugins" + File.separator + "BaronessCloud.jar");
        File tempDir = new File("plugins" + File.separator + "BaronessCloud" + File.separator + "temp");
        cleanup(tempDir);
        tempDir.mkdirs();

        if (!pluginFile.exists() && !download(pluginFile)) {
            return downloadResult = false;
        }

        String hash = downloadHash(tempDir);
        if (hash.isEmpty()) {
            return downloadResult = false;
        }

        String currentHash;
        try (InputStream in = new FileInputStream(pluginFile)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }

            currentHash = DatatypeConverter.printHexBinary(digest.digest());
        } catch (Exception e) {
            System.out.println(ChatColor.RED + "Could not calc BaronessCloud hash: " + e.getMessage());
            return downloadResult = false;
        }

        if (!hash.equals(currentHash)) {
            pluginFile.delete();

            if (!download(pluginFile)) {
                return downloadResult = false;
            }
        }

        try {
            Bukkit.getPluginManager().loadPlugin(pluginFile);
        } catch (InvalidPluginException | InvalidDescriptionException e) {
            System.out.println(ChatColor.RED + "Could not load BaronessCloud: " + e.getMessage());
            return downloadResult = false;
        }
        return downloadResult = true;
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
