package com.lying.misc19.init;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.apache.commons.compress.utils.Lists;

import com.lying.misc19.Misc19;
import com.lying.misc19.magic.ComponentCircle;
import com.lying.misc19.magic.ComponentGlyph;
import com.lying.misc19.magic.ISpellComponent;
import com.lying.misc19.magic.ISpellComponent.Category;
import com.lying.misc19.magic.ISpellComponentBuilder;
import com.lying.misc19.magic.component.ComparisonGlyph;
import com.lying.misc19.magic.component.FunctionGlyph;
import com.lying.misc19.magic.component.OperationGlyph;
import com.lying.misc19.magic.component.RootGlyph;
import com.lying.misc19.magic.component.StackGlyph;
import com.lying.misc19.magic.component.VariableGlyph;
import com.lying.misc19.magic.component.VectorGlyph;
import com.lying.misc19.magic.variable.IVariable;
import com.lying.misc19.magic.variable.VarBool;
import com.lying.misc19.magic.variable.VarStack;
import com.lying.misc19.magic.variable.VarVec;
import com.lying.misc19.magic.variable.VariableSet;
import com.lying.misc19.magic.variable.VariableSet.Slot;
import com.lying.misc19.reference.Reference;

import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

public class Components
{
	public static final ResourceKey<Registry<ISpellComponentBuilder>> REGISTRY_KEY				= ResourceKey.createRegistryKey(new ResourceLocation(Reference.ModInfo.MOD_ID, "components"));
	public static final DeferredRegister<ISpellComponentBuilder> COMPONENTS					= DeferredRegister.create(REGISTRY_KEY, Reference.ModInfo.MOD_ID);
	public static final Supplier<IForgeRegistry<ISpellComponentBuilder>> COMPONENTS_REGISTRY	= COMPONENTS.makeRegistry(() -> (new RegistryBuilder<ISpellComponentBuilder>()).hasTags());
	
	/**
	 * Every arrangement starts at a ROOT glyph<br>
	 * The ROOT populates the variables with specific constant values, according to its type.<br>
	 * The ROOT is then surrounded by a CIRCLE, which contains other GLYPHS and CIRCLES to perform functions.<br>
	 * The ROOT is what is called by spell objects during execution, and extracts the needed mana from the caster.<br>
	 * Arrangements have a hard limit on how many glyphs can be executed per execution call, after which no more glyphs will be run.<br>
	 */
	
	// Root glyphs
	public static final ResourceLocation ROOT_DUMMY = make("dummy_root");
	public static final ResourceLocation ROOT_CASTER = make("caster_root");
	public static final ResourceLocation ROOT_TARGET = make("target_root");
	public static final ResourceLocation ROOT_POSITION = make("position_root");
	
	// Circles
	public static final ResourceLocation CIRCLE_BASIC = make("basic_circle");
	public static final ResourceLocation CIRCLE_STEP = make("step_circle");
	
	// Constants
	public static final ResourceLocation GLYPH_FALSE = make("false_glyph");
	public static final ResourceLocation GLYPH_TRUE = make("true_glyph");
	public static final ResourceLocation GLYPH_2 = make("two_glyph");
	public static final ResourceLocation GLYPH_4 = make("four_glyph");
	public static final ResourceLocation GLYPH_8 = make("eight_glyph");
	public static final ResourceLocation GLYPH_16 = make("sixteen_glyph");
	public static final ResourceLocation GLYPH_PI = make("pi_glyph");
	public static final ResourceLocation GLYPH_XYZ = make("xyz_glyph");
	
	public static final ResourceLocation GLYPH_DUMMY = make("dummy_glyph");
	
	// Arithmetic operations
	public static final ResourceLocation GLYPH_SET = make("set_glyph");
	public static final ResourceLocation GLYPH_ADD = make("add_glyph");
	public static final ResourceLocation GLYPH_SUB = make("sub_glyph");
	public static final ResourceLocation GLYPH_MUL = make("mul_glyph");
	public static final ResourceLocation GLYPH_DIV = make("div_glyph");
	
	// Boolean operations
	public static final ResourceLocation GLYPH_EQU = make("equals_glyph");
	public static final ResourceLocation GLYPH_AND = make("and_glyph");
	public static final ResourceLocation GLYPH_OR = make("or_glyph");
	public static final ResourceLocation GLYPH_XOR = make("xor_glyph");
	public static final ResourceLocation GLYPH_NAND = make("not_glyph");
	public static final ResourceLocation GLYPH_GRE = make("greater_glyph");
	public static final ResourceLocation GLYPH_LES = make("less_glyph");
	
