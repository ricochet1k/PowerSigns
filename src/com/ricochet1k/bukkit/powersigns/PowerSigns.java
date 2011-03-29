package com.ricochet1k.bukkit.powersigns;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.server.Entity;
import net.minecraft.server.EntityFallingSand;
import net.minecraft.server.EntitySnowball;
import net.minecraft.server.EntityTNTPrimed;
import net.minecraft.server.TileEntity;
import net.minecraft.server.World;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftDispenser;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;



public class PowerSigns extends JavaPlugin
{
	////////////// Settings /////////////////
	
	// General settings
	public static int maxDistance = 25;
	
	// Cannon settings
	public static int powerPerTNT = 50; // use up multiple TNT's for more power
	public static int maxCannonPower = 200;
	
	//////////// End Settings ////////////
	
	
	// junk
	public static final Logger log = Logger.getLogger("Minecraft");
	public final PowerSignsBlockListener blockListener = new PowerSignsBlockListener(this);
	public static final Random random = new Random();

	@Override
	public void onEnable()
	{
		log.info("Enabling PowerSigns 0.5");
		getServer().getPluginManager().registerEvent(Type.REDSTONE_CHANGE, blockListener, Priority.Highest, this);
	}

	@Override
	public void onDisable()
	{
		log.info("Disabling PowerSigns");
	}

	static final Material[] signMaterials = new Material[] { Material.SIGN_POST, Material.WALL_SIGN };
	static final Pattern actionPattern = Pattern.compile("!([a-z_]+)", Pattern.CASE_INSENSITIVE);
	static final Pattern repeatPattern = Pattern.compile("\\s*(?:\\*(\\d{1,2}))?");
	static final Pattern verticalPattern = Pattern.compile("\\s*([ud])?", Pattern.CASE_INSENSITIVE);
	static final Pattern moveDirPattern = Pattern.compile("\\s*([^v<>])?");
	static final Pattern cannonTypePattern = Pattern.compile("\\s*(tnt|sand|gravel)?", Pattern.CASE_INSENSITIVE);
	static final Pattern directionPattern = Pattern.compile("\\s*([fblruds])", Pattern.CASE_INSENSITIVE);
	
	static final Pattern lineSpecPattern = Pattern.compile("([fblruds])([1234]) ([fblruds])([1234])",
	                                                       Pattern.CASE_INSENSITIVE);
	
	String failMsg = "";
	protected boolean debugFail(String msg)
	{
		if (failMsg == null || failMsg.length() == 0)
			failMsg = msg;
		return false; // so you can return debugFail("...")
	}
	
	public static String[] matchPatterns(String input, Pattern... patterns)
	{
		String[] matches = new String[patterns.length+1];
		int i = 0;
		
		for (Pattern pattern : patterns)
		{
			Matcher m = pattern.matcher(input);
			
			if (!m.lookingAt())
				return null;
			
			matches[i++] = m.group(1);
			input = input.substring(m.end());
		}
		
		matches[i] = input;
		
		return matches;
	}
	public static String[] matchPatternsEnd(String input, Pattern... patterns)
	{
		String[] result = matchPatterns(input, patterns);
		if (result == null || (result[result.length-1] != null && result[result.length-1].length() != 0)) return null;
		return result;
	}
	
