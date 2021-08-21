package ru.baronessdev.lib.cloud.downloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.*;
import java.net.HttpURLConnection;
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

        Class<?> cloudClass;
        try {
            cloudClass = Class.forName("ru.baronessdev.lib.cloud.BaronessCloud");
            pluginFile = new File(cloudClass.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath());
        } catch (ClassNotFoundException ignored) {
        }

        if (!pluginFile.exists() && !download(pluginFile)) {
            downloadResult = false;
            return;
        }

        String hash = getHash();
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

    private static String getHash() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://github.com/Baroness-s-Dev/BaronessCloud/releases/latest/download/BaronessCloud.sha256").openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String hash = br.readLine().toUpperCase();
            br.close();
            connection.disconnect();
            return hash;
        } catch (Exception e) {
            System.out.println(ChatColor.RED + "Could not download BaronessCloud hash: " + e.getMessage());
            return "";
        }
    }
}
