package com.example.examplemod.entity.ai;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.entity.Mob;

public abstract class Node
{
	private final String name;
	private boolean isRunning = false;
	private boolean started = false;
	private Predicate<Pair<Mob,Whiteboard<?>>> specialInterrupt = Predicates.alwaysFalse();
	
	protected Node(String nameIn)
	{
		name = nameIn;
	}
	
	public final String name() { return this.name; }
	
	/** Returns true if this node can start running */
	public abstract boolean canRun(Mob mobIn, Whiteboard<?> storage);
	
	/** Returns true if this node is currently running */
	public boolean isRunning() { return this.isRunning; }
	
	/** Starts or stops this node from running */
	public void setRunning(boolean input) { this.isRunning = input; }
	
	public Node getActiveNodeRecursive() { return this; }
	
	/** Returns true if this node should be interrupted for any reason */
	public final boolean shouldInterrupt(Mob mobIn, Whiteboard<?> storage)
	{
		return isRunning() && (interruptCondition(mobIn, storage) || this.specialInterrupt.apply(Pair.of(mobIn, storage)));
	}
	
	/** Applies standard interrupt logic for this node */
	protected boolean interruptCondition(Mob mobIn, Whiteboard<?> storage) { return false; } 
	
	/** 
	 * Sets the special interrupt logic for this node.<br>
	 * This should generally be due to factors external to this node.
	 */
	public Node setToInterrupt(Predicate<Pair<Mob,Whiteboard<?>>> conditionIn) { this.specialInterrupt = conditionIn; return this; }
	
	/** Forces this node to stop its current action prematurely */
	public final void interrupt(Mob mobIn, Whiteboard<?> storage)
	{
		stop(mobIn, storage);
		resetNode();
	}
	
	/** Performs startup operations when this node first starts running */
	protected void start(Mob mobIn, Whiteboard<?> storage) { }
	
	/** Continues the current run */
	public final void tick(Mob mobIn, Whiteboard<?> storage)
	{
		if(isRunning())
			if(started)
				run(mobIn, storage);
			else
			{
				start(mobIn, storage);
				started = true;
//				MinecraftForge.EVENT_BUS.post(new StateChange(mobIn));
			}
	}
	
	protected final void resetNode()
	{
		this.started = false;
		this.isRunning = false;
	}
	
	/** Updates continual operation whilst node is running */
	protected void run(Mob mobIn, Whiteboard<?> storage) { }
	
	/**
	 * Performs necessary cleanup when this node is interrupted.<br>
	 * Note that descendant nodes should call interrupt.
	 */
	public void stop(Mob mobIn, Whiteboard<?> storage) { }
	
	public NodeMap map(int depth) { return new NodeMap(this, depth); }
	
	public static class NodeMap
	{
		public final Node parent;
		private final int depth;
		private final List<NodeMap> childMaps = Lists.newArrayList();
		
		public NodeMap(Node nodeIn, int depthIn)
		{
			parent = nodeIn;
			depth = depthIn;
		}
		
		public NodeMap(Node nodeIn, int depthIn, Node... childrenIn)
		{
			this(nodeIn, depthIn);
			for(Node node : childrenIn)
				childMaps.add(node.map(depth + 1));
		}
		
		public List<NodeMap> getChildren(){ return childMaps; }
		
		@Nullable
		public NodeMap getChild(int index)
		{
			return index < getChildren().size() ? getChildren().get(index) : null;
		}
		
		/**
		 * Recursively searches this node and its descendants for the given node.<br>
		 * @param nodeIn
		 * @return The address of the given node from this node, or null if it wasn't found.
		 */
		@Nullable
		public String getLocalAddressOf(Node nodeIn)
		{
			for(int i=0; i<childMaps.size(); i++)
			{
				NodeMap child = childMaps.get(i);
				if(child.parent == nodeIn)
					return String.valueOf(i);
				
				String childAddress = child.getLocalAddressOf(nodeIn);
				if(childAddress != null)
					return String.valueOf(i) + "-" + childAddress;
			}
			return null;
		}
		
		public void print()
		{
			String nodeName = parent.name();
			if(parent.isRunning())
				nodeName = "[" + nodeName + "]";
			System.out.println(new String(new char[depth]).replace("\0", "  ") + "> "+nodeName);
			childMaps.forEach((map) -> map.print());
		}
		
