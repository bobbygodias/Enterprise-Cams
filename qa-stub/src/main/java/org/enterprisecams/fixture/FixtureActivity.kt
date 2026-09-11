package org.enterprisecams.fixture

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** Test fixture installed ONLY in the disposable CI emulator. Never distribute. */
class FixtureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "SIMULADOR QA\n\nEncaminhamento recebido.\nEste NÃO é o aplicativo Yoosee.\nNenhuma câmera real conectada."
            textSize = 24f
            setPadding(32, 100, 32, 32)
        })
    }
}
