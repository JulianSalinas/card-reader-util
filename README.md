# Card Reader Util
Tool created to read credit card's information using NFC.  
Credits to Julien Millau: devnied  
Here is his [REPO](https://github.com/devnied/EMV-NFC-Paycard-Enrollment)


### How to install? 
Write these lines in your `build.gradle`

```groovy
implementation 'com.github.devnied.emvnfccard:library:3.0.0'
implementation 'com.gbsys.card-reader-util:card-reader-util:0.1.0'
```

Add the necessary permissions to your `AndroidManifest.xml` 

```xml
<manifest>
    <uses-permission android:name="android.permission.NFC" />
    <uses-feature android:name="android.hardware.nfc" android:required="true" />
</manifest>
```

Add the following `intent-filter` to the activity in your `AndroidManifest.xml` 

```xml 
<!-- Activity which is going to use NFC-->
<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.nfc.action.TAG_DISCOVERED"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>
</activity>
```

### How to use it?
Declare the NFC Adapter and one Pending Intent in your Activity.

```kotlin
class MainActivity : AppCompatActivity() {
    private var nfcAdapter : NfcAdapter? = null 
    private var nfcPendingIntent: PendingIntent? = null
    /// other stuff...
}
```

Initialize them in the `OnCreate` lifecycle event as follows:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    /// ...other stuff
    nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    val intent = Intent(this, javaClass)
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
}
```

Remember to enable the NFC Adapter in the `OnResume` lifecycle event: 

```kotlin
override fun onResume() {
    super.onResume()
    // Alternative: Add intent filter
    nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null);
}
```

And disable it in the `OnPause` lifecycle event:

```kotlin
override fun onPause() {
    super.onPause()
    nfcAdapter?.disableForegroundDispatch(this);
}
```

When contactless chip is read, `onNewIntent` is triggered.

```kotlin
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    if (intent != null) processIntent(intent)
}
```

We must be sure the correct intent's action is `ACTION_TAG_DISCOVERED`. 
Furthermore, the tag should not be null, otherwise it was not read correctly.


```kotlin
fun processIntent(intent: Intent) {
    val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
    val isRequiredAction = intent.action == NfcAdapter.ACTION_TAG_DISCOVERED
    if (!isRequiredAction || tag == null) return else processTag(tag)
}
```

Finally, we execute the `doRead` to get the card's information.


```kotlin
private fun processTag(tag: Tag) {
    try {
        val terminal = Terminal(tag)
        val reader = CardReader(terminal)
        
        // We have gotten the information correctly
        val paycard = reader.doRead()
        
        // Render Stuff
        val fragment = CardFragment.newInstance()
        val bundle = Bundle()
        bundle.putParcelable("paycard", paycard)
        // ...
    }
    catch (e: Exception) {
        Log.e("MainActivity", e.message ?: "Unknown exception")
    }
}
```