	// Vector operations
	public static final ResourceLocation GLYPH_MAKEVEC = make("make_vec_glyph");
	public static final ResourceLocation GLYPH_DOT = make("dot_glyph");
	public static final ResourceLocation GLYPH_CROSS = make("cross_glyph");
	public static final ResourceLocation GLYPH_NORMALISE = make("normalize_glyph");
	public static final ResourceLocation GLYPH_LENGTH = make("length_glyph");
	// TODO Operation to rotate vectors
	
	// Stack operations
	public static final ResourceLocation GLYPH_STACK_GET = make("stack_get_glyph");
	public static final ResourceLocation GLYPH_STACK_ADD = make("stack_add_glyph");
	public static final ResourceLocation GLYPH_STACK_SUB = make("stack_sub_glyph");
	
	// Functions
	public static final ResourceLocation GLYPH_DEBUG = make("debug_glyph");
	
	public static ResourceLocation make(String path) { return new ResourceLocation(Reference.ModInfo.MOD_ID, path); }
	
	static
	{
		register(ROOT_DUMMY, () -> () -> new RootGlyph.Dummy());
		register(ROOT_CASTER, () -> () -> new RootGlyph.Self());
		register(ROOT_TARGET, () -> () -> new RootGlyph.Target());
		register(ROOT_POSITION, () -> () -> new RootGlyph.Position());
		
		register(CIRCLE_BASIC, () -> () -> new ComponentCircle.Basic());
		register(CIRCLE_STEP, () -> () -> new ComponentCircle.Step());
		
		register(GLYPH_FALSE, () -> () -> new VariableGlyph.Constant(VarBool.FALSE));
		register(GLYPH_TRUE, () -> () -> new VariableGlyph.Constant(VarBool.TRUE));
		register(GLYPH_2, () -> () -> VariableGlyph.Constant.doubleConst(2D));
		register(GLYPH_4, () -> () -> VariableGlyph.Constant.doubleConst(4D));
		register(GLYPH_8, () -> () -> VariableGlyph.Constant.doubleConst(8D));
		register(GLYPH_16, () -> () -> VariableGlyph.Constant.doubleConst(16D));
		register(GLYPH_PI, () -> () -> VariableGlyph.Constant.doubleConst(Math.PI));
		registerLocalVariables();
		registerVectorConstants();
		
		register(GLYPH_DUMMY, () -> () -> new ComponentGlyph.Dummy());
		
		register(GLYPH_SET, () -> () -> new OperationGlyph.Set());
		register(GLYPH_ADD, () -> () -> new OperationGlyph.Add());
		register(GLYPH_SUB, () -> () -> new OperationGlyph.Subtract());
		register(GLYPH_MUL, () -> () -> new OperationGlyph.Multiply());
		register(GLYPH_DIV, () -> () -> new OperationGlyph.Divide());
		
		register(GLYPH_EQU, () -> () -> new ComparisonGlyph.Equals());
		register(GLYPH_GRE, () -> () -> new ComparisonGlyph.Greater());
		register(GLYPH_LES, () -> () -> new ComparisonGlyph.Less());
		register(GLYPH_AND, () -> () -> new ComparisonGlyph.And());
		register(GLYPH_OR, () -> () -> new ComparisonGlyph.Or());
		register(GLYPH_NAND, () -> () -> new ComparisonGlyph.NAnd());
		register(GLYPH_XOR, () -> () -> new ComparisonGlyph.XOR());
		
		register(GLYPH_MAKEVEC, () -> () -> new VectorGlyph.Compose());
		register(GLYPH_DOT, () -> () -> new VectorGlyph.Dot());
		register(GLYPH_CROSS, () -> () -> new VectorGlyph.Cross());
		register(GLYPH_NORMALISE, () -> () -> new VectorGlyph.Normalise());
		register(GLYPH_LENGTH, () -> () -> new VectorGlyph.Length());
		
		register(GLYPH_STACK_GET, () -> () -> new StackGlyph.StackGet());
		register(GLYPH_STACK_ADD, () -> () -> new StackGlyph.StackAdd());
		register(GLYPH_STACK_SUB, () -> () -> new StackGlyph.StackSub());
		
		register(GLYPH_DEBUG, () -> () -> new FunctionGlyph.Debug());
	}
	
