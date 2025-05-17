package dev.rexe.hotelremote.remote.fragments.schedule

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dev.rexe.hotelremote.R

class RemoteScheduleFragment : Fragment() {

    companion object {
        fun newInstance() = RemoteScheduleFragment()
    }

    private val viewModel: RemoteScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val inflated = inflater.inflate(R.layout.fragment_remote_schedule, container, false)

        return inflated
    }
}