package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class DataAccessSignPlugin extends AimedSign
{
	public static void register() {
		DataAccessSignPlugin dasp = new DataAccessSignPlugin();
		PowerSigns.register("dataset", "[u|d][@(0-99)]", dasp);
		PowerSigns.register("dataget", "[u|d][@(0-99)]", dasp);
	}
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm,
			BlockFace signDir, BlockFace forward, Block startBlock)
	{
		Sign signState = (Sign) signBlock.getState();
		
		if (action.equals("dataset"))
		{
			startBlock.setData(Byte.parseByte(signState.getLine(1)));
		}
		else if (action.equals("dataget"))
		{
			signState.setLine(1, Byte.toString(startBlock.getData()));
			plugin.updateSignState(signState);
		}
		
		return true;
	}

}
