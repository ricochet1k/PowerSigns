package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import com.avaje.ebeaninternal.server.lib.util.InvalidDataException;
import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class MathSignPlugin extends ArgsSign
{
	static final String line = "\\s*(s|[fblrud]+)([1234])";
	public MathSignPlugin()
	{
		super(line + "\\s*([+-/*&|^]|and|or|xor)" + line + "(?:\\s*=" + line + ")?");
	}
	
	public static void register() {
		PowerSigns.register("math", "(vector) (+-/*&|^ and or xor) (vector) [= (vector)]", new MathSignPlugin());
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
		Sign signState = (Sign) signBlock.getState();
		
		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		
		BlockState state1 = PowerSigns.blockFromVector(signBlock, argsm.group(1), signDir).getState();
		BlockState state2 = PowerSigns.blockFromVector(signBlock, argsm.group(4), signDir).getState();
		BlockState state3 = null;
		if (argsm.group(6) != null)
			state3 = PowerSigns.blockFromVector(signBlock, argsm.group(6), signDir).getState();
		
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
