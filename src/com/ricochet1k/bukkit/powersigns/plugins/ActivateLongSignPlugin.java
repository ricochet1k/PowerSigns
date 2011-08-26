package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.ricochet1k.bukkit.powersigns.PowerSigns;
import com.ricochet1k.bukkit.powersigns.utils.BlockLine;

public class ActivateLongSignPlugin extends AimedSign2
{
	public static void register() {
		PowerSigns.register("activatelong", AimedSign.syntax, new ActivateLongSignPlugin());
	}
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm,
			BlockFace signDir, BlockFace forward, Block startBlock)
	{
		BlockLine line = new BlockLine(startBlock, forward);
		line.skipEmpty();
		if (PowerSigns.materialsMatch(line.getNextBlock().getType(), PowerSigns.signMaterials))
		{
			if (plugin.tryPowerSign(line.getNextBlock()))
				return true;
			else
				return plugin.debugFail("sign failed");
		}
		return plugin.debugFail("no match: "+line.getNextBlock().getType().toString());
	}

}
