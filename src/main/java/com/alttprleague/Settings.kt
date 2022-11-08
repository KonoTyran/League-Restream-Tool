package com.alttprleague

import java.util.*
import java.util.prefs.Preferences

class Settings() {

    private val obsPasswordKey = "obs password"
    private val obsPortKey = "obs port"
    private val obsServerKey = "obs server"
    private val restreamerKey = "restreamer code"
    private val root = Preferences.userRoot().node(LeagueRestreamTool::class.java.name.lowercase(Locale.getDefault()))

    var authKey: String = root[restreamerKey, ""]
        set(value) {
            field = value;
            root.put(restreamerKey, value)
        }

    var obsServer: String = root[obsServerKey, "localhost"]
        set(value) {
            field = value;
            root.put(obsServerKey, value)
        }

    var obsPassword: String = root[obsPasswordKey, ""]
        set(value) {
            field = value;
            root.put(obsPasswordKey, value)
        }

    var obsPort: Int = root.getInt(obsPortKey, 8456)
        set(value) {
            field = value;
            root.putInt(obsPortKey, value)
        }
}