	private static RegistryObject<ISpellComponentBuilder> register(ResourceLocation nameIn, Supplier<ISpellComponentBuilder> miracleIn)
	{
		return COMPONENTS.register(nameIn.getPath(), miracleIn);
	}
	
	private static void registerLocalVariables()
	{
		for(VariableSet.Slot slot : VariableSet.Slot.values())
			register(slot.glyph(), () -> () -> Slot.makeGlyph(slot));
	}
	
	private static void registerVectorConstants()
	{
		List<IVariable> dirVariables = Lists.newArrayList();
		for(Direction dir : Direction.values())
		{
			Vec3i normal = dir.getNormal();
			IVariable dirVar = new VarVec(new Vec3(normal.getX(), normal.getY(), normal.getZ()));
			register(make(dir.getSerializedName()+"_glyph"), () -> () -> new VariableGlyph.Constant(dirVar));
			dirVariables.add(dirVar);
		}
		
		register(GLYPH_XYZ, () -> () -> new VariableGlyph.Constant(new VarStack(dirVariables.toArray(new IVariable[0]))));
	}
	
	public static ISpellComponent create(ResourceLocation registryName)
	{
		for(RegistryObject<ISpellComponentBuilder> entry : COMPONENTS.getEntries())
			if(entry.getId().equals(registryName))
			{
				ISpellComponent component = entry.get().create();
				component.setRegistryName(entry.getId());
				return component;
			}
		
		return create(GLYPH_DUMMY);
	}
	
	public static void reportInit(final FMLLoadCompleteEvent event)
	{
		Misc19.LOG.info("# Reporting registered spell components #");
			for(Category cat : Category.values())
			{
				List<ResourceLocation> components = Lists.newArrayList();
				for(RegistryObject<ISpellComponentBuilder> entry : COMPONENTS.getEntries())
					if(entry.get().create().category() == cat)
						components.add(entry.getId());
				
				if(!components.isEmpty())
				{
					Misc19.LOG.info("# Added "+components.size()+" "+cat.name());
					components.forEach((id) -> Misc19.LOG.info("# * "+id.toString()));
				}
			}
		Misc19.LOG.info("# "+COMPONENTS.getEntries().size()+" total components #");
		
		runComponentTests();
	}
	
	private static void runComponentTests()
	{
		Misc19.LOG.info("Running component tests");
		
		ISpellComponent testIndex = create(CIRCLE_BASIC).addInputs(create(GLYPH_XYZ)).addOutputs(create(GLYPH_SET).addInputs(create(Slot.INDEX.glyph())).addOutputs(create(Slot.BAST.glyph())));
		Misc19.LOG.info("Circle index test: "+((VariableGlyph)create(GLYPH_XYZ)).get(null).asDouble()+" runs = index "+testIndex.execute(new VariableSet()).get(Slot.BAST).asDouble());
		
		runArithmeticTests();
		runAdderTests();
	}
	
	private static void runArithmeticTests()
	{
		ISpellComponent testAdd = create(GLYPH_ADD).addInputs(create(GLYPH_4), create(GLYPH_2)).addOutputs(create(Slot.BAST.glyph()));
		Misc19.LOG.info("Add 4 + 2 test: "+testAdd.execute(new VariableSet()).get(Slot.BAST).asDouble());
		
		ISpellComponent testSub = create(GLYPH_SUB).addInputs(create(GLYPH_4), create(GLYPH_2)).addOutputs(create(Slot.BAST.glyph()));
		Misc19.LOG.info("Subtract 4 - 2 test: "+testSub.execute(new VariableSet()).get(Slot.BAST).asDouble());
		
		ISpellComponent testMul = create(GLYPH_MUL).addInputs(create(GLYPH_4), create(GLYPH_2)).addOutputs(create(Slot.BAST.glyph()));
		Misc19.LOG.info("Multiply 4 * 2 test: "+testMul.execute(new VariableSet()).get(Slot.BAST).asDouble());
		
		ISpellComponent testDiv = create(GLYPH_DIV).addInputs(create(GLYPH_4), create(GLYPH_2)).addOutputs(create(Slot.BAST.glyph()));
		Misc19.LOG.info("Divide 4 / 2 test: "+testDiv.execute(new VariableSet()).get(Slot.BAST).asDouble());
	}
	
