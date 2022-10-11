package com.example.examplemod.entity.ai.tree;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.tree.TreeNode.CompoundNode;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;

public class SelectorSmart extends CompoundNode
{
	private final TreeNodeSmart[] nodes;
	private Map<Integer, Integer> sortMap = new HashMap<>();
	
	public SelectorSmart(TreeNodeSmart... nodesIn)
	{
		super(smartToArray(nodesIn));
		this.nodes = nodesIn;
		for(int i=0; i<this.nodes.length; i++)
			this.nodes[i].index = i;
	}
	
	protected static TreeNode[] smartToArray(TreeNodeSmart... nodesIn)
	{
		List<TreeNode> children = Lists.newArrayList();
		for(int i=0; i<nodesIn.length; i++)
			children.add(nodesIn[i].branch());
		
		return children.toArray(new TreeNode[0]);
	}
	
	protected void save(CompoundTag compound)
	{
		super.save(compound);
		ListTag sortList = new ListTag();
		for(int val : sortMap.keySet())
		{
			CompoundTag data = new CompoundTag();
			data.putInt("Key", val);
			data.putInt("Index", sortMap.get(val));
			sortList.add(data);
		}
		compound.put("SortMap", sortList);
	}
	
	protected void load(CompoundTag compound)
	{
		super.load(compound);
		ListTag sortList = compound.getList("SortMap", Tag.TAG_COMPOUND);
		for(int i=0; i<sortList.size(); i++)
		{
			CompoundTag data = sortList.getCompound(i);
			sortMap.put(data.getInt("Key"), data.getInt("Index"));
		}
	}
	
	public Status tick(PathfinderMob mob, Whiteboard<?> storage)
	{
		if(index < 0)
		{
			evaluateNodes(mob, storage);
			for(int i=0; i<sortMap.size(); i++)
			{
				int mainIndex = sortMap.get(i);
				TreeNode node = this.nodes[mainIndex].branch();
				Status result = node.doTick(mob, storage);
				if(result != Status.FAILURE)
				{
					index = mainIndex;
					return result;
				}
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
				index = -1;
				return result;
			}
		}
	}
	
	public void evaluateNodes(PathfinderMob mob, Whiteboard<?> storage)
	{
		List<TreeNodeSmart> nodeList = Lists.newArrayList();
		for(TreeNodeSmart smart : nodes)
			nodeList.add(smart);
		
		RandomSource rand = mob.getRandom();
		nodeList.sort(new Comparator<TreeNodeSmart>()
		{
			public int compare(TreeNodeSmart o1, TreeNodeSmart o2)
			{
				/**
				 * If scores are even, randomise sequence
				 */
				float score1 = Mth.clamp(o1.score(mob, storage), 0F, 1F);
				float score2 = Mth.clamp(o2.score(mob, storage), 0F, 1F);
				return score1 > score2 ? -1 : score1 < score2 ? 1 : rand.nextInt(3) - 1;
			}
		});
		
		this.sortMap.clear();
		for(int i=0; i<nodeList.size(); i++)
			this.sortMap.put(i, nodeList.get(i).index);
	}
	
	public static class TreeNodeSmart
	{
		private final TreeNode node;
		private final SmartNode predicate;
		
		public int index = 0;
		
		private TreeNodeSmart(SmartNode predicateIn, TreeNode nodeIn)
		{
			this.node = nodeIn;
			this.predicate = predicateIn;
		}
		
		public static TreeNodeSmart smart(SmartNode predicate, TreeNode branch) { return new TreeNodeSmart(predicate, branch); }
		
		public float score(PathfinderMob mob, Whiteboard<?> storage) { return predicate.utility(mob, storage); }
		
		public TreeNode branch() { return node; }
	}
	
	@FunctionalInterface
	public static interface SmartNode
	{
		public float utility(PathfinderMob mob, Whiteboard<?> storage);
	}
}
