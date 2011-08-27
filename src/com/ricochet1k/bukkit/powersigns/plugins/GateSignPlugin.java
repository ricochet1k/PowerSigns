package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class GateSignPlugin extends ArgsSign
{
	public GateSignPlugin()
	{
		super("\\s+(?:" + "(?:([<>])?([1-4]))" + "|" + "(NOT|N?AND|N?OR|XN?OR)" + ")");
		// TODO Auto-generated constructor stub
	}
	
	public static void register()
	{
		PowerSigns.register("gate", "[<>](1-4) | NOT | [N]AND | [N]OR | X[N]OR", new GateSignPlugin(), true);
	}

	public static final BlockFace[] wallSignAdjacents = new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
        BlockFace.DOWN };
	
	public static final BlockFace[] signPostAdjacents = new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
        BlockFace.DOWN };
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm, Boolean isOn)
	{
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		final Block output = signBlock.getRelative(signDir, signBlock.getType() == Material.WALL_SIGN ? 2 : 1);
		
		if (output.getType() != Material.LEVER)
			return plugin.debugFail("output must be LEVER not " + output.getType());
		
		boolean currentOutput = (output.getData() & 8) != 0;
		
		int totalInputs = 0;
		int onInputs = 0;
		
		for (int i = 0; i < 5; i++)
		{
			Block input = null;
			switch (i)
			{
			case 0:
				input = signBlock.getRelative(BlockFace.NORTH);
				break;
			case 1:
				input = signBlock.getRelative(BlockFace.SOUTH);
				break;
			case 2:
				input = signBlock.getRelative(BlockFace.EAST);
				break;
			case 3:
				input = signBlock.getRelative(BlockFace.WEST);
				break;
			case 4:
				if (signBlock.getType() == Material.WALL_SIGN)
					input = signBlock.getRelative(BlockFace.DOWN);
				else
					input = signBlock.getRelative(BlockFace.DOWN, 2);
				break;
			}
			
			if (input.equals(output))
				continue;
			
			switch (input.getType())
			{
			case REDSTONE_WIRE:
			case REDSTONE_TORCH_OFF:
			case REDSTONE_TORCH_ON:
				totalInputs ++;
				if (input.getBlockPower() > 0)
					onInputs ++;
			}
		}
		
		String gate = argsm.group(3);
		
		boolean out;
		
		if (gate != null) // gate
		{
			gate = gate.toLowerCase();
			
			boolean invert;
			
			if (gate.startsWith("x")) // xor
			{
				out = (onInputs == 1);
				invert = gate.startsWith("xn");
			}
			else
			{
				invert = gate.startsWith("n");
				if (gate.equals("not") || gate.endsWith("and")) // and
					out = (onInputs == totalInputs);
				else // or
					out = (onInputs >= 1);
			}
			if (invert)
				out = !out;
		}
		else
		{
			int requiredInputs = Integer.parseInt(argsm.group(2));
			
			if (argsm.group(1) != null)
			{
				if (argsm.group(1).equals("<"))
					out = (onInputs < requiredInputs);
				else //argsm.group(1).equals(">");
					out = (onInputs > requiredInputs);
			}
			else
				out = (onInputs == requiredInputs);
		}
		
		//PowerSigns.log.info("Gate: " + currentOutput + " -> " + out + "  " + onInputs + "/" + totalInputs);
		
		if (currentOutput != out)
		{
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
				{
					@Override
					public void run()
					{
						output.setData((byte) (output.getData() ^ 8), true);
					}
				}, 1);
		}
		
		return true;
	}

}