	public boolean tryPowerSign(Block signBlock)
	{
		if(!matchesMaterials(signBlock, signMaterials)) return false; // not a sign
		
		Sign signState = (Sign) signBlock.getState();

		String command = signState.getLine(0);
		if (command.length() == 0 || command.charAt(0) != '!') return false;
		
		failMsg = "";
		
		final String action;
		
		String[] result = matchPatterns(command, actionPattern);
		if (result == null) { debugFail("syntax"); doDebugging(signBlock, 2); return false; }
		
		action = result[0];

		

		try
		{
			boolean ret = false;
			// Execute the action
			if (action.equals("push"))
			{
				result = matchPatternsEnd(result[1], repeatPattern, verticalPattern, moveDirPattern);
				if (result == null) { debugFail("param syntax"); doDebugging(signBlock, 2); return false; }
				
				final int repeat = (result[0] != null) ? Integer.parseInt(result[0]) : 1;
				if (repeat <= 0) return false;

				BlockFace signDir = getSignDirection(signBlock);
				BlockFace forward = getForward(signDir, result[1]);
				Block startBlock = getStartBlock(signBlock, signDir, forward).getFace(forward, 1);
				BlockFace direction = getDirection(result[2], signDir, result[1]);

				Material[] moveTypes = getMaterials(signState.getLine(1));
				
				ret = tryPushBlocks(startBlock, forward, moveTypes, repeat);
			}
			else if (action.equals("pull"))
			{
				result = matchPatternsEnd(result[1], repeatPattern, verticalPattern, moveDirPattern);
				if (result == null) { debugFail("param syntax"); doDebugging(signBlock, 2); return false; }
				
				final int repeat = (result[0] != null) ? Integer.parseInt(result[0]) : 1;
				if (repeat <= 0) return false;

				BlockFace signDir = getSignDirection(signBlock);
				BlockFace forward = getForward(signDir, result[1]);
				Block startBlock = getStartBlock(signBlock, signDir, forward).getFace(forward, 1);
				BlockFace direction = getDirection(result[2], signDir, result[1]);

				Material[] moveTypes = getMaterials(signState.getLine(1));
				
				ret = tryPullBlocks(startBlock, forward, moveTypes, repeat);
			}
			else if (action.equals("detect"))
			{
				result = matchPatternsEnd(result[1], verticalPattern);
				if (result == null) { debugFail("param syntax"); doDebugging(signBlock, 2); return false; }
				
				BlockFace signDir = getSignDirection(signBlock);
				BlockFace forward = getForward(signDir, result[0]);
				Block startBlock = getStartBlock(signBlock, signDir, forward).getFace(forward, 1);
				
				signState.setLine(1, startBlock.getType().toString().toLowerCase());
				return true;
			}
			else if (action.equals("linecopy") || action.equals("lineswap"))
			{
				BlockFace signDir = getSignDirection(signBlock);
				
				ret = tryLineAction(action, signState.getLine(1), signBlock, signDir);
			}
			else if (action.equals("activate"))
			{
				String[] oldResult = result;
				result = matchPatternsEnd(result[1], verticalPattern);
				if (result == null) { debugFail("param syntax"); doDebugging(signBlock, 2); log.warning(Arrays.toString(oldResult)); return false; }
				
				BlockFace signDir = getSignDirection(signBlock);
				BlockFace forward = getForward(signDir, result[0]);
				Block startBlock = getStartBlock(signBlock, signDir, forward).getFace(forward, 1);

				ret = tryActivate(startBlock, forward);
			}
			else if (action.equals("activate_long"))
			{
				result = matchPatternsEnd(result[1], verticalPattern);
				if (result == null) { failMsg = "param syntax"; doDebugging(signBlock, 2); return false; }
				
				BlockFace signDir = getSignDirection(signBlock);
				BlockFace forward = getForward(signDir, result[0]);
				Block startBlock = getStartBlock(signBlock, signDir, forward).getFace(forward, 1);

				ret = tryActivateLong(startBlock, forward);
			}
			else if (action.equals("cannon"))
			{
				result = matchPatternsEnd(result[1], cannonTypePattern, verticalPattern);
				if (result == null) { debugFail("param syntax"); doDebugging(signBlock, 2); return false; }
				
				BlockFace signDir = getSignDirection(signBlock);
				BlockFace forward = getForward(signDir, result[1]);
				Block startBlock = getStartBlock(signBlock, signDir, forward).getFace(forward, 1);

				ret = tryCannonSign(startBlock, forward, signState.getLine(1), result[0]);
			}
			else if (action.equals("invput"))
			{
				result = matchPatternsEnd(result[1], repeatPattern, verticalPattern, directionPattern);
				if (result == null) { debugFail("param syntax"); doDebugging(signBlock, 2); return false; }
				
				final int repeat = (result[0] != null) ? Integer.parseInt(result[0]) : 1;
				if (repeat <= 0) return false;
				
				BlockFace signDir = getSignDirection(signBlock);
				BlockFace forward = getForward(signDir, result[1]);
				Block startBlock = getStartBlock(signBlock, signDir, forward).getFace(forward, 1);
				
				BlockFace dir = strToDirection(result[2], forward);
				if (dir == null) { debugFail("bad dir: "+result[2]); doDebugging(signBlock, 2); return false; }// shouldn't happen
					

				Material[] moveTypes = getMaterials(signState.getLine(1));
				
				ret = tryInvPut(signBlock.getFace(dir), startBlock, forward, moveTypes, repeat);
			}
			
			debugFail("unknown");
			doDebugging(signBlock, ret? 0 : 1);
			return ret;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			doDebugging(signBlock, 10);
		}

		return false;
	}


