package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import com.avaje.ebeaninternal.server.lib.util.InvalidDataException;
import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class MathSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		PowerSigns.register("math", "([+-/*&|^]|and|or|xor)", new MathSignPlugin());
	}
	
	static final Pattern argsPattern = Pattern.compile("\\s*([+-/*&|^]|and|or|xor)", Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Matcher m = argsPattern.matcher(args);
		if (!m.matches()) return plugin.debugFail("syntax");
		
		Sign signState = (Sign) signBlock.getState();
		
		String op = m.group(1);
		int op1 = Integer.parseInt(signState.getLine(2));
		int op2 = Integer.parseInt(signState.getLine(1));
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
		
		signState.setLine(2, Integer.toString(val));
		plugin.updateSignState(signState);
		
		return true;
	}

}