	private static void runAdderTests()
	{
		runAdderTest(GLYPH_FALSE, GLYPH_FALSE, GLYPH_FALSE);
		runAdderTest(GLYPH_TRUE, GLYPH_FALSE, GLYPH_FALSE);
		runAdderTest(GLYPH_FALSE, GLYPH_TRUE, GLYPH_FALSE);
		runAdderTest(GLYPH_TRUE, GLYPH_TRUE, GLYPH_FALSE);
		runAdderTest(GLYPH_FALSE, GLYPH_FALSE, GLYPH_TRUE);
		runAdderTest(GLYPH_TRUE, GLYPH_FALSE, GLYPH_TRUE);
		runAdderTest(GLYPH_FALSE, GLYPH_TRUE, GLYPH_TRUE);
	}
	
	private static void runAdderTest(ResourceLocation bit0, ResourceLocation bit1, ResourceLocation bit2)
	{
		ISpellComponent circle = create(ROOT_DUMMY).addOutputs(create(CIRCLE_BASIC).addOutputs(
				create(GLYPH_SET).addInputs(create(bit0)).addOutputs(create(Slot.BAST.glyph())),
				create(GLYPH_SET).addInputs(create(bit1)).addOutputs(create(Slot.THOTH.glyph())),
				create(GLYPH_SET).addInputs(create(bit2)).addOutputs(create(Slot.SUTEKH.glyph())),
				create(GLYPH_XOR).addInputs(create(Slot.BAST.glyph()), create(Slot.THOTH.glyph())).addOutputs(create(Slot.ANUBIS.glyph())),
				create(GLYPH_AND).addInputs(create(Slot.ANUBIS.glyph()), create(Slot.SUTEKH.glyph())).addOutputs(create(Slot.HORUS.glyph())),
				create(GLYPH_AND).addInputs(create(Slot.BAST.glyph()), create(Slot.THOTH.glyph())).addOutputs(create(Slot.ISIS.glyph())),
				create(GLYPH_OR).addInputs(create(Slot.HORUS.glyph()), create(Slot.ISIS.glyph())).addOutputs(create(Slot.RA.glyph())),
				create(GLYPH_XOR).addInputs(create(Slot.ANUBIS.glyph()), create(Slot.SUTEKH.glyph())).addOutputs(create(Slot.OSIRIS.glyph()))));
		
		CompoundTag circleData = ISpellComponent.saveToNBT(circle);
		ISpellComponent circle2 = readFromNBT(circleData);
		
		VariableSet variables = new VariableSet();
		variables = circle2.execute(variables);
		
		boolean var0 = ((VariableGlyph)create(bit0)).get(null).asBoolean();
		boolean var1 = ((VariableGlyph)create(bit1)).get(null).asBoolean();
		boolean var2 = ((VariableGlyph)create(bit2)).get(null).asBoolean();
		
		boolean anubis = (var0 || var1) && var0 != var1;
		boolean osiris = (anubis || var2) && anubis != var2;
		boolean ra = (anubis && var2) || (var0 && var1);
		
		boolean testPassed = osiris == variables.get(Slot.OSIRIS).asBoolean() && ra == variables.get(Slot.RA).asBoolean();
		
		Misc19.LOG.info("Adder test: "+var0+" + "+var1+" ("+var2+") = "+variables.get(Slot.OSIRIS).asBoolean()+" ("+variables.get(Slot.RA).asBoolean()+") "+(testPassed ? "PASSED" : "FAILED"));
	}
	
	@Nonnull
	public static ISpellComponent readFromNBT(CompoundTag nbt)
	{
		ResourceLocation registryName = nbt.contains("ID", Tag.TAG_STRING) ? new ResourceLocation(Reference.ModInfo.MOD_ID, nbt.getString("ID")) : GLYPH_DUMMY;
		ISpellComponent component = create(registryName);
		
		if(nbt.contains("Position", Tag.TAG_LIST))
			component.setPosition(nbt.getList("Position", Tag.TAG_FLOAT).getFloat(0), nbt.getList("Position", Tag.TAG_FLOAT).getFloat(1));
		
		if(nbt.contains("Data", Tag.TAG_COMPOUND))
			component.deserialiseNBT(nbt.getCompound("Data"));
		
		if(nbt.contains("Input", Tag.TAG_LIST))
			nbt.getList("Input", Tag.TAG_COMPOUND).forEach((tag) -> component.addInput(readFromNBT((CompoundTag)tag)));
		
		if(nbt.contains("Output", Tag.TAG_LIST))
			nbt.getList("Output", Tag.TAG_COMPOUND).forEach((tag) -> component.addOutput(readFromNBT((CompoundTag)tag)));
		
		return component;
	}
}
