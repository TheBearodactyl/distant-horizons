package com.seibel.distanthorizons.fabric.mixins.server;

import com.seibel.distanthorizons.common.wrappers.DependencySetupDoneCheck;

import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelTicks.class)
public class MixinLevelTicks<T>
{
	// TODO put in a common location
	private static boolean isWorldGenThread()
	{ return DependencySetupDoneCheck.isDone && DependencySetupDoneCheck.getIsCurrentThreadDistantGeneratorThread.get(); }
	
	
	
	@Inject(method = "schedule", at = @At(value = "HEAD"), cancellable = true)
	private void onChunkSave(ScheduledTick<T> tick, CallbackInfo ci)
	{
		// In MC 1.21.4 an error check was added to log attempting to schedule ticks for unloaded chunks
		// this caused a lot of unnecessary errors when generating sand (FallingBlock.class).
		if (isWorldGenThread())
		{
			ci.cancel();
		}
	}
	
}
