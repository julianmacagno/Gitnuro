package com.jetpackduba.gitnuro.data.git.signers

import org.apache.sshd.common.NamedResource
import org.apache.sshd.common.config.keys.FilePasswordProvider
import org.apache.sshd.common.config.keys.KeyUtils
import org.apache.sshd.common.signature.BuiltinSignatures
import org.apache.sshd.common.util.buffer.ByteArrayBuffer
import org.apache.sshd.common.util.security.SecurityUtils
import java.io.File
import java.security.MessageDigest
import java.util.Base64

private const val MAGIC = "SSHSIG"
private const val NAMESPACE = "git"
private const val HASH_ALGORITHM = "sha512"

/**
 * Creates an armored SSH signature (the format `git` uses for `gpg.format=ssh`, see PROTOCOL.sshsig in OpenSSH).
 * Throws if the key can't be loaded (wrong password included).
 */
fun signWithSshKey(data: ByteArray, privateKeyPath: String, password: String?): String {
    val keyPair = File(privateKeyPath).inputStream().use { input ->
        SecurityUtils.loadKeyPairIdentities(
            null,
            NamedResource.ofName(privateKeyPath),
            input,
            if (password.isNullOrEmpty()) FilePasswordProvider.EMPTY else FilePasswordProvider.of(password),
        )
    }.firstOrNull() ?: error("No private key found in $privateKeyPath")

    val keyType = KeyUtils.getKeyType(keyPair)
    val signatureFactory = when (keyType) {
        KeyUtils.RSA_ALGORITHM, "ssh-rsa" -> BuiltinSignatures.rsaSHA512
        else -> BuiltinSignatures.fromFactoryName(keyType)
    } ?: error("Unsupported key type $keyType")

    val messageHash = MessageDigest.getInstance("SHA-512").digest(data)

    // What is actually signed
    val toSign = ByteArrayBuffer().apply {
        putRawBytes(MAGIC.toByteArray())
        putString(NAMESPACE)
        putString("") // reserved
        putString(HASH_ALGORITHM)
        putBytes(messageHash)
    }.compactData

    val signer = signatureFactory.create()
    signer.initSigner(null, keyPair.private)
    signer.update(null, toSign)
    val rawSignature = signer.sign(null)

    val signatureBlob = ByteArrayBuffer().apply {
        // SSH algorithm name (rsa-sha2-512, ssh-ed25519...). Cast needed: `name` on the enum would be its constant name
        putString((signatureFactory as NamedResource).name)
        putBytes(rawSignature)
    }.compactData

    val blob = ByteArrayBuffer().apply {
        putRawBytes(MAGIC.toByteArray())
        putInt(1) // version
        putPublicKey(keyPair.public)
        putString(NAMESPACE)
        putString("") // reserved
        putString(HASH_ALGORITHM)
        putBytes(signatureBlob)
    }.compactData

    val encoded = Base64.getMimeEncoder(70, "\n".toByteArray()).encodeToString(blob)

    return "-----BEGIN SSH SIGNATURE-----\n$encoded\n-----END SSH SIGNATURE-----\n"
}
