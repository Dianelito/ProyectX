package dianelito.antirob;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class AntiRoboPlugin extends JavaPlugin implements Listener {

    private final Map<String, String> playerIPs = new HashMap<>(); // Almacena IPs en memoria

    private File dataFile;

    @Override
    public void onEnable() {
        getLogger().info("AntiRoboPlugin ha sido activado.");
        getServer().getPluginManager().registerEvents(this, this);

        // Crear archivo .yml si no existe
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("No se pudo crear el archivo data.yml: " + e.getMessage());
            }
        }

        // Cargar datos del archivo al iniciar el plugin
        loadPlayerIPs();
    }

    @Override
    public void onDisable() {
        getLogger().info("AntiRoboPlugin ha sido desactivado.");
        savePlayerIPs(); // Guardar datos antes de apagar el plugin
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        String playerIP = event.getAddress().getHostAddress();

        // Verificar si la IP está registrada para este jugador
        if (playerIPs.containsKey(playerName)) {
            String registeredIP = playerIPs.get(playerName);
            if (!registeredIP.equals(playerIP)) {
                // IP no coincide: banear al jugador
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "⚠️ Acceso denegado. IP no coincide.");

                // Ejecutar comando en el hilo principal
                Bukkit.getScheduler().runTask(this, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + playerName + " Seguridad, Abre ticket");
                });

                playerIPs.remove(playerName); // Eliminar su información
                savePlayerIPs(); // Guardar cambios
            }
        } else {
            // Registrar nueva IP si es la primera vez
            playerIPs.put(playerName, playerIP);
            savePlayerIPs();
        }
    }

    private void loadPlayerIPs() {
        try (Scanner scanner = new Scanner(dataFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        String playerName = parts[0];
                        String playerIP = parts[1];
                        playerIPs.put(playerName, playerIP);
                    }
                }
            }
        } catch (IOException e) {
            getLogger().severe("Error al cargar data.yml: " + e.getMessage());
        }
    }

    private void savePlayerIPs() {
        try {
            StringBuilder content = new StringBuilder();
            for (Map.Entry<String, String> entry : playerIPs.entrySet()) {
                content.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
            }
            java.nio.file.Files.write(dataFile.toPath(), content.toString().getBytes());
        } catch (IOException e) {
            getLogger().severe("Error al guardar data.yml: " + e.getMessage());
        }
    }
}