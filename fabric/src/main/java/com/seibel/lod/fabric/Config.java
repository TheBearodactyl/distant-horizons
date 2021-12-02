/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.fabric;

import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IAdvanced.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IGraphics.*;
import com.seibel.lod.core.wrapperInterfaces.config.ILodConfigWrapperSingleton.IClient.IWorldGenerator;
import com.seibel.lod.fabric.wrappers.config.ConfigGui;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * This handles any configuration the user has access to.
 * @author coolGi2007
 * @version 12-02-2021
 */
@me.shedaniel.autoconfig.annotation.Config(name = ModInfo.ID)
public class Config implements ConfigData
//public class Config extends ConfigGui
{
	// CONFIG STRUCTURE
	// 	-> Client
	//		|
	//		|-> Graphics
	//		|		|-> Quality
	//		|		|-> FogQuality
	//		|		|-> AdvancedGraphics
	//		|
	//		|-> World Generation
	//		|
	//		|-> Advanced
	//				|-> Threads
	//				|-> Buffers
	//				|-> Debugging

	// Since the original config system uses forge stuff, that means we have to rewrite the whole config system

	@ConfigEntry.Gui.CollapsibleObject
	public Client client = new Client();

	public static class Client
	{
		@ConfigEntry.Gui.CollapsibleObject
		public Graphics graphics = new Graphics();

		@ConfigEntry.Gui.CollapsibleObject
		public WorldGenerator worldGenerator = new WorldGenerator();

		@ConfigEntry.Gui.CollapsibleObject
		public Advanced advanced = new Advanced();


		public static class Graphics
		{
			@ConfigEntry.Gui.CollapsibleObject
			public Quality quality = new Quality();

			@ConfigEntry.Gui.CollapsibleObject
			public FogQuality fogQuality = new FogQuality();

			@ConfigEntry.Gui.CollapsibleObject
			public AdvancedGraphics advancedGraphicsOption = new AdvancedGraphics();


			public static class Quality
			{
				@ConfigEntry.Category("lod.Graphics.QualityOption")
				@ConfigEntry.Gui.Tooltip
				public static HorizontalResolution drawResolution = IQuality.DRAW_RESOLUTION_DEFAULT;

				@ConfigEntry.Category("lod.Graphics.QualityOption")
				@ConfigEntry.Gui.Tooltip
				@ConfigEntry.BoundedDiscrete(min = 16, max = 1024)
				public static int lodChunkRenderDistance = IQuality.LOD_CHUNK_RENDER_DISTANCE_MIN_DEFAULT_MAX.defaultValue;

				@ConfigEntry.Category("lod.Graphics.QualityOption")
				@ConfigEntry.Gui.Tooltip
				public static VerticalQuality verticalQuality = IQuality.VERTICAL_QUALITY_DEFAULT;

				@ConfigEntry.Category("lod.Graphics.QualityOption")
				@ConfigEntry.Gui.Tooltip
				public static HorizontalScale horizontalScale = IQuality.HORIZONTAL_SCALE_DEFAULT;

				@ConfigEntry.Category("lod.Graphics.QualityOption")
				@ConfigEntry.Gui.Tooltip
				public static HorizontalQuality horizontalQuality = IQuality.HORIZONTAL_QUALITY_DEFAULT;
			}

			public static class FogQuality
			{
				@ConfigEntry.Category("lod.Graphics.FogQualityOption")
				@ConfigEntry.Gui.Tooltip
				public static FogDistance fogDistance = IFogQuality.FOG_DISTANCE_DEFAULT;

				@ConfigEntry.Category("lod.Graphics.FogQualityOption")
				@ConfigEntry.Gui.Tooltip
				public static FogDrawMode fogDrawMode = IFogQuality.FOG_DRAW_MODE_DEFAULT;

				@ConfigEntry.Category("lod.Graphics.FogQualityOption")
				@ConfigEntry.Gui.Tooltip
				public static FogColorMode fogColorMode = IFogQuality.FOG_COLOR_MODE_DEFAULT;

				@ConfigEntry.Category("lod.Graphics.FogQualityOption")
				@ConfigEntry.Gui.Tooltip
				public static boolean disableVanillaFog = IFogQuality.DISABLE_VANILLA_FOG_DEFAULT;
			}

			public static class AdvancedGraphics
			{
				@ConfigEntry.Category("lod.Graphics.AdvancedGraphicsOption")
				@ConfigEntry.Gui.Tooltip
				public static LodTemplate lodTemplate = IAdvancedGraphics.LOD_TEMPLATE_DEFAULT;

				@ConfigEntry.Category("lod.Graphics.AdvancedGraphicsOption")
				@ConfigEntry.Gui.Tooltip
				public static boolean disableDirectionalCulling = IAdvancedGraphics.DISABLE_DIRECTIONAL_CULLING_DEFAULT;

				@ConfigEntry.Category("lod.Graphics.AdvancedGraphicsOption")
				@ConfigEntry.Gui.Tooltip
				public static boolean alwaysDrawAtMaxQuality = IAdvancedGraphics.ALWAYS_DRAW_AT_MAD_QUALITY_DEFAULT;

				@ConfigEntry.Category("lod.Graphics.AdvancedGraphicsOption")
				@ConfigEntry.Gui.Tooltip
				public static VanillaOverdraw vanillaOverdraw = IAdvancedGraphics.VANILLA_OVERDRAW_DEFAULT;

				@ConfigEntry.Category("lod.Graphics.AdvancedGraphicsOption")
				@ConfigEntry.Gui.Tooltip
				public static boolean useExtendedNearClipPlane = IAdvancedGraphics.USE_EXTENDED_NEAR_CLIP_PLANE_DEFAULT;
			}
		}


		//========================//
		// WorldGenerator Configs //
		//========================//
		public static class WorldGenerator
		{
			@ConfigEntry.Category("lod.WorldGenerator")
			@ConfigEntry.Gui.Tooltip
			public static GenerationPriority generationPriority = IWorldGenerator.GENERATION_PRIORITY_DEFAULT;

			@ConfigEntry.Category("lod.WorldGenerator")
			@ConfigEntry.Gui.Tooltip
			public static DistanceGenerationMode distanceGenerationMode = IWorldGenerator.DISTANCE_GENERATION_MODE_DEFAULT;

			@ConfigEntry.Category("lod.WorldGenerator")
			@ConfigEntry.Gui.Tooltip
			public static boolean allowUnstableFeatureGeneration = IWorldGenerator.ALLOW_UNSTABLE_FEATURE_GENERATION_DEFAULT;

			@ConfigEntry.Category("lod.WorldGenerator")
			@ConfigEntry.Gui.Tooltip
			public static BlocksToAvoid blocksToAvoid = IWorldGenerator.BLOCKS_TO_AVOID_DEFAULT;

			/*
			@ConfigEntry.Category("lod.WorldGenerator")
			@ConfigEntry.Gui.Tooltip
			public static boolean useExperimentalPreGenLoading = false;
			 */
		}


		//==================//
		// Advanced Configs //
		//==================//
		public static class Advanced
		{
			@ConfigEntry.Gui.CollapsibleObject
			public Threading threading = new Threading();

			@ConfigEntry.Gui.CollapsibleObject
			public Debugging debugging = new Debugging();

			@ConfigEntry.Gui.CollapsibleObject
			public Buffers buffers = new Buffers();


			public static class Threading
			{
				@ConfigEntry.Category("lod.AdvancedModOptions.Threading")
				@ConfigEntry.Gui.Tooltip
				// Find a way to set the max to a variable
				@ConfigEntry.BoundedDiscrete(min = 1, max = 50)
				public static int numberOfWorldGenerationThreads = IThreading.NUMBER_OF_WORLD_GENERATION_THREADS_DEFAULT.defaultValue;

				@ConfigEntry.Category("lod.AdvancedModOptions.Threading")
				@ConfigEntry.Gui.Tooltip
				// Find a way to set the max to a variable
				@ConfigEntry.BoundedDiscrete(min = 1, max = 50)
				public static int numberOfBufferBuilderThreads = IThreading.NUMBER_OF_BUFFER_BUILDER_THREADS_MIN_DEFAULT_MAX.defaultValue;
			}




			//===============//
			// Debug Options //
			//===============//
			public static class Debugging
			{
				@ConfigEntry.Category("lod.AdvancedModOptions.Debugging")
				@ConfigEntry.Gui.Tooltip
				public static boolean drawLods = IDebugging.DRAW_LODS_DEFAULT;

				@ConfigEntry.Category("lod.AdvancedModOptions.Debugging")
				@ConfigEntry.Gui.Tooltip
				public static DebugMode debugMode = IDebugging.DEBUG_MODE_DEFAULT;

				@ConfigEntry.Category("lod.AdvancedModOptions.Debugging")
				@ConfigEntry.Gui.Tooltip
				public static boolean enableDebugKeybindings = IDebugging.DEBUG_KEYBINDINGS_ENABLED_DEFAULT;
			}


			public static class Buffers
			{
				@ConfigEntry.Category("lod.Graphics.AdvancedGraphicsOption")
				@ConfigEntry.Gui.Tooltip
				public static GpuUploadMethod gpuUploadMethod = IBuffers.GPU_UPLOAD_METHOD_DEFAULT;

				@ConfigEntry.Category("lod.Graphics.AdvancedGraphicsOption")
				@ConfigEntry.Gui.Tooltip
				@ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
				public static int gpuUploadTimeoutInMilleseconds = IBuffers.GPU_UPLOAD_TIMEOUT_IN_MILLISECONDS_DEFAULT.defaultValue;

				@ConfigEntry.Category("lod.AdvancedModOptions.Buffers")
				@ConfigEntry.Gui.Tooltip
				public static BufferRebuildTimes rebuildTimes = IBuffers.REBUILD_TIMES_DEFAULT;
			}
		}
	}
}
