package com.example.service.scanner

import com.example.data.model.DeviceType
import java.util.Locale

object VendorLookup {
    private val vendorPrefixes = mapOf(
        "00:03:93" to "Apple, Inc.",
        "00:05:02" to "Apple, Inc.",
        "00:0A:27" to "Apple, Inc.",
        "00:0A:95" to "Apple, Inc.",
        "00:0D:93" to "Apple, Inc.",
        "00:10:FA" to "Apple, Inc.",
        "00:11:24" to "Apple, Inc.",
        "00:14:51" to "Apple, Inc.",
        "00:16:CB" to "Apple, Inc.",
        "00:17:F2" to "Apple, Inc.",
        "00:19:E3" to "Apple, Inc.",
        "00:1B:63" to "Apple, Inc.",
        "00:1C:B3" to "Apple, Inc.",
        "00:1D:4F" to "Apple, Inc.",
        "00:1E:52" to "Apple, Inc.",
        "00:1E:C2" to "Apple, Inc.",
        "00:21:E9" to "Apple, Inc.",
        "00:22:41" to "Apple, Inc.",
        "00:23:12" to "Apple, Inc.",
        "00:23:32" to "Apple, Inc.",
        "00:23:6C" to "Apple, Inc.",
        "00:24:36" to "Apple, Inc.",
        "00:25:00" to "Apple, Inc.",
        "00:25:4B" to "Apple, Inc.",
        "00:26:08" to "Apple, Inc.",
        "00:26:4A" to "Apple, Inc.",
        "00:26:B0" to "Apple, Inc.",
        "00:26:BB" to "Apple, Inc.",
        "3C:06:30" to "Apple, Inc.",
        "3C:15:C2" to "Apple, Inc.",
        "40:6C:8F" to "Apple, Inc.",
        "48:60:5F" to "Google LLC",
        "54:60:09" to "Google LLC",
        "70:3E:AC" to "Google LLC",
        "AC:37:43" to "Google LLC",
        "D8:6C:63" to "Google LLC",
        "F4:F5:D8" to "Google LLC",
        "00:12:FB" to "Samsung Electronics",
        "00:15:99" to "Samsung Electronics",
        "00:16:6B" to "Samsung Electronics",
        "00:17:C9" to "Samsung Electronics",
        "00:18:AF" to "Samsung Electronics",
        "00:1A:8A" to "Samsung Electronics",
        "00:21:19" to "Samsung Electronics",
        "00:23:D7" to "Samsung Electronics",
        "00:24:54" to "Samsung Electronics",
        "00:26:5D" to "Samsung Electronics",
        "08:EE:8B" to "Samsung Electronics",
        "14:49:E0" to "Samsung Electronics",
        "18:1E:78" to "Samsung Electronics",
        "24:4B:03" to "Samsung Electronics",
        "2C:4D:54" to "Xiaomi Communications",
        "34:80:0D" to "Xiaomi Communications",
        "58:44:98" to "Xiaomi Communications",
        "64:09:80" to "Xiaomi Communications",
        "74:23:44" to "Xiaomi Communications",
        "7C:49:EB" to "Xiaomi Communications",
        "8C:DE:52" to "Xiaomi Communications",
        "00:1E:10" to "Huawei Technologies",
        "00:25:9E" to "Huawei Technologies",
        "00:25:68" to "Huawei Technologies",
        "04:25:C5" to "Huawei Technologies",
        "08:19:A6" to "Huawei Technologies",
        "20:F4:1B" to "Huawei Technologies",
        "24:69:A5" to "Espressif Systems (IoT)",
        "30:AE:A4" to "Espressif Systems (IoT)",
        "3C:71:BF" to "Espressif Systems (IoT)",
        "40:F5:20" to "Espressif Systems (IoT)",
        "48:55:19" to "Espressif Systems (IoT)",
        "5C:CF:7F" to "Espressif Systems (IoT)",
        "60:01:94" to "Espressif Systems (IoT)",
        "84:F3:EB" to "Espressif Systems (IoT)",
        "A4:CF:12" to "Espressif Systems (IoT)",
        "B8:27:EB" to "Raspberry Pi Foundation",
        "DC:A6:32" to "Raspberry Pi Foundation",
        "E4:5F:01" to "Raspberry Pi Foundation",
        "00:04:96" to "Extreme Networks",
        "00:09:B7" to "Cisco Systems",
        "00:0E:D7" to "Cisco Systems",
        "00:11:20" to "Cisco Systems",
        "00:17:59" to "Cisco Systems",
        "00:1B:54" to "Cisco Systems",
        "00:1E:13" to "Cisco Systems",
        "00:14:D1" to "TP-Link Corporation",
        "00:19:E0" to "TP-Link Corporation",
        "00:21:27" to "TP-Link Corporation",
        "00:23:CD" to "TP-Link Corporation",
        "14:CC:20" to "TP-Link Corporation",
        "30:B5:C2" to "TP-Link Corporation",
        "50:C7:BF" to "TP-Link Corporation",
        "54:E6:FC" to "TP-Link Corporation",
        "00:0E:2E" to "Edimax Technology",
        "00:0C:43" to "Ralink Technology",
        "00:0F:66" to "ASUSTek Computer",
        "00:11:2F" to "ASUSTek Computer",
        "00:17:31" to "ASUSTek Computer",
        "00:1A:92" to "ASUSTek Computer",
        "00:26:18" to "ASUSTek Computer",
        "00:11:95" to "D-Link Systems",
        "00:13:46" to "D-Link Systems",
        "00:15:E9" to "D-Link Systems",
        "00:17:9A" to "D-Link Systems",
        "00:1E:58" to "D-Link Systems",
        "00:09:5B" to "Netgear Inc.",
        "00:0F:B5" to "Netgear Inc.",
        "00:14:6C" to "Netgear Inc.",
        "00:1B:2F" to "Netgear Inc.",
        "00:1F:33" to "Netgear Inc.",
        "00:0E:58" to "Sonos, Inc.",
        "00:0E:FE" to "Sonos, Inc.",
        "5C:AA:FD" to "Sonos, Inc.",
        "78:28:CA" to "Sonos, Inc.",
        "94:9F:3E" to "Sonos, Inc.",
        "B8:E9:37" to "Sonos, Inc.",
        "00:17:88" to "Philips Lighting (Hue)",
        "EC:B5:FA" to "Philips Lighting (Hue)",
        "00:04:4B" to "NVIDIA Corporation",
        "00:04:75" to "3Com",
        "00:07:E9" to "Intel Corporate",
        "00:0E:0C" to "Intel Corporate",
        "00:13:E8" to "Intel Corporate",
        "00:15:00" to "Intel Corporate",
        "00:16:76" to "Intel Corporate",
        "00:19:D1" to "Intel Corporate",
        "00:1B:21" to "Intel Corporate",
        "00:1C:BF" to "Intel Corporate",
        "00:1D:E0" to "Intel Corporate",
        "00:1E:67" to "Intel Corporate",
        "00:21:6A" to "Intel Corporate",
        "00:24:D7" to "Intel Corporate",
        "00:50:56" to "VMware, Inc.",
        "08:00:27" to "Oracle VirtualBox",
        "00:15:5D" to "Microsoft Corporation",
        "00:12:17" to "Cisco-Linksys",
        "00:14:BF" to "Cisco-Linksys",
        "00:16:B6" to "Cisco-Linksys",
        "00:18:39" to "Cisco-Linksys",
        "00:1A:70" to "Cisco-Linksys",
        "00:1C:10" to "Cisco-Linksys",
        "00:1D:CE" to "Cisco-Linksys",
        "00:1E:E5" to "Cisco-Linksys",
        "00:01:E3" to "Siemens AG",
        "00:03:BA" to "Sun Microsystems",
        "00:0A:E4" to "Sony Corporation",
        "00:13:A9" to "Sony Corporation",
        "00:15:C1" to "Sony Corporation",
        "00:19:C5" to "Sony Corporation",
        "00:1D:BA" to "Sony Corporation",
        "00:1E:DC" to "LG Electronics",
        "00:1F:E3" to "LG Electronics",
        "00:21:FB" to "LG Electronics",
        "00:24:83" to "LG Electronics",
        "00:07:70" to "Amazon Technologies",
        "0C:47:C9" to "Amazon Technologies",
        "34:D2:70" to "Amazon Technologies",
        "40:B4:CD" to "Amazon Technologies",
        "44:65:0D" to "Amazon Technologies",
        "68:37:E9" to "Amazon Technologies",
        "74:C2:46" to "Amazon Technologies",
        "84:D6:D0" to "Amazon Technologies",
        "FC:65:DE" to "Amazon Technologies",
        "FC:A6:67" to "Amazon Technologies",
        "00:0C:42" to "MikroTik",
        "48:8F:5A" to "MikroTik",
        "64:D1:54" to "MikroTik",
        "B8:69:F4" to "MikroTik",
        "C4:AD:34" to "MikroTik",
        "D4:CA:6D" to "MikroTik",
        "E4:8D:8C" to "MikroTik"
    )

