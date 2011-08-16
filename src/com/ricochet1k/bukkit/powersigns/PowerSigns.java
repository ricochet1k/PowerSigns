package com.ricochet1k.bukkit.powersigns;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntitySnowball;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.ricochet1k.bukkit.powersigns.plugins.ActivateIfSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.ActivateLongSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.ActivateSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.CannonSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.DataAccessSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.DetectSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.FlingSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.InvCountSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.InvOpSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.LineOpSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.MathSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.MoneySignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.PluginInfo;
import com.ricochet1k.bukkit.powersigns.plugins.PowerSignsPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.PullSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.PushSignPlugin;
import com.ricochet1k.bukkit.powersigns.plugins.ToggleSignPlugin;
import com.ricochet1k.bukkit.powersigns.utils.FilteredIterator;
import com.ricochet1k.bukkit.powersigns.utils.Predicate;
import com.ricochet1k.bukkit.powersigns.utils.TransformedIterator;
import com.ricochet1k.bukkit.powersigns.utils.Transformer;

public class PowerSigns extends JavaPlugin
{
	////////////// Settings /////////////////
	
	// General settings
	public static int maxDistance = 50;
	
	// Cannon settings
	public static int powerPerTNT = 50; // use up multiple TNT's for more power
	public static int maxCannonPower = 200;
	
	// Fling settings
	public static int maxFlingPower = 900;
	
	
	//////////// End Settings ////////////
	
	
	// junk
	public static final Logger log = Logger.getLogger("Minecraft");
	public final PowerSignsBlockListener blockListener = new PowerSignsBlockListener(this);
	public final PowerSignsPlayerListener playerListener = new PowerSignsPlayerListener(this);
	public static final Random random = new Random();
	private boolean disabled = false;
	
	public boolean isDisabled()
	{
		return disabled;
	}
	
	public static PermissionHandler Permissions = null;

	@Override
	public void onEnable()
	{
		Plugin test = getServer().getPluginManager().getPlugin("Permissions");
		if (test != null)
		{
			PowerSigns.Permissions = ((Permissions)test).getHandler();
			log.info("["+getDescription().getName()+"] Enabling " + getDescription().getFullName() + " [Permissions active]");
		}
		else
		{
			log.info("Permissions not found for PowerSigns...");
			log.info("["+getDescription().getName()+"] Enabling " + getDescription().getFullName() + "[Permissions not found]");
		}
		
		getServer().getPluginManager().registerEvent(Type.REDSTONE_CHANGE, blockListener, Priority.Highest, this);
		getServer().getPluginManager().registerEvent(Type.SIGN_CHANGE, blockListener, Priority.Highest, this);
		
		// Once Bukkit has support for unregistering events this should be handled in setDebugRightClick
		getServer().getPluginManager().registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
		
		
		PushSignPlugin.register();
		PullSignPlugin.register();
		DetectSignPlugin.register();
		LineOpSignPlugin.register();
		ActivateSignPlugin.register();
		ActivateIfSignPlugin.register();
		ActivateLongSignPlugin.register();
		CannonSignPlugin.register();
		InvOpSignPlugin.register();
		InvCountSignPlugin.register();
		FlingSignPlugin.register();
		DataAccessSignPlugin.register();
		MathSignPlugin.register();
		ToggleSignPlugin.register();
		MoneySignPlugin.register(this);
		
		reloadPowerSigns();
	}

	@Override
	public void onDisable()
	{
		log.info("["+getDescription().getName()+"] Disabling PowerSigns");
		
		plugins.clear();
	}
	
