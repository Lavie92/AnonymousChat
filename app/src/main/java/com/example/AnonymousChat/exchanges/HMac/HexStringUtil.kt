package com.example.AnonymousChat.exchanges.HMac

object HexStringUtil {
    private val HEX_CHAR_TABLE = "0123456789abcdef"

    /**
     * Convert a byte array to a hexadecimal string
     *
     * @param raw A raw byte array
     * @return Hexadecimal string
     */
    @JvmStatic
    fun byteArrayToHexString(raw: ByteArray): String {
        val hex = StringBuilder(2 * raw.size)
        for (b in raw) {
            val v = b.toInt() and 0xFF
            hex.append(HEX_CHAR_TABLE[v ushr 4])
            hex.append(HEX_CHAR_TABLE[v and 0xF])
        }
        return hex.toString()
    }

    /**
     * Convert a hexadecimal string to a byte array
     *
     * @param hex A hexadecimal string
     * @return The byte array
     */
    fun hexStringToByteArray(hex: String): ByteArray {
        val hexStandard = hex.lowercase()
        val sz = hexStandard.length / 2
        val bytesResult = ByteArray(sz)
        var idx = 0
        for (i in 0 until sz) {
            bytesResult[i] =
                ((Character.digit(hexStandard[idx], 16) shl 4) + Character.digit(hexStandard[idx + 1], 16)).toByte()
            idx += 2
        }
        return bytesResult
    }
}
