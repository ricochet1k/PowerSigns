package com.ricochet1k.bukkit.powersigns;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.server.TileEntity;
import net.minecraft.server.World;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;



public class PowerSigns extends JavaPlugin
{
	private static final Set<String> blockMoverSigns = Collections.unmodifiableSet(new HashSet<String>(Arrays
	        .asList("push", "pull")));
	private static final Set<String> aimedSigns = Collections.unmodifiableSet(new HashSet<String>(Arrays
	        .asList("push", "pull", "detect", "line")));
	private static final Set<String> verticalAimableSigns = Collections.unmodifiableSet(new HashSet<String>(Arrays
	        .asList("push", "pull", "detect")));

	public static final int maxDistance = 25;
	public final Logger log = Logger.getLogger("Minecraft");
	public final PowerSignsBlockListener blockListener = new PowerSignsBlockListener(this);

	public PowerSigns(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin,
	        ClassLoader cLoader)
	{
		super(pluginLoader, instance, desc, folder, plugin, cLoader);

	}

	@Override
	public void onEnable()
	{
		log.info("Enabling PowerSigns");
		getServer().getPluginManager().registerEvent(Type.REDSTONE_CHANGE, blockListener, Priority.Highest, this);
	}

	@Override
	public void onDisable()
	{
		log.info("Disabling PowerSigns");
	}

	public void tryPowerSign(Block signBlock)
	{
		Sign signState = (Sign) signBlock.getState();
		// String[] lines = signState.getLines();

		String firstline = signState.getLine(0);

		if (firstline.length() > 2 && firstline.startsWith("[") && firstline.endsWith("]"))
			firstline = firstline.substring(1, firstline.length() - 1);
		else
			return; // cannot be a PowerSign

		String[] parts = firstline.split(" ");
		final String action = parts[0];



		if (aimedSigns.contains(action))
		{
			Block startBlock;
			BlockFace forward = null;

			if (verticalAimableSigns.contains(action) && parts.length > 1)
			{
				// Check for up/down
				if (parts[1].equals("up"))
				{
					forward = BlockFace.UP;
				}
				else if (parts[1].equals("down"))
				{
					forward = BlockFace.DOWN;
				}
				else
				{
					return; // bad direction
				}
			}

			if (signBlock.getType().equals(Material.WALL_SIGN))
			{
				BlockFace facingOpposite = getOppositeFace(getWallSignFacing(signState));
				if (forward == null)
					forward = facingOpposite;

				// skip the block it's attached to
				startBlock = signBlock.getFace(facingOpposite, 1);

				startBlock = startBlock.getFace(forward, 1);
			}
			else if (signBlock.getType().equals(Material.SIGN_POST))
			{
				if (forward == null)
				{
					BlockFace facing = getSignPostFacing(signState);
					if (facing == null)
						return; // Sign is not orthogonal to world
					forward = getOppositeFace(facing);
				}

				startBlock = signBlock.getFace(forward, 1);
				if (forward == BlockFace.DOWN) // let the sign post stand on something
					startBlock = startBlock.getFace(forward, 1);
			}
			else
				return; // not a sign, shouldn't be necessary

			// get material to move
			Material moveType = null;
			if (blockMoverSigns.contains(action))
			{
				moveType = Material.matchMaterial(signState.getLine(1));
				if (moveType == null)
					return;
			}

			// Execute the action
			if (action.equals("push"))
			{
				tryPushBlocks(startBlock, forward, moveType);
			}
			else if (action.equals("pull"))
			{
				tryPullBlocks(startBlock, forward, moveType);
			}
			else if (action.equals("detect"))
			{
				signState.setLine(1, startBlock.getType().toString().toLowerCase());
			}
			else if (action.equals("line") && parts.length == 2)
			{
				Matcher m =
				        Pattern.compile("^([fblruds])([1234]) ([fblruds])([1234])$", Pattern.CASE_INSENSITIVE)
				                .matcher(signState.getLine(1));
				if (!m.matches())
				{
					log.info("Doesn't match");
					return;
				}

				Sign[] signStates = new Sign[] { null, null };
				int[] signLines = new int[] { -1, -1 };

				for (int i = 0; i < 2; i++)
				{
					BlockFace signDir;
					int skip = 1;

					String d = m.group(1 + 2 * i);
					if (d.equals("f"))
					{
						signDir = forward;
					}
					else if (d.equals("b"))
					{
						signDir = getOppositeFace(forward);
					}
					else if (d.equals("l"))
					{
						signDir = rotateFaceLeft(forward);
					}
					else if (d.equals("r"))
					{
						signDir = rotateFaceRight(forward);
					}
					else if (d.equals("u"))
					{
						signDir = BlockFace.UP;
					}
					else if (d.equals("d"))
					{
						signDir = BlockFace.DOWN;
						skip = 2;
					}
					else if (d.equals("s"))
					{
						signDir = BlockFace.SELF;
					}
					else
					{
						log.info("Bad direction: " + d);
						return; // Shouldn't happen
					}

					Block found;
					
					if (signDir == BlockFace.SELF)
					{
    					Block start = signBlock.getFace(signDir, skip);
    					found = start.getFace(signDir, countEmptyInLine(start, signDir));
					}
					else
					{
						found = signBlock;
					}

					if (found.getType().equals(Material.SIGN_POST) || found.getType().equals(Material.WALL_SIGN))
					{
						signStates[i] = (CraftSign) found.getState();
					}
					else
					{
						log.info("Bad block: " + found.getType().toString());
						return;
					}

					signLines[i] = Integer.parseInt(m.group(2 + 2 * i)) - 1;
				}

				if (parts[1].equals("copy"))
				{
					signStates[1].setLine(signLines[1], signStates[0].getLine(signLines[0]));

					signStates[1].update();
				}
				else if (parts[1].equals("swap"))
				{
					String line1 = signStates[1].getLine(signLines[1]);
					signStates[1].setLine(signLines[1], signStates[0].getLine(signLines[0]));
					signStates[0].setLine(signLines[0], line1);

					signStates[0].update();
					signStates[1].update();
				}
			}
		}

	}