	public static boolean hasPermission(Player player, String permission)
	{
		if (PowerSigns.Permissions != null)
		{
			if (PowerSigns.Permissions.has(player, permission)) 
				return true;
			else return false;
		}
		return player.isOp();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		Player player = null;
		
		log.info("["+getDescription().getName()+"]" + "got command: "+ sender.toString());
		
		if (label.equalsIgnoreCase("PowerSigns") || label.equalsIgnoreCase("ps"))
		{
			// §
			if (sender instanceof Player)
				player = (Player)sender;
			else
			{
				log.info("Error: That command must be used by a player.");
				return false;
			}
				
			
			if (args.length == 0)
			{
				//sendUsage(player);
				player.sendMessage(ChatColor.RED + "PowerSigns commands: (alias /ps)");
				if(PowerSigns.hasPermission(player, "powersigns.debug"))
					player.sendMessage(ChatColor.RED + "/powersigns debug (snow[balls] | rightclick | rc)");
				if(PowerSigns.hasPermission(player, "powersigns.syntax"))
					player.sendMessage(ChatColor.RED + "/powersigns syntax [signtype]");
				return true;
			}
			
			
			if (args.length > 0)
			{
				if (args[0].equalsIgnoreCase("debug"))
				{
					//improper arguments, display syntax
					if (args.length == 1)
					{
						player.sendMessage(ChatColor.RED + "PowerSigns usage: /powersigns debug (snow[balls] | rightclick | rc ...");
						return true;
					}
					
					//snowball debugging
					if (args[1].equalsIgnoreCase("snowballs") || args[1].equalsIgnoreCase("snow"))
					{
						if (!hasPermission(player, "powersigns.debug.snowballs"))
						{
							player.sendMessage(ChatColor.RED + "You don't have permission to do that.");
							return true;
						}
						
						if (args.length > 2)
						{
							if (args[2].equalsIgnoreCase("on"))
								setDebugSnowballs(true);
							else if (args[2].equalsIgnoreCase("off"))
								setDebugSnowballs(false);
							else
							{
								player.sendMessage(ChatColor.RED + "PowerSigns usage: /powersigns debug snow[balls] [on|off]");
								return true;
							}
						}
						else
							setDebugSnowballs(!debugSnowballs);
						
						player.sendMessage(ChatColor.GREEN + "Debugging snowballs " + (debugSnowballs? "enabled." : "disabled."));
						return true;
					}
					//right-click debugging
					else if (args[1].equalsIgnoreCase("rightclick") || args[1].equalsIgnoreCase("rc"))
					{
						if (!hasPermission(player, "powersigns.debug.rightclick"))
						{
							player.sendMessage(ChatColor.RED + "You don't have permission to do that.");
							return true;
						}
						
						boolean newDebugRC = !getDebugRightClick(player);
						
						if (args.length > 2)
						{
							if (args[2].equalsIgnoreCase("on")) newDebugRC = true;
							else if (args[2].equalsIgnoreCase("off")) newDebugRC = false;
							else
							{
								player.sendMessage(ChatColor.RED + "PowerSigns usage: /powersigns debug rightclick [on|off]");
								return true;
							}
						}
						
						setDebugRightClick(player, newDebugRC);
						
						player.sendMessage(ChatColor.GREEN + "Debugging right click " + (newDebugRC? "enabled." : "disabled."));
						return true;
					}
					//right-click debugging
					else if (args[1].equalsIgnoreCase("redstone") || args[1].equalsIgnoreCase("rs"))
					{
						if (!hasPermission(player, "powersigns.debug.redstone"))
						{
							player.sendMessage(ChatColor.RED + "You don't have permission to do that.");
							return true;
						}
						
						debugRedstone = !debugRedstone;
						
						player.sendMessage(ChatColor.GREEN + "Debugging redstone " + (debugRedstone? "enabled." : "disabled."));
						return true;
					}
				}
				else if(args[0].equalsIgnoreCase("syntax"))
				{
					if(!PowerSigns.Permissions.has(player, "powersigns.syntax"))
					{
						player.sendMessage(ChatColor.RED + "You do not have access to this command.");
						return true;
					}
					
					if(args.length == 1)
					{
						//boolean hasAnything = false;
						//if !permissions player.sendMessage(ChatColor.RED + "You do not have access to this command.");
						player.sendMessage(ChatColor.GREEN + "Syntax browsing: /powersigns syntax [sign]");
						/*player.sendMessage(ChatColor.GOLD + "Signs available to you:");
						//show a list of the signs they have access to
						for(int i = 0; i < syntaxMessages[0].length; i++)
							//permissions check against corresponding string
							if(PowerSigns.hasPermission(player, syntaxMessages[1][i]))
							{
								player.sendMessage(syntaxMessages[0][i]);
								hasAnything = true;
							}
						player.sendMessage(hasAnything? 
								(ChatColor.GOLD + "End list.")
								:(ChatColor.RED + "You don't have access to any PowerSigns!"));*/
						StringBuilder builder = new StringBuilder(ChatColor.GOLD + "Available signs: ");
						boolean first = true;
						for (PluginInfo psplugin : plugins)
						{
							if (!hasPermission(player, "powersigns.create."+psplugin.action))
								continue;
							
							if (first) first = false;
							else builder.append(", ");
							builder.append(psplugin.action);
						}
						if (first)
							player.sendMessage(ChatColor.RED + "No available signs for you.");
						else
							player.sendMessage(builder.toString());
						return true;
					}
					else if (args.length == 2) //sign-specific syntax
					{
						String sign = args[1];
						for (PluginInfo psplugin : plugins)
						{
							if (psplugin.action.equalsIgnoreCase(sign))
							{
								//if (psplugin.syntax.length() == 0)
								//	player.sendMessage(ChatColor.RED + "No syntax is available for !" + psplugin.action);
								//else
								player.sendMessage(ChatColor.GOLD + "!" + psplugin.action + " " + psplugin.syntax);
								return true;
							}
						}
						player.sendMessage(ChatColor.RED + "Unknown sign: "+sign);
						return true;
					}
				}
				else if(args[0].equalsIgnoreCase("reload"))
				{
					if(!PowerSigns.hasPermission(player, "powersigns.reload"))
					{
						player.sendMessage(ChatColor.RED + "You do not have access to this command.");
						return true;
					}
					reloadPowerSigns();
				}
				/*else if((args[0].equalsIgnoreCase("wires"))) //display "wiring status"
				{
					if(!PowerSigns.hasPermission(player, "powersigns.wires"))
					{
						player.sendMessage(ChatColor.RED + "You do not have access to this command.");
						return true;
					}
					//DISPLAY STATUSES FOR GLOBAL WIRE-ON/OFF SETTINGS
				}*/
				//handle incorrect commands
				else
					player.sendMessage(ChatColor.RED + "["+getDescription().getName()+"] Command not found");
			}
		}
		
		return false;
	}
	
