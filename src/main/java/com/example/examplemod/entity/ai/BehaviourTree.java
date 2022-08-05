package com.example.examplemod.entity.ai;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.TestEntity;
import com.example.examplemod.entity.ai.Node.NodeMap;
import com.example.examplemod.init.ExEntities;

import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;

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
	private final Whiteboard<Mob> whiteboard;
	private final Node root;
	
	private final List<BehaviourTree> subTrees = Lists.newArrayList();
	
	public BehaviourTree(String nameIn, Node node)
	{
		this(nameIn, null, node);
	}
	
	public BehaviourTree(String nameIn, Whiteboard<Mob> board, Node node)
	{
		treeName = nameIn;
		whiteboard = board;
		root = node;
	}
	
	public void addSubTree(BehaviourTree treeIn) { this.subTrees.add(treeIn); }
	
	public final String name() { return this.treeName; }
	
	public void tick(Mob mobIn)
	{
		if(whiteboard == null)
			return;
		
		whiteboard.tick(mobIn);
		updateTree(this, mobIn, whiteboard);
		
		for(BehaviourTree child : subTrees)
			updateTree(child, mobIn, whiteboard);
	}
	
	private static void updateTree(BehaviourTree tree, Mob mobIn, Whiteboard<Mob> storage)
	{
		Node root = tree.getRoot();
		if(root.isRunning())
		{
			root.tick(mobIn, storage);
			
			if(root.shouldInterrupt(mobIn, storage))
				tree.interrupt(mobIn, storage);
		}
		else if(root.canRun(mobIn, storage))
			root.setRunning(true);
	}
	
	public void interrupt(Mob mobIn, Whiteboard<Mob> storage) throws RuntimeException
	{
		root.interrupt(mobIn, storage);
		if(root.isRunning())
		{
			Node node = getCurrentActiveNode();
			Throwable cause = new Throwable("Node "+getAddress(node)+" "+node.name()+" not interrupted!");
			throw new RuntimeException("Run stop failure!", cause);
		}
	}
	
	public final Node getRoot() { return this.root; }
	
	public final NodeMap mapTree() { return root.map(0); }
	
	/**
	 * Returns the node in this tree based on its address within the tree.<br>
	 * Address format is child indices separated by '-', eg R-0-1-2.<br>
	 * Note: The root of the tree is always ignored, use getNode() instead.<br>
	 * @param address
	 * @return The designated node, or null if the address is invalid.
	 */
	@Nullable
	public final Node getNode(String address)
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
	public final String getAddress(Node nodeIn)
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
	
	/**
	 * @return The lowest active node in this tree, if any.
	 */
	public final Node getCurrentActiveNode()
	{
		return root.getActiveNodeRecursive();
	}
	
	static
	{
		MinecraftForge.EVENT_BUS.addListener(BehaviourTree::onStateChange);
	}
	
	public static void onStateChange(StateChange event)
	{
		Mob mob = (Mob)event.getEntity();
		if(mob.getType() == ExEntities.TEST.get())
		{
			ExampleMod.LOG.info(" = State change reported");
			((TestEntity)mob).getTree().mapTree().printToLog(ExampleMod.LOG);
		}
	}
	
	public static class StateChange extends LivingEvent
	{
		public StateChange(Mob mobIn) { super(mobIn); }
	}
}
