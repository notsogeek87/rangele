package com.rangele.inventory.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rangele.inventory.R
import com.rangele.inventory.data.local.entity.ProductEntity

/** Posts the local "products expiring soon" notification checked daily by [ExpirationCheckWorker]. */
class ExpirationNotifier(
    private val context: Context,
) {
    fun notifyExpiringProducts(products: List<ProductEntity>) {
        if (products.isEmpty()) return
        ensureChannel()

        val contentText =
            if (products.size == 1) {
                "${products.first().name} arrive à expiration."
            } else {
                "${products.size} produits arrivent à expiration."
            }
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_yakwa_mark)
                .setContentTitle("Péremption à surveiller")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(products.joinToString("\n") { it.name }))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Péremption des produits",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Alerte quand un produit de l'inventaire approche de sa date de péremption."
            }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "expiration_alerts"
        const val NOTIFICATION_ID = 1001
    }
}