    fun findVendor(mac: String): String {
        val cleanMac = mac.uppercase(Locale.US).replace("-", ":").trim()
        if (cleanMac.length >= 8) {
            val prefix = cleanMac.substring(0, 8)
            vendorPrefixes[prefix]?.let { return it }
        }
        return when {
            cleanMac.startsWith("00:00:00") -> "Loopback / Virtual"
            cleanMac.isEmpty() || cleanMac.contains("UNKNOWN") -> "Generic Device"
            else -> "Unassigned / Private MAC"
        }
    }

    fun inferDeviceType(
        vendor: String,
        hostname: String,
        ip: String,
        gatewayIp: String
    ): DeviceType {
        val h = hostname.lowercase(Locale.ROOT)
        val v = vendor.lowercase(Locale.ROOT)

        if (ip == gatewayIp || h.contains("router") || h.contains("gateway") || v.contains("mikrotik") || v.contains("tp-link") || v.contains("d-link") || v.contains("netgear") || v.contains("cisco")) {
            return DeviceType.ROUTER
        }
        if (h.contains("tv") || h.contains("bravia") || h.contains("roku") || h.contains("firetv") || h.contains("shield") || h.contains("cast")) {
            return DeviceType.SMART_TV
        }
        if (h.contains("speaker") || h.contains("sonos") || h.contains("nest") || h.contains("echo") || h.contains("homepod") || v.contains("sonos")) {
            return DeviceType.SPEAKER
        }
        if (h.contains("watch") || h.contains("gear") || h.contains("wear") || h.contains("band")) {
            return DeviceType.SMARTWATCH
        }
        if (h.contains("phone") || h.contains("iphone") || h.contains("galaxy") || h.contains("pixel") || h.contains("redmi") || h.contains("oneplus")) {
            return DeviceType.PHONE
        }
        if (h.contains("ipad") || h.contains("tab")) {
            return DeviceType.TABLET
        }
        if (h.contains("macbook") || h.contains("pc") || h.contains("desktop") || h.contains("laptop") || h.contains("thinkpad") || v.contains("intel") || v.contains("microsoft")) {
            return DeviceType.LAPTOP
        }
        if (v.contains("espressif") || v.contains("raspberry") || h.contains("esp32") || h.contains("esp8266") || h.contains("hue") || h.contains("iot")) {
            return DeviceType.IOT
        }
        if (v.contains("apple") || v.contains("samsung") || v.contains("xiaomi") || v.contains("huawei")) {
            return DeviceType.PHONE
        }
        return DeviceType.UNKNOWN
    }

    fun inferOsName(
        ttlEstimate: Int,
        vendor: String,
        hostname: String,
        deviceType: DeviceType
    ): String {
        val h = hostname.lowercase(Locale.ROOT)
        val v = vendor.lowercase(Locale.ROOT)

        if (h.contains("iphone") || h.contains("ipad") || h.contains("ios")) return "Apple iOS / iPadOS"
        if (h.contains("macbook") || h.contains("macos") || (v.contains("apple") && deviceType == DeviceType.LAPTOP)) return "Apple macOS"
        if (h.contains("android") || (v.contains("samsung") && deviceType == DeviceType.PHONE) || v.contains("xiaomi")) return "Android OS"
        if (h.contains("win") || h.contains("desktop") || ttlEstimate in 115..135) return "Microsoft Windows"
        if (deviceType == DeviceType.ROUTER) return "Embedded Linux / RouterOS"
        if (v.contains("raspberry")) return "Raspberry Pi OS (Linux)"
        if (v.contains("espressif")) return "FreeRTOS (ESP-IDF)"
        if (ttlEstimate in 50..70) return "Linux / Unix-like"
        if (ttlEstimate > 200) return "Network OS (Cisco/BSD)"
        return "Unknown Embedded OS"
    }
}
