package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.server.EntityFallingBlock;
import net.minecraft.server.EntityTNTPrimed;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftDispenser;
import org.bukkit.inventory.ItemStack;

import com.ricochet1k.bukkit.powersigns.PowerSigns;

public class CannonSignPlugin extends AimedSign
{
	public CannonSignPlugin()
	{
		super("\\s*(sand|gravel|tnt)");
	}

	public static void register() {
		PowerSigns.register("cannon", "[u|d][@(0-99)] (sand|gravel|tnt)  /  (0-999) (0-99) [ns]", new CannonSignPlugin());
	}
	
	static final Pattern cannonBallisticsPattern = Pattern.compile("(\\d{1,3})\\s+(\\d{1,2})(?:\\s+(ns))?");
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, Matcher argsm, Boolean isOn,
			BlockFace signDir, BlockFace forward, Block startBlock)
	{
		Sign signState = (Sign) signBlock.getState();
		
		String type = argsm.group(1);
		
		
		int minCost = 1;

		// get the information about the ballistics for the TNT projectile on the second line
		Matcher m = cannonBallisticsPattern.matcher(signState.getLine(1));
		if (!m.matches())
			return plugin.debugFail("parse ballistics");
		int power = Integer.parseInt(m.group(1));
		
		if (power > PowerSigns.maxCannonPower)
		{
			//log.info("["+getDescription().getName()+"]" + "Power input exceeded - limit is " + maxCannonPower);
			power = PowerSigns.maxCannonPower;
		}
		double powerd = power / 100.0;
		
		double angle = Integer.parseInt(m.group(2));
		
		if (Math.abs(angle) > 90)
			return plugin.debugFail("bad angle: " + Double.toString(angle));
		
		
		// make sure it's a dispenser
		if (!startBlock.getType().equals(Material.DISPENSER))
			return plugin.debugFail("found "+startBlock.getType()+", not dispenser");
		Block dispenserBlock = startBlock;
		// check that the dispenser's facing isn't obstructed
		final Block placeHere = dispenserBlock.getRelative(PowerSigns.notchToFacing(dispenserBlock.getData()));
		if (!PowerSigns.isEmpty(placeHere))
			return plugin.debugFail("dispenser blocked by "+placeHere.getType());
		
		int tntCost = power / PowerSigns.powerPerTNT;
		
		Material entityMaterial = null;
		ItemStack[] neededItems;
		if (type == null || type.equals("tnt"))
		{
			neededItems = new ItemStack[] {new ItemStack(Material.TNT, minCost + tntCost)};
		}
		else
		{
			entityMaterial = Material.matchMaterial(type);
			neededItems = new ItemStack[] {new ItemStack(Material.TNT, tntCost), new ItemStack(entityMaterial, minCost)};
		}
		if (!PowerSigns.tryRemoveItems(neededItems, ((CraftDispenser)dispenserBlock.getState()).getInventory()))
			return plugin.debugFail("not enough items (" + neededItems[0].getType() + " " + neededItems[0].getAmount() + ")"
					+ (neededItems.length < 2? "" : (" (" + neededItems[1].getType() + " " + neededItems[1].getAmount() + ")")));

		// spawn the entity
		final CraftWorld cWorld = (CraftWorld) dispenserBlock.getWorld();
		final net.minecraft.server.Entity projectile;
		if (type == null || type.equals("tnt"))
		{
    		projectile = new EntityTNTPrimed(cWorld.getHandle(), placeHere.getX() + 0.5f, placeHere.getY() + 0.5f,
    		                placeHere.getZ() + 0.5f);
		}
		else if (type.equals("sand") || type.equals("gravel"))
		{
			projectile = new EntityFallingBlock(cWorld.getHandle(), placeHere.getX() + 0.5f, placeHere.getY() + 0.5f,
    		                placeHere.getZ() + 0.5f, entityMaterial.getId(), 0);
		}
		else
			return false; //shouldn't happen unless they try an invalid type
		// some messy vector math for the ballistics input
		double delta_x = (placeHere.getX() - dispenserBlock.getX());
		double delta_z = (placeHere.getZ() - dispenserBlock.getZ());
		
		double vector_x = powerd * Math.cos(angle * Math.PI / 180) * delta_x;
		double vector_y = powerd * Math.sin(angle * Math.PI / 180);
		double vector_z = powerd * Math.cos(angle * Math.PI / 180) * delta_z;
		
		if (m.group(3) == null)
		{
    		// randomize the velocity a bit
    		vector_x += PowerSigns.random.nextGaussian() * 0.045d * powerd;
    		vector_y += PowerSigns.random.nextGaussian() * 0.045d * powerd;
    		vector_z += PowerSigns.random.nextGaussian() * 0.045d * powerd;
		}
		
		// adjust velocity
		projectile.f(vector_x, vector_y, vector_z);
		
		// Fire!
		cWorld.getHandle().addEntity(projectile);
		
		return true;
	}

}
