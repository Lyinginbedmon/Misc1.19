package com.example.examplemod.entity;

import com.example.examplemod.entity.ai.Whiteboard;
import com.example.examplemod.entity.ai.tree.BehaviourTree;

import net.minecraft.world.entity.PathfinderMob;

public interface ITreeEntity
{
	public Whiteboard<PathfinderMob> getWhiteboard(PathfinderMob mobIn);
	
	public BehaviourTree getTree();
}
