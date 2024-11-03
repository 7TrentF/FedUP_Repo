package com.example.fedup_foodwasteapp


import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    fun wrap(Context: Context, languageCode: String): ContextWrapper {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        val newContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Context.createConfigurationContext(config)
        } else {
            Context.resources.updateConfiguration(config, Context.resources.displayMetrics)
            Context
        }

        return ContextWrapper(newContext)
    }
}
