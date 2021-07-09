package com.seibel.lod.builders;

import com.seibel.lod.handlers.LodConfig;
import com.seibel.lod.objects.LodChunk;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.NearFarBuffer;
import com.seibel.lod.objects.quadTree.LodNodeData;
import com.seibel.lod.objects.quadTree.LodQuadTree;
import com.seibel.lod.objects.quadTree.LodQuadTreeDimension;
import com.seibel.lod.render.LodNodeRenderer;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.LodUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.WorldWorkerManager;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This object is used to create NearFarBuffer objects.
 * 
 * @author James Seibel
 * @version 06-19-2021
 */
public class LodNodeBufferBuilder
{
	private Minecraft mc;

	/** This holds the thread used to generate new LODs off the main thread. */
	private ExecutorService genThread = Executors.newSingleThreadExecutor();

	private LodNodeBuilder lodChunkBuilder;

	/** The buffers that are used to create LODs using near fog */
	public volatile BufferBuilder buildableNearBuffer;
	/** The buffers that are used to create LODs using far fog */
	public volatile BufferBuilder buildableFarBuffer;

	/** if this is true the LOD buffers are currently being
	 * regenerated. */
	public volatile boolean generatingBuffers = false;

	/** if this is true new LOD buffers have been generated
	 * and are waiting to be swapped with the drawable buffers*/
	private volatile boolean switchBuffers = false;

	/** If this is greater than 0 no new chunk generation requests will be made
	 * this is to prevent chunks from being generated for a long time in an area
	 * the player is no longer in. */
	public volatile int numberOfChunksWaitingToGenerate = 0;

	/** how many chunks to generate outside of the player's
	 * view distance at one time. (or more specifically how
	 * many requests to make at one time) */
	public int maxChunkGenRequests = Runtime.getRuntime().availableProcessors();


	public LodNodeBufferBuilder(LodNodeBuilder newLodBuilder)
	{
		mc = Minecraft.getInstance();
		lodChunkBuilder = newLodBuilder;
	}
	
	
	private BiomeContainer biomeContainer = null;
	private LodQuadTreeDimension previousDimension = null;
	