		public void printToLog(Logger logIn)
		{
			String nodeName = parent.name();
			if(parent.isRunning())
				nodeName = "[" + nodeName + "]";
			logIn.info(new String(new char[depth]).replace("\0", "  ") + "> "+nodeName);
			childMaps.forEach((map) -> map.printToLog(logIn));
		}
	}
	
	public static abstract class CompoundNode extends Node
	{
		protected List<Node> childNodes = Lists.newArrayList();
		
		public CompoundNode(String nameIn, Node... nodesIn)
		{
			super(nameIn);
			for(Node node : nodesIn)
				if(node != null)
					childNodes.add(node);
		}
		
		public NodeMap map(int depth) { return new NodeMap(this, depth, childNodes.toArray(new Node[0])); }
		
		@Nullable
		protected abstract Node getActiveNode();
		
		public Node getActiveNodeRecursive() { return getActiveNode() != null ? getActiveNode().getActiveNodeRecursive() : null; }
		
		public void stop(Mob mobIn, Whiteboard<?> storage)
		{
			childNodes.forEach((child) -> { if(child.isRunning()) { child.interrupt(mobIn, storage); } });
		}
		
		protected boolean interruptCondition(Mob mobIn, Whiteboard<?> storage)
		{
			return getActiveNode() == null ? false : getActiveNode().shouldInterrupt(mobIn, storage);
		}
	}
	
	/** Performs the first viable descendant node */
	public static class Selector extends CompoundNode
	{
		private Node selected = null;
		private boolean lockUntilComplete = false;
		
		public static Selector root(Node... nodesIn) { return new Selector("root", nodesIn); }
		
		public Selector(String nameIn, Node... nodesIn){ super(nameIn, nodesIn); }
		
		/** Causes this selector to continually choose its last-selected node until that node can no longer run */
		public Selector setLock() { lockUntilComplete = true; return this; }
		
		public boolean canRun(Mob mobIn, Whiteboard<?> storage)
		{
			if(selected != null && lockUntilComplete && selected.canRun(mobIn, storage))
				return true;
			
			selected = null;
			for(Node child : childNodes)
				if(child.canRun(mobIn, storage))
				{
					selected = child;
					break;
				}
			return selected != null;
		}
		
		public void start(Mob mobIn, Whiteboard<?> storage)
		{
			selected.setRunning(true);
			selected.start(mobIn, storage);
		}
		
		protected void run(Mob mobIn, Whiteboard<?> storage)
		{
			selected.tick(mobIn, storage);
			if(!selected.isRunning())
				reset();
		}
		
		public void stop(Mob mobIn, Whiteboard<?> storage)
		{
			if(selected != null)
				selected.stop(mobIn, storage);
			reset();
		}
		
		@Nullable
		protected Node getActiveNode() { return selected; }
		
		private void reset()
		{
			if(!lockUntilComplete)
				selected = null;
			resetNode();
		}
	}
	
	/** Performs each viable descendant node in order */
	public static class Sequence extends CompoundNode
	{
		private List<Node> activeNodes = Lists.newArrayList();
		
		public Sequence(String nameIn, Node... nodesIn){ super(nameIn, nodesIn); }
		
		public boolean canRun(Mob mobIn, Whiteboard<?> storage)
		{
			List<Node> nodes = Lists.newArrayList();
			nodes.addAll(childNodes);
			while(!nodes.isEmpty())
				if(nodes.get(0).canRun(mobIn, storage))
					return true;
				else
					nodes.remove(0);
			
			return false;
		}
		
		protected Node getActiveNode() { return activeNodes.isEmpty() ? null : activeNodes.get(0); }
		
		public NodeMap map(int depth) { return new NodeMap(this, depth, activeNodes.toArray(new Node[0])); }
		
		public void start(Mob mobIn, Whiteboard<?> storage)
		{
			activeNodes.clear();
			activeNodes.addAll(childNodes);
			startFirstValidNode(mobIn, storage);
		}
		
		protected void run(Mob mobIn, Whiteboard<?> storage)
		{
			Node node = currentNode();
			if(node == null)
			{
				reset();
				return;
			}
			
			if(node.isRunning())
				node.tick(mobIn, storage);
			else
			{
				activeNodes.remove(0);
				startFirstValidNode(mobIn, storage);
			}
		}
		
		private Node currentNode() { return activeNodes.isEmpty() ? null : activeNodes.get(0); }
		
