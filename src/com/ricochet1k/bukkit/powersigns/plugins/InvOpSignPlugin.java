package com.ricochet1k.bukkit.powersigns.plugins;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.entity.CraftItem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.ricochet1k.bukkit.powersigns.PowerSigns;
import com.ricochet1k.bukkit.powersigns.utils.BlockLine;

public class InvOpSignPlugin implements PowerSignsPlugin
{
	public static void register() {
		InvOpSignPlugin iosp = new InvOpSignPlugin();
		PowerSigns.register("invpush", "[*(0-99)] [u|d] (s|(fblrud)+)", iosp);
		PowerSigns.register("invpull", "[*(0-99)] [u|d] (s|(fblrud)+)", iosp);
		PowerSigns.register("invdrop", "[*(0-99)] (s|(fblrud)+)", iosp);
		PowerSigns.register("invsuck", "[*(0-99)] (s|(fblrud)+)", iosp);
		PowerSigns.register("take", "[*(0-99)] (s|(fblrud)+)", iosp);
		PowerSigns.register("give", "[*(0-99)] (s|(fblrud)+)", iosp);
	}
	
	static final Pattern argsPattern = Pattern.compile(
			PowerSigns.join(PowerSigns.repeatPart,PowerSigns.verticalPart,PowerSigns.vectorPart),
			Pattern.CASE_INSENSITIVE);
	
