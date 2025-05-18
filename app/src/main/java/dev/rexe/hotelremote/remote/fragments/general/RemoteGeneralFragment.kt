package dev.rexe.hotelremote.remote.fragments.general

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView

import dev.rexe.hotelremote.R
import dev.rexe.hotelremote.managers.BluetoothDoorManager
import dev.rexe.hotelremote.managers.RequestsManager

class RemoteGeneralFragment : Fragment() {

    companion object {
        fun newInstance() = RemoteGeneralFragment()
    }

    private val viewModel: RemoteGeneralViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val inflated = inflater.inflate(R.layout.fragment_remote_general, container, false)

        viewModel.lightStatus.observe(viewLifecycleOwner) { data ->
            view?.findViewById<FloatingActionButton>(R.id.remote_general_quick_access_button)?.alpha = (if (data) 1.0 else 0.6).toFloat()
        }
        viewModel.doorLockStatus.observe(viewLifecycleOwner) { data ->
            view?.findViewById<MaterialTextView>(R.id.remote_general_door_status)?.text = getString(R.string.ttl_remote_general_door_lock,
                if (!data) getString(R.string.sts_remote_general_door_lock_open) else
                    getString(R.string.sts_remote_general_door_lock_close))
            view?.findViewById<MaterialButton>(R.id.remote_general_door_button)?.text =
                if (!data) getString(R.string.ttl_remote_general_door_lock_close) else
                    getString(R.string.ttl_remote_general_door_lock_open)
        }
        viewModel.dndMode.observe(viewLifecycleOwner) { data ->
            view?.findViewById<MaterialButton>(R.id.remote_general_dnd_button)?.text =
                if (!data) getString(R.string.ttl_remote_general_dnd_enable) else
                    getString(R.string.ttl_remote_general_dnd_disable)
        }

        viewModel.setDoorLockStatus(BluetoothDoorManager.getDoorLockStatus())
        viewModel.setLightEnabled(BluetoothDoorManager.getLightStatus())
        viewModel.setDNDMode(viewModel.dndMode.value ?: false)

        inflated.findViewById<MaterialButton>(R.id.remote_general_door_button).setOnClickListener { v ->
            BluetoothDoorManager.setDoorLockStatus(!BluetoothDoorManager.getDoorLockStatus())
            viewModel.setDoorLockStatus(BluetoothDoorManager.getDoorLockStatus())
            viewModel.setDNDMode(false)
        }
        inflated.findViewById<FloatingActionButton>(R.id.remote_general_quick_access_button).setOnClickListener { v ->
            BluetoothDoorManager.setLightStatus(!BluetoothDoorManager.getLightStatus())
            viewModel.setLightEnabled(BluetoothDoorManager.getLightStatus())
        }
        inflated.findViewById<MaterialButton>(R.id.remote_general_dnd_button).setOnClickListener { v ->
            BluetoothDoorManager.setDoorLockStatus(locked = true)
            viewModel.setDoorLockStatus(BluetoothDoorManager.getDoorLockStatus())
            viewModel.setDNDMode(!(viewModel.dndMode.value ?: true))

            RequestsManager.sendNewRequest(RequestsManager.RequestObject(
                roomNumber = BluetoothDoorManager.shr?.getString("roomNumber", "0")!!.toInt(),
                categoryName = "Обслуживание и комфорт",
                comment = "Не беспокоить"
            ))
        }
        inflated.findViewById<MaterialTextView>(R.id.remote_general_statistic_title).text = getString(R.string.ttl_remote_general_statistic,
            BluetoothDoorManager.shr?.getString("roomNumber", "0")!!)
        return inflated
    }

}