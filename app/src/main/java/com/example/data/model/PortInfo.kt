package com.example.data.model

enum class PortRisk(val label: String) {
    SAFE("Low Risk / Standard"),
    INFO("Informational"),
    WARNING("Potential Risk"),
    CRITICAL("High Security Risk")
}

data class PortInfo(
    val port: Int,
    val protocol: String = "TCP",
    val service: String,
    val isOpen: Boolean = false,
    val description: String = "",
    val risk: PortRisk = PortRisk.SAFE,
    val banner: String = ""
)
