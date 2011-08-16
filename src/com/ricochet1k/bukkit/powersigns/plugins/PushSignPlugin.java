package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

import com.ricochet1k.bukkit.powersigns.PowerSigns;
import com.ricochet1k.bukkit.powersigns.utils.BlockLine;

public class PushSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		PowerSigns.register("push", "[u|d][@(0-99)] [0-99][*(0-99)]", new PushSignPlugin());
	}
	
	static final Pattern argsPattern = Pattern.compile(
			PowerSigns.join(PowerSigns.verticalPart, PowerSigns.skipPart, "(?:\\s+(\\d{1,2}))?(?:\\s*\\*(\\d{1,3}))?" /*, PowerSigns.moveDirPart*/),
			Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Matcher m = argsPattern.matcher(args);
		if (!m.matches()) return plugin.debugFail("syntax");
		
		Sign signState = (Sign) signBlock.getState();
		
		int count = (m.group(3) != null) ? Integer.parseInt(m.group(3)) : -1;
		int repeat = (m.group(4) != null) ? Integer.parseInt(m.group(4)) : 1;
		if (repeat <= 0) return plugin.debugFail("bad repeat");

		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		BlockFace forward = PowerSigns.getForward(signDir, m.group(1));
		Block startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward, m.group(2));
		
		Material[] moveTypes = PowerSigns.getMaterials(signState.getLine(1));
		
		if (moveTypes == null || moveTypes.length == 0)
			return plugin.debugFail("Bad Mats: "+signState.getLine(1));

		return pushLine(plugin, startBlock, forward, moveTypes, count, repeat);
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



		// have to move them backwards so that the blocks aren't overwritten during the copy
		/*BlockFace backward = forward.getOppositeFace();
		if (backward == null)
		{
			PowerSigns.log.severe("[PowerSigns.Push]" + "backward == null!! forward: "+forward.toString());
			return plugin.debugFail("backward == null");
		}*/

		materialLine.flip().moveTo(emptyLine.flip(), numToPush);

		return true;
	}

}