	// ////////////////////////////////// Helper Methods ///////////////////////////////////

	public static BlockFace getForward(BlockFace signDir, String verticalStr)
	{
		if (verticalStr != null)
		{
			if (verticalStr.equals("u"))
				return BlockFace.UP;
			else
				// if (verticalStr.equals("d"))
				return BlockFace.DOWN;
		}

		return signDir;
	}

	public static Block getStartBlock(Block signBlock, BlockFace signDir, BlockFace forward)
	{
		if (signBlock.getType().equals(Material.WALL_SIGN))
			return signBlock.getFace(signDir, 1); // skip the block it's attached to
		else if (signBlock.getType().equals(Material.SIGN_POST) && forward == BlockFace.DOWN)
			return signBlock.getFace(forward, 1); // let the sign post stand on something
		return signBlock;
	}

	public static BlockFace getDirection(String directionStr, BlockFace signDir, String verticalStr)
	{
		if (directionStr == null)
			return null;

		if (directionStr.equals("^"))
		{
			if (verticalStr == null)
				return BlockFace.UP;
			else
				return signDir;
		}
		else if (directionStr.equals("v"))
		{
			if (verticalStr == null)
				return BlockFace.DOWN;
			else
				return getOppositeFace(signDir);
		}
		else if (directionStr.equals("<"))
			return rotateFaceLeft(signDir);
		else if (directionStr.equals(">"))
			return rotateFaceRight(signDir);

		return null;
	}

	public static Material[] getMaterials(String materialsStr)
	{
		String[] materialStrs = materialsStr.split("/");
		Material[] materials = new Material[materialStrs.length];

		for (int i = 0; i < materialStrs.length; i++)
		{
			materials[i] = Material.matchMaterial(materialStrs[i]);

			if (materials[i] == null)
				return null; // failure to match one of the materials
		}

		return materials;
	}

	public static boolean matchesMaterials(Block block, Material[] materials)
	{
		if (block == null) return false;
		for (Material material : materials)
		{
			if (material != null && material.getId() == block.getTypeId())
				return true;
		}
		return false;
	}
	
	public void doDebugging(Block block, int num)
	{
		if (block == null)
		{
			log.warning("null debug block");
			return;
		}
		if (num > 0)
		{
    		Location loc = block.getLocation();
    		CraftWorld cworld = ((CraftWorld) block.getWorld());
    		for (int i = 0; i < num; i++)
    		{
        		EntitySnowball snowball = new EntitySnowball(cworld.getHandle(), loc.getX()+0.5d, loc.getY()+1.0d, loc.getZ()+0.5d);
        		snowball.a(0.0d, 1.0d, 0.0d, 0.2f + 0.03f*(float)i, 4.0f);
        		cworld.getHandle().a(snowball);
    		}
		} else
			failMsg = "";
		
		BlockState state = block.getState();
		if (state instanceof Sign)
		{
			Sign sign = ((Sign) state);
			if (sign.getLine(2).equals("stacktrace"))
			{
				sign.setLine(2, "");
				(new Throwable("Stacktrace")).fillInStackTrace().printStackTrace();
			}
			else if (sign.getLine(2).equals("debug"))
			{
				sign.setLine(3, failMsg);
			}
			sign.update(true);
			failMsg = "";
		}
	}

	public void moveBlockLine(BlockLineIterator fromLine, BlockLineIterator toLine, int howMany)
	{
		if (fromLine == toLine || fromLine.nextBlock == toLine.nextBlock)
		{
			doDebugging(fromLine.nextBlock, 4);
			
			debugFail("fromLine == toLine");
			
    		return;
		}
		if (howMany <= 0)
			return;

		int i = 0;

		//moveBlock(fromLine.currentBlock, toLine.currentBlock);

		for (; i < howMany; i++)
		{
			moveBlock(fromLine.next(), toLine.next());
		}
	}

