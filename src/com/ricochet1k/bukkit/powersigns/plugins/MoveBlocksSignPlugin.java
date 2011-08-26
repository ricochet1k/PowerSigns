package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

import com.ricochet1k.bukkit.powersigns.PowerSigns;
import com.ricochet1k.bukkit.powersigns.utils.BlockLine;

public class MoveBlocksSignPlugin extends AimedSign
{
	public MoveBlocksSignPlugin()
	{
		super("(?:\\s+(\\d{1,2}))?(?:\\s*\\*(\\d{1,3}))?");
	}

	public static void register() {
		MoveBlocksSignPlugin mbsp = new MoveBlocksSignPlugin();
		PowerSigns.register("pull", "[u|d][@(0-99)] [0-99][*(0-99)]", mbsp);
		PowerSigns.register("push", "[u|d][@(0-99)] [0-99][*(0-99)]", mbsp);
	}
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm,
			BlockFace signDir, BlockFace forward, Block startBlock)
	{
		int count = (argsm.group(1) != null) ? Integer.parseInt(argsm.group(1)) : -1;
		int repeat = (argsm.group(2) != null) ? Integer.parseInt(argsm.group(2)) : 1;
		if (repeat <= 0) return plugin.debugFail("bad repeat");

		Sign signState = (Sign) signBlock.getState();
		
		Material[] moveTypes = PowerSigns.getMaterials(signState.getLine(1));
		
		if (moveTypes == null || moveTypes.length == 0)
			return plugin.debugFail("Bad Mats: "+signState.getLine(1));
		
		if (action.equals("push"))
			return pushLine(plugin, startBlock, forward, moveTypes, count, repeat);
		else
			return pullLine(plugin, startBlock, forward, moveTypes, count, repeat);
	}
	
	public static boolean pullLine(PowerSigns plugin, Block startBlock, BlockFace forward, Material[] moveTypes, int count, int repeat)
	{
		assert repeat > 0;

		int numEmpty = 0;
		int numToPull = 0;
		
		
		BlockLine materialLine = new BlockLine(startBlock, forward);

		numEmpty = materialLine.skipEmpty();

		if (numEmpty < repeat)
			return plugin.debugFail("not enough space " + numEmpty);

		numToPull = materialLine.matches(moveTypes).count((count == -1)? PowerSigns.maxDistance : count);
		if (numToPull == 0)
			return plugin.debugFail("nothing to pull " + numToPull);
		if (count != -1)
		{
			if (numToPull != -1)
				return plugin.debugFail("cant pull exact " + numToPull);
			numToPull = count;
		}

		materialLine.moveTo(new BlockLine(materialLine.getNextBlock().getRelative(forward, -1 * repeat), forward), numToPull);

		return true;
	}
	
	public static boolean pushLine(PowerSigns plugin, Block startBlock, BlockFace forward, Material[] moveTypes, int count, int repeat)
	{
		assert repeat > 0;

		int numToPush = 0;
		int numEmpty = 0;

		BlockLine materialLine = new BlockLine(startBlock, forward);

		numToPush = materialLine.skipMatching(moveTypes, (count == -1)?PowerSigns.maxDistance:count);
		if (count != -1)
		{
			if (numToPush != -1)
				return plugin.debugFail("cant push exact " + numToPush);
			numToPush = count;
		}
		if (numToPush < 1)
			return plugin.debugFail("nothing to push "+ numToPush);
		


		BlockLine emptyLine = materialLine.copy();
		
		numEmpty = emptyLine.skipEmpty(repeat);

		if (numEmpty != -1) // -1 means it hit the max, anything else means it didn't find enough
			return plugin.debugFail("not enough space " + numEmpty);


		materialLine.flip().moveTo(emptyLine.flip(), numToPush);

		return true;
	}
}
