package com.example.fabricopt.mixins;

import com.example.fabricopt.FabricOptMod;
import com.example.fabricopt.MixinBridge;
import com.example.fabricopt.MetricsManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
 
@Mixin(Entity.class)
public abstract class EntityTickMixin {
    @org.spongepowered.asm.mixin.Unique
    private long fabricopt$lastTicked = Long.MIN_VALUE;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        try {
            if (!MixinBridge.isEnabled()) return;
            long serverTick = MixinBridge.getServerTick();
            // Do not skip players
            if ((Object) this instanceof Player) return;
            if (MixinBridge.shouldLogObservation((Entity)(Object)this, serverTick)) {
                FabricOptMod.LOGGER.debug("{} mixin observed entity {} at tick {} throttle={}", FabricOptMod.MOD_ID, ((Object)this).getClass().getSimpleName(), serverTick, MixinBridge.getThrottleInterval());
                return;
            }
            if (MixinBridge.isThrottlingEnabled((Entity)(Object)this)) {
                int throttle = MixinBridge.getThrottleInterval();
                long last = this.fabricopt$lastTicked;
                if (last != Long.MIN_VALUE && serverTick - last < throttle) {
                    com.example.fabricopt.MetricsManager.recordCanceled();
                    ci.cancel();
                    return;
                }
                this.fabricopt$lastTicked = serverTick;
            }
        } catch (Exception e) {
            FabricOptMod.LOGGER.error("{} mixin error while processing entity {} at tick {}", FabricOptMod.MOD_ID, ((Object)this).getClass().getName(), MixinBridge.getServerTick(), e);
        }
    }
}
