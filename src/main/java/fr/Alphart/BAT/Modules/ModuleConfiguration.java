package fr.Alphart.BAT.Modules;

import me.mattstudios.config.SettingsHolder;
import me.mattstudios.config.properties.Property;

import java.util.Map;

public abstract class ModuleConfiguration implements SettingsHolder {

	private ModuleConfiguration() {}

	public static final Property<Boolean> enabled = Property.create(true);

	public static final Property<Map<String, Boolean>> commands = Property.create(Boolean.class, Map.of("module", true));

	/**
	 * Get the names of the enabled commands for this module
	 * 
	 * @return list of the enabled commands
	 */
//	public List<String> getEnabledCmds() {
//		return commands.entrySet().stream().filter(Map.Entry::getValue).collect(Collectors.toMap(Function.identity(), Function.identity()));
//	}

	/**
	 * Add commands provided by this module into the configuration file
	 * 
	 * @param cmds
	 *            list
	 */
//	public void setProvidedCmds(final List<String> cmds) {
//		Collections.sort(cmds);
//		// Add new commands if there are
//		for (final String cmdName : cmds) {
//			if (!commands.containsKey(cmdName)) {
//				commands.put(cmdName, true);
//			}
//		}
//		// Iterate through the commands map and remove the ones who don't exist (e.g because of an update)
//		commands.entrySet().removeIf(cmdEntry -> !cmds.contains(cmdEntry.getKey()));
//	}
}