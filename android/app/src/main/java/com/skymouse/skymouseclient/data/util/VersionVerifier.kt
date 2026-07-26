package com.skymouse.skymouseclient.data.util


data class Version(val major: Int, val minor: Int) {
    companion object {
        /**
         * Expects a string in the format "X.Y", where X and Y are integers.
         * X is the major version and Y is the minor version.
         * Returns null if the string is not in the expected format.
         */
        fun parse(version: String): Version? {
            val parts = version.split(".")
            if (parts.size != 2) return null

            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null

            return Version(major, minor)
        }
    }
}

sealed interface VersionVerificationResult {
    data object Valid : VersionVerificationResult
    data class Warning(val message: String) : VersionVerificationResult
    data class Mismatch(val reason: String) : VersionVerificationResult
}

object VersionVerifier {

    fun verify(clientVersion: String, serverVersion: String): VersionVerificationResult {
        val client = Version.parse(clientVersion)
            ?: return VersionVerificationResult.Mismatch("Client version parse failed: $clientVersion")

        val server = Version.parse(serverVersion)
            ?: return VersionVerificationResult.Mismatch("Server version parse failed: $serverVersion")

        if (client.major != server.major) {
            return VersionVerificationResult.Mismatch("Major version mismatch (Client: $clientVersion, Server: $serverVersion)")
        }

        return when {
            server.minor > client.minor ->
                VersionVerificationResult.Warning("Server minor version ($serverVersion) is higher than client ($clientVersion)\nClient update recommended")
            server.minor < client.minor ->
                VersionVerificationResult.Warning("Server minor version ($serverVersion) is lower than client ($clientVersion)\nServer update recommended")
            else ->
                VersionVerificationResult.Valid
        }
    }
}