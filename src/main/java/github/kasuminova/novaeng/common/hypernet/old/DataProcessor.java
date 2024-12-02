package github.kasuminova.novaeng.common.hypernet.old;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeTickEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.mmce.common.helper.IDynamicPatternInfo;
import github.kasuminova.mmce.common.util.concurrent.Queues;
import github.kasuminova.novaeng.common.handler.HyperNetEventHandler;
import github.kasuminova.novaeng.common.hypernet.old.upgrade.ProcessorModuleCPU;
import github.kasuminova.novaeng.common.hypernet.old.upgrade.ProcessorModuleRAM;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenSetter;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ZenRegister
@ZenClass("novaeng.hypernet.DataProcessor")
public class DataProcessor extends NetNode {
    private final Lock lock = new ReentrantLock();

    private final Queue<Long> recentEnergyUsage = Queues.createConcurrentQueue();
    private final Queue<Double> recentCalculation = Queues.createConcurrentQueue();

    private final DataProcessorType type;
    private final LinkedList<Double> computationalLoadHistory = new LinkedList<>();

    private final List<ProcessorModuleCPU> moduleCPUS = new CopyOnWriteArrayList<>();
    private final List<ProcessorModuleRAM> moduleRAMS = new CopyOnWriteArrayList<>();

    private volatile int dynamicPatternSize = 0;

    private volatile double maxGeneration = 0;
    private final AtomicReference<Double> generated = new AtomicReference<>(0D);

    private int storedHU = 0;
    private boolean overheat = false;
    private double computationalLoadHistoryCache = 0;
    private double computationalLoad = 0;

    public DataProcessor(final TileMultiblockMachineController owner) {
        super(owner);
        this.type = RegistryHyperNet.getDataProcessorType(
                Objects.requireNonNull(owner.getFoundMachine()).getRegistryName().getPath()
        );
    }

    @ZenMethod
    public void onRecipeCheck(RecipeCheckEvent event) {
        if (centerPos == null || center == null) {
            event.setFailed("未连接至星辰网络！");
            return;
        }

        if (overheat) {
            event.setFailed("处理器过热！");
            return;
        }

        if (moduleCPUS.isEmpty() && moduleRAMS.isEmpty()) {
            event.setFailed("未找到处理器和内存模块！");
            return;
        }

        if (moduleCPUS.isEmpty()) {
            event.setFailed("至少需要安装一个 CPU 或 GPU 模块！");
            return;
        }

        if (moduleRAMS.isEmpty()) {
            event.setFailed("至少需要安装一个内存模块！");
        }
    }

    @ZenMethod
    public void heatDistributionRecipeCheck(RecipeCheckEvent event, int heatDistribution) {
        if (storedHU - heatDistribution < 0) {
            event.setFailed("novaeng.hypernet.craftcheck.heat_distribution.failed");
        }
    }

    @ZenMethod
    public void onWorkingTick(FactoryRecipeTickEvent event) {
        event.getActiveRecipe().setTick(0);
        if (workingCheck(event)) {
            return;
        }

        long baseEnergyUsage = type.getEnergyUsage();
        long energyUsage = 0;

        Long usage;
        while ((usage = recentEnergyUsage.poll()) != null) {
            energyUsage += usage;
        }

        float heatPercent = getOverHeatPercent();
        if (heatPercent <= 0.1F) {
            energyUsage += (baseEnergyUsage / 10) * dynamicPatternSize > 0 ? dynamicPatternSize : 1;
        } else if (heatPercent <= 0.5F) {
            energyUsage += (baseEnergyUsage / 5) * dynamicPatternSize > 0 ? dynamicPatternSize : 1;
        } else if (heatPercent <= 0.75F) {
            energyUsage += (baseEnergyUsage / 2) * dynamicPatternSize > 0 ? dynamicPatternSize : 1;
        } else {
            energyUsage += baseEnergyUsage * dynamicPatternSize > 0 ? dynamicPatternSize : 1;
        }

        float mul = (float) ((double) (energyUsage + baseEnergyUsage) / baseEnergyUsage);
        event.getRecipeThread().addModifier("energy", new RecipeModifier(
                RequirementTypesMM.REQUIREMENT_ENERGY,
                IOType.INPUT, mul, 1, false
        ));
    }

