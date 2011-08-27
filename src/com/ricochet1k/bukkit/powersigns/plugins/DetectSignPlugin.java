package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class DetectSignPlugin extends AimedSign
{
	public static void register() {
		PowerSigns.register("detect", "[u|d][@(0-99)]", new DetectSignPlugin());
	}
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm, Boolean isOn,
			BlockFace signDir, BlockFace forward, Block startBlock)
	{
		Sign signState = (Sign) signBlock.getState();
		
		signState.setLine(1, startBlock.getType().toString().toLowerCase());
		signState.setLine(2, Integer.toString(startBlock.getType().getId()));
		plugin.updateSignState(signState);
		return true;
	}

}