	@Override
	public boolean doPowerSign(PowerSigns plugin, Block signBlock, String action, String args)
	{
		Matcher m = argsPattern.matcher(args);
		if (!m.matches()) return plugin.debugFail("syntax");
		
		Sign signState = (Sign) signBlock.getState();
		
		int repeat = (m.group(1) != null) ? Integer.parseInt(m.group(1)) : 1;
		if (repeat <= 0) return plugin.debugFail("bad repeat");

		BlockFace signDir = PowerSigns.getSignDirection(signBlock);
		BlockFace forward = PowerSigns.getForward(signDir, m.group(2));
		Block startBlock = PowerSigns.getStartBlock(signBlock, signDir, forward).getRelative(forward, 1);
		
		Vector dir = PowerSigns.strToVector(m.group(3), signDir);
		
		Block invBlock = signBlock.getRelative(dir.getBlockX(), dir.getBlockY(), dir.getBlockZ());
		
		Inventory inventory;
		BlockState state = invBlock.getState();
		if (state instanceof ContainerBlock)
			inventory = ((ContainerBlock)state).getInventory();
		else
			return plugin.debugFail("bad inv:"+invBlock.getType().toString()+" "+dir.toString());
		
		
		Material[] materials = PowerSigns.getMaterials(signState.getLine(1));
		
		
		
		if (action.equalsIgnoreCase("invpush"))
		{
			ItemStack[] items = inventory.getContents();
			
			int count = PowerSigns.inventoryCount(inventory, materials);
			
			if (count < repeat)
				return plugin.debugFail("not enough items");
			
			BlockLine line = new BlockLine(startBlock, forward);
			
			for (int x = repeat; x > 0; x--)
			{
				if (PowerSigns.isEmpty(line.next()))
					continue;
				if (!PushSignPlugin.pushLine(plugin, line.getPrevBlock(), forward, materials, -1, x))
					return plugin.debugFail("couldn't push");
				break;
			}
			
			
			line = new BlockLine(startBlock, forward);
			
			
			for (int i = 0; i < items.length; i++)
			{
				ItemStack itemStack = items[i];
				if (itemStack == null || !PowerSigns.materialsMatch(itemStack.getType(), materials))
					continue;
				
				int iAmount = itemStack.getAmount();
				int howMany;
				if (repeat <= iAmount)
					howMany = repeat;
				else
					howMany = iAmount;
				
				repeat -= howMany;
				if (iAmount == howMany)
				{
					inventory.setItem(i, null);
				}
				else
				{
					itemStack.setAmount(iAmount - howMany);
					inventory.setItem(i, itemStack);
				}
				
				for (int j = 0; j < howMany; j++)
					line.next().setTypeIdAndData(itemStack.getTypeId(), (itemStack.getData()==null)?0:itemStack.getData().getData(), true);
			}
			
			
			return true;
		}
		else if (action.equalsIgnoreCase("invpull"))
		{
			BlockLine line = new BlockLine(startBlock, forward);
			BlockLine matchLine = line.matches(materials);
			
			if (matchLine.count(repeat) != -1)
				return plugin.debugFail("no match:"+matchLine.getNextBlock().getType().toString());
			
			for (int i = 0; i < repeat; i++)
			{
				Block block = line.next();
				ItemStack itemStack = new ItemStack(block.getType(), 1, (short)0, (Byte)block.getData());
				
				if (!inventory.addItem(itemStack).isEmpty())
					return plugin.debugFail("inv full");
				
				block.setTypeIdAndData(0, (byte)0, true);
			}
			
			PullSignPlugin.pullLine(plugin, startBlock, forward, materials, -1, repeat);
			return true;
		}
		else if (action.equalsIgnoreCase("invdrop"))
		{
			Material dropMaterial = null;
			ItemStack[] items = inventory.getContents();
			
			for (Material material : materials)
			{
				int count = PowerSigns.inventoryCount(inventory, material);
				if (count >= repeat)
				{
					dropMaterial = material;
					break;
				}
			}
			
			if (dropMaterial == null)
				return plugin.debugFail("not enough items");
			
			
			for (int i = 0; i < items.length; i++)
			{
				ItemStack itemStack = items[i];
				if (itemStack == null || !itemStack.getType().equals(dropMaterial))
					continue;
				
				int iAmount = itemStack.getAmount();
				int howMany;
				if (repeat <= iAmount)
					howMany = repeat;
				else
					howMany = iAmount;
				
				World world = startBlock.getWorld();
				Location loc = startBlock.getLocation().add(0.5, 0, 0.5);
				Material type = itemStack.getType();
				short damage = itemStack.getDurability();
				
				Random random = new Random();
				
				for (int j = 0; j < howMany; j++)
				{
					Item entity = world.dropItem(loc, new ItemStack(type, 1, damage));
					
					entity.setVelocity(new Vector(random.nextDouble() * 0.02, 0, random.nextDouble() * 0.02));
				}
				
				repeat -= howMany;
				if (iAmount == howMany)
				{
					inventory.setItem(i, null);
				}
				else
				{
					itemStack.setAmount(iAmount - howMany);
					inventory.setItem(i, itemStack);
				}
			}
			
			return true;
		}
		else if (action.equalsIgnoreCase("invsuck"))
		{
			CraftItem[] foundItems = new CraftItem[repeat];
			int index = 0;
			for (Entity entity : PowerSigns.entitiesNearBlock(startBlock, 1))
			{
				if (entity instanceof CraftItem)
				{
					CraftItem item = (CraftItem)entity;
					
					Material material = item.getItemStack().getType();
					for (Material mat : materials)
						if (mat == material)
						{
							foundItems[index++] = item;
							break;
						}
					
					if (index == foundItems.length) break;
				}
			}
			if (index != foundItems.length)
				return plugin.debugFail("too few items");
			
			
			for (int i = 0; i < repeat; i++)
			{
				ItemStack itemStack = foundItems[i].getItemStack();
				if (!inventory.addItem(itemStack).isEmpty())
					return plugin.debugFail("inv full");
				
				itemStack.setAmount(0);
				foundItems[i].setItemStack(itemStack);
				foundItems[i].remove();
			}
			
			return true;
		}
		else if (action.equalsIgnoreCase("take") || action.equalsIgnoreCase("give"))
		{
			boolean didSomething = false;
			for (Entity entity : PowerSigns.entitiesNearBlock(startBlock, 1))
			{
				Inventory inv = null;
				
				if (entity instanceof StorageMinecart)
					inv = ((StorageMinecart)entity).getInventory();
				else if (entity instanceof Player)
					inv = ((Player)entity).getInventory();
				else
					continue;
				
				Inventory fromInv, toInv;
				
				if (action.equalsIgnoreCase("take"))
				{
					fromInv = inv;
					toInv = inventory;
				}
				else
				{
					fromInv = inventory;
					toInv = inv;
				}
				
				int count = PowerSigns.inventoryCount(fromInv, materials);
				
				if (count < repeat)
					continue;
				
				didSomething = true;
				
				count = repeat;
				
				ItemStack[] items = fromInv.getContents();
				for (int i = 0; i < items.length; i++)
				{
					ItemStack item = items[i];
					if (item == null) continue;
					
					Material itemType = item.getType();
					for (int j = 0; j < materials.length; j++)
					{
						if (itemType != materials[j])
							continue;
						
						int amount = item.getAmount();
						int newAmount = 0;
						if (amount > count)
							newAmount = amount - count;
						
						ItemStack newItem = item.clone();
						newItem.setAmount(amount - newAmount);
						toInv.addItem(newItem);
						
						item.setAmount(newAmount);
						if (newAmount == 0)
							item = null;
						fromInv.setItem(i, item);
						
						count -= amount - newAmount;
					}
					
					if (count == 0)
						break;
				}
			}
			return didSomething;
		}
		
		return false;
	}

}
