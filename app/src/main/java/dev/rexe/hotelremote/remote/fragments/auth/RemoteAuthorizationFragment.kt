package dev.rexe.hotelremote.remote.fragments.auth

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import dev.rexe.hotelremote.R
import dev.rexe.hotelremote.remote.RemoteActivity
import dev.rexe.hotelremote.scanner.QRScannerActivity

class RemoteAuthorizationFragment : Fragment() {
    companion object {
        fun newInstance() = RemoteAuthorizationFragment()
    }

    private val viewModel: RemoteAuthorizationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val inflated = inflater.inflate(R.layout.fragment_remote_authorization, container, false)

        inflated.findViewById<MaterialButton>(R.id.remote_authentication_required_scan_button).setOnClickListener { v ->
            val intent: Intent = Intent(context, QRScannerActivity::class.java)
            startActivityForResult(intent, RemoteActivity.Companion.QR_SCANNER_LAUNCH)
        }

        return inflated
    }
}