		private void startFirstValidNode(Mob mobIn, Whiteboard<?> storage)
		{
			while(!activeNodes.isEmpty())
			{
				Node child = currentNode();
				if(child.canRun(mobIn, storage))
				{
					child.setRunning(true);
					child.start(mobIn, storage);
					break;
				}
				else
					activeNodes.remove(0);
			}
			
			if(activeNodes.isEmpty())
				resetNode();
		}
		
		public void stop(Mob mobIn, Whiteboard<?> storage)
		{
			super.stop(mobIn, storage);
			if(!activeNodes.isEmpty())
			{
				currentNode().interrupt(mobIn, storage);
				reset();
			}
		}
		
		private void reset()
		{
			activeNodes.clear();
			resetNode();
		}
	}
	
	/** Performs all descendant nodes until one is nonviable */
	public static class Parallel extends CompoundNode
	{
		private final Style style;
		
		public static Parallel root(Node... nodesIn) { return new Parallel("root", nodesIn); }
		
		public Parallel(String nameIn, Node... nodesIn)
		{
			this(nameIn, Style.AND, nodesIn);
		}
		
		public Parallel(String nameIn, Style styleIn, Node... nodesIn)
		{
			super(nameIn, nodesIn);
			this.style = styleIn;
		}
		
		public boolean canRun(Mob mobIn, Whiteboard<?> storage)
		{
			for(Node node : childNodes)
				if(!node.canRun(mobIn, storage))
					return false;
			return true;
		}
		
		public boolean isRunning()
		{
			switch(style)
			{
				case AND:
					for(Node node : childNodes)
						if(!node.isRunning())
						{
							setRunning(false);
							return false;
						}
					return true;
				case OR:
					for(Node node : childNodes)
						if(node.isRunning())
							return true;
					return false;
			}
			return false;
		}
		
		public void setRunning(boolean input)
		{
			childNodes.forEach((node) -> node.setRunning(input));
		}
		
		protected Node getActiveNode() { return isRunning() ? childNodes.get(0) : null; }
		
		public void start(Mob mobIn, Whiteboard<?> storage)
		{
			childNodes.forEach((node) -> node.start(mobIn, storage));
		}
		
		protected void run(Mob mobIn, Whiteboard<?> storage)
		{
			childNodes.forEach((node) -> node.tick(mobIn, storage));
		}
		
		public void stop(Mob mobIn, Whiteboard<?> storage)
		{
			childNodes.forEach((node) -> node.stop(mobIn, storage));
		}
		
		public static enum Style
		{
			AND,
			OR;
		}
	}
	
	/** Performs its descendant node under specific conditions */
	public static class Decorator extends CompoundNode
	{
		private final Predicate<Pair<Mob, Whiteboard<?>>> condition;
		
		public Decorator(String nameIn, Predicate<Pair<Mob, Whiteboard<?>>> conditionIn, Node childIn)
		{
			super(nameIn, childIn);
			condition = conditionIn;
		}
		
		private Node child() { return childNodes.get(0); }
		
		public boolean canRun(Mob mobIn, Whiteboard<?> storage)
		{
			return condition.apply(Pair.of(mobIn, storage)) && child().canRun(mobIn, storage);
		}
		
		public boolean isRunning() { return child().isRunning(); }
		
		public void setRunning(boolean input) { this.child().setRunning(input); }
		
		public void stop(Mob mobIn, Whiteboard<?> storage) { this.child().stop(mobIn, storage); }
		
		public Node getActiveNode() { return isRunning() ? child() : null; }
		
		protected void run(Mob mobIn, Whiteboard<?> storage){ child().tick(mobIn, storage); }
	}
	
	/** Performs an action when ticked */
	public static class Leaf extends Node
	{
		public Leaf(String nameIn) { super(nameIn); }
		
		public boolean canRun(Mob mobIn, Whiteboard<?> storage){ return true; }
		
		protected void run(Mob mobIn, Whiteboard<?> storage){ }
	}
	
	/** Single-run version of Leaf */
	public static abstract class LeafSingle extends Leaf
	{
		public LeafSingle(String nameIn) { super(nameIn); }
		
		protected final void start(Mob mobIn, Whiteboard<?> storage) { doAction(mobIn, storage); resetNode(); }
		
		protected abstract void doAction(Mob mobIn, Whiteboard<?> storage);
	}
}