	private void reloadPowerSigns()
	{
		if (!new File(getDataFolder(),"config.yml").exists())
		{
			getConfiguration().setProperty("maxDistance", maxDistance);
			getConfiguration().setProperty("powerPerTNT", powerPerTNT);
			getConfiguration().setProperty("maxCannonPower", maxCannonPower);
			getConfiguration().setProperty("maxFlingPower", maxFlingPower);
				
			getConfiguration().save();
		}
		
		getConfiguration().load();
		maxDistance = getConfiguration().getInt("maxDistance", 50);
		powerPerTNT = getConfiguration().getInt("powerPerTNT", 50);
		maxCannonPower = getConfiguration().getInt("maxCannonPower", 200);
		maxFlingPower = getConfiguration().getInt("maxFlingPower", 900);
		
		
	}

	public static final Material[] signMaterials = new Material[] { Material.SIGN_POST, Material.WALL_SIGN };
	
	
	/////////////// Patterns //////////////////
	
	public static final String repeatPart = "(?:\\s*\\*(\\d{1,3}))?";
	public static final String skipPart = "(?:\\s*@([0-9]+))?";
	public static final String verticalPart = "(?:\\s+([ud]))?";
	public static final String moveDirPart = "(?:\\s+([^v<>]))?";
	public static final String cannonTypePart = "(?:\\s+(tnt|sand|gravel))?";
	public static final String directionPart = "(?:\\s+([fblruds]))";
	public static final String vectorPart = "(?:\\s+(s|[fblrud]+))";
	public static final String allPart = "(?:\\s+(all))?";
	
