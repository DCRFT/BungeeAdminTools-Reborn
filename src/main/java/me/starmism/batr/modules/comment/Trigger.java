package me.starmism.batr.modules.comment;

import me.starmism.batr.BATR;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Trigger {
	private int triggerNumber;
	private List<String> pattern;
	private List<String> commands;

	public Trigger(int triggerNumber, List<String> pattern, List<String> commands) {
		this.triggerNumber = triggerNumber;
		this.pattern = pattern;
		this.commands = commands;
	}

	public Trigger() {
		this.triggerNumber = -1;
		this.pattern = Collections.singletonList("");
		this.commands = Arrays.asList("alert {player} sparks a trigger. Reason: {reason}","gtempmute {player} 30m");
	}

	public void onTrigger(final String pName, final String reason){
		final PluginManager pm = ProxyServer.getInstance().getPluginManager();
		final CommandSender console = ProxyServer.getInstance().getConsole();
		long delay = 100;
		for (final String command : commands) {
		    ProxyServer.getInstance().getScheduler().schedule(BATR.getInstance(), () ->
					pm.dispatchCommand(console, command.replace("{player}", pName).replace("{reason}", reason)), delay, TimeUnit.MILLISECONDS);
		    delay += 500;
		}
	}

	public int getTriggerNumber() {
		return this.triggerNumber;
	}

	public List<String> getPattern() {
		return this.pattern;
	}

	public List<String> getCommands() {
		return this.commands;
	}

	public void setTriggerNumber(int triggerNumber) {
		this.triggerNumber = triggerNumber;
	}

	public void setPattern(List<String> pattern) {
		this.pattern = pattern;
	}

	public void setCommands(List<String> commands) {
		this.commands = commands;
	}
}
