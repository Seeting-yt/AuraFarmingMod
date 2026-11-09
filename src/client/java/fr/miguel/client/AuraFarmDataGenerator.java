package fr.miguel.client;

import fr.miguel.AuraFarm;
import fr.miguel.items.ModItems;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AuraFarmDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        pack.addProvider(AdvancementProvider::new);
    }


    private static class AdvancementProvider extends FabricAdvancementProvider {


        protected AdvancementProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
            super(output, registryLookup);
        }

        @Override
        public void generateAdvancement(HolderLookup.@NonNull Provider registryLookup,
                                        @NonNull Consumer<AdvancementHolder> consumer) {


            AdvancementHolder rootAdvancement = Advancement.Builder
                            .advancement()
                            .display(
                                    new DisplayInfo(
                                            new ItemStackTemplate(ModItems.DICE_ITEM),
                                            Component.translatable("advancements.aurafarm.root.title"),
                                            Component.translatable("advancements.aurafarm.root.description"),
                                            Optional.of(
                                                    new ClientAsset.ResourceTexture(
                                                            Identifier.withDefaultNamespace(
                                                                    "textures/gui/advancements/backgrounds/adventure.png"))
                                            ),
                                            AdvancementType.TASK,
                                            true, true, false
                                    )
                            )
                            .addCriterion("base", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                            .save(consumer, AuraFarm.id("root"));

            Advancement.Builder.advancement()
                    .display(
                            new DisplayInfo(
                                    new ItemStackTemplate(Items.OBSIDIAN),
                                    Component.translatable("advancements.aurafarm.obsidian.title"),
                                    Component.translatable("advancements.aurafarm.obsidian.description"),
                                    Optional.empty(),
                                    AdvancementType.CHALLENGE,
                                    true,
                                    true,
                                    true
                            )
                    )
                    .parent(rootAdvancement)
                    .addCriterion("manual_triger", CriteriaTriggers.IMPOSSIBLE
                            .createCriterion(new ImpossibleTrigger.TriggerInstance()))
                    .save(consumer, AuraFarm.id("mine_obsidian"));

        }
    }
}
