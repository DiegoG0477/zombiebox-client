package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.shared.GatewayApi
import java.net.InetAddress
import java.net.ServerSocket
import org.junit.Assert.*
import org.junit.Test

class DownloadSampleTest {
    @Test
    fun streamsBoundedAuthenticatedSampleAndRefusesRedirects() {
        val server = ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"))
        server.soTimeout = 5000
        val requests = mutableListOf<String>()
        val worker = Thread {
            repeat(2) { index ->
                server.accept().use { socket ->
                    socket.soTimeout = 5000
                    val reader = socket.getInputStream().bufferedReader()
                    val headers = StringBuilder()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        headers.append(line).append('\n')
                    }
                    requests.add(headers.toString())
                    val output = socket.getOutputStream()
                    if (index == 1)
                        output.write(
                            "HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1:1/\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                                .toByteArray()
                        )
                    else {
                        output.write(
                            ("HTTP/1.1 200 OK\r\nContent-Length: 1048576\r\nX-Zombie-Sample: " +
                                    "a".repeat(32) +
                                    "\r\nConnection: close\r\n\r\n")
                                .toByteArray()
                        )
                        val block = ByteArray(8192)
                        repeat(128) { output.write(block) }
                    }
                    output.flush()
                }
            }
        }
        worker.start()
        val api = GatewayApi()
        try {
            api.configure("http://127.0.0.1:${server.localPort}", "paired", "test-token")
            val sample = api.downloadSample()
            assertEquals(1048576, sample.bytes)
            assertEquals("a".repeat(32), sample.id)
            assertTrue(sample.elapsedMs in 1..4500)
            try {
                api.downloadSample()
                fail("redirect followed")
            } catch (_: Exception) {}
            worker.join(5000)
            assertEquals(2, requests.size)
            for (headers in requests) {
                assertTrue(headers.contains("Authorization: Bearer test-token"))
                assertTrue(headers.contains("X-Zombie-Device: paired"))
                assertTrue(headers.contains("Accept-Encoding: identity"))
            }
        } finally {
            api.close()
            server.close()
            worker.join(5000)
        }
    }
}
