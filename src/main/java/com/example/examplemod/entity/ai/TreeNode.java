package com.example.examplemod.entity.ai;

import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;

public abstract class TreeNode
{
	private Status lastTick = Status.FAILURE;
	private String customName = "";
	
	private BehaviourTree parentTree;
	private boolean wasActive = false;
	private boolean hideDescendants = false;
	
	public boolean hasParentTree() { return this.parentTree != null; }
	public void setParentTree(BehaviourTree treeIn)
	{
		this.parentTree = treeIn;
		if(hasChildren())
			getChildren().forEach((child) -> child.setParentTree(treeIn));
	}
	public BehaviourTree getParentTree() { return this.parentTree; }
	public boolean wasActive() { return hasParentTree() && this.parentTree.nodeWasActiveLastTick(this) || wasActive; }
	
	public String getDisplayName() { return this.hasCustomName() ? getCustomName() : getClass().getSimpleName().toLowerCase(); }
	
	public boolean hasCustomName() { return this.customName.length() > 0; }
	public String getCustomName() { return this.customName; }
	public TreeNode setCustomName(String nameIn) { this.customName = nameIn; return this; }
	
	public boolean shouldLogChildren() { return !this.hideDescendants; }
	public TreeNode setDiscrete() { this.hideDescendants = true; return this; }
	
	public final Status doTick(PathfinderMob mob, Whiteboard<?> storage)
	{
		Status prev = lastTick;
		lastTick = tick(mob, storage);
		if(lastTick != prev)
		{
			if(lastTick.isEndState())
				stop(mob, storage);
		}
		if(lastTick != Status.FAILURE && hasParentTree())
			getParentTree().reportNodeActive(this);
		
		return lastTick;
	}
	
	protected Status tick(PathfinderMob mob, Whiteboard<?> storage) { return Status.FAILURE; }
	
	/** Performs all necessary cleanup when this node is stopped for any reason */
	protected void stop(PathfinderMob mob, Whiteboard<?> storage) { }
	
	/** Returns the last status reported by tick from this node */
	public Status previousResult() { return this.lastTick; }
	
	public final void saveToNBT(CompoundTag compound)
	{
		compound.putInt("LastTick", lastTick.ordinal());
		if(hasCustomName())
			compound.putString("CustomName", this.customName);
		compound.putBoolean("WasActive", wasActive());
		compound.putBoolean("Discrete", this.hideDescendants);
		save(compound);
	}
	
	public final void loadFromNBT(CompoundTag compound)
	{
		lastTick = Status.values()[compound.getInt("LastTick")];
		if(compound.contains("CustomName"))
			customName = compound.getString("CustomName");
		wasActive = compound.getBoolean("WasActive");
		hideDescendants = compound.getBoolean("Discrete");
		load(compound);
	}
	
	protected void save(CompoundTag compound) { }
	
	protected void load(CompoundTag compound) { }
	
	public boolean hasChildren() { return false; }
	public List<TreeNode> getChildren(){ return Lists.newArrayList(); }
	
	public static enum Status
	{
		FAILURE(true, ChatFormatting.RED.getColor()),
		SUCCESS(true, ChatFormatting.GREEN.getColor()),
		RUNNING(false, ChatFormatting.GOLD.getColor());
		
		private final boolean isEnd;
		private final int color;
		
		private Status(boolean isEndState, int colorIn)
		{
			isEnd = isEndState;
			color = colorIn;
		}
		
		public boolean isEndState() { return this.isEnd; }
		
		public int hudColor() { return this.color; }
	}
	
	public NodeMap map(int depth) { return new NodeMap(this, depth); }
	
	public static class NodeMap
	{
		public final TreeNode parent;
		private final int depth;
		private final List<NodeMap> childMaps = Lists.newArrayList();
		
		public NodeMap(TreeNode nodeIn, int depthIn)
		{
			parent = nodeIn;
			depth = depthIn;
			nodeIn.getChildren().forEach((child) -> childMaps.add(child.map(depth + 1)) );
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
		public String getLocalAddressOf(TreeNode nodeIn)
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
		
		public int depth() { return this.depth; }
		
		public void print()
		{
			String prefix = parent.hasParentTree() && parent.getParentTree().nodeWasActiveLastTick(parent) ? String.valueOf(parent.previousResult().name().charAt(0)) : "-";
			String nodeName = String.join(" ", prefix, parent.getDisplayName());
			
			System.out.println(new String(new char[depth]).replace("\0", "  ") + "> "+nodeName);
			if(parent.shouldLogChildren())
				childMaps.forEach((map) -> map.print());
		}
		
		public void printToLog(Logger logIn)
		{
			String prefix = parent.hasParentTree() && parent.getParentTree().nodeWasActiveLastTick(parent) ? String.valueOf(parent.previousResult().name().charAt(0)) : "-";
			String nodeName = String.join(" ", prefix, parent.getDisplayName());
			
			logIn.info(new String(new char[depth]).replace("\0", "  ") + "> "+nodeName);
			if(parent.shouldLogChildren())
				childMaps.forEach((map) -> map.printToLog(logIn));
		}
	}
	
