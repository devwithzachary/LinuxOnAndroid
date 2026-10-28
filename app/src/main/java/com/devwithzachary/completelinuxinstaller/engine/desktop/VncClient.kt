package com.devwithzachary.completelinuxinstaller.engine.desktop

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class VncClient {

    companion object {
        private const val TAG = "VncClient"
        private const val RFB_VERSION_3_8 = "RFB 003.008\n"

        // RFB Encodings
        const val ENCODING_RAW = 0
        const val ENCODING_COPY_RECT = 1
        const val ENCODING_DESKTOP_SIZE = -223

        // RFB Client-to-Server Message Types
        const val MSG_SET_PIXEL_FORMAT = 0
        const val MSG_SET_ENCODINGS = 2
        const val MSG_FRAMEBUFFER_UPDATE_REQUEST = 3
        const val MSG_KEY_EVENT = 4
        const val MSG_POINTER_EVENT = 5
        const val MSG_CLIENT_CUT_TEXT = 6

        // RFB Server-to-Client Message Types
        const val MSG_FRAMEBUFFER_UPDATE = 0
        const val MSG_SET_COLOUR_MAP_ENTRIES = 1
        const val MSG_BELL = 2
        const val MSG_SERVER_CUT_TEXT = 3

        // Security Types
        const val SEC_TYPE_INVALID = 0
        const val SEC_TYPE_NONE = 1
        const val SEC_TYPE_VNC_AUTH = 2

        fun reverseBits(b: Byte): Byte {
            var v = b.toInt() and 0xFF
            var r = 0
            for (i in 0 until 8) {
                r = (r shl 1) or (v and 1)
                v = v ushr 1
            }
            return r.toByte()
        }

        fun encryptChallenge(challenge: ByteArray, password: String): ByteArray {
            val keyBytes = ByteArray(8)
            val passBytes = password.toByteArray(Charsets.US_ASCII)
            for (i in 0 until 8) {
                if (i < passBytes.size) {
                    keyBytes[i] = reverseBits(passBytes[i])
                } else {
                    keyBytes[i] = 0
                }
            }
            val keySpec = SecretKeySpec(keyBytes, "DES")
            val cipher = Cipher.getInstance("DES/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            return cipher.doFinal(challenge)
        }
    }

    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionJob: Job? = null

    private var socket: Socket? = null
    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null

    private val _connectionState = MutableStateFlow<VncConnectionState>(VncConnectionState.Disconnected)
    val connectionState: StateFlow<VncConnectionState> = _connectionState.asStateFlow()

    private val _refreshTrigger = MutableStateFlow<Long>(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

    private val _clipboardText = MutableStateFlow<String?>(null)
    val clipboardText: StateFlow<String?> = _clipboardText.asStateFlow()

    var desktopWidth: Int = 1280
        private set
    var desktopHeight: Int = 720
        private set
    var desktopName: String = "Linux Desktop"
        private set

    @Volatile
    var bitmap: Bitmap? = null
        private set

    // Reusable buffers to avoid allocating in tight render loop
    private var rawPixelBuffer = ByteArray(1280 * 720 * 4)
    private var argbBuffer = IntArray(1280 * 720)

    val isConnected: Boolean
        get() = _connectionState.value is VncConnectionState.Connected

    fun connect(
        host: String = "127.0.0.1",
        port: Int = 5901,
        password: String = "user"
    ) {
        disconnect()

        connectionJob = clientScope.launch {
            try {
                _connectionState.value = VncConnectionState.Connecting("Connecting to $host:$port...")

                val s = Socket()
                s.tcpNoDelay = true
                s.connect(InetSocketAddress(host, port), 5000)
                socket = s

                val dis = DataInputStream(BufferedInputStream(s.getInputStream(), 65536))
                val dos = DataOutputStream(BufferedOutputStream(s.getOutputStream(), 16384))
                inputStream = dis
                outputStream = dos

                // 1. Version Handshake
                val versionBuf = ByteArray(12)
                dis.readFully(versionBuf)
                val serverVersion = String(versionBuf, Charsets.US_ASCII)
                Log.d(TAG, "Server RFB version: ${serverVersion.trim()}")

                // Reply with 3.8 protocol
                dos.write(RFB_VERSION_3_8.toByteArray(Charsets.US_ASCII))
                dos.flush()

                // 2. Security Handshake
                val numSecTypes = dis.readUnsignedByte()
                if (numSecTypes == 0) {
                    val reasonLen = dis.readInt()
                    val reasonBytes = ByteArray(reasonLen)
                    dis.readFully(reasonBytes)
                    val reason = String(reasonBytes, Charsets.UTF_8)
                    throw IllegalStateException("VNC connection rejected: $reason")
                }

                val secTypes = ByteArray(numSecTypes)
                dis.readFully(secTypes)
                Log.d(TAG, "Available security types: ${secTypes.map { it.toInt() and 0xFF }}")

                val chosenSecType = when {
                    secTypes.any { it.toInt() == SEC_TYPE_NONE } -> SEC_TYPE_NONE
                    secTypes.any { it.toInt() == SEC_TYPE_VNC_AUTH } -> SEC_TYPE_VNC_AUTH
                    else -> throw IllegalStateException("No supported VNC security type. Available: ${secTypes.toList()}")
                }

                dos.writeByte(chosenSecType)
                dos.flush()

                if (chosenSecType == SEC_TYPE_VNC_AUTH) {
                    val challenge = ByteArray(16)
                    dis.readFully(challenge)
                    val response = encryptChallenge(challenge, password)
                    dos.write(response)
                    dos.flush()

                    val secResult = dis.readInt()
                    if (secResult != 0) {
                        var errMsg = "VNC Authentication failed"
                        try {
                            val errLen = dis.readInt()
                            val errBytes = ByteArray(errLen)
                            dis.readFully(errBytes)
                            errMsg += ": " + String(errBytes, Charsets.UTF_8)
                        } catch (_: Exception) {}
                        throw IllegalStateException(errMsg)
                    }
                } else if (chosenSecType == SEC_TYPE_NONE) {
                    val secResult = dis.readInt()
                    if (secResult != 0) {
                        throw IllegalStateException("Security result failed with None type ($secResult)")
                    }
                }

                // 3. ClientInit (Shared = 1)
                dos.writeByte(1)
                dos.flush()

                // 4. ServerInit
                desktopWidth = dis.readUnsignedShort()
                desktopHeight = dis.readUnsignedShort()

                // 16 bytes pixel format
                val bpp = dis.readUnsignedByte()
                val depth = dis.readUnsignedByte()
                val bigEndian = dis.readUnsignedByte()
                val trueColor = dis.readUnsignedByte()
                val redMax = dis.readUnsignedShort()
                val greenMax = dis.readUnsignedShort()
                val blueMax = dis.readUnsignedShort()
                val redShift = dis.readUnsignedByte()
                val greenShift = dis.readUnsignedByte()
                val blueShift = dis.readUnsignedByte()
                dis.skipBytes(3) // padding

                val nameLen = dis.readInt()
                val nameBytes = ByteArray(nameLen)
                dis.readFully(nameBytes)
                desktopName = String(nameBytes, Charsets.UTF_8)
                Log.d(TAG, "Connected to '$desktopName' ($desktopWidth x $desktopHeight, $bpp bpp)")

                // 5. Send SetPixelFormat (Request 32-bit little-endian ARGB)
                sendSetPixelFormat(dos)

                // 6. Send SetEncodings (Raw, CopyRect, DesktopSize)
                sendSetEncodings(dos)

                // Allocate bitmap on UI/engine
                val newBitmap = Bitmap.createBitmap(desktopWidth, desktopHeight, Bitmap.Config.ARGB_8888)
                bitmap = newBitmap
                _connectionState.value = VncConnectionState.Connected(desktopName, desktopWidth, desktopHeight)

                // 7. Request initial full framebuffer update
                sendFramebufferUpdateRequest(dos, incremental = false, 0, 0, desktopWidth, desktopHeight)

                // 8. Server message processing loop
                processServerMessages(dis, dos)

            } catch (e: CancellationException) {
                Log.d(TAG, "VNC connection cancelled")
                _connectionState.value = VncConnectionState.Disconnected
            } catch (e: Exception) {
                Log.e(TAG, "VNC connection error", e)
                _connectionState.value = VncConnectionState.Error(e.message ?: "Unknown VNC connection error")
            } finally {
                cleanup()
            }
        }
    }

    private fun sendSetPixelFormat(dos: DataOutputStream) {
        dos.writeByte(MSG_SET_PIXEL_FORMAT)
        dos.writeByte(0) // padding
        dos.writeByte(0)
        dos.writeByte(0)

        // PixelFormat struct:
        dos.writeByte(32) // bits-per-pixel
        dos.writeByte(24) // depth
        dos.writeByte(0)  // big-endian-flag (0 = little endian)
        dos.writeByte(1)  // true-colour-flag
        dos.writeShort(255) // red-max
        dos.writeShort(255) // green-max
        dos.writeShort(255) // blue-max
        dos.writeByte(16) // red-shift
        dos.writeByte(8)  // green-shift
        dos.writeByte(0)  // blue-shift
        dos.writeByte(0)  // padding
        dos.writeByte(0)
        dos.writeByte(0)
        dos.flush()
    }

    private fun sendSetEncodings(dos: DataOutputStream) {
        dos.writeByte(MSG_SET_ENCODINGS)
        dos.writeByte(0) // padding
        dos.writeShort(3) // num encodings

        dos.writeInt(ENCODING_RAW)
        dos.writeInt(ENCODING_COPY_RECT)
        dos.writeInt(ENCODING_DESKTOP_SIZE)
        dos.flush()
    }

    private fun sendFramebufferUpdateRequest(
        dos: DataOutputStream,
        incremental: Boolean,
        x: Int,
        y: Int,
        w: Int,
        h: Int
    ) {
        try {
            synchronized(dos) {
                dos.writeByte(MSG_FRAMEBUFFER_UPDATE_REQUEST)
                dos.writeByte(if (incremental) 1 else 0)
                dos.writeShort(x)
                dos.writeShort(y)
                dos.writeShort(w)
                dos.writeShort(h)
                dos.flush()
            }
        } catch (_: Exception) {}
    }

    private fun processServerMessages(dis: DataInputStream, dos: DataOutputStream) {
        while (clientScope.isActive && socket?.isConnected == true) {
            val msgType = dis.readUnsignedByte()
            when (msgType) {
                MSG_FRAMEBUFFER_UPDATE -> {
                    dis.readByte() // padding
                    val numRects = dis.readUnsignedShort()

                    for (r in 0 until numRects) {
                        val rx = dis.readUnsignedShort()
                        val ry = dis.readUnsignedShort()
                        val rw = dis.readUnsignedShort()
                        val rh = dis.readUnsignedShort()
                        val encoding = dis.readInt()

                        when (encoding) {
                            ENCODING_RAW -> {
                                val byteCount = rw * rh * 4
                                if (byteCount > rawPixelBuffer.size) {
                                    rawPixelBuffer = ByteArray(byteCount)
                                    argbBuffer = IntArray(rw * rh)
                                }
                                dis.readFully(rawPixelBuffer, 0, byteCount)

                                var byteIdx = 0
                                val pixelCount = rw * rh
                                for (p in 0 until pixelCount) {
                                    val b = rawPixelBuffer[byteIdx].toInt() and 0xFF
                                    val g = rawPixelBuffer[byteIdx + 1].toInt() and 0xFF
                                    val r = rawPixelBuffer[byteIdx + 2].toInt() and 0xFF
                                    argbBuffer[p] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                                    byteIdx += 4
                                }

                                bitmap?.setPixels(argbBuffer, 0, rw, rx, ry, rw, rh)
                            }

                            ENCODING_COPY_RECT -> {
                                val srcX = dis.readUnsignedShort()
                                val srcY = dis.readUnsignedShort()

                                val currentBitmap = bitmap
                                if (currentBitmap != null) {
                                    val copyPixels = IntArray(rw * rh)
                                    currentBitmap.getPixels(copyPixels, 0, rw, srcX, srcY, rw, rh)
                                    currentBitmap.setPixels(copyPixels, 0, rw, rx, ry, rw, rh)
                                }
                            }

                            ENCODING_DESKTOP_SIZE -> {
                                desktopWidth = rw
                                desktopHeight = rh
                                Log.d(TAG, "Desktop resized to: $rw x $rh")
                                bitmap = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888)
                                _connectionState.value = VncConnectionState.Connected(desktopName, rw, rh)
                            }

                            else -> {
                                Log.w(TAG, "Unsupported RFB encoding received: $encoding")
                            }
                        }
                    }

                    // Notify listeners to redraw
                    _refreshTrigger.value = System.currentTimeMillis()

                    // Request next incremental update
                    sendFramebufferUpdateRequest(dos, incremental = true, 0, 0, desktopWidth, desktopHeight)
                }

                MSG_SET_COLOUR_MAP_ENTRIES -> {
                    dis.readByte() // padding
                    val firstColour = dis.readUnsignedShort()
                    val numColours = dis.readUnsignedShort()
                    dis.skipBytes(numColours * 6)
                }

                MSG_BELL -> {
                    // Audio beep notification from X server
                }

                MSG_SERVER_CUT_TEXT -> {
                    dis.skipBytes(3) // padding
                    val length = dis.readInt()
                    val textBytes = ByteArray(length)
                    dis.readFully(textBytes)
                    val text = String(textBytes, Charsets.ISO_8859_1)
                    _clipboardText.value = text
                }

                else -> {
                    Log.w(TAG, "Unknown server message type: $msgType")
                }
            }
        }
    }

    fun sendPointerEvent(x: Int, y: Int, buttonMask: Int) {
        val dos = outputStream ?: return
        clientScope.launch {
            try {
                val clampedX = x.coerceIn(0, (desktopWidth - 1).coerceAtLeast(0))
                val clampedY = y.coerceIn(0, (desktopHeight - 1).coerceAtLeast(0))
                synchronized(dos) {
                    dos.writeByte(MSG_POINTER_EVENT)
                    dos.writeByte(buttonMask and 0xFF)
                    dos.writeShort(clampedX)
                    dos.writeShort(clampedY)
                    dos.flush()
                }
            } catch (_: Exception) {}
        }
    }

    fun sendKeyEvent(keysym: Int, down: Boolean) {
        val dos = outputStream ?: return
        clientScope.launch {
            try {
                synchronized(dos) {
                    dos.writeByte(MSG_KEY_EVENT)
                    dos.writeByte(if (down) 1 else 0)
                    dos.writeByte(0) // padding
                    dos.writeByte(0)
                    dos.writeInt(keysym)
                    dos.flush()
                }
            } catch (_: Exception) {}
        }
    }

    fun sendClientCutText(text: String) {
        val dos = outputStream ?: return
        clientScope.launch {
            try {
                val bytes = text.toByteArray(Charsets.ISO_8859_1)
                synchronized(dos) {
                    dos.writeByte(MSG_CLIENT_CUT_TEXT)
                    dos.writeByte(0) // padding
                    dos.writeByte(0)
                    dos.writeByte(0)
                    dos.writeInt(bytes.size)
                    dos.write(bytes)
                    dos.flush()
                }
            } catch (_: Exception) {}
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        cleanup()
        _connectionState.value = VncConnectionState.Disconnected
    }

    private fun cleanup() {
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        socket = null
    }
}
