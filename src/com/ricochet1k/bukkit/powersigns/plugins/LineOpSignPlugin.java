package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.util.Vector;

import com.avaje.ebeaninternal.server.lib.util.InvalidDataException;
import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class LineOpSignPlugin extends ArgsSign
{
	public LineOpSignPlugin()
	{
		super("\\s+(s|[fblrud]+)([1234])\\s*(|~)\\s*(s|[fblrud]+)([1234])");
	}
	
	public static void register() {
		LineOpSignPlugin lasp = new LineOpSignPlugin();
		PowerSigns.register("line", "(s|(fblrud)+)(1234) (|~) (s|(fblrud)+)(1234)", lasp);
	}
	
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args, Boolean isOn)
	{
		Sign signState = (Sign) signBlock.getState();
		if (args.isEmpty()) args = signState.getLine(1);
		
		return super.doPowerSign(plugin, signBlock, action, args, isOn);
	}
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm, Boolean isOn)
	{
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		

		Sign[] signStates = new Sign[2];
		int[] signLines = new int[2];

		for (int i = 0; i < 2; i++)
		{
			Vector dir = PowerSigns.strToVector(argsm.group(1 + i*3), signDir);


			Block found = signBlock.getRelative(dir.getBlockX(), dir.getBlockY(), dir.getBlockZ());
			
			if (!PowerSigns.materialsMatch(found.getType(), PowerSigns.signMaterials))
			{
				return plugin.debugFail("Not sign: " + found.getType().toString());
			}
			
			signStates[i] = (Sign) found.getState();

			signLines[i] = Integer.parseInt(argsm.group(2 + i*3)) - 1;
		}
		
		String op = argsm.group(3);
		if (op == null) op = "";
		
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