	/**
	 * Create a thread to asynchronously generate LOD buffers
	 * centered around the given camera X and Z.
	 * <br>
	 * This method will write to the drawableNearBuffers and drawableFarBuffers.
	 * <br>
	 * After the buildable buffers have been generated they must be
	 * swapped with the drawable buffers in the LodRenderer to be drawn.
	 */
	public void generateLodBuffersAsync(LodNodeRenderer renderer, LodQuadTreeDimension lodDim,
										double playerX, double playerZ, int numbChunksWide)
	{
		// only allow one generation process to happen at a time
		if (generatingBuffers)
			return;
		
		if (buildableNearBuffer == null || buildableFarBuffer == null)
			throw new IllegalStateException("generateLodBuffersAsync was called before the buildableNearBuffer and buildableFarBuffer were created.");
		
		if (previousDimension != lodDim)
		{
			biomeContainer = LodUtil.getServerWorldFromDimension(lodDim.dimension).getChunk(0, 0, ChunkStatus.EMPTY).getBiomes();
			previousDimension = lodDim;
		}
		
		
		
		generatingBuffers = true;
		
		
		
		// this seemingly useless math is required,
		// just using (int) playerX/Z doesn't work
		int playerXChunkOffset = ((int) playerX / LodChunk.WIDTH) * LodChunk.WIDTH;
		int playerZChunkOffset = ((int) playerZ / LodChunk.WIDTH) * LodChunk.WIDTH;
		// this is where we will start drawing squares
		// (exactly half the total width)
		int startX = (-LodChunk.WIDTH * (numbChunksWide / 2)) + playerXChunkOffset;
		int startZ = (-LodChunk.WIDTH * (numbChunksWide / 2)) + playerZChunkOffset;
		
		
		Thread t = new Thread(()->
		{
			// index of the chunk currently being added to the
			// generation list
			int chunkGenIndex = 0;
			
			ChunkPos[] chunksToGen = new ChunkPos[maxChunkGenRequests];
			// if we don't have a full number of chunks to generate in chunksToGen
			// we can top it off from the reserve
			ChunkPos[] chunksToGenReserve = new ChunkPos[maxChunkGenRequests];
			int minChunkDist = Integer.MAX_VALUE;
			ChunkPos playerChunkPos = new ChunkPos((int)playerX / LodChunk.WIDTH, (int)playerZ / LodChunk.WIDTH);
			
			
			// generate our new buildable buffers
			buildableNearBuffer.begin(GL11.GL_QUADS, LodRenderer.LOD_VERTEX_FORMAT);
			buildableFarBuffer.begin(GL11.GL_QUADS, LodRenderer.LOD_VERTEX_FORMAT);


			List<LodNodeData> lodList = new ArrayList<>();
			lodList.addAll(lodDim.getNodeToRender((int) playerX,(int)playerZ,(byte) 9, 100000,8000));
			lodList.addAll(lodDim.getNodeToRender((int)playerX,(int)playerZ,(byte) 8, 8000,4000));
			lodList.addAll(lodDim.getNodeToRender((int)playerX,(int)playerZ,(byte) 7, 4000,2000));
			lodList.addAll(lodDim.getNodeToRender((int)playerX,(int)playerZ,(byte) 6, 2000,1000));
			lodList.addAll(lodDim.getNodeToRender((int)playerX,(int)playerZ,(byte) 5, 1000,500));
			lodList.addAll(lodDim.getNodeToRender((int)playerX,(int)playerZ,(byte) 4, 500,250));
			lodList.addAll(lodDim.getNodeToRender((int)playerX,(int)playerZ,(byte) 3, 250,0));

			for(LodNodeData data : lodList){
				BufferBuilder currentBuffer = null;
/*
				if (isCoordinateInNearFogArea(i, j, numbChunksWide / 2))
					currentBuffer = buildableNearBuffer;
				else
					currentBuffer = buildableFarBuffer;
 */
				currentBuffer = buildableFarBuffer;

				// get the desired LodTemplate and
				// add this LOD to the buffer
				LodConfig.CLIENT.lodTemplate.get().
						template.addLodToBuffer(currentBuffer, lodDim, data,
						data.startX, 0, data.startZ, renderer.debugging);
			}
			// x axis
			/*
			for (int i = 0; i < numbChunksWide; i++)
			{
				// z axis
				for (int j = 0; j < numbChunksWide; j++)
				{
					int chunkX = i + (startX / LodChunk.WIDTH);
					int chunkZ = j + (startZ / LodChunk.WIDTH);
					
					// skip any chunks that Minecraft is going to render
					if(isCoordInCenterArea(i, j, (numbChunksWide / 2)) 
						&& renderer.vanillaRenderedChunks.contains(new ChunkPos(chunkX, chunkZ)))
					{
						continue;
					}
					
					
					// set where this square will be drawn in the world
					double xOffset = (LodChunk.WIDTH * i) + // offset by the number of LOD blocks
									startX; // offset so the center LOD block is centered underneath the player
					double yOffset = 0;
					double zOffset = (LodChunk.WIDTH * j) + startZ;

					LodNodeData lod = lodDim.getLodFromCoordinates(chunkX, chunkZ, LodNodeData.CHUNK_LEVEL);
					
					if (lod == null || lod.voidNode)
					{
						// generate a new chunk if no chunk currently exists
						// and we aren't waiting on any other chunks to generate
						if (lod == null && numberOfChunksWaitingToGenerate < maxChunkGenRequests)
						{
							ChunkPos pos = new ChunkPos(chunkX, chunkZ);
							
							// determine if this position is closer to the player
							// than the previous
							int newDistance = playerChunkPos.getChessboardDistance(pos);
							
							if (newDistance < minChunkDist)
							{
								// this chunk is closer, clear any previous
								// positions and update the new minimum distance
								minChunkDist = newDistance;
								chunksToGenReserve = chunksToGen;
								
								chunkGenIndex = 0;
								chunksToGen = new ChunkPos[maxChunkGenRequests];
								chunksToGen[chunkGenIndex] = pos;
								chunkGenIndex++;
							}
							else if (newDistance <= minChunkDist)
							{
								// this chunk position is as close or closers than the
								// minimum distance
								if(chunkGenIndex < maxChunkGenRequests)
								{
									// we are still under the number of chunks to generate
									// add this position to the list
									chunksToGen[chunkGenIndex] = pos;
									chunkGenIndex++;
								}
							}
							
						}
						// don't render this null chunk
						continue;
					}


					

				}
			}
			*/
			// TODO add a way for a server side mod to generate chunks requested here
			/*
			if(mc.hasSingleplayerServer())
			{
		        ServerWorld serverWorld = LodUtil.getServerWorldFromDimension(lodDim.dimension);
				
		        // make sure we have as many chunks to generate as we are allowed
		        if (chunkGenIndex < maxChunkGenRequests)
		        {
		        	for(int i = chunkGenIndex, j = 0; i < maxChunkGenRequests; i++, j++)
		        	{
		        		chunksToGen[i] = chunksToGenReserve[j];
		        	}
		        }
		        
				// start chunk generation
				for(ChunkPos chunkPos : chunksToGen)
				{
					if(chunkPos == null)
						break;
					
					numberOfChunksWaitingToGenerate++;
					
					LodChunkGenWorker.java genWorker = new LodChunkGenWorker.java(chunkPos, renderer, lodChunkBuilder, this, lodDim, serverWorld, biomeContainer);
					WorldWorkerManager.addWorker(genWorker);
				}
			}
			*/
			
			// finish the buffer building
			buildableNearBuffer.end();
			buildableFarBuffer.end();
			
			// mark that the buildable buffers as ready to swap
			generatingBuffers = false;
			switchBuffers = true;
		});
		
		genThread.execute(t);
		
		return;
	}
	
	
	
	
	
	
	
	
	
	
	//====================//
	// generation helpers //
	//====================//
	
