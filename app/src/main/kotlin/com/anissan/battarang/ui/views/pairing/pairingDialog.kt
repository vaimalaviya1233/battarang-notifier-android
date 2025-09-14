package com.anissan.battarang.ui.views.pairing

import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.anissan.battarang.BuildConfig
import com.anissan.battarang.R
import com.anissan.battarang.databinding.DialogPairBinding
import com.anissan.battarang.network.SupportedService
import com.anissan.battarang.ui.MainActivity
import com.anissan.battarang.utils.logE
import com.anissan.battarang.utils.logV
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.net.URI

private lateinit var pairingDialog: AlertDialog

fun MainActivity.showPairingDialog() {
  val dialogContentView = DialogPairBinding.inflate(layoutInflater)

  dialogContentView.receiverLinkButton.apply {
    text = URI(BuildConfig.RECEIVER_WEBSITE_SHORT_LINK).host

    setOnClickListener {
      val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, BuildConfig.RECEIVER_WEBSITE_SHORT_LINK)
      }

      startActivity(Intent.createChooser(shareIntent, null))
    }

    setOnLongClickListener { dialog -> dialog.performClick() }
  }

  dialogContentView.telegramLinkButton.setOnClickListener {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, BuildConfig.TELEGRAM_BOT_URL.toUri()))
    } catch (_: ActivityNotFoundException) {
      Toast.makeText(this, R.string.telegram_app_not_found, Toast.LENGTH_LONG).show()
    }
  }

  pairingDialog = MaterialAlertDialogBuilder(this, R.style.PairingDialog)
    .setIcon(R.drawable.ic_external_link)
    .setTitle(R.string.pair_dialog_title)
    .setView(dialogContentView.root)
    .setNeutralButton(getString(R.string.pair_dialog_paste_button)) { _, _ ->
      val clipboard = (getSystemService(AppCompatActivity.CLIPBOARD_SERVICE)) as? ClipboardManager
      val clipboardText: CharSequence =
        clipboard?.primaryClip?.getItemAt(0)?.text ?: ""

      saveToken(clipboardText.toString())
    }
    .setPositiveButton(getString(R.string.pair_dialog_scan_button)) { _, _ ->
      try {
        launchQrScanner(::saveToken)
      } catch (e: Exception) {
        logE(e)
        showSnackbar(R.string.camera_unavailable, Snackbar.LENGTH_SHORT)
      }
    }
    .show()
}

fun MainActivity.saveToken(providedText: String) {
  logV { "Scanned QR / pasted text: $providedText" }

  try {
    val (service, token) = providedText.split(":", limit = 2)
    localKvStore.pairedServiceTag = SupportedService.valueOf(service).name
    localKvStore.receiverToken = token.ifBlank { throw Exception("Token can not be blank.") }
  } catch (e: Exception) {
    logE(e)
    showSnackbar(R.string.invalid_token)
    return
  }
}

private const val PAIRING_DIALOG = "pairing_dialog"

fun savePairingDialogState(outState: Bundle) {
  outState.putBoolean(
    PAIRING_DIALOG,
    if (::pairingDialog.isInitialized) pairingDialog.isShowing else false,
  )
}

fun MainActivity.restorePairingDialogState(savedInstanceState: Bundle) {
  if (savedInstanceState.getBoolean(PAIRING_DIALOG)) showPairingDialog()
}