    protected boolean workingCheck(final FactoryRecipeTickEvent event) {
        if (centerPos == null) {
            event.setFailed(true, "未连接至星辰网络！");
            return true;
        }
        if (center == null) {
            event.preventProgressing("未连接至星辰网络！");
            return true;
        }
        if (overheat) {
            event.setFailed(true, "处理器过热！");
            return true;
        }
        if (moduleCPUS.isEmpty() && moduleRAMS.isEmpty()) {
            event.setFailed(true, "未找到处理器和内存模块！");
            return true;
        }
        if (moduleCPUS.isEmpty()) {
            event.setFailed(true, "至少需要安装一个 CPU 或 GPU 模块！");
            return true;
        }
        if (moduleRAMS.isEmpty()) {
            event.setFailed(true, "至少需要安装一个内存模块！");
            return true;
        }
        return false;
    }

    @ZenMethod
    public void onMachineTick() {
        super.onMachineTick();

        if (!isWorking()) {
            generated.set(0D);
            computationalLoad = 0F;
            computationalLoadHistoryCache = 0F;
            computationalLoadHistory.clear();
        } else {
            double totalCalculation = 0F;
            Double calculation;
            while ((calculation = recentCalculation.poll()) != null) {
                totalCalculation += calculation;
            }

            computationalLoadHistory.addFirst(totalCalculation);
            computationalLoadHistoryCache += totalCalculation;
            if (computationalLoadHistory.size() > 100) {
                computationalLoadHistoryCache -= computationalLoadHistory.pollLast();
            }

            computationalLoad = computationalLoadHistoryCache / computationalLoadHistory.size();
            HyperNetEventHandler.addTickStartAction(() -> generated.set(maxGeneration));
        }

        if (owner.getTicksExisted() % 20 == 0) {
            maxGeneration = getComputationPointProvision(0xFFFFFF);
            IDynamicPatternInfo dynamicPattern = owner.getDynamicPattern(type.getDynamicPatternName());
            if (dynamicPattern != null) {
                dynamicPatternSize = dynamicPattern.getSize();
            } else {
                dynamicPatternSize = 0;
            }
            writeNBT();
        }

        if (storedHU > 0) {
            int heatDist = calculateHeatDist();

            storedHU -= Math.min(heatDist, storedHU);
            if (storedHU <= 0) {
                overheat = false;
            }
            maxGeneration = getComputationPointProvision(0xFFFFFF);
            writeNBT();
        }
    }

    @ZenMethod
    public void onStructureUpdate() {
        try {
            lock.lock();
            moduleCPUS.clear();
            moduleRAMS.clear();
            moduleCPUS.addAll(ProcessorModuleCPU.filter(owner.getFoundUpgrades().values()));
            moduleRAMS.addAll(ProcessorModuleRAM.filter(owner.getFoundUpgrades().values()));
        } finally {
            lock.unlock();
        }
    }

    private int calculateHeatDist() {
        float heatPercent = getOverHeatPercent();
        float heatDist = type.getHeatDistribution();
        if (dynamicPatternSize > 1) {
            heatDist *= dynamicPatternSize;
        }

        if (heatPercent <= 0.25F) {
            heatDist *= 0.25F;
        } else if (heatPercent <= 0.75F) {
            heatDist *= 0.25F + (heatPercent);
        } else {
            heatDist *= 1.0F;
        }

        return (int) heatDist;
    }

    @Override
    public double requireComputationPoint(final double maxGeneration, final boolean doCalculate) {
        if (!isConnected() || center == null || !isWorking()) {
            return 0F;
        }

        double[] polledCounter = {0};
        this.generated.updateAndGet(generated -> {
            if (generated < maxGeneration) {
                polledCounter[0] = generated;
                return 0D;
            }

            polledCounter[0] = maxGeneration;
            return generated - maxGeneration;
        });

        if (polledCounter[0] <= 0) {
            return 0F;
        }

        double maxCanGenerated = polledCounter[0];
        double generated = calculateComputationPointProvision(maxCanGenerated, doCalculate) * getEfficiency();

        if (doCalculate) {
            doHeatGeneration(generated);
            this.generated.updateAndGet(counter -> counter + (maxCanGenerated - generated));
        }

        return generated;
    }

