package org.enterprisecams.app.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import org.enterprisecams.app.data.CameraProvider

enum class AppAvailability { READY, MISSING, UNAVAILABLE }
enum class LaunchResult { OPENED_APP, OPENED_STORE, MISSING, UNAVAILABLE, FAILED }

class OfficialApps(private val context: Context) {
    private val pm: PackageManager get() = context.packageManager

    @Suppress("DEPRECATION")
    fun availability(provider: CameraProvider): AppAvailability = try {
        val info = pm.getApplicationInfo(provider.packageName, 0)
        if (info.enabled && pm.getLaunchIntentForPackage(provider.packageName) != null) AppAvailability.READY
        else AppAvailability.UNAVAILABLE
    } catch (_: PackageManager.NameNotFoundException) {
        AppAvailability.MISSING
    } catch (_: SecurityException) {
        AppAvailability.UNAVAILABLE
    }

    fun open(provider: CameraProvider): LaunchResult {
        // The provider catalog owns the package. Neither an imported file nor an
        // external Intent can choose an arbitrary activity, extra, URI or package.
        return try {
            when (availability(provider)) {
                AppAvailability.MISSING -> LaunchResult.MISSING
                AppAvailability.UNAVAILABLE -> LaunchResult.UNAVAILABLE
                AppAvailability.READY -> {
                    val intent = pm.getLaunchIntentForPackage(provider.packageName)
                        ?: return LaunchResult.UNAVAILABLE
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    LaunchResult.OPENED_APP
                }
            }
        } catch (_: ActivityNotFoundException) { LaunchResult.FAILED }
        catch (_: SecurityException) { LaunchResult.FAILED }
    }

    fun install(provider: CameraProvider): LaunchResult {
        // Installation remains an explicit action in the official store.
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${provider.packageName}"))
            .setPackage("com.android.vending")
        if (tryStart(market)) return LaunchResult.OPENED_STORE
        val web = Intent(Intent.ACTION_VIEW, Uri.parse(provider.storeUrl)).addCategory(Intent.CATEGORY_BROWSABLE)
        return if (tryStart(web)) LaunchResult.OPENED_STORE else LaunchResult.FAILED
    }

    private fun tryStart(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) { false }
    catch (_: SecurityException) { false }
}
