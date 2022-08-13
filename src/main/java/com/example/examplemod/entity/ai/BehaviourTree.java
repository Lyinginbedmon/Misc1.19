package com.example.examplemod.entity.ai;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entity.ai.TreeNode.NodeMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.PathfinderMob;

/**
 * AI by flowchart!<br>
 * Allows for diverse mob behaviours without relying on randoms and weights like the task system.<br>
 * Good for when a mob needs to be reliable and smart, such as companions.<br>
 * @author Lying
 *
 */
public class BehaviourTree
{
	private final String treeName;
	private final Whiteboard<PathfinderMob> whiteboard;
	
	private final TreeNode root;
	/** List of all nodes that reported either RUNNING or SUCCESS in the latest tick */
	private final List<TreeNode> nodesOfLastRun = Lists.newArrayList();
	
	private final List<BehaviourTree> subTrees = Lists.newArrayList();
	
	public BehaviourTree(String nameIn, TreeNode node)
	{
		this(nameIn, null, node);
	}
	
	public BehaviourTree(String nameIn, Whiteboard<PathfinderMob> board, TreeNode node)
	{
		treeName = nameIn;
		whiteboard = board;
		root = node;
		root.setParentTree(this);
	}
	
	public void addSubTree(BehaviourTree treeIn) { this.subTrees.add(treeIn); }
	
	public final String name() { return this.treeName; }
	
	public void tick(PathfinderMob mobIn)
	{
		if(whiteboard == null)
			return;
		
		whiteboard.tick(mobIn);
		updateTree(this, mobIn, whiteboard);
		
		for(BehaviourTree child : subTrees)
			updateTree(child, mobIn, whiteboard);
	}
	
	private static void updateTree(BehaviourTree tree, PathfinderMob mobIn, Whiteboard<PathfinderMob> storage)
	{
		tree.nodesOfLastRun.clear();
		tree.getRoot().doTick(mobIn, storage);
//		if(!tree.nodesOfLastRun.isEmpty())
//		{
//			ExampleMod.LOG.info("Behaviour tree "+tree.name()+" returned status "+tree.getRoot().previousResult().name());
//			tree.mapTree().printToLog(ExampleMod.LOG);
//		}
	}
	
	public final TreeNode getRoot() { return this.root; }
	
	public void reportNodeActive(TreeNode nodeIn) { this.nodesOfLastRun.add(nodeIn); }
	
	public boolean nodeWasActiveLastTick(TreeNode nodeIn) { return this.nodesOfLastRun.contains(nodeIn); }
	
	/** Returns a list of all nodes in this behaviour tree */
	public final List<TreeNode> nodeContents()
	{
		List<TreeNode> nodes = Lists.newArrayList();
		
		List<TreeNode> currentSet = Lists.newArrayList();
		currentSet.add(getRoot());
		while(!currentSet.isEmpty())
		{
			List<TreeNode> nextSet = Lists.newArrayList();
			currentSet.forEach((node) -> nextSet.addAll(node.getChildren()));
			
			nodes.addAll(currentSet);
			currentSet.clear();
			currentSet.addAll(nextSet);
		}
		
		return nodes;
	}
	
	public void load(CompoundTag compound)
	{
		ListTag dataSet = compound.getList("Nodes", 10);
		List<TreeNode> nodes = nodeContents();
		for(int i=0; i<dataSet.size(); i++)
		{
			TreeNode node = nodes.get(i);
			CompoundTag data = dataSet.getCompound(i);
			node.loadFromNBT(data);
		}
	}
	
	public void save(CompoundTag compound)
	{
		ListTag nodeList = new ListTag();
		nodeContents().forEach((node) -> 
		{
			CompoundTag data = new CompoundTag();
			node.saveToNBT(data);
			nodeList.add(data);
		});
		compound.put("Nodes", nodeList);
	}
	
	public final NodeMap mapTree() { return root.map(0); }
	
	/**
	 * Returns the node in this tree based on its address within the tree.<br>
	 * Address format is child indices separated by '-', eg R-0-1-2.<br>
	 * Note: The root of the tree is always ignored, use getNode() instead.<br>
	 * @param address
	 * @return The designated node, or null if the address is invalid.
	 */
	@Nullable
	public final TreeNode getNode(String address)
	{
		String[] ids = address.split("-");
		if(ids.length == 1 && ids[0].charAt(0) == 'R')
			return this.root;
		
		NodeMap currentNode = mapTree();
		for(int index = 0; index<ids.length; index++)
		{
			String val = ids[index];
			if(val.charAt(0) == 'R')
				continue;
			else
				currentNode = currentNode.getChild(Integer.valueOf(ids[index]));
			if(currentNode == null)
				return null;
		}
		return currentNode.parent;
	}
	
	/**
	 * Returns the string address of the given node in this tree.
	 * @param nodeIn
	 * @return String address from the root of this tree to the given node, or null if it does not exist.
	 */
	@Nullable
	public final String getAddress(TreeNode nodeIn)
	{
		if(nodeIn == this.root)
			return "R";
		else
		{
			String recursiveAddress = mapTree().getLocalAddressOf(nodeIn);
			if(recursiveAddress != null)
				return "R-" + recursiveAddress;
		}
		return null;
	}
}
