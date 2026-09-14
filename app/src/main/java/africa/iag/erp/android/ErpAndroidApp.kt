package africa.iag.erp.android

import android.app.Application
import android.os.Handler
import android.os.Looper
import africa.iag.erp.core.ErpApi
import africa.iag.erp.core.ErpConfig
import africa.iag.erp.core.ErpStore
import africa.iag.erp.core.KeyValueStore

class ErpAndroidApp : Application() {
    lateinit var store: ErpStore
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("iag-erp-android", MODE_PRIVATE)
        val persistence = object : KeyValueStore {
            override fun get(key: String): String? = prefs.getString(key, null)
            override fun put(key: String, value: String) {
                prefs.edit().putString(key, value).apply()
            }
        }
        val main = Handler(Looper.getMainLooper())
        val origin = ErpConfig.origin(persistence)
        store = ErpStore(
            persistence = persistence,
            api = ErpApi(origin = origin),
            onMain = { action ->
                if (Looper.myLooper() == Looper.getMainLooper()) action()
                else main.post(action)
            },
        )
        store.load()
        Thread { store.resumeRemoteSession() }.start()
    }
}
