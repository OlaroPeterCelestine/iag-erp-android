package africa.iag.erp.android

import android.app.Application
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.KeyValueStore

class ErpAndroidApp : Application() {
    lateinit var store: ErpStore
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("iag-erp-android", MODE_PRIVATE)
        store = ErpStore(
            persistence = object : KeyValueStore {
                override fun get(key: String): String? = prefs.getString(key, null)
                override fun put(key: String, value: String) {
                    prefs.edit().putString(key, value).apply()
                }
            },
        )
        store.load()
    }
}
