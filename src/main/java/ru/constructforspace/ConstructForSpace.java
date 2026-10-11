package ru.constructforspace;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;
import ru.constructforspace.network.PacketDuplicate;
import ru.constructforspace.network.PacketDuplicateResult;
import ru.constructforspace.network.PacketGive;
import ru.constructforspace.network.PacketGiveResult;
import ru.constructforspace.network.PacketRegenerationRequest;
import ru.constructforspace.network.PacketRegenerationStatus;
import ru.constructforspace.server.DuplicateServerQueue;
import ru.constructforspace.server.GiveServerQueue;
import ru.constructforspace.server.RegenerationController;

@Mod(modid = ConstructForSpace.MOD_ID, name = ConstructForSpace.MOD_NAME, version = ConstructForSpace.VERSION)
public class ConstructForSpace {
    public static final String MOD_ID = "constructforspace";
    public static final String MOD_NAME = "construct for space";
    public static final String VERSION = "1.3.0";

    public static SimpleNetworkWrapper network;

    @SidedProxy(
        clientSide = "ru.constructforspace.client.ClientProxy",
        serverSide = "ru.constructforspace.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID);
        network.registerMessage(PacketDuplicate.Handler.class, PacketDuplicate.class, 0, Side.SERVER);
        network.registerMessage(PacketDuplicateResult.Handler.class, PacketDuplicateResult.class, 1, Side.CLIENT);
        network.registerMessage(PacketGive.Handler.class, PacketGive.class, 2, Side.SERVER);
        network.registerMessage(PacketGiveResult.Handler.class, PacketGiveResult.class, 3, Side.CLIENT);
        network.registerMessage(PacketRegenerationRequest.Handler.class, PacketRegenerationRequest.class, 4, Side.SERVER);
        network.registerMessage(PacketRegenerationStatus.Handler.class, PacketRegenerationStatus.class, 5, Side.CLIENT);
        FMLCommonHandler.instance().bus().register(new DuplicateServerQueue());
        FMLCommonHandler.instance().bus().register(new GiveServerQueue());
        RegenerationController regeneration = new RegenerationController();
        FMLCommonHandler.instance().bus().register(regeneration);
        MinecraftForge.EVENT_BUS.register(regeneration);
        proxy.init();
    }
}
