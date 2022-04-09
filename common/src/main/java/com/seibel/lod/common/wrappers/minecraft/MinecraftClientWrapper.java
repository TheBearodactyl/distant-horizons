/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

package com.seibel.lod.common.wrappers.minecraft;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.seibel.lod.core.ModInfo;
import com.seibel.lod.core.api.ApiShared;
import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.misc.ILightMapWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;
import com.seibel.lod.common.wrappers.McObjectConverter;
import com.seibel.lod.common.wrappers.block.BlockPosWrapper;
import com.seibel.lod.common.wrappers.chunk.ChunkPosWrapper;
import com.seibel.lod.common.wrappers.misc.LightMapWrapper;
import com.seibel.lod.common.wrappers.world.DimensionTypeWrapper;
import com.seibel.lod.common.wrappers.world.WorldWrapper;

import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

/**
 * A singleton that wraps the Minecraft object.
 *
 * @author James Seibel
 * @version 3-5-2022
 */
public class MinecraftClientWrapper implements IMinecraftClientWrapper
{
    public static final MinecraftClientWrapper INSTANCE = new MinecraftClientWrapper();

    public final Minecraft mc = Minecraft.getInstance();

    /**
     * The lightmap for the current:
     * Time, dimension, brightness setting, etc.
     */
    private NativeImage lightMap = null;

    private ProfilerWrapper profilerWrapper;


    private MinecraftClientWrapper()
    {

    }



    //================//
    // helper methods //
    //================//

    /**
     * This should be called at the beginning of every frame to
     * clear any Minecraft data that becomes out of date after a frame. <br> <br>
     * <p>
     * LightMaps and other time sensitive objects fall in this category. <br> <br>
     * <p>
     * This doesn't affect OpenGL objects in any way.
     */
    @Override
    public void clearFrameObjectCache()
    {
        lightMap = null;
    }



    //=================//
    // method wrappers //
    //=================//

    @Override
    public float getShade(LodDirection lodDirection) {
        if (mc.level != null)
        {
            Direction mcDir = McObjectConverter.Convert(lodDirection);
            return mc.level.getShade(mcDir, true);
        }
        else return 0.0f;
    }

    @Override
    public boolean hasSinglePlayerServer()
    {
        return mc.hasSingleplayerServer();
    }

    @Override
    public String getCurrentServerName()
    {
        return mc.getCurrentServer().name;
    }

    @Override
    public String getCurrentServerIp()
    {
        return mc.getCurrentServer().ip;
    }

    @Override
    public String getCurrentServerVersion()
    {
        return mc.getCurrentServer().version.getString();
    }

    /** Returns the dimension the player is currently in */
    @Override
    public IDimensionTypeWrapper getCurrentDimension()
    {
        if (mc.player != null)
            return DimensionTypeWrapper.getDimensionTypeWrapper(mc.player.level.dimensionType());
        else return null;
    }

    @Override
    public String getCurrentDimensionId()
    {
        return LodUtil.getDimensionIDFromWorld(WorldWrapper.getWorldWrapper(mc.level));
    }

    //=============//
    // Simple gets //
    //=============//

    public LocalPlayer getPlayer()
    {
        return mc.player;
    }

    @Override
    public boolean playerExists()
    {
        return mc.player != null;
    }

    @Override
    public BlockPosWrapper getPlayerBlockPos()
    {
        BlockPos playerPos = getPlayer().blockPosition();
        return new BlockPosWrapper(playerPos.getX(), playerPos.getY(), playerPos.getZ());
    }

    @Override
    public ChunkPosWrapper getPlayerChunkPos()
    {
        #if MC_VERSION_1_17_1 || MC_VERSION_1_18_1 || MC_VERSION_1_18_2
        ChunkPos playerPos = getPlayer().chunkPosition();
        #elif MC_VERSION_1_16_5
        ChunkPos playerPos = new ChunkPos(getPlayer().blockPosition());
        #endif
        return new ChunkPosWrapper(playerPos.x, playerPos.z);
    }

    public Options getOptions()
    {
        return mc.options;
    }