	public void moveBlock(Block from, Block to)
	{
		if (from == to)
		{
			debugFail("from == to");
			doDebugging(from, 3);
			return;
		}
		World world = ((CraftChunk) from.getChunk()).getHandle().d;

		to.setTypeId(from.getTypeId());
		to.setData(from.getData());

		TileEntity tileEntity = world.getTileEntity(from.getX(), from.getY(), from.getZ());

		if (tileEntity != null)
		{
			world.n(from.getX(), from.getY(), from.getZ()); // delete old tile entity
			world.setTileEntity(to.getX(), to.getY(), to.getZ(), tileEntity);
		}

		from.setTypeId(0); // clear from
	}

	public BlockFace strToDirection(String s, BlockFace forward)
	{
		if (s.equals("f"))
			return forward;
		else if (s.equals("b"))
			return getOppositeFace(forward);
		else if (s.equals("l"))
			return rotateFaceLeft(forward);
		else if (s.equals("r"))
			return rotateFaceRight(forward);
		else if (s.equals("u"))
			return BlockFace.UP;
		else if (s.equals("d"))
			return BlockFace.DOWN;
		else if (s.equals("s"))
			return BlockFace.SELF;
		else
			return null;
	}
	

	// //////////////////////// Action Methods ///////////////////////////

	public boolean tryPushBlocks(Block startBlock, BlockFace forward, Material[] materials, int amount)
	{
		assert amount > 0;

		int numToPush = 0;
		int numEmpty = 0;

		BlockLineIterator line = blocksInLine(startBlock, forward);

		numToPush = skipMatching(line, materials);
		if (numToPush < 1)
		{ return debugFail("nothing to push"); }

		Block pushEnd = line.currentBlock;

		
		numEmpty = skipEmpty(line, amount);

		if (numEmpty != -1) // -1 means it hit the max, anything else means it didn't find enough
			return debugFail("not enough space");


		Block emptyEnd = line.currentBlock;

		// have to move them backwards so that the blocks aren't overwritten during the copy
		BlockFace backward = getOppositeFace(forward);
		if (backward == null)
		{
			log.severe("backward == null!! forward: "+forward.toString());
			return debugFail("backward == null");
		}

		moveBlockLine(blocksInLine(pushEnd, backward), blocksInLine(emptyEnd, backward), numToPush);

		return true;
	}

	public boolean tryPullBlocks(Block startBlock, BlockFace forward, Material[] materials, int amount)
	{
		assert amount > 0;

		int numEmpty = 0;
		int numToPull = 0;
		
		
		BlockLineIterator line = blocksInLine(startBlock, forward);

		numEmpty = skipEmpty(line);

		if (numEmpty < amount)
		{
			return debugFail("not enough space");
		}

		Block blockToPull = line.nextBlock;

		numToPull = skipMatching(line, materials);
		if (numToPull == 0)
		{return debugFail("nothing to pull"); }


		moveBlockLine(blocksInLine(blockToPull, forward),
		              blocksInLine(blockToPull.getFace(forward, -1 * amount), forward), numToPull);

		return true;
	}

