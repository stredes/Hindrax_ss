package com.hindrax.ss.domain.devices

data class KnownPeerIdentity(
    val id: String,
    val nickname: String?,
    val lastKnownAddress: String
)

data class ReusablePeerIdentity(
    val previousId: String,
    val nickname: String?
)

object PeerIdentityResolver {
    fun findReusableIdentity(
        deviceId: String,
        address: String,
        knownPeers: List<KnownPeerIdentity>
    ): ReusablePeerIdentity? {
        val normalizedAddress = address.trim()
        if (normalizedAddress.isBlank()) return null

        return knownPeers
            .firstOrNull { peer ->
                peer.id != deviceId &&
                    peer.lastKnownAddress.equals(normalizedAddress, ignoreCase = true) &&
                    !peer.nickname.isNullOrBlank()
            }
            ?.let { peer ->
                ReusablePeerIdentity(
                    previousId = peer.id,
                    nickname = peer.nickname
                )
            }
    }
}