    @Override
    public boolean isWorking() {
        if (!(owner instanceof final TileFactoryController factory)) {
            return false;
        }

        FactoryRecipeThread thread = factory.getCoreRecipeThreads().get(DataProcessorType.PROCESSOR_WORKING_THREAD_NAME);

        return owner.isWorking() && thread != null && thread.isWorking();
    }

    @ZenGetter("maxGeneration")
    public double getMaxGeneration() {
        return maxGeneration;
    }

    public float getEfficiency() {
        float overHeatPercent = getOverHeatPercent();
        return overHeatPercent >= 0.85F ? (1.0F - overHeatPercent) / 0.15F : 1F;
    }

    @ZenGetter("overHeatPercent")
    public float getOverHeatPercent() {
        return overheat ? 1F : (float) storedHU / type.getOverheatThreshold();
    }

    public void doHeatGeneration(double computationPointGeneration) {
        storedHU += (int) (computationPointGeneration * 2);
        if (storedHU >= type.getOverheatThreshold()) {
            overheat = true;
        }
    }

    public double calculateComputationPointProvision(double maxGeneration, boolean doCalculate) {
        if (overheat || !isWorking()) {
            return 0;
        }

        if (owner.getFoundUpgrades().isEmpty()) {
            return 0;
        }

        if (moduleCPUS.isEmpty()) {
            return 0;
        }

        if (moduleRAMS.isEmpty()) {
            return 0;
        }

        long totalEnergyConsumption = 0;

        double generationLimit = 0F;
        double totalGenerated = 0F;

        for (ProcessorModuleRAM ram : moduleRAMS) {
            double generated = ram.calculate(doCalculate, maxGeneration - generationLimit);
            generationLimit += generated;
            if (doCalculate) {
                totalEnergyConsumption += (long) ((generated / ram.getComputationPointGenerationLimit()) * ram.getEnergyConsumption());
            }
        }
        for (final ProcessorModuleCPU cpu : moduleCPUS) {
            double generated = cpu.calculate(doCalculate, generationLimit - totalGenerated);
            totalGenerated += generated;
            if (doCalculate) {
                totalEnergyConsumption += (long) ((generated / cpu.getComputationPointGeneration()) * cpu.getEnergyConsumption());
            }

            if (totalGenerated >= generationLimit) {
                break;
            }
        }

        if (doCalculate) {
            recentCalculation.offer(totalGenerated);
            recentEnergyUsage.offer(totalEnergyConsumption);
        }

        return totalGenerated;
    }

    @Override
    public void readNBT(final NBTTagCompound customData) {
        super.readNBT(customData);
        this.storedHU = customData.getInteger("storedHU");
        if (customData.hasKey("overheat")) {
            this.overheat = customData.getBoolean("overheat");
        }

        this.computationalLoad = customData.getFloat("computationalLoad");
        this.maxGeneration = customData.getFloat("maxGeneration");
    }

    @Override
    public void writeNBT() {
        super.writeNBT();
        NBTTagCompound tag = owner.getCustomDataTag();
        tag.setInteger("storedHU", storedHU);
        tag.setBoolean("overheat", overheat);
        tag.setDouble("computationalLoad", computationalLoad);
        tag.setDouble("maxGeneration", maxGeneration);
    }

    @Override
    public double getComputationPointProvision(final double maxGeneration) {
        return calculateComputationPointProvision(maxGeneration, false) * getEfficiency();
    }

    @ZenGetter("computationalLoad")
    public double getComputationalLoad() {
        return computationalLoad;
    }

    @ZenGetter("type")
    public DataProcessorType getType() {
        return type;
    }

    @ZenGetter("storedHU")
    public int getStoredHU() {
        return storedHU;
    }

    @ZenSetter("storedHU")
    public void setStoredHU(final int storedHU) {
        this.storedHU = storedHU;
        writeNBT();
    }

    @ZenGetter("overheat")
    public boolean isOverheat() {
        return overheat;
    }
}
