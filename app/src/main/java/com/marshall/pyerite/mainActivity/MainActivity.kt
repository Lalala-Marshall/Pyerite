package com.marshall.pyerite.mainActivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.marshall.pyerite.appNavHost.AppNavHost
import com.marshall.pyerite.eveAuthModule.sso.EveSsoCallbackBus
import com.marshall.pyerite.eveAuthModule.sso.EveSsoConfig
import com.marshall.pyerite.ui.theme.PyeriteTheme
import org.koin.android.ext.android.inject
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    private val ssoCallbackBus: EveSsoCallbackBus by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleSsoCallbackIntent(intent)
        setContent {
            PyeriteTheme {
                AppNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSsoCallbackIntent(intent)
    }

    private fun handleSsoCallbackIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (!isSsoCallback(uri)) return
        ssoCallbackBus.offer(uri)
    }

    private fun isSsoCallback(uri: Uri): Boolean {
        val expected = EveSsoConfig.redirectUri.toUri()
        val schemeOk = uri.scheme.equals(expected.scheme, ignoreCase = true)
        val hostOk = uri.host.equals(expected.host, ignoreCase = true)
        val expectedPath = expected.path.orEmpty().trimEnd('/')
        val actualPath = uri.path.orEmpty().trimEnd('/')
        val pathOk = expectedPath.isEmpty() ||
            expectedPath.equals(actualPath, ignoreCase = true)
        return schemeOk && hostOk && pathOk
    }
}
