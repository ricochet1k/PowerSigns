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
		super("(?:\\s+(\\d{1,2}))?(?:\\s*\\*(\\d{1,3}))?(?:\\s*([v^<>]))?");
	}

	public static void register() {
		MoveBlocksSignPlugin mbsp = new MoveBlocksSignPlugin();
		PowerSigns.register("pull", "[u|d][@(0-99)] [0-99][*(0-99)] [^v<>]", mbsp);
		PowerSigns.register("push", "[u|d][@(0-99)] [0-99][*(0-99)] [^v<>]", mbsp);
	}
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm, Boolean isOn,
			BlockFace signDir, BlockFace forward, Block startBlock)
	{
		int count = (argsm.group(1) != null) ? Integer.parseInt(argsm.group(1)) : -1;
		int repeat = (argsm.group(2) != null) ? Integer.parseInt(argsm.group(2)) : 1;
		if (repeat <= 0) return true; // repeat 0 means do nothing
		
		String perpStr = argsm.group(3);

		Sign signState = (Sign) signBlock.getState();
		
		Material[] moveTypes = PowerSigns.getMaterials(signState.getLine(1));
		
		if (moveTypes == null || moveTypes.length == 0)
			return plugin.debugFail("Bad Mats: "+signState.getLine(1));
		
		if (perpStr != null)
		{
			BlockFace perpDir = PowerSigns.getDirection(perpStr, signDir, forward);
			return perpMoveLine(plugin, startBlock, forward, moveTypes, count, repeat, action.equals("push"), perpDir);
		}
		else if (action.equals("push"))
			return pushLine(plugin, startBlock, forward, moveTypes, count, repeat);
		else //if (action.equals("pull"))
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
			return plugin.debugFail("not enough space (" + numEmpty + ") " + materialLine.getNextBlock().getType());

		numToPull = materialLine.matches(moveTypes).count((count == -1)? PowerSigns.maxDistance : count);
		if (numToPull == 0)
			return plugin.debugFail("nothing to pull (" + numToPull + ") " + materialLine.getNextBlock().getType());
		if (count != -1)
		{
			if (numToPull != -1)
				return plugin.debugFail("cant pull exact (" + numToPull + ") " + materialLine.getNextBlock().getType());
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
				return plugin.debugFail("cant push exact (" + numToPush + ") " + materialLine.getNextBlock().getType());
			numToPush = count;
		}
		if (numToPush < 1)
			return plugin.debugFail("nothing to push (" + numToPush + ") " + materialLine.getNextBlock().getType());
		


		BlockLine emptyLine = materialLine.copy();
		
		numEmpty = emptyLine.skipEmpty(repeat);

		if (numEmpty != -1) // -1 means it hit the max, anything else means it didn't find enough
			return plugin.debugFail("not enough space (" + numEmpty + ") " + emptyLine.getNextBlock().getType());


		materialLine.flip().moveTo(emptyLine.flip(), numToPush);

		return true;
	}

	public static boolean perpMoveLine(PowerSigns plugin, Block startBlock, BlockFace forward, Material[] moveTypes, int count, int repeat, boolean pushing, BlockFace perpDir)
	{
		assert repeat > 0;

		int numToMove = 0;
		
		Block fromBlock, toBlock;
		
		BlockLine fromLine, toLine;
		
		if (pushing)
		{
			fromBlock = startBlock;
			toBlock = startBlock.getRelative(perpDir, repeat);
			perpDir = perpDir.getOppositeFace();
		}
		else
		{
			toBlock = startBlock;
			fromBlock = startBlock.getRelative(perpDir, repeat);
		}
		
		//PowerSigns.log.info("Perp move: " + pushing + " " + perpDir + " " + forward);
		
		fromLine = new BlockLine(fromBlock, forward);
		toLine = new BlockLine(toBlock, forward);

		int maxCount = (count == -1)? PowerSigns.maxDistance : count;
		
		numToMove = fromLine.matches(moveTypes).count(maxCount);
		if (numToMove == 0)
			return plugin.debugFail("nothing to move");
		if (count != -1)
		{
			if (numToMove != -1)
				return plugin.debugFail("not enough matching ("+numToMove+")");
			numToMove = count;
		}
		
		// make sure all the lines in-between are empty, including the toLine
		for (int i = 0; i < repeat; i++)
		{
			BlockLine checkLine = new BlockLine(toBlock.getRelative(perpDir, i), forward).empties();
			int numEmpties = checkLine.count(numToMove);
			if (numEmpties != -1)
			{
				if (count != -1)
					return plugin.debugFail("not enough empty, line " + i + " ("+numEmpties+") " + checkLine.getNextBlock().getType());
				else
				{
					numToMove = Math.min(numToMove, numEmpties);
					if (numToMove == 0)
						return plugin.debugFail("no space to move");
				}
			}
		}
		
		//PowerSigns.log.info("Moving: " + numToMove);

		fromLine.moveTo(toLine, numToMove);

		return true;
	}
	
}