	public static final Pattern actionPattern = Pattern.compile("^!([a-z_]+\\b)(.*)$", Pattern.CASE_INSENSITIVE);
	
	private static ArrayList<PluginInfo> plugins = new ArrayList<PluginInfo>();
	private static Map<String, PluginInfo> pluginMap = new HashMap<String, PluginInfo>();
	
	public static void register(String action, PowerSignsPlugin psplugin)
	{
		register(action, "", psplugin);
	}
	
	public static void register(String action, String syntax, PowerSignsPlugin psplugin)
	{
		PluginInfo plugininfo = new PluginInfo(action, syntax, psplugin);
		
		plugins.add(plugininfo);
		pluginMap.put(action, plugininfo);
		
		//log.info("[PowerSigns] Registering sign "+action);
	}
	
	public boolean tryPowerSign(Block signBlock)
	{
		if(!materialsMatch(signBlock.getType(), signMaterials)) return false; // not a sign
		
		Sign signState = (Sign) signBlock.getState();

		String command = signState.getLine(0);
		if (command.length() <= 1 || command.charAt(0) != '!') return false;
		
		failMsg = "";
		
		//final String action;
		
		//String[] result = matchPatterns(command, actionPattern);
		//if (result == null) { debugFail("syntax"); doDebugging(signBlock, 2); return false; }
		
		//action = m.group(0);

		

		try
		{
			boolean ret = false;
			
			Matcher m = actionPattern.matcher(command);
			
			if (m.matches())
			{
				String action = m.group(1).toLowerCase();
				String args = m.group(2);
			
				PluginInfo info = pluginMap.get(action);
				if (info != null)
					ret = info.plugin.doPowerSign(this, signBlock, action, args);
				else
					ret = debugFail("bad action");
			}
			
			if (!ret) debugFail("unknown");
			doDebugging(signBlock, ret? 0 : 1);
			return ret;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			debugFail("exception");
			doDebugging(signBlock, 10);
		}

		return false;
	}
	
