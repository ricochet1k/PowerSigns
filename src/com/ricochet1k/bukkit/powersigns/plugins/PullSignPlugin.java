package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

import com.ricochet1k.bukkit.powersigns.PowerSigns;
import com.ricochet1k.bukkit.powersigns.utils.BlockLine;

public class PullSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		PowerSigns.register("pull", "[0-99][*(0-99)] [u|d]", new PullSignPlugin());
	}
	
	static final Pattern argsPattern = Pattern.compile(
			PowerSigns.join("(?:\\s+(\\d{1,2}))?(?:\\s*\\*(\\d{1,3}))?", PowerSigns.verticalPart/*, PowerSigns.moveDirPart*/),
			Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Matcher m = argsPattern.matcher(args);
		if (!m.matches()) return plugin.debugFail("syntax");
		
		Sign signState = (Sign) signBlock.getState();
		
		int count = (m.group(1) != null) ? Integer.parseInt(m.group(1)) : -1;
		int repeat = (m.group(2) != null) ? Integer.parseInt(m.group(2)) : 1;
		if (repeat <= 0) return plugin.debugFail("bad repeat");

		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		BlockFace forward = PowerSigns.getForward(signDir, m.group(3));
		Block startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward).getFace(forward, 1);
		//BlockFace direction = PowerSigns.getDirection(m.group(4), signDir, m.group(3));

		Material[] moveTypes = PowerSigns.getMaterials(signState.getLine(1));
		
		if (moveTypes == null || moveTypes.length == 0)
			return plugin.debugFail("Bad Mats: "+signState.getLine(1));
		
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
			return plugin.debugFail("not enough space");

		//Block blockToPull = line.nextBlock;

		numToPull = materialLine.matches(moveTypes).count((count == -1)? PowerSigns.maxDistance : count);
		if (numToPull == 0)
		{ return plugin.debugFail("nothing to pull"); }
		if (count != -1)
		{
			if (numToPull != -1)
			{ return plugin.debugFail("cant pull exact"); }
			numToPull = count;
		}


		materialLine.moveTo(new BlockLine(materialLine.getNextBlock().getFace(forward, -1 * repeat), forward), numToPull);

		return true;
	}

}
