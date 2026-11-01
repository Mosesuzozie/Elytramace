package elytramace;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Random;
import java.util.function.Predicate;

public class ElytraMaceClient implements ClientModInitializer {
    private enum Phase { IDLE, LEGIT }

    private Phase phase = Phase.IDLE;
    private int ticks = 0;
    private int regearDelay = -1;
    private boolean rolledThisUse = false;
    private final Random rng = new Random();

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        // auto-save when the game closes
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ModConfig.save());
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) { phase = Phase.IDLE; return; }
        ModConfig c = ModConfig.INSTANCE;
        if (!c.enabled) return;
        tickElytra(mc, p, c);
        tickSpear(mc, p, c);
    }

    // ---------------------------------------------------------------- elytra
    private void tickElytra(Minecraft mc, LocalPlayer p, ModConfig c) {
        if (phase == Phase.IDLE) {
            if (!c.legitUnequipElytra || !p.isFallFlying()) return;
            Player t = findTarget(mc, p, c.range, c.minHeight);
            if (t == null) return;
            if (!swapChest(mc, p, s -> s.is(ItemTags.CHEST_ARMOR))) return;
            phase = Phase.LEGIT;
            ticks = 0;
            regearDelay = -1;
        }

        ticks++;
        if (c.autoMace && ticks >= 1) {
            Player t = findTarget(mc, p, c.range + 0.3, -100);
            if (t != null) maceHit(mc, p, t);
        }

        if (regearDelay >= 0) {
            if (regearDelay-- == 0) finishRegear(mc, p, c);
            return;
        }
        boolean done = p.onGround() || ticks > 40
                || (ticks > 6 && findTarget(mc, p, c.range + 2, -100) == null);
        if (!done) return;

        swapChest(mc, p, s -> s.is(Items.ELYTRA)); // put the elytra back on
        regearDelay = 2;
    }

    private void finishRegear(Minecraft mc, LocalPlayer p, ModConfig c) {
        phase = Phase.IDLE;
        if (p.onGround() || !p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
        startGliding(p);
        if (c.rocketRacing) fireRocket(mc, p);
    }

    private static void startGliding(LocalPlayer p) {
        if (p.tryToStartFallFlying()) {
            p.connection.send(new ServerboundPlayerCommandPacket(p, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
    }

    private void fireRocket(Minecraft mc, LocalPlayer p) {
        int slot = findHotbar(p, s -> s.is(Items.FIREWORK_ROCKET));
        if (slot < 0) return;
        int prev = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
        p.getInventory().setSelectedSlot(prev);
    }

    private boolean swapChest(Minecraft mc, LocalPlayer p, Predicate<ItemStack> want) {
        int src = -1;
        for (int i = 0; i < 36; i++) {
            if (want.test(p.getInventory().getItem(i))) { src = i; break; }
        }
        if (src < 0) return false;
        int id = p.inventoryMenu.containerId;
        final int CHEST_SLOT = 6;
        if (src < 9) {
            mc.gameMode.handleInventoryMouseClick(id, CHEST_SLOT, src, ClickType.SWAP, p);
        } else {
            int sel = p.getInventory().getSelectedSlot();
            mc.gameMode.handleInventoryMouseClick(id, src, sel, ClickType.SWAP, p);
            mc.gameMode.handleInventoryMouseClick(id, CHEST_SLOT, sel, ClickType.SWAP, p);
            mc.gameMode.handleInventoryMouseClick(id, src, sel, ClickType.SWAP, p);
        }
        return true;
    }

    // ---------------------------------------------------------------- spear double tap
    private void tickSpear(Minecraft mc, LocalPlayer p, ModConfig c) {
        boolean using = p.isUsingItem() && p.getMainHandItem().is(ItemTags.SPEARS) && mc.options.keyUse.isDown();
        if (!using) { rolledThisUse = false; return; }
        if (!c.spearDoubleTap || rolledThisUse) return;

        Player target = null;
        double best = 4.5;
        for (Player o : mc.level.players()) {
            if (o == p || o.isSpectator() || !o.isAlive()) continue;
            double d = o.distanceTo(p);
            if (d <= best && o.hurtTime >= 8) { best = d; target = o; }
        }
        if (target == null) return;
        rolledThisUse = true;
        if (rng.nextInt(100) >= c.spearDoubleTapChance) return;
        maceHit(mc, p, target);
    }

    // ---------------------------------------------------------------- helpers
    private void maceHit(Minecraft mc, LocalPlayer p, Player target) {
        int mace = findHotbar(p, s -> s.is(Items.MACE));
        if (mace < 0 || p.distanceTo(target) > 3.4f) return;
        int prev = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(mace);
        mc.gameMode.attack(p, target);
        p.swing(InteractionHand.MAIN_HAND);
        p.getInventory().setSelectedSlot(prev);
    }

    private static int findHotbar(LocalPlayer p, Predicate<ItemStack> test) {
        for (int i = 0; i < 9; i++) if (test.test(p.getInventory().getItem(i))) return i;
        return -1;
    }

    private static Player findTarget(Minecraft mc, LocalPlayer p, double range, double minAbove) {
        Player best = null;
        double bd = Double.MAX_VALUE;
        for (Player o : mc.level.players()) {
            if (o == p || o.isSpectator() || !o.isAlive()) continue;
            double dx = o.getX() - p.getX(), dz = o.getZ() - p.getZ();
            double h = Math.sqrt(dx * dx + dz * dz);
            double above = p.getY() - o.getY();
            if (h > range || above < minAbove) continue;
            double d = p.distanceTo(o);
            if (d < bd) { bd = d; best = o; }
        }
        return best;
    }
}