	/**
	 * Returns if the given coordinate is in the loaded area of the world.
	 * @param centerCoordinate the center of the loaded world
	 */
	private boolean isCoordInCenterArea(int i, int j, int centerCoordinate)
	{
		return (i >= centerCoordinate - mc.options.renderDistance
				&& i <= centerCoordinate + mc.options.renderDistance) 
				&& 
				(j >= centerCoordinate - mc.options.renderDistance
				&& j <= centerCoordinate + mc.options.renderDistance);
	}
	
	
	/**
	 * Find the coordinates that are in the center half of the given
	 * 2D matrix, starting at (0,0) and going to (2 * lodRadius, 2 * lodRadius).
	 */
	private static boolean isCoordinateInNearFogArea(int chunkX, int chunkZ, int lodRadius)
	{
		int halfRadius = lodRadius / 2;
		
		return (chunkX >= lodRadius - halfRadius 
				&& chunkX <= lodRadius + halfRadius) 
				&& 
				(chunkZ >= lodRadius - halfRadius
				&& chunkZ <= lodRadius + halfRadius);
	}
	
	
	
	
	
	//===============================//
	// BufferBuilder related methods //
	//===============================//
	
	
	/**
	 * Called from the LodRenderer to create the
	 * BufferBuilders at the right size.
	 * 
	 * @param bufferMaxCapacity
	 */
	public void setupBuffers(int bufferMaxCapacity)
	{
		buildableNearBuffer = new BufferBuilder(bufferMaxCapacity);
		buildableFarBuffer = new BufferBuilder(bufferMaxCapacity);
	}
	
	/**
	 * Swap the drawable and buildable buffers and return
	 * the old drawable buffers.
	 * @param drawableNearBuffer
	 * @param drawableFarBuffer
	 */
	public NearFarBuffer swapBuffers(BufferBuilder drawableNearBuffer, BufferBuilder drawableFarBuffer)
	{
		// swap the BufferBuilders
		BufferBuilder tmp = buildableNearBuffer;
		buildableNearBuffer = drawableNearBuffer;
		drawableNearBuffer = tmp;
		
		tmp = buildableFarBuffer;
		buildableFarBuffer = drawableFarBuffer;
		drawableFarBuffer = tmp;
		
		
		// the buffers have been swapped
		switchBuffers = false;
		
		return new NearFarBuffer(drawableNearBuffer, drawableFarBuffer);
	}
	
	/**
	 * If this is true the buildable near and far
	 * buffers have been generated and are ready to be
	 * sent to the LodRenderer. 
	 */
	public boolean newBuffersAvaliable() 
	{
		return switchBuffers;
	}
	
	
	
	
}