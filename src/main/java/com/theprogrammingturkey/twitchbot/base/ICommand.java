package com.theprogrammingturkey.twitchbot.base;

public interface ICommand
{
	public void onMessage(String channel, String sender, String[] args);
}
