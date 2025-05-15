package dev.rexe.hotelremote.ui.management

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import dev.rexe.hotelremote.R

class ManagementFragment : Fragment() {

    companion object {
        fun newInstance() = ManagementFragment()
    }

    private val viewModel: ManagementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_management, container, false)
    }

}