	@FunctionalInterface
	public static interface NodePredicate
	{
		boolean test(PathfinderMob mob, Whiteboard<?> storage);
	}
	
	/** Pseudo-node that only returns SUCCESS or FAILURE based on a given predicate */
	public static class Condition extends TreeNode
	{
		private final NodePredicate condition;
		
		public static TreeNode alwaysTrue() { return new Condition((mob,storage) -> { return true; }).setCustomName("always_true"); }
		public static TreeNode alwaysFalse() { return new Condition((mob,storage) -> { return false; }).setCustomName("always_false"); }
		
		public Condition(NodePredicate conditionIn)
		{
			condition = conditionIn;
		}
		
		public final Status tick(PathfinderMob mob, Whiteboard<?> storage)
		{
			return condition.test(mob, storage) ? Status.SUCCESS : Status.FAILURE;
		}
	}
	
	protected static abstract class CompoundNode extends TreeNode
	{
		protected final TreeNode[] nodes;
		protected int index = -1;
		
		public CompoundNode(TreeNode... nodesIn)
		{
			nodes = nodesIn;
		}
		
		protected void save(CompoundTag compound)
		{
			compound.putInt("Index", this.index);
		}
		
		protected void load(CompoundTag compound)
		{
			this.index = compound.getInt("Index");
		}
		
		public boolean hasChildren() { return nodes.length > 0; }
		
		public List<TreeNode> getChildren()
		{
			List<TreeNode> list = Lists.newArrayList();
			for(TreeNode node : nodes)
				list.add(node);
			return list;
		}
		
		@Nullable
		protected TreeNode getCurrentNode() { return index >= 0 && hasChildren() ? nodes[index%nodes.length] : null; }
		
		protected void stop(PathfinderMob mob, Whiteboard<?> storage)
		{
			if(getCurrentNode() != null)
				getCurrentNode().stop(mob, storage);
		}
		
		protected void stopAllChildren(PathfinderMob mob, Whiteboard<?> storage) { getChildren().forEach((node) -> node.stop(mob, storage)); }
	}
	
	public static class Decorator extends CompoundNode
	{
		private Type type;
		private int runs;
		private int attempts = -1;
		
		protected Decorator(Type typeIn, int triesIn, TreeNode nodeIn)
		{
			super(new TreeNode[] {nodeIn});
			type = typeIn;
			runs = triesIn;
			index = 0;
			
			if(nodeIn instanceof Condition)
				this.setDiscrete();
		}
		
		protected void save(CompoundTag compound)
		{
			compound.putInt("Type", this.type.ordinal());
			if(this.type.needsLimit())
			{
				compound.putInt("Runs", this.runs);
				compound.putInt("Attempts", this.attempts);
			}
		}
		
		protected void load(CompoundTag compound)
		{
			this.type = Type.values()[compound.getInt("Type")];
			if(this.type.needsLimit())
			{
				this.runs = compound.getInt("Runs");
				this.attempts = compound.getInt("Attempts");
			}
		}
		
		/** Returns FAILURE as long as the child node does not return RUNNING */
		public static TreeNode forceFailure(TreeNode nodeIn) { return new Decorator(Type.FORCE_FAILURE, 0, nodeIn).setCustomName("force_failure"); }
		/** Returns SUCCESS as long as the child node does not return RUNNING */
		public static TreeNode forceSuccess(TreeNode nodeIn) { return new Decorator(Type.FORCE_SUCCESS, 0, nodeIn).setCustomName("force_success"); }
		/** Returns RUNNING as long as the child node does not return FAILURE */
		public static TreeNode forceRunning(TreeNode nodeIn) { return new Decorator(Type.FORCE_RUNNING, 0, nodeIn).setCustomName("force_running"); }
		/** Returns FAILURE if the child node returns SUCCESS and vice versa */
		public static TreeNode inverter(TreeNode nodeIn) { return new Decorator(Type.INVERTER, 0, nodeIn).setCustomName("inverter"); }
		/** Ticks the child node N times until it does not return SUCCESS */
		public static TreeNode repeat(int times, TreeNode nodeIn) { return new Decorator(Type.REPEAT, times, nodeIn).setCustomName("repeat_"+times); }
		/** Ticks the child node N times until it does not return FAILURE */
		public static TreeNode retry(int times, TreeNode nodeIn) { return new Decorator(Type.RETRY, times, nodeIn).setCustomName("retry_"+times); }
		
		@Nullable
		protected TreeNode getCurrentNode() { return nodes.length > 0 ? nodes[0] : null; }
		
