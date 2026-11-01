package elytramace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("elytramace.json");
    public static ModConfig INSTANCE = new ModConfig();

    public boolean enabled = true;
    // legitunequipelytra: really swaps the elytra for a chestplate
    public boolean legitUnequipElytra = true;
    public boolean rocketRacing = true;
    public boolean autoMace = true;
    public double range = 3.0;
    public double minHeight = 2.0;
    // speardoubletap
    public boolean spearDoubleTap = true;
    public int spearDoubleTapChance = 100; // percent

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                ModConfig c = GSON.fromJson(Files.readString(FILE), ModConfig.class);
                if (c != null) INSTANCE = c;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
                }
