package com.jetpackduba.gitnuro.data.git.signers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.nio.file.Path

/**
 * Signs with [signWithSshKey] and verifies with OpenSSH's own `ssh-keygen -Y check-novalidate`.
 */
class SshSignatureTest {
    @TempDir
    lateinit var dir: Path

    private fun run(vararg command: String, stdin: File? = null): Int {
        val (code, output) = runWithOutput(*command, stdin = stdin)
        if (code != 0) println("${command.toList()} -> $code: $output")
        return code
    }

    private fun runWithOutput(vararg command: String, stdin: File? = null): Pair<Int, String> {
        val process = ProcessBuilder(*command)
            .directory(dir.toFile())
            .redirectErrorStream(true)
            .apply { if (stdin != null) redirectInput(stdin) }
            .start()
        val output = String(process.inputStream.readAllBytes())
        return process.waitFor() to output
    }

    @ParameterizedTest
    @ValueSource(strings = ["ed25519", "rsa", "ecdsa"])
    fun `signature is accepted by ssh-keygen`(type: String) {
        assumeTrue(runCatching { run("ssh-keygen", "-?") }.isSuccess)

        val key = File(dir.toFile(), "key")
        assertEquals(0, run("ssh-keygen", "-q", "-t", type, "-N", "secret", "-f", key.path))

        val data = "tree abc\nauthor Someone\n\nA commit".toByteArray()
        val dataFile = File(dir.toFile(), "data").apply { writeBytes(data) }
        val signature = File(dir.toFile(), "data.sig").apply {
            writeText(signWithSshKey(data, key.path, "secret"))
        }

        assertEquals(
            0,
            run("ssh-keygen", "-Y", "check-novalidate", "-n", "git", "-s", signature.path, stdin = dataFile),
        )
    }
}
