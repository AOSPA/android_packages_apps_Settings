package com.android.settings

import android.widget.TextView
import android.content.res.Configuration
import com.google.android.material.chip.Chip
import com.android.settings.core.BasePreferenceController
import androidx.preference.PreferenceScreen
import com.android.settingslib.widget.LayoutPreference
import androidx.preference.PreferenceViewHolder
import android.content.Context
import com.android.settingslib.Utils
import androidx.core.graphics.ColorUtils

class DisplayImagePreference(val context: Context, key: String):BasePreferenceController (context, key){

    override fun getAvailabilityStatus() = AVAILABLE

    fun TextView.changeColorTo(active: Boolean){
        if(active){
            val accent = Utils.getColorAttrDefaultColor(context, android.R.attr.colorAccent)
            background.setTint(ColorUtils.setAlphaComponent(accent, 120))
/*            val colorOnDef = context.getColor(android.R.color.black)
            setTextColor(colorOnDef)*/
        } else {
            val defColor = context.getColor(android.R.color.white)
            val colorOnDef = context.getColor(android.R.color.black)
/*            background.setTint(ColorUtils.setAlphaComponent(defColor, 120))
            setTextColor(colorOnDef)*/
        }
    }

    override fun displayPreference(s: PreferenceScreen){
        super.displayPreference(s)
        val preferenceViewHolder:LayoutPreference = s.findPreference("screen_display")!!
        val darkMode : TextView = preferenceViewHolder.findViewById(R.id.darkMode)
        val lightMode :TextView = preferenceViewHolder.findViewById(R.id.lightMode)
        val z = context.getResources().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO
	darkMode.changeColorTo(!z)
	lightMode.changeColorTo(z)
    }
}