	public boolean tryLineAction(final String action, String lineSpec, Block signBlock, BlockFace forward)
	{
		Matcher m = lineSpecPattern.matcher(lineSpec);
		if (!m.matches())
		{return debugFail("parse linespec"); }

		// Sign[] signStates = new Sign[] { null, null };
		Location[] signLocs = new Location[] { null, null };
		int[] signLines = new int[] { -1, -1 };

		for (int i = 0; i < 2; i++)
		{
			int skip = 1;

			BlockFace dir = strToDirection(m.group(1 + 2 * i), forward);
			if (dir == null)
			{
				log.warning("Bad direction: " + m.group(1 + 2 * i));
				return false; // Shouldn't happen
			}
			
			if (dir.equals(BlockFace.DOWN))
				skip = 2;
			

			Block found;

			if (dir != BlockFace.SELF)
			{
				Block start = signBlock.getFace(dir, skip);
				BlockLineIterator line = blocksInLine(start, dir);
				skipEmpty(line);
				found = line.nextBlock; //start.getFace(dir, countEmpty(start, dir));
			}
			else
			{
				found = signBlock;
			}
			
			if (found.getType().equals(Material.SIGN_POST) || found.getType().equals(Material.WALL_SIGN))
			{
				signLocs[i] = found.getLocation();
			}
			else
			{
				log.info("Bad block: " + found.getType().toString());
				return false;
			}

			signLines[i] = Integer.parseInt(m.group(2 + 2 * i)) - 1;
		}


		// final String lineOp = parts[1];
		final Location[] rsignLocs = signLocs;
		final int[] rsignLines = signLines;

		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
		{

			@Override
			public void run()
			{
				CraftSign sign0 = ((CraftSign) rsignLocs[1].getBlock().getState());
				CraftSign sign1 = ((CraftSign) rsignLocs[1].getBlock().getState());
				if (action.equals("linecopy"))
				{
					sign1.setLine(rsignLines[1], sign0.getLine(rsignLines[0]));

					sign1.update();
				}
				else if (action.equals("lineswap"))
				{
					String line1 = sign1.getLine(rsignLines[1]);
					sign1.setLine(rsignLines[1], sign0.getLine(rsignLines[0]));
					sign0.setLine(rsignLines[0], line1);

					sign0.update();
					sign1.update();
				}
			}
		});

