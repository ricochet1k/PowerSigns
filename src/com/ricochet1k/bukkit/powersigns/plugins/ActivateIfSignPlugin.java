package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.util.Vector;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class ActivateIfSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		PowerSigns.register("activateif", "[u|d]  /  activate[fail] (vector) | (vector)(line) (=|!=|<|<=|>|>=) (vector)(line)", new ActivateIfSignPlugin());
	}
	
	static final Pattern argsPattern = Pattern.compile(
			PowerSigns.join(PowerSigns.verticalPart), Pattern.CASE_INSENSITIVE);
	
	static final Pattern activatePattern = Pattern.compile(
			"activate(fail)?\\s+" + PowerSigns.vectorPart, Pattern.CASE_INSENSITIVE);
	static final Pattern lineComparePattern = Pattern.compile(
			"(s|[fblrud]+)([1-4])\\s*(!?=|<=?|>=?)\\s*(s|[fblrud]+)([1-4])", Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Matcher argsm = argsPattern.matcher(args);
		if (!argsm.matches()) return plugin.debugFail("syntax");
		
		
		Sign signState = (Sign) signBlock.getState();
		
		Matcher activatem = activatePattern.matcher(signState.getLine(1));
		Matcher lineComparem = lineComparePattern.matcher(signState.getLine(1));
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		BlockFace forward = PowerSigns.getForward(signDir, argsm.group(1));
		Block startBlock;
		if (forward != signDir && signBlock.getType().equals(Material.WALL_SIGN))
			startBlock = signBlock.getRelative(forward, 1);
		else
			startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward).getRelative(forward, 1);
		
		
		if (activatem.matches())
		{
			boolean fail = activatem.group(1) != null && !activatem.group(1).isEmpty();
			
			Vector vector = PowerSigns.strToVector(activatem.group(2), signDir);
			Block testBlock = signBlock.getRelative(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
			
			if (PowerSigns.materialsMatch(testBlock.getType(), PowerSigns.signMaterials))
			{
				boolean ret = plugin.tryPowerSign(testBlock);
				if (ret == fail)
					return plugin.debugFail("test failed");
			}
			else
				return plugin.debugFail("not a sign");
		}
		else if (lineComparem.matches())
		{
			String left, right;
			
			Vector vector = PowerSigns.strToVector(lineComparem.group(1), signDir);
			Block block = signBlock.getRelative(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
			if (!PowerSigns.materialsMatch(block.getType(), PowerSigns.signMaterials))
				return plugin.debugFail("left not sign");
			Sign sign = (Sign) block.getState();
			left = sign.getLine(Integer.parseInt(lineComparem.group(2))-1);
			
			
			vector = PowerSigns.strToVector(lineComparem.group(4), signDir);
			block = signBlock.getRelative(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
			if (!PowerSigns.materialsMatch(block.getType(), PowerSigns.signMaterials))
				return plugin.debugFail("right not sign");
			sign = (Sign) block.getState();
			right = sign.getLine(Integer.parseInt(lineComparem.group(5))-1);
			
			String op = lineComparem.group(3);
			boolean result = false;
			     if (op.equals("="))	result = left.equals(right);
			else if (op.equals("!="))	result = !left.equals(right);
			else
			{
				try
				{
					int leftNum = Integer.parseInt(left);
					int rightNum = Integer.parseInt(right);
					
					     if (op.equals("<"))	result = leftNum < rightNum;
					else if (op.equals("<="))	result = leftNum <= rightNum;
					else if (op.equals(">"))	result = leftNum > rightNum;
					else if (op.equals(">="))	result = leftNum >= rightNum;
				}
				catch (NumberFormatException e)
				{
					int cmp = left.compareTo(right);
				         if (op.equals("<"))	result = cmp < 0;
					else if (op.equals("<="))	result = cmp <= 0;
					else if (op.equals(">"))	result = cmp > 0;
					else if (op.equals(">="))	result = cmp >= 0;
				}
			}
			
			if (!result) return plugin.debugFail("test failed");
		}
		else
			return plugin.debugFail("bad line 2");
		
		if (PowerSigns.materialsMatch(startBlock.getType(), PowerSigns.signMaterials))
		{
			plugin.tryPowerSign(startBlock);
			return true;
		}
		else
			return plugin.debugFail("not a sign");
	}

}
