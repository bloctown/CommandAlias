package de.elliepotato.commandalias.command

import com.google.common.collect.Lists
import de.elliepotato.commandalias.CommandAlias
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * Created by Ellie on 27/07/2017 for PublicPlugins.
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
class CommandHandle(private val plugin: CommandAlias) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!plugin.error.isNullOrEmpty())
            msg(sender, "${ChatColor.RED}Warning! The plugin has detected an error on start up! Check console. Error description: ${plugin.error}")

        if (args.isEmpty()) {
            msg(sender, correctUsage())
            msg(sender, "${ChatColor.GRAY}You can find support at ${ChatColor.AQUA}www.elliepotato.de/support")
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            "toggle" -> handleToggle(sender, args)
            else -> msg(sender, correctUsage())
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        if (args.size == 1) {
            return listOf("reload", "toggle").filter { s -> s.startsWith(args[0].lowercase()) }.toMutableList()
        }

        return Lists.newArrayList()
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("commandalias.reload"))
            return msg(sender, plugin.noPermission)

        plugin.reload()
        if (!plugin.error.isNullOrEmpty()) msg(sender, "${ChatColor.RED}Warning! The plugin has detected an error whilst reloading! Check console. Error description: ${plugin.error}")
        msg(sender, "Reloaded.")
    }

    private fun handleToggle(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("commandalias.toggle"))
            return msg(sender, plugin.noPermission)

        if (!plugin.error.isNullOrEmpty()) {
            msg(sender, "${ChatColor.RED}Warning! The plugin has detected an error on start up so " +
                    "this sub-command cannot be executed! Check console. Error description: ${plugin.error}")
            return // soz
        }

        if (args.size != 2) {
            msg(sender, correctUsage())
            return
        }
        val label = args[1]

        val changed = plugin.toggleAlias(label)
        if (!changed)
            msg(sender, "Couldn't find alias with the label of '$label'.")
        else {
            msg(sender, "Toggled '$label'.")
        }
    }

    private fun msg(sender: CommandSender, msg: String) = sender.sendMessage(plugin.prefix + msg)

    private fun correctUsage(): String = "Correct usage: ${ChatColor.GRAY}/ca <reload | toggle <label>>"

}