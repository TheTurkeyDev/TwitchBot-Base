package com.theprogrammingturkey.twitchbot.base;

import java.util.HashMap;
import java.util.Map;

public class CommandManager
{
	private static Map<String, Map<String, ICommand>> commands = new HashMap<>();

	public static void registerCommand(String prefix, String command, ICommand commandObj)
	{
		Map<String, ICommand> prefixCommands = commands.computeIfAbsent(prefix, key -> new HashMap<>());
		prefixCommands.put(command, commandObj);
	}

	public static void handleMessage(String channel, String sender, String message)
	{
		String[] args = message.split(" ");
		for(String prefix : commands.keySet())
		{
			if(message.startsWith(prefix))
			{
				String command = args[0].substring(prefix.length());
				Map<String, ICommand> prefixCommands = commands.get(prefix);
				if(prefixCommands.containsKey(command))
					prefixCommands.get(command).onMessage(channel, sender, args);
			}
		}
	}
}
