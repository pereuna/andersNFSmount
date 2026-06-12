package dev.andersnfs

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread {
            try {
                val server = "10.77.0.1"
                val export = "/home/lapere"

                // NFS-kirjaston API riippuu käytetystä kirjastosta.
                val client = Nfs3(server)
                val fs = client.mount(export)

                val files = fs.list("/")

                runOnUiThread {
                    println(files)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
