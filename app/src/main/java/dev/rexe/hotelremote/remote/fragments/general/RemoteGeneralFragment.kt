package dev.rexe.hotelremote.remote.fragments.general

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import dev.rexe.hotelremote.R

class RemoteGeneralFragment : Fragment() {

    companion object {
        fun newInstance() = RemoteGeneralFragment()
    }

    private val viewModel: RemoteGeneralViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_remote_general, container, false)
    }

}