		return true;
	}
	
	public boolean tryActivate(Block startBlock, BlockFace forward)
	{
		boolean didSomething = false;
		for (Block block : matchingBlocksInLine(startBlock, forward, signMaterials))
		{
			if (!tryPowerSign(block))
				break;
			didSomething = true;
		}
		return didSomething;
	}

	public boolean tryActivateLong(Block startBlock, BlockFace forward)
	{
		BlockLineIterator line = blocksInLine(startBlock, forward);
		skipEmpty(line);
		if (matchesMaterials(line.currentBlock, signMaterials))
		{
			return tryPowerSign(line.currentBlock);
		}
		return false;
	}
	
	static final Pattern cannonBallisticsPattern = Pattern.compile("(\\d{1,3})\\s+(\\d{1,2})(?:\\s+(ns))?");
	public boolean tryCannonSign(Block dispenserBlock, BlockFace forward, String ballisticsLine, String type)
	{
		//Sign signState = (Sign) signBlock.getState();

		// get the information about the ballistics for the TNT projectile on the second line
		Matcher m = cannonBallisticsPattern.matcher(ballisticsLine);
		if (!m.matches())
			return debugFail("parse ballistics");
		int power = Integer.parseInt(m.group(1));
		
		if (power > maxCannonPower)
		{
			//log.info("Power input exceeded - limit is " + maxCannonPower);
			power = maxCannonPower;
		}
		double powerd = power / 100.0;
		
		double angle = Integer.parseInt(m.group(2));
		
		if (Math.abs(angle) > 90)
			return debugFail("bad angle: " + Double.toString(angle));
		
		
		// make sure it's a dispenser
		if (!dispenserBlock.getType().equals(Material.DISPENSER))
			return debugFail("not dispenser");
		// check that the dispenser's facing isn't obstructed
		final Block placeHere = dispenserBlock.getFace(notchToFacing(dispenserBlock.getData()));
		if (!isEmpty(placeHere))
			return debugFail("dispenser blocked");
		
		int tntCost = power / powerPerTNT;
		
		Material entityMaterial = null;
		ItemStack[] neededItems;
		if (type == null || type.equals("tnt"))
		{
			neededItems = new ItemStack[] {new ItemStack(Material.TNT, 1 + tntCost)};
		}
		else
		{
			entityMaterial = Material.matchMaterial(type);
			neededItems = new ItemStack[] {new ItemStack(Material.TNT, tntCost), new ItemStack(entityMaterial, 1)};
		}
		if (!tryRemoveItems(neededItems, ((CraftDispenser)dispenserBlock.getState()).getInventory()))
			return debugFail("not enough items");

		// spawn the entity
		final CraftWorld cWorld = (CraftWorld) dispenserBlock.getWorld();
		final Entity projectile;
		if (type == null || type.equals("tnt"))
		{
    		projectile = new EntityTNTPrimed(cWorld.getHandle(), placeHere.getX() + 0.5f, placeHere.getY() + 0.5f,
    		                placeHere.getZ() + 0.5f);
		}
		else
		{
			projectile = new EntityFallingSand(cWorld.getHandle(), placeHere.getX() + 0.5f, placeHere.getY() + 0.5f,
    		                placeHere.getZ() + 0.5f, entityMaterial.getId());
		}

		// some messy vector math for the ballistics input
		double vector_x = powerd * Math.cos(angle * Math.PI / 180) * (placeHere.getX() - dispenserBlock.getX());
		double vector_y = powerd * Math.sin(angle * Math.PI / 180);
		double vector_z = powerd * Math.cos(angle * Math.PI / 180) * (placeHere.getZ() - dispenserBlock.getZ());
		
		if (m.group(3) == null)
		{
    		// randomize the velocity a bit
    		vector_x += random.nextGaussian() * 0.045d * powerd;
    		vector_y += random.nextGaussian() * 0.045d * powerd;
    		vector_z += random.nextGaussian() * 0.045d * powerd;
		}
		
		// adjust velocity
		projectile.f(vector_x, vector_y, vector_z);
		
		// Fire!
		cWorld.getHandle().a(projectile);
		
		return true;
	}
	
	public boolean tryInvPut(Block invBlock, Block startBlock, BlockFace forward, Material[] materials, int amount)
	{
		Inventory inventory;
		BlockState state = invBlock.getState();
		if (state instanceof ContainerBlock)
			inventory = ((ContainerBlock)state).getInventory();
		else
			return debugFail("bad inv block");
		
		Material putMaterial = null;
		ItemStack[] items = inventory.getContents();
		
		for (Material material : materials)
		{
			int count = 0;
			for (ItemStack itemStack : items)
			{
				if (!itemStack.getType().equals(material))
					continue;
				count += itemStack.getAmount();
			}
			if (count >= amount)
			{
				putMaterial = material;
				break;
			}
		}
		
		if (putMaterial == null)
			return debugFail("not enough items");
		
		if (countEmpty(startBlock, forward, amount) != -1) // -1 means count hit max
			return debugFail("not enough space");
		
		
		BlockLineIterator line = blocksInLine(startBlock, forward);
		
		
		for (int i = 0; i < items.length; i++)
		{
			ItemStack itemStack = items[i];
			if (!itemStack.getType().equals(putMaterial))
				continue;
			
			int iAmount = itemStack.getAmount();
			int howMany;
			if (amount <= iAmount)
				howMany = amount;
			else
				howMany = iAmount;
			
			amount -= howMany;
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
				line.next().setTypeIdAndData(itemStack.getTypeId(), (byte)itemStack.getDurability(), true);
		}
		
		
		return true;
	}
	
	//////////////////////// Block Iterator Methods ///////////////////////////////

	public static BlockLineIterator blocksInLine(Block start, BlockFace dir)
	{
		return new BlockLineIterator(start, dir);
	}

	public static BlockLineIterator matchingBlocksInLine(Block start, BlockFace dir, Material[] mats)
	{
		final Material[] materials = mats;
		return new BlockLineIterator(start, dir)
		{
			@Override
			public boolean blockMatches(Block block)
			{
				return matchesMaterials(block, materials);
			}
		};
	}

	public static BlockLineIterator emptyBlocksInLine(Block start, BlockFace dir)
	{
		return new BlockLineIterator(start, dir)
		{
			@Override
			public boolean blockMatches(Block block)
			{
				return isEmpty(block);
			}
		};
	}

	
	public static int countEmpty(Block startBlock, BlockFace direction)
	{
		return countEmpty(startBlock, direction, maxDistance);
	}
	public static int countEmpty(Block startBlock, BlockFace direction, int max)
	{
		int found = 0;
		BlockLineIterator line = emptyBlocksInLine(startBlock, direction);
		while(line.hasNext())
		{
			line.next();
			
			found += 1;
			if (found >= max)
				return -1; // Prevent searching off into oblivion
		}

		return found;
	}
	
	public static int skipEmpty(BlockLineIterator line)
	{
		return skipEmpty(line, maxDistance);
	}
	public static int skipEmpty(BlockLineIterator line, int max)
	{
		int found = 0;
		while(line.hasNext() && isEmpty(line.nextBlock))
		{
			line.next();
			found += 1;
			if (found >= max)
				return -1; // Prevent searching off into oblivion
		}

		return found;
	}

	public static int skipMatching(BlockLineIterator line, Material[] materials)
	{
		return skipMatching(line, materials, maxDistance);
	}
	
	public static int skipMatching(BlockLineIterator line, Material[] materials, int max)
	{
		int found = 0;
		while(line.hasNext() && matchesMaterials(line.nextBlock, materials))
		{
			line.next();
			found += 1;
			if (found >= max)
				return -1; // Prevent searching off into oblivion
		}

		return found;
	}

	

    //////////////////////// Simple Helper Methods //////////////////////// 
	// / These are helper methods I wish were added to their respective classes
	
	public static boolean hasItems(ItemStack[] reqItems, Inventory inventory)
	{
		int[] itemCounts = new int[reqItems.length];
		for (ItemStack item : inventory.getContents())
		{
			for (int i = 0; i < reqItems.length; i++)
			{
				if (reqItems[i].getType().equals(item.getType()))
				{
					itemCounts[i] += item.getAmount();
				}
			}
		}
		for (int i = 0; i < reqItems.length; i++)
		{
			if (reqItems[i].getAmount() > itemCounts[i])
    			return false;
		}
		return true;
	}
	
	public static boolean tryRemoveItems(ItemStack[] reqItems, Inventory inventory)
	{
		if (!hasItems(reqItems, inventory))
    			return false;
		inventory.removeItem(reqItems);
		return true;
	}

	/**
	 * Is the block empty? (Air, or flowing water/lava)
	 * 
	 * @param block
	 * @return
	 */
	public static boolean isEmpty(Block block)
	{
		return (block.getType().equals(Material.AIR) || ((block.getType().equals(Material.STATIONARY_WATER) || block
		        .getType().equals(Material.STATIONARY_LAVA)) && block.getData() != 0));
	}

	public static BlockFace getSignDirection(Block signBlock)
	{
		if (signBlock.getType().equals(Material.WALL_SIGN))
		{
			return getOppositeFace(getWallSignFacing((Sign) signBlock.getState()));
		}
		else if (signBlock.getType().equals(Material.SIGN_POST))
		{
			return getOppositeFace(getSignPostFacing((Sign) signBlock.getState()));
		}

		return null; // not a sign, shouldn't be necessary
	}

	public static BlockFace getWallSignFacing(Sign signState)
	{
		int direction = signState.getData().getData();
		return notchToFacing(direction);
	}
	
	public static BlockFace notchToFacing(int notch)
	{
		if (notch == 2)
			return BlockFace.EAST;
		if (notch == 3)
			return BlockFace.WEST;
		if (notch == 4)
			return BlockFace.NORTH;
		if (notch == 5)
			return BlockFace.SOUTH;
		return null;
	}

	public static BlockFace getSignPostFacing(Sign signState)
	{
		int direction = signState.getData().getData();
		if (direction == 0)
			return BlockFace.WEST;
		if (direction == 4)
			return BlockFace.NORTH;
		if (direction == 8)
			return BlockFace.EAST;
		if (direction == 12)
			return BlockFace.SOUTH;
		return null;
	}

	public static BlockFace getOppositeFace(BlockFace face)
	{
		switch (face)
		{
		case NORTH:
			return BlockFace.SOUTH;
		case SOUTH:
			return BlockFace.NORTH;
		case EAST:
			return BlockFace.WEST;
		case WEST:
			return BlockFace.EAST;
		case UP:
			return BlockFace.DOWN;
		case DOWN:
			return BlockFace.UP;
		default:
			return null;
		}
	}

	public static BlockFace rotateFaceLeft(BlockFace face)
	{
		switch (face)
		{
		case NORTH:
			return BlockFace.WEST;
		case SOUTH:
			return BlockFace.EAST;
		case EAST:
			return BlockFace.NORTH;
		case WEST:
			return BlockFace.SOUTH;
		default:
			return null;
		}
	}

	public static BlockFace rotateFaceRight(BlockFace face)
	{
		switch (face)
		{
		case NORTH:
			return BlockFace.EAST;
		case SOUTH:
			return BlockFace.WEST;
		case EAST:
			return BlockFace.SOUTH;
		case WEST:
			return BlockFace.NORTH;
		default:
			return null;
		}
	}
}
