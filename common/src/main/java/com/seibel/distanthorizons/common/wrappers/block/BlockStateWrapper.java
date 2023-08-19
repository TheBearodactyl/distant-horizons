package com.seibel.distanthorizons.common.wrappers.block;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

#if MC_1_16_5 || MC_1_17_1
import net.minecraft.core.Registry;
#elif MC_1_18_2 || MC_1_19_2
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
#else
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.EmptyBlockGetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
#endif

public class BlockStateWrapper implements IBlockStateWrapper
{
	/** example "minecraft:plains" */
	public static final String RESOURCE_LOCATION_SEPARATOR = ":";
	/** example "minecraft:water_STATE_{level:0}" */
	public static final String STATE_STRING_SEPARATOR = "_STATE_";
	
	
	// must be defined before AIR, otherwise a null pointer will be thrown
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
    public static final ConcurrentHashMap<BlockState, BlockStateWrapper> cache = new ConcurrentHashMap<>();
	public static final BlockStateWrapper AIR = fromBlockState(BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse("minecraft:air")).defaultBlockState(), null);
	public static final String AIR_SERIALIZATION_RESULT = "AIR";
	
	/**
	 * Cached so it can be quickly used as a semi-stable hashing method. <br>
	 * This may also fix the issue where we can serialize and save after a level has been shut down.
	 */
	private String serializationResult = null;
	
	
	//==============//
	// constructors //
	//==============//
	
    public static BlockStateWrapper fromBlockState(BlockState blockState, @Nullable ILevelWrapper levelWrapper)
	{
		if (Objects.requireNonNull(blockState).isAir() && AIR != null)
			return AIR;
		
        return cache.computeIfAbsent(blockState, blockState1 -> new BlockStateWrapper(blockState1, levelWrapper));
    }

    public final BlockState blockState;
	@CheckForNull
	public final ILevelWrapper levelWrapper;

    BlockStateWrapper(BlockState blockState, @Nullable ILevelWrapper levelWrapper)
    {
        this.blockState = blockState;
        this.levelWrapper = blockState.isAir() 
		        ? null 
		        : Objects.requireNonNull(levelWrapper);
		
        LOGGER.trace("Created BlockStateWrapper for ["+blockState+"]");
    }
	
	
	
	//=========//
	// methods //
	//=========//
	
	@Override
	public int getOpacity()
	{
		// this method isn't perfect, but works well enough for our use case
		if (this.isAir() || !this.blockState.canOcclude())
		{
			// completely transparent
			return 0;
		}
		else
		{
			// completely opaque
			return 16;
		}
	}
	
	@Override
	public int getLightEmission() { return (this.blockState != null) ? this.blockState.getLightEmission() : 0; }

	@Override
    public String serialize()
	{
		// the result can be quickly used as a semi-stable hashing method, so it's going to be cached
		if (this.serializationResult != null)
			return this.serializationResult;
		
		if (this.blockState.isAir())
			return this.serializationResult = AIR_SERIALIZATION_RESULT;
		
		Objects.requireNonNull(levelWrapper);
		RegistryAccess registryAccess = ((Level) levelWrapper.getWrappedMcObject()).registryAccess();
		
		ResourceLocation resourceLocation;
		#if MC_1_16_5 || MC_1_17_1
		resourceLocation = Registry.BLOCK.getKey(this.blockState.getBlock());
		#elif MC_1_18_2 || MC_1_19_2
		resourceLocation = registryAccess.registryOrThrow(Registry.BLOCK_REGISTRY).getKey(this.blockState.getBlock());
		#else
		resourceLocation = registryAccess.registryOrThrow(Registries.BLOCK).getKey(this.blockState.getBlock());
		#endif
		Objects.requireNonNull(resourceLocation);
		
		this.serializationResult = resourceLocation.getNamespace() + RESOURCE_LOCATION_SEPARATOR + resourceLocation.getPath()
				+ STATE_STRING_SEPARATOR + serializeBlockStateProperties(this.blockState);

		return this.serializationResult;
	}

	@Override
	@Nullable
	public ILevelWrapper getLevelWrapper() {
		return levelWrapper;
	}

	public static BlockStateWrapper deserialize(String resourceStateString, ILevelWrapper levelWrapper) throws IOException
	{
		if (resourceStateString.isEmpty())
			throw new IOException("resourceStateString is empty");
		
		if (resourceStateString.equals(AIR_SERIALIZATION_RESULT))
			return AIR;
		
		// Parse the BlockState
		int stateSeparatorIndex = resourceStateString.indexOf(STATE_STRING_SEPARATOR);
		if (stateSeparatorIndex == -1)
		{
			throw new IOException("Unable to parse BlockState out of string: [" + resourceStateString + "].");
		}
		String blockStatePropertiesString = resourceStateString.substring(stateSeparatorIndex + STATE_STRING_SEPARATOR.length());
		resourceStateString = resourceStateString.substring(0, stateSeparatorIndex);
		
		// parse the resource location
		int resourceSeparatorIndex = resourceStateString.indexOf(RESOURCE_LOCATION_SEPARATOR);
		if (resourceSeparatorIndex == -1)
		{
			throw new IOException("Unable to parse Resource Location out of string: [" + resourceStateString + "].");
		}
		ResourceLocation resourceLocation = new ResourceLocation(resourceStateString.substring(0, resourceSeparatorIndex), resourceStateString.substring(resourceSeparatorIndex + 1));
		
		
		
		// attempt to get the BlockState from all possible BlockStates
		try
		{
			Block block;
			#if MC_1_16_5 || MC_1_17_1
			block = Registry.BLOCK.get(resourceLocation);
			#elif MC_1_18_2 || MC_1_19_2
			net.minecraft.core.RegistryAccess registryAccess = ((Level)levelWrapper.getWrappedMcObject()).registryAccess();
			block = registryAccess.registryOrThrow(Registry.BLOCK_REGISTRY).get(resourceLocation);
			#else
			net.minecraft.core.RegistryAccess registryAccess = ((Level)levelWrapper.getWrappedMcObject()).registryAccess();
			block = registryAccess.registryOrThrow(Registries.BLOCK).get(resourceLocation);
			#endif
			
			
			
			BlockState foundState = null;
			List<BlockState> possibleStateList = block.getStateDefinition().getPossibleStates();
			for (BlockState possibleState : possibleStateList)
			{
				String possibleStatePropertiesString = serializeBlockStateProperties(possibleState);
				if (possibleStatePropertiesString.equals(blockStatePropertiesString))
				{
					foundState = possibleState;
					break;
				}
			}
			
			Objects.requireNonNull(foundState);
			return new BlockStateWrapper(foundState, levelWrapper);
		}
		catch (Exception e)
		{
			throw new IOException("Failed to deserialize the string [" + resourceStateString + "] into a BlockStateWrapper: " + e.getMessage(), e);
		}
	}
	
	/** used to compare and save BlockStates based on their properties */
	private static String serializeBlockStateProperties(BlockState blockState)
	{
		// get the property list for this block (doesn't contain this block state's values, just the names and possible values)
		java.util.Collection<net.minecraft.world.level.block.state.properties.Property<?>> blockPropertyCollection = blockState.getProperties();
		
		// alphabetically sort the list, so they are always in the same order
		List<net.minecraft.world.level.block.state.properties.Property<?>> sortedBlockPropteryList = new ArrayList<>(blockPropertyCollection);
		sortedBlockPropteryList.sort((a, b) -> a.getName().compareTo(b.getName()));
		
		
		StringBuilder stringBuilder = new StringBuilder();
		for (net.minecraft.world.level.block.state.properties.Property<?> property : sortedBlockPropteryList)
		{
			String propertyName = property.getName();
			
			String value = "NULL";
			if (blockState.hasProperty(property))
			{
				value = blockState.getValue(property).toString();
			}
			
			stringBuilder.append("{");
			stringBuilder.append(propertyName).append(RESOURCE_LOCATION_SEPARATOR).append(value);
			stringBuilder.append("}");
		}
		
		return stringBuilder.toString();
	}
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		if (obj == null || this.getClass() != obj.getClass())
		{
			return false;
		}
		
        BlockStateWrapper that = (BlockStateWrapper) obj;
	    // the serialized value is used, so we can test the contents instead of the references
        return Objects.equals(this.serialize(), that.serialize());
    }

    @Override
    public int hashCode() {
		return Objects.hash(this.serialize());
	}
	
	@Override
	public Object getWrappedMcObject() { return this.blockState; }
	
	@Override
	public boolean isAir() { return this.isAir(this.blockState); }
	public boolean isAir(BlockState blockState) { return blockState == null || blockState.isAir(); }
	
	@Override
	public boolean isSolid()
	{
        #if PRE_MC_1_20_1
		return this.blockState.getMaterial().isSolid();
        #else
		return !this.blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
        #endif
	}
	
	@Override
	public boolean isLiquid()
	{
		if (this.isAir())
		{
			return false;
		}
		
        #if PRE_MC_1_20_1
		return this.blockState.getMaterial().isLiquid();
        #else
		return !this.blockState.getFluidState().isEmpty();
        #endif
	}
	
	@Override
	public String toString() {
		return this.serialize();
	}

}
