package com.gbsys.card_reader_util

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.gbsys.card_reader_util.reader.CardReader
import com.gbsys.card_reader_util.reader.Terminal
import com.gbsys.card_reader_util.ui.main.CardFragment
import com.gbsys.card_reader_util.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    private var nfcAdapter : NfcAdapter? = null


    private var nfcPendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        if (savedInstanceState == null) supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, MainFragment.newInstance())
            .commitNow()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        val intent = Intent(this, javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

    }

    override fun onResume() {
        super.onResume()
        // Alternative: Add intent filter
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null);
    }

    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch, as this activity is no longer in the foreground
        nfcAdapter?.disableForegroundDispatch(this);
    }

    /**
     * When contactless chip is read, onNewIntent is fired
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) processIntent(intent)
    }

    /**
     * We must be sure the correct action is ACTION_TAG_DISCOVERED
     * Tag should be null, otherwise the chip was not read correctly
     */
    private fun processIntent(intent: Intent) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val isRequiredAction = intent.action == NfcAdapter.ACTION_TAG_DISCOVERED
        if (!isRequiredAction || tag == null) return else processTag(tag)
    }

    /**
     * Read the card and open the fragment to visualize the information
     */
    private fun processTag(tag: Tag) {

        try {
            val terminal = Terminal(tag)
            val reader = CardReader(terminal)
            val paycard = reader.doRead()

            val fragment = CardFragment.newInstance()
            val bundle = Bundle()
            bundle.putParcelable("paycard", paycard)
            fragment.arguments = bundle
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
        }
        catch (e: Exception) {
            val default = "There is not message for this exception"
            Log.e("MainActivity", e.message ?: default)
        }

    }
    
    fun goBack() {
        val fragment = MainFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

}