		public Status tick(PathfinderMob mob, Whiteboard<?> storage)
		{
			TreeNode child = getCurrentNode();
			
			if(attempts < 0 && type.needsLimit())
				attempts = runs;
			
			switch(type)
			{
				case FORCE_FAILURE:
					if(child.doTick(mob, storage) == Status.RUNNING)
						return Status.RUNNING;
					return Status.FAILURE;
				case FORCE_SUCCESS:
					if(child.doTick(mob, storage) == Status.RUNNING)
						return Status.RUNNING;
					return Status.SUCCESS;
				case FORCE_RUNNING:
					if(child.doTick(mob, storage) == Status.FAILURE)
						return Status.FAILURE;
					return Status.RUNNING;
				case INVERTER:
					switch(child.doTick(mob, storage))
					{
						case SUCCESS:	return Status.FAILURE;
						case FAILURE:	return Status.SUCCESS;
						default:		return Status.RUNNING;
					}
				case REPEAT:
					if(child.doTick(mob, storage).isEndState())
						--attempts;
					
					return attempts > 0 ? Status.RUNNING : Status.SUCCESS;
				case RETRY:
					switch(child.doTick(mob, storage))
					{
						case SUCCESS:	return Status.SUCCESS;
						case FAILURE:	--attempts;
						default:		return Status.RUNNING;
					}
			}
			return Status.FAILURE;
		}
		
		protected void stop(PathfinderMob mob, Whiteboard<?> storage)
		{
			super.stop(mob, storage);
			if(type.needsLimit())
				attempts = -1;
		}
		
		private static enum Type
		{
			INVERTER(false),
			FORCE_SUCCESS(false),
			FORCE_FAILURE(false),
			FORCE_RUNNING(false),
			REPEAT(true),
			RETRY(true);
			
			private final boolean needsCount;
			
			private Type(boolean countIn)
			{
				needsCount = countIn;
			}
			
			public boolean needsLimit() { return this.needsCount; }
		}
	}
	
	public static class Selector extends CompoundNode
	{
		private Type type;
		
		protected Selector(Type typeIn, TreeNode... nodesIn)
		{
			super(nodesIn);
			this.type = typeIn;
		}
		
		public static TreeNode root(TreeNode... nodesIn) { return new Selector(Type.SEQUENTIAL, nodesIn).setCustomName("root"); }
		/** Returns a selector which will check its descendant nodes in the order they were entered */
		public static TreeNode sequential(TreeNode... nodesIn) { return new Selector(Type.SEQUENTIAL, nodesIn); }
		/** Returns a selector which will check its descendant nodes in a random order */
		public static TreeNode random(TreeNode... nodesIn) { return new Selector(Type.RANDOM, nodesIn); }
		
		protected void save(CompoundTag compound)
		{
			super.save(compound);
			compound.putInt("Type", this.type.ordinal());
		}
		
		protected void load(CompoundTag compound)
		{
			super.load(compound);
			this.type = Type.values()[compound.getInt("Type")];
		}
		
		public Status tick(PathfinderMob mob, Whiteboard<?> storage)
		{
			if(index < 0)
			{
				switch(type)
				{
					case SEQUENTIAL:
						for(int i=0; i<nodes.length; i++)
						{
							Status result = nodes[i].doTick(mob, storage);
							if(result != Status.FAILURE)
							{
								index = i;
								return result;
							}
						}
						break;
					case RANDOM:
						List<Integer> indices = Lists.newArrayList();
						for(int i=0; i<nodes.length; i++)
							indices.add(i);
						
						RandomSource rand = mob.getRandom();
						int check = rand.nextInt(indices.size());
						Status result = Status.FAILURE;
						while(!indices.isEmpty() && (result = nodes[check].doTick(mob, storage)) == Status.FAILURE)
						{
							indices.remove(check);
							check = rand.nextInt(indices.size());
						}
						
						if(result != Status.FAILURE)
						{
							index = check;
							return result;
						}
						
						break;
				}
				return Status.FAILURE;
			}
			else
			{
				Status result = getCurrentNode().doTick(mob, storage);
				if(result == Status.RUNNING)
					return Status.RUNNING;
				else
				{
					getCurrentNode().stop(mob, storage);
					index = -1;
					return result;
				}
			}
		}
		
		private static enum Type
		{
			SEQUENTIAL,
			RANDOM;
		}
	}
	
	/** Returns a reactive sequence with the given node and a condition of the given predicate */
	public static TreeNode conditional(NodePredicate predicate, TreeNode child)
	{
		return Sequence.reactive(new Condition(predicate), child);
	}
	
	public static class Sequence extends CompoundNode
	{
		private Type type;
		
		protected Sequence(Type typeIn, TreeNode... nodesIn)
		{
			super(nodesIn);
			index = 0;
			type = typeIn;
		}
		
