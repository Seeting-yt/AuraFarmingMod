package fr.miguel.client.events;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.platform.TextInputManager;
import fr.miguel.player.CustomPlayer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.apache.logging.log4j.core.pattern.TextRenderer;

import java.awt.*;

public class ClientHUDEvents {


    public static void createAuraBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (true)
            return;
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) return;

        Font textRenderer = client.font; // Récupère le TextRenderer officiel
        String text = String.valueOf(CustomPlayer.getCustomPlayerOrNew(client.player).getPlayerAura());

        // Les dimensions se récupèrent via graphics ou le client selon vos versions exactes
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - 52;

        // Rendu direct et sécurisé
        graphics.text(
                textRenderer,
                Component.literal(text),
                x,
                y,
                0xFFFFFFFF, // Blanc opaque
                true      // true = Active l'ombre portée (Shadow)
        );
    }
}