	public static boolean tryPushBlocks(Block startBlock, BlockFace forward, Material material)
	{
		int numToPush = countMaterialInLine(startBlock, forward, material);

		if (numToPush > 0)
		{
			if (isEmpty(startBlock.getFace(forward, numToPush)))
			{
				moveBlockLine(startBlock, forward, numToPush, 1);

				return true;
			}
		}
		return false;
	}

	public static boolean tryPullBlocks(Block startBlock, BlockFace forward, Material material)
	{
		int numEmpty = countEmptyInLine(startBlock, forward);

		if (numEmpty > 0)
		{
			Block blockToPull = startBlock.getFace(forward, numEmpty);

			int numToPull = countMaterialInLine(blockToPull, forward, material);

			if (numToPull > 0)
			{
				moveBlockLine(blockToPull, forward, numToPull, -1);
				return true;
			}
		}
		return false;
	}

	public static int countEmptyInLine(Block block, BlockFace direction)
	{
		int found = 0;
		while (isEmpty(block))
		{
			found += 1;
			block = block.getFace(direction);

			if (found >= maxDistance)
				return -1; // Prevent searching off into oblivion
		}

		return found;
	}

	public static int countMaterialInLine(Block block, BlockFace direction, Material material)
	{
		int found = 0;
		while (block.getType().equals(material))
		{
			found += 1;
			block = block.getFace(direction);

			if (found >= maxDistance)
				return -1; // Prevent searching off into oblivion
		}

		return found;
	}

	public static void moveBlockLine(Block block, BlockFace forward, int howMany, int moveAmount)
	{
		BlockFace backward = getOppositeFace(forward);
		World world = ((CraftChunk) block.getChunk()).getHandle().d;

		if (moveAmount > 0)
		{
			block = block.getFace(forward, howMany - 1);
		}
		else
		{
			moveAmount = -moveAmount;
			BlockFace temp = forward;
			forward = backward;
			backward = temp;
		}

		for (int i = 0; i < howMany; i++)
		{
			Block moveTarget = block.getFace(forward, moveAmount);
			moveTarget.setTypeId(block.getTypeId());
			moveTarget.setData(block.getData());

			TileEntity tileEntity = world.getTileEntity(block.getX(), block.getY(), block.getZ());

			if (tileEntity != null)
			{
				world.setTileEntity(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ(), tileEntity);
				world.n(block.getX(), block.getY(), block.getZ());
			}

			block = block.getFace(backward);
		}

		for (int i = 0; i < Math.min(moveAmount, howMany); i++)
		{
			block = block.getFace(forward);
			block.setTypeId(0);
		}
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

	public static BlockFace getWallSignFacing(Sign signState)
	{
		int direction = signState.getData().getData();
		if (direction == 2)
			return BlockFace.EAST;
		if (direction == 3)
			return BlockFace.WEST;
		if (direction == 4)
			return BlockFace.NORTH;
		if (direction == 5)
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
