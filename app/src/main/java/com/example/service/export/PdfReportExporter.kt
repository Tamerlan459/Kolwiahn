package com.example.service.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.NetworkDevice
import com.example.data.model.NetworkStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportExporter(private val context: Context) {

    suspend fun generatePdfReport(
        stats: NetworkStats,
        devices: List<NetworkDevice>
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // A4 standard point width
            val pageHeight = 842 // A4 standard point height
            var pageNumber = 1

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDoc.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(11, 19, 43)
                textSize = 18f
                isFakeBoldText = true
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(80, 90, 110)
                textSize = 10f
            }
            val headerBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(240, 244, 250)
                style = Paint.Style.FILL
            }
            val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(26, 42, 70)
                textSize = 9f
                isFakeBoldText = true
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(40, 45, 55)
                textSize = 8.5f
            }
            val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(110, 115, 125)
                textSize = 7.5f
            }
            val alertPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(215, 38, 56)
                textSize = 8f
                isFakeBoldText = true
            }
            val okPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(40, 167, 69)
                textSize = 8f
                isFakeBoldText = true
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(220, 225, 235)
                strokeWidth = 1f
            }

            var y = 40f

            // App Title & Header
            canvas.drawText("NetPulse Pro - Network Audit & Security Report", 40f, y, titlePaint)
            y += 16f
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            canvas.drawText("Generated on $dateStr | Network: ${stats.ssid} | Gateway: ${stats.gatewayIp}", 40f, y, subtitlePaint)
            y += 20f

            // Summary Card Box
            canvas.drawRoundRect(40f, y, (pageWidth - 40).toFloat(), y + 65f, 8f, 8f, headerBoxPaint)
            val boxY = y + 20f
            canvas.drawText("TOTAL DEVICES: ${devices.size}", 55f, boxY, tableHeaderPaint)
            canvas.drawText("ONLINE: ${devices.count { it.isOnline }}", 180f, boxY, tableHeaderPaint)
            canvas.drawText("SUSPICIOUS / BLOCKED: ${devices.count { it.isSuspicious || it.isBlocked }}", 290f, boxY, if (devices.any { it.isSuspicious || it.isBlocked }) alertPaint else okPaint)
            canvas.drawText("LOCAL IP: ${stats.localIp}", 55f, boxY + 22f, subtitlePaint)
            canvas.drawText("SUBNET MASK: ${stats.subnetMask}", 180f, boxY + 22f, subtitlePaint)
            canvas.drawText("AVG PING: ${if (devices.isNotEmpty()) devices.map { it.pingMs.coerceAtLeast(0) }.average().toInt() else 0} ms", 290f, boxY + 22f, subtitlePaint)
            y += 85f

            // Table Header
            canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, linePaint)
            y += 14f
            canvas.drawText("IP ADDRESS", 42f, y, tableHeaderPaint)
            canvas.drawText("MAC ADDRESS", 130f, y, tableHeaderPaint)
            canvas.drawText("VENDOR / HOSTNAME", 235f, y, tableHeaderPaint)
            canvas.drawText("OS / TYPE", 375f, y, tableHeaderPaint)
            canvas.drawText("PING", 465f, y, tableHeaderPaint)
            canvas.drawText("STATUS", 505f, y, tableHeaderPaint)
            y += 6f
            canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, linePaint)
            y += 16f

            // Rows
            for (dev in devices) {
                // Check if page overflow
                if (y > pageHeight - 50f) {
                    // Draw footer
                    canvas.drawText("NetPulse Security Audit - Page $pageNumber", 40f, pageHeight - 20f, smallPaint)
                    pdfDoc.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDoc.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f

                    // Re-draw table header on subsequent pages
                    canvas.drawText("IP ADDRESS", 42f, y, tableHeaderPaint)
                    canvas.drawText("MAC ADDRESS", 130f, y, tableHeaderPaint)
                    canvas.drawText("VENDOR / HOSTNAME", 235f, y, tableHeaderPaint)
                    canvas.drawText("OS / TYPE", 375f, y, tableHeaderPaint)
                    canvas.drawText("PING", 465f, y, tableHeaderPaint)
                    canvas.drawText("STATUS", 505f, y, tableHeaderPaint)
                    y += 6f
                    canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, linePaint)
                    y += 16f
                }

                canvas.drawText(dev.ip, 42f, y, textPaint)
                canvas.drawText(dev.mac, 130f, y, smallPaint)

                val vendorDisplay = (dev.customName ?: dev.vendor).take(22)
                canvas.drawText(vendorDisplay, 235f, y, textPaint)
                if (dev.hostname.isNotEmpty()) {
                    canvas.drawText(dev.hostname.take(24), 235f, y + 9f, smallPaint)
                }

                val typeOs = "${dev.deviceType.name} / ${dev.osName.take(16)}"
                canvas.drawText(typeOs, 375f, y, smallPaint)

                canvas.drawText("${dev.pingMs}ms", 465f, y, textPaint)

                val statusText = when {
                    dev.isBlocked -> "BLOCKED"
                    dev.isSuspicious -> "SUSPICIOUS"
                    dev.isOnline -> "TRUSTED"
                    else -> "OFFLINE"
                }
                val sPaint = when {
                    dev.isBlocked || dev.isSuspicious -> alertPaint
                    dev.isOnline -> okPaint
                    else -> smallPaint
                }
                canvas.drawText(statusText, 505f, y, sPaint)

                y += if (dev.hostname.isNotEmpty()) 22f else 17f
                canvas.drawLine(40f, y - 4f, (pageWidth - 40).toFloat(), y - 4f, linePaint)
            }

            // Security Recommendations block
            y += 20f
            if (y < pageHeight - 80f) {
                canvas.drawText("Security Recommendations:", 40f, y, tableHeaderPaint)
                y += 14f
                canvas.drawText("• Enable WPA3/WPA2-AES encryption on gateway router.", 45f, y, smallPaint)
                y += 12f
                canvas.drawText("• Isolate IoT sensors (smart plugs, cameras) on a dedicated 2.4 GHz Guest VLAN.", 45f, y, smallPaint)
                y += 12f
                canvas.drawText("• Block unfamiliar MAC addresses and disable UPnP on router if not required.", 45f, y, smallPaint)
            }

            // Final footer
            canvas.drawText("NetPulse Security Audit - Page $pageNumber of $pageNumber", 40f, pageHeight - 20f, smallPaint)
            pdfDoc.finishPage(page)

            // Save file
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val fileName = "NetPulse_Audit_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
            val file = File(reportsDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDoc.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sharePdfReport(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "NetPulse Network Security Audit Report")
                putExtra(Intent.EXTRA_TEXT, "Attached is the latest network scan and security audit report generated by NetPulse Pro.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share Network Audit PDF Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }
}
