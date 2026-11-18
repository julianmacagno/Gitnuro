package com.jetpackduba.gitnuro.data.git.lfs

import com.jetpackduba.gitnuro.data.git.credentials.SshCredentialsProvider
import com.jetpackduba.gitnuro.domain.extensions.readUntilValue
import com.jetpackduba.gitnuro.domain.interfaces.IAuthenticateLfsServerWithSshGitAction
import com.jetpackduba.gitnuro.domain.lfs.LfsSshAuthenticateResult
import com.jetpackduba.gitnuro.domain.models.LfsOperationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.eclipse.jgit.lfs.errors.LfsException
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FS
import javax.inject.Inject
import javax.inject.Provider


class AuthenticateLfsServerWithSshGitAction @Inject constructor(
    private val sshCredentialsProvider: Provider<SshCredentialsProvider>,
) : IAuthenticateLfsServerWithSshGitAction {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend operator fun invoke(
        lfsServerUrl: String,
        lfsOperationType: LfsOperationType,
    ): LfsSshAuthenticateResult = withContext(Dispatchers.IO) {
        val operation = when (lfsOperationType) {
            LfsOperationType.UPLOAD -> "upload"
            LfsOperationType.DOWNLOAD -> "download"
        }

        val uri = URIish(lfsServerUrl)

        val sshRemoteSession = SshSessionFactory.getInstance()
            .getSession(uri, sshCredentialsProvider.get(), FS.DETECTED, 0 /*no timeout*/)

        val inputString: String
        val errorString: String

        try {
            val process = sshRemoteSession.exec("git-lfs-authenticate ${uri.path} $operation", 0)

            inputString = String(process.inputStream.readUntilValue(-1))
            errorString = String(process.errorStream.readUntilValue(-1))
        } finally {
            sshRemoteSession.disconnect()
        }

        if (errorString.isNotBlank()) {
            throw LfsException(errorString)
        } else if (inputString.isBlank()) {
            throw LfsException("SSH LFS Authentication failed, server returned invalid empty data.")
        }

        json.decodeFromString<LfsSshAuthenticateResult>(inputString)
    }
}