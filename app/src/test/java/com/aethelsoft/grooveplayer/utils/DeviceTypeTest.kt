package com.aethelsoft.grooveplayer.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceTypeTest {
    @Test
    fun `width below 600dp selects phone`() {
        assertEquals(DeviceType.PHONE, deviceTypeForWidth(599.99f))
    }

    @Test
    fun `600dp through below 840dp selects tablet`() {
        assertEquals(DeviceType.TABLET, deviceTypeForWidth(600f))
        assertEquals(DeviceType.TABLET, deviceTypeForWidth(839.99f))
    }

    @Test
    fun `840dp and wider selects large tablet`() {
        assertEquals(DeviceType.LARGE_TABLET, deviceTypeForWidth(840f))
        assertEquals(DeviceType.LARGE_TABLET, deviceTypeForWidth(1280f))
    }
}
