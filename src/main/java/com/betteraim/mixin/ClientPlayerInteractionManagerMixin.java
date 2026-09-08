package com.betteraim.mixin;

import com.betteraim.config.ConfigManager;
import com.betteraim.feedback.HitFeedbackManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts player attack actions to trigger Better AIM hit-feedback visuals.
 *
 * This mixin does NOT modify combat mechanics in any way.
 * It only reads the attack event to fire client-side visual effects.
 *
 * Method target: {@code ClientPlayerInteractionManager#attackEntity(PlayerEntity, Entity)}
 */
@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity",
            at = @At("TAIL"))
    private void betterAim$onAttackEntity(PlayerEntity player,
                                          Entity entity,
                                          CallbackInfo ci) {
        if (!ConfigManager.getConfig().enabled) return;
        if (!ConfigManager.getConfig().hitFeedback.enabled) return;

        if (entity instanceof LivingEntity living && living.isAlive()) {
            HitFeedbackManager.triggerHit(living);
        }
    }
}
