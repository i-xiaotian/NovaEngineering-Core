package github.kasuminova.novaeng.common.handler;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class SpaceStationCardEventHandler {

    public static final SpaceStationCardEventHandler INSTANCE = new SpaceStationCardEventHandler();
    private static final ResourceLocation SPACE_STATION_CARD_ITEM = new ResourceLocation("minecraft", "paper");
    private static final ResourceLocation STELLAR_ENERGY_CONVERTER_CARD_ITEM = new ResourceLocation("minecraft", "feather");

    private static final String SPACE_STATION_CONTROLLER_NAME = "modularmachinery:space_station";
    private static final String SPACE_MINING_SATELLITE_CONTROLLER_NAME = "modularmachinery:space_mining_satellite";
    private static final String STELLAR_ENERGY_CONVERTER_CONTROLLER_NAME = "modularmachinery:stellar_energy_converter";

    private static final String[] SHIP_KEYS = {"shipOne", "shipTwo", "shipThree", "shipFour"};


    private SpaceStationCardEventHandler() {

    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onSpaceStationCardInteract(PlayerInteractEvent.RightClickBlock event) {

        World world = event.getWorld();
        if (world.isRemote) return;

        final EntityPlayer entityPlayer = event.getEntityPlayer();
        final ItemStack itemStack = entityPlayer.getHeldItemMainhand();
        final Item item = itemStack.getItem();
        if (item == Items.AIR) return;

        final ResourceLocation registryName = item.getRegistryName();
        if (Objects.isNull(registryName) || !registryName.equals(SPACE_STATION_CARD_ITEM)) return;

        final BlockPos pos = event.getPos();
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof final TileMultiblockMachineController ctrl)) return;
        event.setCanceled(true);

        final String formedMachineName = ctrl.getFormedMachineName();
        if (StringUtils.isBlank(formedMachineName)) {
            entityPlayer.sendMessage(new TextComponentString("§c当前右键机械未成型"));
            return;
        }
        if (StringUtils.equals(SPACE_STATION_CONTROLLER_NAME, formedMachineName) ||
                StringUtils.equals(SPACE_MINING_SATELLITE_CONTROLLER_NAME, formedMachineName)) {

            final WorldProvider provider = world.provider;
            final int x = pos.getX();
            final int y = pos.getY();
            final int z = pos.getZ();
            final int dimension = provider.getDimension();

            if (StringUtils.equals(SPACE_STATION_CONTROLLER_NAME, formedMachineName)) {
                final List<String> loreStrings = new ArrayList<>();
                loreStrings.add("§9已存储星畔港主机");
                loreStrings.add(String.format("§9坐标：(§ax: §b%d§a, y: §b%d§a, z: §b%d§9)", x, y, z));
                final NBTTagCompound display = itemStack.getOrCreateSubCompound("display");
                display.setTag("Lore", getLoreNbtList(loreStrings));

                final NBTTagCompound bindInfo = itemStack.getOrCreateSubCompound("bindInfo");
                bindInfo.setTag("spaceStationX", new NBTTagInt(x));
                bindInfo.setTag("spaceStationY", new NBTTagInt(y));
                bindInfo.setTag("spaceStationZ", new NBTTagInt(z));
                bindInfo.setTag("dimensionId", new NBTTagInt(dimension));

                entityPlayer.sendMessage(new TextComponentString("§9已记录星畔港坐标位置至绑定卡"));

            } else if (StringUtils.equals(SPACE_MINING_SATELLITE_CONTROLLER_NAME, formedMachineName)) {
                final NBTTagCompound tagCompound = itemStack.getTagCompound();
                if (Objects.isNull(tagCompound)) {
                    entityPlayer.sendMessage(new TextComponentString("§c未知错误, 请换张卡重试"));
                    return;
                }
                if (!tagCompound.hasKey("bindInfo")) {
                    entityPlayer.sendMessage(new TextComponentString("§c请先绑定星畔港"));
                    return;
                }
                final NBTTagCompound bindInfo = tagCompound.getCompoundTag("bindInfo");
                final int spaceStationX = bindInfo.getInteger("spaceStationX");
                final int spaceStationY = bindInfo.getInteger("spaceStationY");
                final int spaceStationZ = bindInfo.getInteger("spaceStationZ");
                final int dimensionId = bindInfo.getInteger("dimensionId");
                if (dimensionId != dimension) {
                    entityPlayer.sendMessage(new TextComponentString("§c星畔港不在当前世界"));
                    return;
                }

                final NBTTagCompound ctrlTag = ctrl.getCustomDataTag();

                ctrlTag.setTag("stationX", new NBTTagInt(spaceStationX));
                ctrlTag.setTag("stationY", new NBTTagInt(spaceStationY));
                ctrlTag.setTag("stationZ", new NBTTagInt(spaceStationZ));

                final String format = String.format("§9星畔港坐标：(§ax: §b%d§a, y: §b%d§a, z: §b%d§9) 已绑定至此星舟", x, y, z);
                entityPlayer.sendMessage(new TextComponentString(format));
            }

        }
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onStellarEnergyConverterInteract(PlayerInteractEvent.RightClickBlock event) {

        World world = event.getWorld();
        if (world.isRemote) return;

        final EntityPlayer entityPlayer = event.getEntityPlayer();
        final ItemStack itemStack = entityPlayer.getHeldItemMainhand();
        final Item item = itemStack.getItem();
        if (item == Items.AIR) return;

        final ResourceLocation registryName = item.getRegistryName();
        if (Objects.isNull(registryName) || !registryName.equals(STELLAR_ENERGY_CONVERTER_CARD_ITEM)) return;

        final BlockPos pos = event.getPos();
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof final TileMultiblockMachineController ctrl)) return;
        event.setCanceled(true);

        final String formedMachineName = ctrl.getFormedMachineName();
        if (StringUtils.isBlank(formedMachineName)) {
            entityPlayer.sendMessage(new TextComponentString("§c当前右键机械未成型"));
            return;
        }
        if (StringUtils.equals(STELLAR_ENERGY_CONVERTER_CONTROLLER_NAME, formedMachineName) ||
                StringUtils.equals(SPACE_MINING_SATELLITE_CONTROLLER_NAME, formedMachineName)) {

            final WorldProvider provider = world.provider;
            final int x = pos.getX();
            final int y = pos.getY();
            final int z = pos.getZ();
            final int dimension = provider.getDimension();

            if (StringUtils.equals(STELLAR_ENERGY_CONVERTER_CONTROLLER_NAME, formedMachineName)) {
                if (entityPlayer.isSneaking()) {

                    final NBTTagCompound customDataTag = ctrl.getCustomDataTag();
                    Arrays.stream(SHIP_KEYS).forEach(customDataTag::removeTag);
                    entityPlayer.sendMessage(new TextComponentString("§9已清空该星辰物质逆变收集器所有已绑定星舟"));

                } else {
                    final List<String> loreStrings = new ArrayList<>();
                    loreStrings.add("§9已存储星辰物质逆变收集器主机");
                    loreStrings.add(String.format("§9坐标：(§ax: §b%d§a, y: §b%d§a, z: §b%d§9)", x, y, z));
                    final NBTTagCompound display = itemStack.getOrCreateSubCompound("display");
                    display.setTag("Lore", getLoreNbtList(loreStrings));

                    final NBTTagCompound bindInfo = itemStack.getOrCreateSubCompound("bindInfo");
                    bindInfo.setTag("stellarEnergyConverterX", new NBTTagInt(x));
                    bindInfo.setTag("stellarEnergyConverterY", new NBTTagInt(y));
                    bindInfo.setTag("stellarEnergyConverterZ", new NBTTagInt(z));
                    bindInfo.setTag("dimensionId", new NBTTagInt(dimension));

                    entityPlayer.sendMessage(new TextComponentString("§9已记录星辰物质逆变收集器坐标位置至绑定卡"));
                }


            } else if (StringUtils.equals(SPACE_MINING_SATELLITE_CONTROLLER_NAME, formedMachineName)) {
                final NBTTagCompound tagCompound = itemStack.getTagCompound();
                if (Objects.isNull(tagCompound)) {
                    entityPlayer.sendMessage(new TextComponentString("§c请先绑定星辰物质逆变收集器"));
                    return;
                }
                if (!tagCompound.hasKey("bindInfo")) {
                    entityPlayer.sendMessage(new TextComponentString("§c请先绑定星辰物质逆变收集器"));
                    return;
                }
                final NBTTagCompound bindInfo = tagCompound.getCompoundTag("bindInfo");
                final int stellarEnergyConverterX = bindInfo.getInteger("stellarEnergyConverterX");
                final int stellarEnergyConverterY = bindInfo.getInteger("stellarEnergyConverterY");
                final int stellarEnergyConverterZ = bindInfo.getInteger("stellarEnergyConverterZ");
                final int dimensionId = bindInfo.getInteger("dimensionId");
                if (dimensionId != dimension) {
                    entityPlayer.sendMessage(new TextComponentString("§c星辰物质逆变收集器不在当前世界"));
                    return;
                }

                final BlockPos convertPos = new BlockPos(stellarEnergyConverterX, stellarEnergyConverterY, stellarEnergyConverterZ);
                final TileEntity stellarEnergyConverterController = world.getTileEntity(convertPos);
                if (!(stellarEnergyConverterController instanceof final TileMultiblockMachineController convert)) {
                    entityPlayer.sendMessage(new TextComponentString("§c当前绑定卡记录的坐标位置的方块不是一台机械"));
                    return;
                }
                final String convertFormedMachineName = convert.getFormedMachineName();
                if (!StringUtils.equals(STELLAR_ENERGY_CONVERTER_CONTROLLER_NAME, convertFormedMachineName)) {
                    entityPlayer.sendMessage(new TextComponentString("§c当前绑定卡记录的坐标位置的方块不是星辰物质逆变收集器"));
                    return;
                }

                final NBTTagCompound customDataTag = convert.getCustomDataTag();
                final int number = addCoordinatesToEmptyShipList(pos, customDataTag);

                if (number == -1) {
                    final String format = String.format("§c该星舟(x: §b%d, §cy: §b%d, §cz: §b%d§c)已经绑定过星辰物质逆变收集器", x, y, z);
                    entityPlayer.sendMessage(new TextComponentString(format));
                    return;
                }

                final String format = number != 0 ? String.format("§9已绑定该星舟坐标到星辰物质逆变收集器 %d 号位置：(§ax: §b%d§a, y: §b%d§a, z: §b%d§9)", number, x, y, z)
                        : "§c星辰物质逆变收集器星舟已满,如需重新绑定,使用此卡shift右键星辰物质逆变收集器";
                entityPlayer.sendMessage(new TextComponentString(format));
            }
        }
    }

    public int addCoordinatesToEmptyShipList(BlockPos pos, NBTTagCompound customDataTag) {

        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        for (int i = 0; i < SHIP_KEYS.length; i++) {
            String key = SHIP_KEYS[i];
            NBTTagList shipList;

            if (customDataTag.hasKey(key)) {
                shipList = (NBTTagList) customDataTag.getTag(key);
            } else {
                shipList = new NBTTagList();
                customDataTag.setTag(key, shipList);
            }

            if (shipList.tagCount() == 3) {
                int storedX = ((NBTTagInt) shipList.get(0)).getInt();
                int storedY = ((NBTTagInt) shipList.get(1)).getInt();
                int storedZ = ((NBTTagInt) shipList.get(2)).getInt();

                if (storedX == x && storedY == y && storedZ == z) {
                    return -1;
                }
            }

            if (shipList.isEmpty()) {
                shipList.appendTag(new NBTTagInt(x));
                shipList.appendTag(new NBTTagInt(y));
                shipList.appendTag(new NBTTagInt(z));
                return i + 1;
            }
        }

        return 0;
    }


    public static NBTTagList getLoreNbtList(List<String> loreStringList) {
        NBTTagList loreList = new NBTTagList();
        if (loreStringList.isEmpty()) return loreList;
        loreStringList.forEach(lore -> {
            final NBTTagString nbtTagString = new NBTTagString(lore);
            loreList.appendTag(nbtTagString);
        });

        return loreList;
    }

}
