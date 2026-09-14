package com.devwithzachary.completelinuxinstaller.engine

import java.io.File

object FastfetchConfig {

    private const val ESC = "\u001B"
    private const val ORANGE = "$ESC[38;5;208m"
    private const val GREEN = "$ESC[38;5;112m"
    private const val YELLOW = "$ESC[38;5;214m"
    private const val WHITE = "$ESC[97;1m"
    private const val DARK = "$ESC[38;5;238m"
    private const val RESET = "$ESC[0m"

    val LOGO_CONTENT: String = buildString {
        append("                     " + DARK + "@@@@@@@" + RESET + "                      \n")
        append("                   " + DARK + "@@@@@@@@@@@" + RESET + "                    \n")
        append("                 " + ORANGE + "++" + DARK + "@@@@@@@@@@@#" + ORANGE + "++" + RESET + "                 \n")
        append("             " + ORANGE + "++++++" + DARK + "%" + ORANGE + "+" + WHITE + "." + YELLOW + "-" + DARK + "@*" + WHITE + ".." + DARK + "*@@#" + ORANGE + "++++++" + RESET + "             \n")
        append("          " + ORANGE + "+++++++++" + DARK + "#*@*" + ORANGE + "+" + YELLOW + "-" + DARK + "@%#@@*" + ORANGE + "++++++++" + RESET + "           \n")
        append("        " + ORANGE + "+++++++++++" + DARK + "*%" + YELLOW + "=-----" + ORANGE + "+" + DARK + "@@*" + ORANGE + "+++++++++++" + RESET + "        \n")
        append("       " + ORANGE + "++" + DARK + "*%%**" + ORANGE + "+++++" + DARK + "*@#" + ORANGE + "+" + YELLOW + "==" + ORANGE + "+" + DARK + "%@@@%" + ORANGE + "++++++++++++" + RESET + "       \n")
        append("      " + ORANGE + "++++" + DARK + "#@@@@%#*#@" + ORANGE + "+" + DARK + "#@@@*" + WHITE + ":." + DARK + "#@@%*" + ORANGE + "+++++++++++" + RESET + "      \n")
        append("     " + ORANGE + "++++++" + DARK + "*@@@@@@@" + YELLOW + "-" + RESET + "  " + WHITE + "::." + RESET + "    " + DARK + "*@@@%" + ORANGE + "+++++++++++" + RESET + "     \n")
        append("    " + ORANGE + "++++++++" + DARK + "*@@@@@" + YELLOW + "-" + WHITE + "..." + RESET + "  " + WHITE + "...." + RESET + " " + WHITE + "." + DARK + "*@@@@*" + ORANGE + "++++++++++" + RESET + "    \n")
        append("    " + ORANGE + "+++++++++" + DARK + "#@@@" + YELLOW + "-" + WHITE + "...." + RESET + "  " + WHITE + "....." + RESET + " " + WHITE + ":" + DARK + "%@@@@*" + ORANGE + "+++++++++" + RESET + "    \n")
        append("   " + ORANGE + "++++++++++" + RESET + " " + DARK + "@@*" + WHITE + "." + RESET + "             " + YELLOW + "=" + DARK + "#@@@@" + ORANGE + "++++++++++" + RESET + "   \n")
        append("   " + ORANGE + "++++++++++" + RESET + " " + DARK + "@@" + YELLOW + "=" + WHITE + "." + RESET + "            " + WHITE + "." + ORANGE + "+" + DARK + "@@@@@*" + ORANGE + "+++++++++" + RESET + "   \n")
        append("   " + ORANGE + "++++++++++" + DARK + "@@@" + YELLOW + "-" + RESET + "             " + YELLOW + "=" + DARK + "@@@@@@#" + ORANGE + "+++++++++" + RESET + "   \n")
        append("   " + ORANGE + "++++++++++" + YELLOW + "---" + WHITE + ":." + RESET + "           " + WHITE + ".:" + YELLOW + "-=" + DARK + "%@@*" + ORANGE + "++++++++++" + RESET + "   \n")
        append("    " + ORANGE + "+++++" + YELLOW + "=------" + WHITE + ":." + RESET + "           " + WHITE + ".:" + YELLOW + "------=" + ORANGE + "++++++++" + RESET + "    \n")
        append("    " + ORANGE + "+++++" + YELLOW + "---------" + WHITE + ":." + RESET + "         " + WHITE + "." + YELLOW + "--------" + ORANGE + "++++++++" + RESET + "    \n")
        append("    " + ORANGE + "+++++" + YELLOW + "----------" + WHITE + ":::...:" + YELLOW + "-" + ORANGE + "+" + DARK + "%*" + YELLOW + "----------" + ORANGE + "+++++" + RESET + "     \n")
        append("     " + ORANGE + "+++" + YELLOW + "=-==---------" + DARK + "@@@@@@@@" + YELLOW + "------=" + ORANGE + "+" + YELLOW + "==" + ORANGE + "+++++" + RESET + "      \n")
        append("       " + ORANGE + "++++" + YELLOW + "====-----=" + ORANGE + "+" + YELLOW + "=---===------=-=" + ORANGE + "+++++" + RESET + "       \n")
        append("        " + ORANGE + "+++" + YELLOW + "======-====--------------==" + ORANGE + "++++" + RESET + "        \n")
        append("          " + GREEN + "======" + WHITE + "..." + GREEN + "===--------" + WHITE + "..." + GREEN + "-----=" + ORANGE + "+" + RESET + "          \n")
        append("         " + GREEN + "============-------------------" + RESET + "          \n")
        append("        " + GREEN + "=============-------------------" + RESET + "          \n")
        append("        " + GREEN + "=============-------------------" + RESET + "          \n")
    }

    val CONFIG_JSONC: String = """
        {
          "${'$'}schema": "https://github.com/fastfetch-cli/fastfetch/raw/master/doc/json_schema.json",
          "logo": {
            "source": "/etc/fastfetch/logo.txt",
            "type": "file-raw",
            "position": "top",
            "padding": {
              "top": 1,
              "bottom": 1
            }
          },
          "display": {
            "separator": "  ->  "
          },
          "modules": [
            "title",
            "separator",
            { "type": "os", "key": "OS", "keyColor": "38;5;208" },
            { "type": "host", "key": "Host", "keyColor": "38;5;208" },
            { "type": "kernel", "key": "Kernel", "keyColor": "38;5;208" },
            { "type": "uptime", "key": "Uptime", "keyColor": "38;5;208" },
            { "type": "packages", "key": "Packages", "keyColor": "38;5;208" },
            { "type": "shell", "key": "Shell", "keyColor": "38;5;208" },
            { "type": "cpu", "key": "CPU", "keyColor": "38;5;208" },
            { "type": "memory", "key": "Memory", "keyColor": "38;5;208" },
            { "type": "disk", "key": "Disk", "keyColor": "38;5;208" },
            "break",
            "colors"
          ]
        }
    """.trimIndent() + "\n"

    fun ensureFastfetchConfig(rootfsDir: File) {
        try {
            val fastfetchDir = File(rootfsDir, "etc/fastfetch")
            if (!fastfetchDir.exists()) {
                fastfetchDir.mkdirs()
            }
            val logoFile = File(fastfetchDir, "logo.txt")
            if (!logoFile.exists() || logoFile.length() == 0L) {
                logoFile.writeText(LOGO_CONTENT)
            }
            val configFile = File(fastfetchDir, "config.jsonc")
            if (!configFile.exists() || configFile.length() == 0L) {
                configFile.writeText(CONFIG_JSONC)
            }
        } catch (_: Exception) {}
    }
}
