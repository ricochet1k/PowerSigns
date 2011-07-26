package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import com.avaje.ebeaninternal.server.lib.util.InvalidDataException;
import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class MathSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		PowerSigns.register("math", "(vector) (+-/*&|^ and or xor) (vector) [= (vector)]", new MathSignPlugin());
	}
	
	static final Pattern argsPattern = Pattern.compile("\\s*(s|[fblrud]+)([1234])\\s*([+-/*&|^]|and|or|xor)\\s*(s|[fblrud]+)([1234])(?:\\s*=\\s*(s|[fblrud]+)([1234]))?", Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Sign signState = (Sign) signBlock.getState();
		if (args.isEmpty()) args = signState.getLine(1);
		Matcher argsm = argsPattern.matcher(args);
		if (!argsm.matches()) return plugin.debugFail("syntax");
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		
		BlockFace flingDir = PowerSigns.strToDirection(argsm.group(1), signDir);
		
		BlockState state1 = PowerSigns.blockFromVector(signBlock, argsm.group(1), flingDir).getState();
		BlockState state2 = PowerSigns.blockFromVector(signBlock, argsm.group(4), flingDir).getState();
		BlockState state3 = null;
		if (argsm.group(6) != null)
			state3 = PowerSigns.blockFromVector(signBlock, argsm.group(6), flingDir).getState();
		
		if (!(state1 instanceof Sign && state2 instanceof Sign && (state3 == null || state3 instanceof Sign))) return plugin.debugFail("not a sign");
		
		Sign sign1 = (Sign) state1;
		Sign sign2 = (Sign) state2;
		Sign sign3 = null;
		if (state3 != null)
			sign3 = (Sign) state3;
		
		String op = argsm.group(3);
		int op1 = Integer.parseInt(sign1.getLine(Integer.parseInt(argsm.group(2))-1));
		int op2 = Integer.parseInt(sign2.getLine(Integer.parseInt(argsm.group(5))-1));
		int val;
		
		if (op.equals("+"))
			val = op1 + op2;
		else if (op.equals("-"))
			val = op1 - op2;
		else if (op.equals("/"))
			val = op1 / op2;
		else if (op.equals("*"))
			val = op1 * op2;
		else if (op.equals("&"))
			val = op1 & op2;
		else if (op.equals("|"))
			val = op1 | op2;
		else if (op.equals("^"))
			val = op1 ^ op2;
		else if (op.equals("and"))
			val = (op1 != 0 && op2 != 0)?1:0;
		else if (op.equals("or"))
			val = (op1 != 0 || op2 != 0)?1:0;
		else if (op.equals("xor"))
			val = ((op1 != 0)?1:0) ^ ((op2 != 0)?1:0);
		else
			throw new InvalidDataException("Bad op: "+op);
		
		if (sign3 != null)
			sign3.setLine(Integer.parseInt(argsm.group(7))-1, Integer.toString(val));
		else
			signState.setLine(2, Integer.toString(val));
		plugin.updateSignState(signState);
		
		return true;
	}

}