		/** Moves through child nodes until either last node returns SUCCESS or current node returns FAILURE */
		public static TreeNode sequence(TreeNode... nodesIn) { return new Sequence(Type.SEQUENCE, nodesIn).setCustomName("sequence"); }
		/** Runs all child nodes simultaneously until one returns FAILURE */
		public static TreeNode reactive(TreeNode... nodesIn) { return new Sequence(Type.REACTIVE, nodesIn).setCustomName("reactive_sequence"); }
		/** Moves through child nodes until every node has returned SUCCESS, even if it has to run failed nodes repeatedly */
		public static TreeNode star(TreeNode... nodesIn) { return new Sequence(Type.STAR, nodesIn).setCustomName("sequence_star"); }
		
		protected void save(CompoundTag compound)
		{
			super.save(compound);
			compound.putInt("Type", this.type.ordinal());
		}
		
		protected void load(CompoundTag compound)
		{
			super.load(compound);
			this.type = Type.values()[compound.getInt("Type")];
		}
		
		public Status tick(PathfinderMob mob, Whiteboard<?> storage)
		{
			if(type == Type.REACTIVE)
			{
				boolean failed = false;
				for(TreeNode child : nodes)
				{
					if(child.doTick(mob, storage) == Status.FAILURE)
					{
						failed = true;
						break;
					}
				}
				
				if(failed)
					for(TreeNode child : nodes) child.stop(mob, storage);
				return failed ? Status.FAILURE : Status.RUNNING;
			}
			else
				switch(getCurrentNode().doTick(mob, storage))
				{
					case FAILURE:
						getCurrentNode().stop(mob, storage);
						switch(type)
						{
							case STAR:
								return Status.RUNNING;
							default:
								return Status.FAILURE;
						}
					case SUCCESS:
						getCurrentNode().stop(mob, storage);
						if(++index >= nodes.length)
							return Status.SUCCESS;
						else
							return Status.RUNNING;
					default:
						return Status.RUNNING;
				}
		}
		
		protected void stop(PathfinderMob mob, Whiteboard<?> storage)
		{
			super.stop(mob, storage);
			index = 0;
		}
		
		private enum Type
		{
			SEQUENCE,
			REACTIVE,
			STAR;
		}
	}
	
	/** Runs all child nodes in parallel until one reports FAILURE or SUCCESS */
	public static class Parallel extends CompoundNode
	{
		protected Parallel(TreeNode... nodesIn)
		{
			super(nodesIn);
		}
		
		/** Stops only when no remaining child returns RUNNING */
		public static TreeNode any(TreeNode... nodesIn) { return new Parallel(nodesIn).setCustomName("parallel_any"); }
		
		public Status tick(PathfinderMob mob, Whiteboard<?> storage)
		{
			if(previousResult().isEndState())
				return startAllChildren(mob, storage);
			
			Status result = Status.RUNNING;
			EnumSet<Status> results = EnumSet.noneOf(Status.class);
			for(TreeNode node : nodes)
			{
				if(!node.previousResult().isEndState())
					results.add(node.doTick(mob, storage));
			}
			
			if(!results.contains(Status.RUNNING))
				result = Status.SUCCESS;
			
			return result;
		}
		
		private Status startAllChildren(PathfinderMob mob, Whiteboard<?> storage)
		{
			Status output = Status.FAILURE;
			for(TreeNode node : nodes)
			{
				Status result = node.doTick(mob, storage);
				if(result.ordinal() > output.ordinal())
					output = result;
				
				if(output.isEndState())
					return output;
			}
			
			return output;
		}
		
		protected void stop(PathfinderMob mob, Whiteboard<?> storage) { stopAllChildren(mob, storage); }
	}
	
	/** Leaf node that performs a single-tick action and cannot return RUNNING */
	public static abstract class LeafSingle extends TreeNode
	{
		public final Status tick(PathfinderMob mob, Whiteboard<?> storage)
		{
			return doAction(mob, storage) ? Status.SUCCESS : Status.FAILURE;
		}
		
		public abstract boolean doAction(PathfinderMob mob, Whiteboard<?> storage);
	}
	
	/** Leaf node that performs an action over multiple ticks */
	public static abstract class LeafRunning extends TreeNode
	{
		public final Status tick(PathfinderMob mob, Whiteboard<?> storage)
		{
			if(previousResult().isEndState())
				return start(mob, storage) ? Status.RUNNING : Status.FAILURE;
			else
				return run(mob, storage);
		}
		
		/**
		 * Called when the previous result of this node is an end state.<br>
		 * Ie. when this call represents the first of a new run cycle
		 */
		protected boolean start(PathfinderMob mob, Whiteboard<?> storage) { return true; }
		/** Called when this node has already started the current run cycle */
		protected abstract Status run(PathfinderMob mob, Whiteboard<?> storage);
	}
}
