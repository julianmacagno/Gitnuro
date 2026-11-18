package com.jetpackduba.gitnuro


import com.jetpackduba.gitnuro.di.DaggerAppComponent
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.awt.Toolkit
import java.security.Security


suspend fun main(args: Array<String>) {
    if (Installer.run(args)) return

    if (args.contains("--graalvm")) {
        val currentDir = System.getProperty("user.dir")

        System.setProperty("java.home", currentDir)
    }

    runCatching {
        Toolkit.getDefaultToolkit().javaClass.getDeclaredField("awtAppClassName").apply {
            isAccessible = true
            set(null, "gitia")
        }
    }

    Security.addProvider(BouncyCastleProvider())

    val app: App = DaggerAppComponent
        .create()
        .app()

    app.start(args)
}
