package dev.andersnfs

/**
 * Minimal JVM-side stand-ins so this proof-of-concept can compile before the
 * repository is converted into a full Android project.
 */
open class AppCompatActivity {
    open fun onCreate(savedInstanceState: Bundle?) = Unit

    fun runOnUiThread(action: () -> Unit) {
        action()
    }
}

class Bundle
