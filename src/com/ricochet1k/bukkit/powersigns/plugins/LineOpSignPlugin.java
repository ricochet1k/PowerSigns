package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.util.Vector;

import com.avaje.ebeaninternal.server.lib.util.InvalidDataException;
import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class LineOpSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		LineOpSignPlugin lasp = new LineOpSignPlugin();
		PowerSigns.register("line", "(s|(fblrud)+)(1234) (|~) (s|(fblrud)+)(1234)", lasp);
		//PowerSigns.register("lineswap", "(s|(fblrud)+)(1234) (s|(fblrud)+)(1234)", lasp);
	}
	
	static final Pattern argsPattern = Pattern.compile("\\s+(s|[fblrud]+)([1234])\\s*(|~)\\s*(s|[fblrud]+)([1234])",
														   Pattern.CASE_INSENSITIVE);
	
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		//Sign signState = (Sign) signBlock.getState();
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		
		
		Matcher m = argsPattern.matcher(args);
		if (!m.matches())
		{return plugin.debugFail("parse linespec"); }

		Sign[] signStates = new Sign[2];
		//Location[] signLocs = new Location[] { null, null };
		int[] signLines = new int[2];

		for (int i = 0; i < 2; i++)
		{
			Vector dir = PowerSigns.strToVector(m.group(1 + i*3), signDir);


			Block found = signBlock.getRelative(dir.getBlockX(), dir.getBlockY(), dir.getBlockZ());
			
			if (!PowerSigns.materialsMatch(found.getType(), PowerSigns.signMaterials))
			{
				PowerSigns.log.info("[LineAlterSignPlugin]" + "Bad block: " + found.getType().toString());
				return false;
			}
			
			//signLocs[i] = found.getLocation();
			signStates[i] = (Sign) found.getState();

			signLines[i] = Integer.parseInt(m.group(2 + i*3)) - 1;
		}
		
		String op = m.group(3);
		if (op == null) op = "";
		
		// final String lineOp = parts[1];
		//final Location[] rsignLocs = signLocs;
		//final int[] rsignLines = signLines;

		
		//Sign sign0 = ((Sign) signLocs[0].getBlock().getState());
		//Sign sign1 = ((Sign) signLocs[1].getBlock().getState());
		if (op.equals("")) // copy
		{
			signStates[1].setLine(signLines[1], signStates[0].getLine(signLines[0]));

			plugin.updateSignState(signStates[1]);
		}
		else if (op.equals("~")) // swap
		{
			String line1 = signStates[1].getLine(signLines[1]);
			signStates[1].setLine(signLines[1], signStates[0].getLine(signLines[0]));
			signStates[0].setLine(signLines[0], line1);

			plugin.updateSignState(signStates[0]);
			plugin.updateSignState(signStates[1]);
		}
		else
		{
			throw new InvalidDataException("Bad Action: "+action);
		}
	
		return true;
	}

}