    public ModelManager getModelManager()
    {
        return mc.getModelManager();
    }

    public ClientLevel getClientLevel()
    {
        return mc.level;
    }

    @Override
    public IWorldWrapper getWrappedServerWorld()
    {
        if (mc.level == null)
            return null;

        DimensionType dimension = mc.level.dimensionType();
        IntegratedServer server = mc.getSingleplayerServer();

        if (server == null)
            return null;

        ServerLevel serverWorld = null;
        Iterable<ServerLevel> worlds = server.getAllLevels();
        for (ServerLevel world : worlds)
        {
            if (world.dimensionType() == dimension)
            {
                serverWorld = world;
                break;
            }
        }
        return WorldWrapper.getWorldWrapper(serverWorld);
    }

    public WorldWrapper getWrappedClientLevel()
    {
        return WorldWrapper.getWorldWrapper(mc.level);
    }

    public WorldWrapper getWrappedServerLevel()
    {

        if (mc.level == null)
            return null;
        DimensionType dimension = mc.level.dimensionType();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null)
            return null;

        Iterable<ServerLevel> worlds = server.getAllLevels();
        ServerLevel returnWorld = null;

        for (ServerLevel world : worlds)
        {
            if (world.dimensionType() == dimension)
            {
                returnWorld = world;
                break;
            }
        }

        return WorldWrapper.getWorldWrapper(returnWorld);
    }

    @Nullable
    @Override
    public IWorldWrapper getWrappedClientWorld()
    {
        return WorldWrapper.getWorldWrapper(mc.level);
    }

    @Override
    public File getGameDirectory()
    {
        return mc.gameDirectory;
    }

    @Override
    public IProfilerWrapper getProfiler()
    {
        if (profilerWrapper == null)
            profilerWrapper = new ProfilerWrapper(mc.getProfiler());
        else if (mc.getProfiler() != profilerWrapper.profiler)
            profilerWrapper.profiler = mc.getProfiler();

        return profilerWrapper;	}

    public ClientPacketListener getConnection()
    {
        return mc.getConnection();
    }

    public GameRenderer getGameRenderer()
    {
        return mc.gameRenderer;
    }

    public Entity getCameraEntity()
    {
        return mc.cameraEntity;
    }

    public Window getWindow()
    {
        return mc.getWindow();
    }

    @Override
    public float getSkyDarken(float partialTicks)
    {
        return mc.level.getSkyDarken(partialTicks);
    }

    public IntegratedServer getSinglePlayerServer()
    {
        return mc.getSingleplayerServer();
    }

    @Override
    public boolean connectedToServer()
    {
        return mc.getCurrentServer() != null;
    }

    public ServerData getCurrentServer()
    {
        return mc.getCurrentServer();
    }

    public LevelRenderer getLevelRenderer()
    {
        return mc.levelRenderer;
    }

    /** Returns all worlds available to the server */
    @Override
    public ArrayList<IWorldWrapper> getAllServerWorlds()
    {
        ArrayList<IWorldWrapper> worlds = new ArrayList<IWorldWrapper>();

        Iterable<ServerLevel> serverWorlds = mc.getSingleplayerServer().getAllLevels();
        for (ServerLevel world : serverWorlds)
        {
            worlds.add(WorldWrapper.getWorldWrapper(world));
        }

        return worlds;
    }



    @Override
    public void sendChatMessage(String string)
    {
        getPlayer().sendMessage(new TextComponent(string), getPlayer().getUUID());
    }

    /**
     * Crashes Minecraft, displaying the given errorMessage <br> <br>
     * In the following format: <br>
     *
     * The game crashed whilst <strong>errorMessage</strong>  <br>
     * Error: <strong>ExceptionClass: exceptionErrorMessage</strong>  <br>
     * Exit Code: -1  <br>
     */
    @Override
    public void crashMinecraft(String errorMessage, Throwable exception)
    {
        ApiShared.LOGGER.error(ModInfo.READABLE_NAME + " had the following error: [" + errorMessage + "]. Crashing Minecraft...");
        CrashReport report = new CrashReport(errorMessage, exception);
        Minecraft.crash(report);
    }





}