	public void updateSignState(final Sign signState)
	{
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					disabled = true;
					signState.update();
				}
				finally
				{
					disabled = false;
				}
			}
		});
	}
	
	// ////////////////////////////////// Helper Methods ///////////////////////////////////

	public static BlockFace getForward(BlockFace signDir)
	{
		return getForward(signDir, null);
	}
	
	public static BlockFace getForward(BlockFace signDir, String verticalStr)
	{
		if (verticalStr != null)
		{
			if (verticalStr.equals("u"))
				return BlockFace.UP;
			if (verticalStr.equals("d"))
				return BlockFace.DOWN;
			log.warning("BAD VERTICAL: "+verticalStr);
		}

		return signDir;
	}

	public static Block getStartBlock(Block signBlock, BlockFace signDir, BlockFace forward, String skipStr)
	{
		int skip = skipStr != null && skipStr.length() > 0? Integer.parseInt(skipStr) : 0;
		Block startBlock = signBlock;
		if (signBlock.getType().equals(Material.WALL_SIGN))
			startBlock = signBlock.getRelative(signDir, 1); // start on the block it's attached to
		else if (signBlock.getType().equals(Material.SIGN_POST) && forward == BlockFace.DOWN)
			startBlock = signBlock.getRelative(forward, 1); // start on what the sign post stands on
		return startBlock.getRelative(forward, skip + 1);
	}

	public static BlockFace getDirection(String directionStr, BlockFace signDir, String verticalStr)
	{
		if (directionStr == null) return null;

		//parse perpendicular move-type sign modifiers
		if (directionStr.equals("^"))
		{
			if (verticalStr == null) return BlockFace.UP;
			else return signDir;
		}
		if (directionStr.equals("v"))
		{
			if (verticalStr == null) return BlockFace.DOWN;
			else return getOppositeFace(signDir);
		}
		if (directionStr.equals("<")) return rotateFaceLeft(signDir);
		if (directionStr.equals(">")) return rotateFaceRight(signDir);
		
		log.warning("BAD DIRECTION: "+directionStr+" "+signDir.toString()+" "+verticalStr);
		
		return null;//shouldn't get to this point
	}

	public static Material[] getMaterials(String materialsStr)
	{
		String[] materialStrs = materialsStr.split(",");
		Material[] materials = new Material[materialStrs.length];

		for (int i = 0; i < materialStrs.length; i++)
		{
			materials[i] = Material.matchMaterial(materialStrs[i]);

			if (materials[i] == null)
				return null; // failure to match one of the materials
		}

		return materials;
	}

	public static boolean materialsMatch(Material mat, Material[] materials)
	{
		if (mat == null || materials == null) return false;
		for (Material material : materials)
		{
			if (material == mat)
				return true;
		}
		return false;
	}
	
	public static int inventoryCount(Inventory inv, Material... materials)
	{
		int count = 0;
		for (ItemStack item : inv.getContents())
		{
			if (item != null && materialsMatch(item.getType(), materials))
				count += item.getAmount();
		}
		return count;
	}

	public static BlockFace strToDirection(String s, BlockFace forward)
	{
		if (s == null || s.isEmpty()) return forward;
		else if (s.equals("f")) 		return forward;
		else if (s.equals("b")) return forward.getOppositeFace();
		else if (s.equals("l")) return rotateFaceLeft(forward);
		else if (s.equals("r")) return rotateFaceRight(forward);
		else if (s.equals("u")) return BlockFace.UP;
		else if (s.equals("d")) return BlockFace.DOWN;
		else if (s.equals("s")) return BlockFace.SELF;
		else
			return null;
	}
	public static Vector faceToVector(BlockFace face)
	{
		return new Vector(face.getModX(), face.getModY(), face.getModZ());
	}
	public static Vector strToVector(String s, BlockFace forward)
	{
		Vector vector = new Vector();
		
		if (s.equalsIgnoreCase("s")) return vector;
		
		for (int i = 0; i < s.length(); i++)
		{
			vector.add(faceToVector(strToDirection(s.substring(i, i+1), forward)));
			//Vector newVec = faceToVector(strToDirection(s.substring(i, i+1), forward));
			//vector.setX(vector.getBlockX() + newVec.getBlockX());
			//vector.setY(vector.getBlockY() + newVec.getBlockY());
			//vector.setZ(vector.getBlockZ() + newVec.getBlockZ());
		}
		
		//log.info(s + ": " + vector.toString());
		
		return vector;
	}
	public static Block blockFromVector(Block block, String s, BlockFace forward)
	{
		Vector vec = strToVector(s, forward);
		return block.getRelative(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
	}

	// //////////////////////// Debugging Stuff ///////////////////////////
	
	// Debug settings
	private boolean debugSnowballs = false;
	//private boolean debugRightClick = false;
	private Map<Player, Boolean> debugRCMap = new HashMap<Player, Boolean>();
	public boolean debugRedstone = false;
	
	
	String failMsg = "";
	
	public boolean debugFail(String msg)
	{
		if (failMsg == null || failMsg.length() == 0)
			failMsg = msg;
		return false; // so you can return debugFail("...")
	}
	
	public void doDebugging(Block block, int num)
	{
		if (block == null)
		{
			log.warning("["+getDescription().getName()+"]" + "null debug block");
			return;
		}
		if (debugSnowballs && num > 0)
		{
    		Location loc = block.getLocation();
    		CraftWorld cworld = ((CraftWorld) block.getWorld());
    		for (int i = 0; i < num; i++)
    		{
        		EntitySnowball snowball = new EntitySnowball(cworld.getHandle(), loc.getX()+0.5d, loc.getY()+1.0d, loc.getZ()+0.5d);
        		snowball.a(0.0d, 1.0d, 0.0d, 0.2f + 0.03f*(float)i, 4.0f);
        		cworld.getHandle().addEntity(snowball);
    		}
		}
		//else
		//	failMsg = "";
		
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
			//sign.update(true);
			//failMsg = "";
		}
	}
	
	public void setDebugSnowballs(boolean debug)
	{
		debugSnowballs = debug;
	}
	
	public boolean getDebugSnowballs()
	{
		return debugSnowballs;
	}
	
	public void setDebugRightClick(Player player, boolean debug)
	{
		if (debug) debugRCMap.put(player, true);
		else debugRCMap.remove(player);
	}
	
	public boolean getDebugRightClick(Player player)
	{
		Boolean debug = debugRCMap.get(player);
		return debug != null && debug == true;
	}
	
	
    //////////////////////// Simple Helper Methods //////////////////////// 
	// These are helper methods I wish were added to their respective classes
	
	public static String join(String... strings)
	{
		StringBuilder b = new StringBuilder();
		for(String string : strings)
			b.append(string);
		return b.toString();
	}
	
	public static String joinBy(String sep, String... strings)
	{
		StringBuilder b = new StringBuilder();
		boolean first = true;
		for(String string : strings)
		{
			if (first)
			{
				first = false;
				b.append(string);
				continue;
			}
			b.append(sep);
			b.append(string);
		}
		return b.toString();
	}
	
	public static boolean hasItems(ItemStack[] reqItems, Inventory inventory)
	{
		int[] itemCounts = new int[reqItems.length];
		
		for (ItemStack item : inventory.getContents())
		{
			if (item == null) continue;
			for (int i = 0; i < reqItems.length; i++)
				if (reqItems[i].getType().equals(item.getType()))
					itemCounts[i] += item.getAmount();
		}
		
		for (int i = 0; i < reqItems.length; i++)
			if (reqItems[i].getAmount() > itemCounts[i])
    			return false;
		return true;
	}
	
	public static boolean tryRemoveItems(ItemStack[] reqItems, Inventory inventory)
	{
		if (!hasItems(reqItems, inventory))
    			return false;
		return inventory.removeItem(reqItems).isEmpty();
	}

	public static boolean isEmpty(Block block)
	{
		switch (block.getType())
		{
		case AIR:
			return true;
		case STATIONARY_WATER: 
		case STATIONARY_LAVA:
			if (block.getData() != 0) return true;
		}
		return false;
	}
	
	public static boolean isFlingable(Block block)
	{
		return (block.getType().equals(Material.AIR)
				|| block.getType().equals(Material.STONE_BUTTON)
				|| block.getType().equals(Material.RED_ROSE)
				|| block.getType().equals(Material.YELLOW_FLOWER)
				|| block.getType().equals(Material.LEVER)
				|| block.getType().equals(Material.STONE_PLATE)
				|| block.getType().equals(Material.WOOD_PLATE)
				|| block.getType().equals(Material.STONE_BUTTON)
				|| block.getType().equals(Material.WALL_SIGN)
				|| block.getType().equals(Material.SIGN_POST)
				|| block.getType().equals(Material.REDSTONE_WIRE)
				|| block.getType().equals(Material.REDSTONE_TORCH_OFF)
				|| block.getType().equals(Material.REDSTONE_TORCH_ON)
				|| block.getType().equals(Material.DIODE_BLOCK_OFF)
				|| block.getType().equals(Material.DIODE_BLOCK_ON));
	}

	public static BlockFace getSignDirection(Block signBlock)
	{
		if (signBlock.getType().equals(Material.WALL_SIGN))
			return getOppositeFace(getWallSignFacing((Sign) signBlock.getState()));
		else if (signBlock.getType().equals(Material.SIGN_POST))
			return getOppositeFace(getSignPostFacing((Sign) signBlock.getState()));

		return null; // not a sign, shouldn't be necessary
	}
	public static BlockFace getWallSignFacing(Sign signState)
	{
		int direction = signState.getData().getData();
		return notchToFacing(direction);
	}
	public static BlockFace notchToFacing(int notch)
	{
		if (notch == 2) return BlockFace.EAST;
		if (notch == 3) return BlockFace.WEST;
		if (notch == 4) return BlockFace.NORTH;
		if (notch == 5) return BlockFace.SOUTH;
		return null;
	}
	public static BlockFace getSignPostFacing(Sign signState)
	{
		int direction = signState.getData().getData();
		if (direction == 0) return BlockFace.WEST;
		if (direction == 4) return BlockFace.NORTH;
		if (direction == 8) return BlockFace.EAST;
		if (direction == 12)return BlockFace.SOUTH;
		return null;
	}
	public static BlockFace getOppositeFace(BlockFace face)
	{
		if (face == null) return null;
		switch (face)
		{
		case NORTH: return BlockFace.SOUTH;
		case SOUTH: return BlockFace.NORTH;
		case EAST: 	return BlockFace.WEST;
		case WEST: 	return BlockFace.EAST;
		case UP: 	return BlockFace.DOWN;
		case DOWN: 	return BlockFace.UP;
		default: 	return null;
		}
	}
	public static BlockFace rotateFaceLeft(BlockFace face)
	{
		if (face == null) return null;
		switch (face)
		{
		case NORTH: return BlockFace.WEST;
		case SOUTH: return BlockFace.EAST;
		case EAST: 	return BlockFace.NORTH;
		case WEST: 	return BlockFace.SOUTH;
		default: 	return null;
		}
	}
	public static BlockFace rotateFaceRight(BlockFace face)
	{
		if (face == null) return null;
		switch (face)
		{
		case NORTH:	return BlockFace.EAST;
		case SOUTH:	return BlockFace.WEST;
		case EAST:	return BlockFace.SOUTH;
		case WEST:	return BlockFace.NORTH;
		default: 	return null;
		}
	}
	
	/*public static List<Player> getPlayersNearBlock(Block lookHere, double radius)
	{
		double radiusSquared = radius * radius;
		Location loc = lookHere.getLocation().add(0.5, 0.5, 0.5);
		ArrayList<Player> playerList = new ArrayList<Player>();
		
		for(Player player : lookHere.getWorld().getPlayers())
		{
			if(player.getLocation().distanceSquared(loc) < radiusSquared)
				playerList.add(player);
		}
		return playerList;
	}
	
	public static List<Entity> getEntitiesNearBlock(Block lookHere, double radius)
	{
		double radiusSquared = radius * radius;
		Location loc = lookHere.getLocation().add(0.5, 0.5, 0.5);
		ArrayList<Entity> entityList = new ArrayList<Entity>();
		
		for(Entity entity : lookHere.getWorld().getEntities())
		{
			if(entity.getLocation().distanceSquared(loc) < radiusSquared)
				entityList.add(entity);
		}
		return entityList;
	}*/
	
	public static Iterable<Entity> entitiesNearBlock(final Block here, double radius)
	{
		final double radiusSquared = radius * radius;
		final Location loc = here.getLocation().add(0.5, 0.5, 0.5);
		
		return new Iterable<Entity>()
		{
			@Override
			public Iterator<Entity> iterator()
			{
				return new FilteredIterator<Entity>(here.getWorld().getEntities().iterator(),
						new Predicate<Entity>()
						{
							@Override
							public boolean apply(Entity entity)
							{
								return entity.getLocation().distanceSquared(loc) < radiusSquared;
							}
						}
					);
			}
		};
	}
	
	public static Iterable<EntityPlayer> playersNearBlock(final Block here, final double radius)
	{
		return new Iterable<EntityPlayer>()
		{
			@Override
			public Iterator<EntityPlayer> iterator()
			{
				return new TransformedIterator<EntityPlayer, Entity>(
						new FilteredIterator<Entity>(entitiesNearBlock(here, radius).iterator(),
								new Predicate<Entity>()
								{
									@Override
									public boolean apply(Entity entity)
									{
										return entity instanceof EntityPlayer;
									}
								}
							),
						new Transformer<EntityPlayer, Entity>()
						{
							@Override
							public EntityPlayer transform(Entity entity)
							{
								return (EntityPlayer)entity;
							}
						});
			}
		};
	}
}
