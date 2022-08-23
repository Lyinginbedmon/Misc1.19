package com.example.examplemod.entity.ai;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;

import com.example.examplemod.entity.ai.SelectorSmart.TreeNodeSmart;
import com.example.examplemod.init.ExRegistries;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;

public class SelectorSmartItem extends TreeNode
{
	private TreeNodeSmart[] nodes = new TreeNodeSmart[0];
	private Map<Integer, Integer> sortMap = new HashMap<>();
	protected int index = -1;
	
	public SelectorSmartItem(){ }
	
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
		compound.putInt("Index", this.index);
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
		this.index = compound.getInt("Index");
		ListTag sortList = compound.getList("SortMap", 10);
		for(int i=0; i<sortList.size(); i++)
		{
			CompoundTag data = sortList.getCompound(i);
			sortMap.put(data.getInt("Key"), data.getInt("Index"));
		}
	}
	
	@Nullable
	protected TreeNode getCurrentNode() { return index >= 0 && nodes.length > 0 ? nodes[index%nodes.length].branch() : null; }
	
	public Status tick(PathfinderMob mob, Whiteboard<?> storage)
	{
		if(index < 0)
		{
			populateNodes(mob);
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
			TreeNode node = getCurrentNode();
			if(node == null)
				return Status.FAILURE;
			
			Status result = node.doTick(mob, storage);
			if(result == Status.RUNNING)
				return Status.RUNNING;
			else
			{
				index = -1;
				return result;
			}
		}
	}
	
	public void populateNodes(PathfinderMob mob)
	{
		List<TreeNodeSmart> smartNodes = Lists.newArrayList();
		boolean needsMelee = false;
		for(EquipmentSlot slot : EquipmentSlot.values())
		{
			ItemStack item = mob.getItemBySlot(slot);
			if(!item.isEmpty() && ExRegistries.ITEM_BRANCH_REGISTRY.containsKey(item.getItem()))
			{
				smartNodes.add(ExRegistries.ITEM_BRANCH_REGISTRY.get(item.getItem()));
			}
			else if(slot.getType() == EquipmentSlot.Type.HAND)
				needsMelee = true;
		}
		if(needsMelee)
			smartNodes.add(ExRegistries.BASIC_MELEE);
		
		this.nodes = smartNodes.toArray(new TreeNodeSmart[0]);
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
	
	protected void stop(PathfinderMob mob, Whiteboard<?> storage)
	{
		if(getCurrentNode() != null)
			getCurrentNode().stop(mob, storage);
	}
}
