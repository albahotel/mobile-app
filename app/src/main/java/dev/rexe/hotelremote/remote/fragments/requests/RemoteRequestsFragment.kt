package dev.rexe.hotelremote.remote.fragments.requests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dev.rexe.hotelremote.R
import dev.rexe.hotelremote.managers.RequestsManager

class RemoteRequestsFragment : Fragment() {

    companion object {
        fun newInstance() = RemoteRequestsFragment()
    }

    private val viewModel: RemoteRequestsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val inflated = inflater.inflate(R.layout.fragment_remote_requests, container, false)

        viewModel.dropdownItems.observe(viewLifecycleOwner) { data ->
            val spinner: Spinner = inflated.findViewById(R.id.requests_add_type_selection)
            val adapter = object : BaseAdapter() {
                override fun getCount(): Int {
                    return data.size
                }

                override fun getItem(p0: Int): Any? {
                    return data[p0]
                }

                override fun getItemId(p0: Int): Long {
                    return data[p0].id.toLong()
                }

                override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View? {
                    val view = p1 ?: inflater.inflate(R.layout.spinner_item, null)
                    view.findViewById<TextView>(R.id.spinner_item_text).text = data[p0].name
                    return view
                }
            }

            spinner.adapter = adapter
        }

        inflated.findViewById<MaterialButton>(R.id.requests_add_submit_button).setOnClickListener { v ->
            val spinner: Spinner = inflated.findViewById<Spinner>(R.id.requests_add_type_selection)
            val textEdit: TextInputEditText = inflated.findViewById<TextInputEditText>(R.id.requests_add_note)
            RequestsManager.sendNewRequest(RequestsManager.RequestObject(
                101,
                (spinner.selectedItem as RequestsManager.RequestType).name,
                textEdit.text?.length?.let { if (it > 0) textEdit.text.toString() else null }))
        }

        return inflated
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        RequestsManager.openRequestsWebsocket()

        RequestsManager.requestTypesList(object : RequestsManager.RequestTypesReturnable {
            override fun onReady(types: ArrayList<RequestsManager.RequestType>) {
                viewModel.setRequestTypes(types.toArray(arrayOf<RequestsManager.RequestType>()))
            }

            override fun onError() {
                TODO("Stub!")
            }
        })
    }
}