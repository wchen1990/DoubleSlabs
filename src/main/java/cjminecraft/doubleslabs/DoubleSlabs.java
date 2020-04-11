package cjminecraft.doubleslabs;

import cjminecraft.doubleslabs.api.SlabSupport;
import cjminecraft.doubleslabs.patches.DynamicSurroundings;
import cjminecraft.doubleslabs.proxy.IProxy;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(name = DoubleSlabs.NAME, version = DoubleSlabs.VERSION, modid = DoubleSlabs.MODID, acceptedMinecraftVersions = DoubleSlabs.ACCEPTED_MC_VERSIONS, updateJSON = DoubleSlabs.UPDATE_URL)
public class DoubleSlabs
{
    public static final String NAME = "DoubleSlabs";
    public static final String MODID = "doubleslabs";
    public static final String VERSION = "${version}";
    public static final String ACCEPTED_MC_VERSIONS = "[1.12,1.12.2]";
    public static final String SERVER_PROXY_CLASS = "cjminecraft.doubleslabs.proxy.ServerProxy";
    public static final String CLIENT_PROXY_CLASS = "cjminecraft.doubleslabs.proxy.ClientProxy";
    public static final String UPDATE_URL = "https://raw.githubusercontent.com/CJMinecraft01/DoubleSlabs/1.12.x/update.json";
    public static final Logger LOGGER = LogManager.getFormatterLogger(NAME);

    @SidedProxy(serverSide = SERVER_PROXY_CLASS, clientSide = CLIENT_PROXY_CLASS)
    public static IProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
        ConfigManager.sync(MODID, Config.Type.INSTANCE);
        DynamicSurroundings.prepare();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
        SlabSupport.init();
    }

    @Mod.EventHandler
    public void processIMC(FMLInterModComms.IMCEvent event) {
        for (FMLInterModComms.IMCMessage message : event.getMessages()) {
            if (!message.isStringMessage()) continue;

            if (message.key.equalsIgnoreCase("register")) {
                LOGGER.info("Received slab support registration from [{}] for class {}", message.getSender(), message.getStringValue());
                SlabSupport.addSupportFromIMC(message.getStringValue());
            }
        }
    }
}
