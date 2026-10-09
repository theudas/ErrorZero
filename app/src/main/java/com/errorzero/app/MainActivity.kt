package com.errorzero.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import com.errorzero.app.ui.AppNav
import com.errorzero.app.ui.theme.ErrorZeroTheme

class MainActivity : ComponentActivity() {
    /** 每次从通知进入时递增，驱动界面切到"重做"页 */
    private val openReviewSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consume(intent)
        setContent {
            ErrorZeroTheme {
                AppNav(openReviewSignal = openReviewSignal.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consume(intent)
    }

    private fun consume(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_REVIEW, false) == true) {
            intent.removeExtra(EXTRA_OPEN_REVIEW)
            openReviewSignal.intValue++
        }
    }

    companion object {
        const val EXTRA_OPEN_REVIEW = "open_review"
    }
}
