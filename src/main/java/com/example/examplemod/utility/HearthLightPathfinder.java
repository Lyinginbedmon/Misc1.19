package com.example.examplemod.utility;

import java.util.List;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.ExampleMod;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class HearthLightPathfinder
{
	/**
	 * Conceits:
	 * 	Cannot path beyond 32 blocks away for lag minimising
	 * 	Cannot path all the way to spawn due to chunk loading concerns
	 * 
	 * Drop breadcrumbs from player periodically, paired by distance to spawn
	 * 	Connect each new breadcrumb to closest existing breadcrumb closer to spawn IF pathfinder can connect
	 * 	Remove breadcrumbs too far from spawn and player?
	 * Pathfinder periodically create path from player to next-nearest breadcrumb
	 * Path simplified to course changes
	 * Path rendered for owner of Hearth Light
	 */
	/** Maximum distance we can search from our destination point */
	public static final double SEARCH_LIMIT = 32D;
	public static final double SEARCH_SQR = SEARCH_LIMIT * SEARCH_LIMIT;
	
	private final Player owner;
	
	private List<BlockPos> currentPath = Lists.newArrayList();
	private AbstractPathingSearch currentSearch;
	
	public HearthLightPathfinder(Player player)
	{
		this.owner = player;
		start();
	}
	
	public BlockPos getDestination() { return new BlockPos(-12, -60, -24); }
	
	public BlockPos getPos() { return new BlockPos(-12, -60, -38); }
	
	public boolean hasPath() { return !currentPath.isEmpty(); }
	
	public List<BlockPos> getPath() { return this.currentPath; }
	
	public void generatePath()
	{
		this.currentPath = findPathBetween(getPos(), getDestination(), this.owner.getLevel());
	}
	
	public boolean searchCompleted() { return this.currentSearch.finished(); }
	
	public void tickPath(Level world)
	{
		if(currentSearch.finished())
			return;
		
		currentSearch.evaluateNextNode(world);
		if(currentSearch.finished())
		{
			currentPath = currentSearch.successful() ? cleanPath(currentSearch.createPath()) : Lists.newArrayList();
			ExampleMod.LOG.info("Hearth Light pathfinder completed search at "+currentSearch.nodesSearched()+" nodes "+(currentSearch.successful() ? "SUCCESS" : "FAILED"));
		}
	}
	
	public AbstractPathingSearch currentSearch() { return this.currentSearch; }
	
	public void start() { this.currentSearch = new PathingSearch(getPos(), getDestination()); }
	
	public List<BlockPos> findPathBetween(BlockPos position, BlockPos destination, Level world)
	{
		ExampleMod.LOG.info("Hearth Light pathfinder attempting path between "+position.toShortString()+" and "+destination.toShortString()+" (distance "+(int)Mth.sqrt((float)position.distSqr(destination))+")");
		start();
		while(!currentSearch.finished())
			currentSearch.evaluateNextNode(world);
		ExampleMod.LOG.info("Hearth Light pathfinder completed search at "+currentSearch.nodesSearched()+" nodes "+(currentSearch.successful() ? "SUCCESS" : "FAILED"));
		return currentSearch.successful() ? cleanPath(currentSearch.createPath()) : Lists.newArrayList();
	}
	
	/** Reduces the given path to the minimum number of nodes necessary */
	public static List<BlockPos> cleanPath(List<BlockPos> path)
	{
		if(path.size() <= 2)
			return path;
		
		List<BlockPos> toClean = Lists.newArrayList();
		for(int i=1; i<path.size() - 1; i++)
		{
			BlockPos nodePrev = path.get(i - 1);
			BlockPos node = path.get(i);
			BlockPos nodeNext = path.get(i + 1);
			if(node.subtract(nodePrev).equals(nodeNext.subtract(node)))
				toClean.add(node);
		}
		path.removeAll(toClean);
		return path;
	}
}
