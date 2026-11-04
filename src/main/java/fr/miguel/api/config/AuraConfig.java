package fr.miguel.api.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class AuraConfig {

    private AuraConfig() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance()
            .getConfigDir().toFile(), "aura_farming_config.json");

    private static Logger CONFIG_LOGGER = LoggerFactory.getLogger("aura_farm_config");
    private static AuraConfig INSTANCE = new AuraConfig();

    public double enchantAuraMultiplier = 5.0;
    public double damageAuraMultiplier = 2.0;

    public double MLGScaleFactor = 0.4222;
    public double MLGExponent = 1.659;

    public int netheriteBonus = 100;
    public int diamondBonus = 50;
    public int ironBonus = 35;
    public int goldBonus = 25;
    public int copperBonus = 15;
    public int stoneBonus = 5;
    public int woodenBonus = 1;

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            INSTANCE.save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            INSTANCE = GSON.fromJson(reader, AuraConfig.class);
        } catch (IOException e) {
            CONFIG_LOGGER.error("Error: ", e);
        }

    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            CONFIG_LOGGER.error("Error: ", e);
        }
    }

    public static AuraConfig getInstance() {
        return INSTANCE;
    }

}
