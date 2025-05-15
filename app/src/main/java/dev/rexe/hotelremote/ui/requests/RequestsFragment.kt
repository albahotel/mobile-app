package dev.rexe.hotelremote.ui.requests

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.rexe.hotelremote.R

class RequestsFragment : Fragment() {

    companion object {
        fun newInstance() = RequestsFragment()
    }

    private val viewModel: RequestsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_requests, container, false)
    }
}