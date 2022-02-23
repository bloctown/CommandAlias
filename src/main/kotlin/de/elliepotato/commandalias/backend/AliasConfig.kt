package de.elliepotato.commandalias.backend

import com.google.common.collect.Maps
import de.elliepotato.commandalias.CommandAlias
import org.bukkit.ChatColor
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.yaml.snakeyaml.scanner.ScannerException
import java.util.function.Consumer
import java.util.logging.Level
import java.util.stream.Collectors

/**
 * Created by Ellie on 23/07/2017 for CommandAlias.
 *
 *    Copyright 2017 Ellie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
class AliasConfig(private val plugin: CommandAlias) {

    private var cfg: FileConfiguration

    init {
        plugin.saveDefaultConfig()

        try {
            cfg = plugin.getConfig()

            var changes = false
            if (!cfg.isConfigurationSection("commands")) {
                cfg.createSection("commands")
                changes = true
            }

            /* Since 1.1 */
            if (!cfg.isSet("prefix")) {
                cfg["prefix"] = "&7[&aCommandAlias&7] &c"
                changes = true
            }

            /* Since 1.2 */
            if (!cfg.isSet("noPermission")) {
                cfg["noPermission"] = "{prefix}No permission!"
                changes = true
            }

            /* Since 1.3.2 */
            if (!cfg.isSet("advanced.keep-iterating-when-match")) {
                cfg["advanced.keep-iterating-when-match"] = false
                changes = true
            }

            /* Since 1.4.2 */
            if (!cfg.isSet("advanced.let-command-event-run-if-no-perm")) {
                cfg["advanced.let-command-event-run-if-no-perm"] = false;
                changes = true
            }

            /* Sike
            if (cfg.get("check-version") == null) {
                cfg.set("check-version", true)
                save(cfg)
            }
             */

            if (changes)
                save()

        } catch (e: Throwable) {
            plugin.log("error loading config, please restart!", Level.SEVERE)
            e.printStackTrace()

            cfg = YamlConfiguration()

        }

    }

    /**
     * Reload cached instance
     */
    fun reloadFile() {
        try {
            plugin.reloadConfig()
            cfg = plugin.getConfig()
        } catch (e: Throwable) {
            plugin.log("error reloading config due to something you did, please fix and try again", Level.SEVERE)
            e.printStackTrace()
        }
    }

    /***
     * Load commands from config and return in a map
     *
     * @return a list of commands fed from the configuration file
     * @throws IllegalStateException If a configuration value of a alias is null
     * @throws ScannerException By the YAML parser if the config is invalid.
     * @throws InvalidConfigurationException By the YAML parser if the config is invalid.
     * @throws NullPointerException If the configuration section "commands" doesn't exist
     */
    fun getCommands(): MutableMap<String, AliasCommand> {
        val commands: MutableMap<String, AliasCommand> = Maps.newHashMap()
        try {
            cfg.getConfigurationSection("commands")!!.getKeys(false).forEach(Consumer { path ->
                var label = path.lowercase()
                // enabled
                val enabled = cfg.getBoolean("commands.$path.enabled", true)
                // perm
                val permission = cfg.getString("commands.$path.permission")
                // aliases
                val aliases = cfg.getStringList("commands.$path.aliases").stream()
                        .map { m -> color(m.lowercase()) }
                        .collect(Collectors.toList())
                // console command
                val consoleCommands =
                        if (cfg.isString("commands.$path.console-command"))
                            listOf(cfg.getString("commands.$path.console-command")!!)
                        else cfg.getStringList("commands.$path.console-command")
                // type
                val type: CommandType = CommandType.values().firstOrNull { path.startsWith(it.prefix) }
                        ?: CommandType.CMD
                if (type != CommandType.CMD)
                    label = label.split(type.prefix)[1]

                val runConditions: LinkedHashMap<String, Any> = Maps.newLinkedHashMap()
                // load run conditions
                if (cfg.isConfigurationSection("commands.$path.conditions")) {
                    cfg.getConfigurationSection("commands.$path.conditions")!!.getKeys(false)
                            .forEach { k -> runConditions[k.lowercase()] = cfg["commands.$path.conditions.$k"]!! }
                }

                try {
                    val command = AliasCommand(label, enabled, permission, aliases, type, runConditions, consoleCommands)
                    commands[label.lowercase()] = command
                } catch (e: IllegalStateException) {
                    plugin.log("The config is improperly defined! Cannot load alias $path.", Level.SEVERE)
                    plugin.error = "Failed to set alias instance (${e.message})"
                    e.printStackTrace()
                }
            })
        } catch (ex: Exception) {
            plugin.log("The config is improperly defined! Please refer to http://www.yamllint.com/", Level.SEVERE)
            ex.printStackTrace()
            when (ex) {
                is ScannerException, is InvalidConfigurationException -> plugin.error = "Bad config"
                is java.lang.NullPointerException -> plugin.error = "Configuration section 'commands' doesn't exist"
                else -> plugin.error = "unknown error occurred, check console for details"
            }
        }

        return commands
    }

    /***
     * Toggles an alias' enabled state in the config and in the given map.
     *
     * @return Pair<HashMap<String, AliasCommand> = The new HashMap, boolean (has the map been modified?)
     */
    fun toggleAlias(commands: MutableMap<String, AliasCommand>, label: String): Pair<MutableMap<String, AliasCommand>, Boolean> {
        val alias: AliasCommand = commands[label.lowercase()] ?: return Pair(commands, false)

        cfg.set("commands.${alias.serialiseLabel().lowercase()}.enabled", !alias.enabled)
        save()
        alias.enabled = !alias.enabled

        return Pair(commands, true)
    }

    /* Setting getters */

    fun getPrefix(): String = color(cfg.getString("prefix", "&7[&aCommandAlias&7] &c")!!)

    fun getNoPerm(): String = color(cfg.getString("noPermission")!!.replace("{prefix}", getPrefix()))

    fun isVersionChecking(): Boolean = cfg.getBoolean("version-check", false)

    fun isBreakAfterAliasMatch(): Boolean = !cfg.getBoolean("advanced.keep-iterating-when-match", false)

    fun isLetCmdRunIfNoPerm(): Boolean = cfg.getBoolean("advanced.let-command-event-run-if-no-perm", false)

    /**
     * Colors a message with '&'
     */
    private fun color(msg: String): String = ChatColor.translateAlternateColorCodes('&', msg)

    private fun save() {
        plugin.saveConfig()
    }

}