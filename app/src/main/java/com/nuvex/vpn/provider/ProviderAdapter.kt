package com.nuvex.vpn.provider

/**
 * Adapter boundary for Vox, KeepSolid OEM, WLVPN, or another provider after written
 * partner approval and credentials are available. This class deliberately does not
 * invent endpoints or embed provider credentials.
 */
interface ProviderAdapter : VpnProvider
