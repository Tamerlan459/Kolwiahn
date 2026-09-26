package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DeviceType
import com.example.service.scanner.VendorLookup
import com.example.service.scanner.WakeOnLan
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NetPulse", appName)
    }

    @Test
    fun `test vendor lookup Apple and Xiaomi`() {
        val appleVendor = VendorLookup.findVendor("00:03:93:12:34:56")
        assertEquals("Apple, Inc.", appleVendor)

        val xiaomiVendor = VendorLookup.findVendor("2C:4D:54:AA:BB:CC")
        assertEquals("Xiaomi Communications", xiaomiVendor)

        val unknownVendor = VendorLookup.findVendor("FF:EE:DD:00:11:22")
        assertNotNull(unknownVendor)
    }

    @Test
    fun `test device type inference`() {
        val routerType = VendorLookup.inferDeviceType("TP-Link Corporation", "gateway.router.local", "192.168.1.1", "192.168.1.1")
        assertEquals(DeviceType.ROUTER, routerType)

        val tvType = VendorLookup.inferDeviceType("Xiaomi Communications", "Mi-Smart-TV-4K", "192.168.1.142", "192.168.1.1")
        assertEquals(DeviceType.SMART_TV, tvType)
    }

    @Test
    fun `test wake on lan magic packet construction`() = runBlocking {
        val result = WakeOnLan.sendMagicPacket("AA:BB:CC:DD:EE:FF")
        assertTrue(result.isSuccess)
    }
}
