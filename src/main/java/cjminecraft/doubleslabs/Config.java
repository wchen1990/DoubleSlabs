package cjminecraft.doubleslabs;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;

public class Config {

    public static final String CATEGORY_GENERAL = "general";

    private static final ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    public static ForgeConfigSpec SERVER_CONFIG;
    public static ForgeConfigSpec CLIENT_CONFIG;

    // region Server Options
    public static ForgeConfigSpec.ConfigValue<ArrayList<String>> SLAB_BLACKLIST;
    public static ForgeConfigSpec.BooleanValue REPLACE_SAME_SLAB;
    public static ForgeConfigSpec.BooleanValue DISABLE_VERTICAL_SLAB_PLACEMENT;
    public static ForgeConfigSpec.BooleanValue ALTERNATE_VERTICAL_SLAB_PLACEMENT;
    // endregion

    // region Client Options
    public static ForgeConfigSpec.ConfigValue<ArrayList<String>> LAZY_VERTICAL_SLABS;
    // endregion

    static {
        SERVER_BUILDER.comment("General Settings").push(CATEGORY_GENERAL);

        SLAB_BLACKLIST = SERVER_BUILDER.comment("The list of slab types and variants to ignore when creating double slabs", "Example: minecraft:purpur_slab")
                .define("slab_blacklist", new ArrayList<>());
        REPLACE_SAME_SLAB = SERVER_BUILDER.comment("Whether to use the custom double slab when combining slabs of the same type")
                .define("replace_same_slab", true);
        DISABLE_VERTICAL_SLAB_PLACEMENT = SERVER_BUILDER.comment("Whether to disable the placement of vertical slabs from regular horizontal slabs when holding shift")
                .define("disable_vertical_slab_placement", false);
        ALTERNATE_VERTICAL_SLAB_PLACEMENT = SERVER_BUILDER.comment("Whether to use an alternate system when placing vertical slabs")
                .define("alternate_vertical_slab_placement", true);

        SERVER_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();

        CLIENT_BUILDER.comment("General Settings").push(CATEGORY_GENERAL);

        LAZY_VERTICAL_SLABS = CLIENT_BUILDER.comment("The list of slabs (or tags) which should use the lazy model rendering technique", "Lazy model rendering does not physically rotate the original slab model, but applies the same texture to a default vertical slab model", "This often yields better looking results with wooden planks and does not necessarily improve the look of all vertical slabs")
                .define("lazy_vertical_slabs", Lists.newArrayList("#doubleslabs:plank_slabs"));

        CLIENT_BUILDER.pop();

        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }

    public static boolean useLazyModel(Block block) {
        if (block.getRegistryName() == null)
            return false;
        return LAZY_VERTICAL_SLABS.get().stream().anyMatch(entry -> {
            if (entry.startsWith("#")) {
                ResourceLocation tagLocation = new ResourceLocation(entry.substring(1));
                Tag<Block> tag = BlockTags.getCollection().get(tagLocation);
                return tag != null && tag.contains(block);
            }
            return entry.equals(block.getRegistryName().toString());
        });
    }

    public static String slabToString(BlockState state) {
        if (state == null)
            return "null";
        if (state.getBlock().getRegistryName() == null)
            return "";
        return state.getBlock().getRegistryName().toString();
    }

}
