package africa.iag.erp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import africa.iag.erp.android.ui.ErpApp
import africa.iag.erp.android.ui.theme.ErpAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ErpAndroidApp
        setContent {
            ErpAndroidTheme(store = app.store) {
                ErpApp(store = app.store)
            }
        }
    }
}
