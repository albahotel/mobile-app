package dev.rexe.hotelremote.remote.fragments.requests

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import dev.rexe.hotelremote.R
import dev.rexe.hotelremote.adapters.RequestsAdapter
import dev.rexe.hotelremote.managers.BluetoothDoorManager
import dev.rexe.hotelremote.managers.HTTPManager
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

        if (viewModel.dropdownItems.value != null)
            setSpinnerAdapter(viewModel.dropdownItems.value!!)

        viewModel.dropdownItems.observe(viewLifecycleOwner) { data ->
            setSpinnerAdapter(data)
        }
        viewModel.requestsListItems.observe(viewLifecycleOwner) { data ->
            view?.findViewById<RecyclerView>(R.id.requests_now_list)?.adapter?.notifyDataSetChanged()
        }
        viewModel.submitEnabled.observe(viewLifecycleOwner) { data ->
            view?.findViewById<MaterialButton>(R.id.requests_add_submit_button)?.isEnabled = data
            view?.findViewById<Spinner>(R.id.requests_add_type_selection)?.isEnabled = data
            view?.findViewById<TextInputEditText>(R.id.requests_add_note)?.isEnabled = data

            view?.findViewById<MaterialCardView>(R.id.requests_add_card)?.alpha = (if (data) 1.0 else 0.6).toFloat()
        }

        inflated.findViewById<MaterialButton>(R.id.requests_add_submit_button).setOnClickListener { v ->
            val spinner: Spinner = inflated.findViewById<Spinner>(R.id.requests_add_type_selection)
            val textEdit: TextInputEditText = inflated.findViewById<TextInputEditText>(R.id.requests_add_note)
            viewModel.setSubmitEnabled(false)
            RequestsManager.sendNewRequest(RequestsManager.RequestObject(
                roomNumber = BluetoothDoorManager.shr?.getString("roomNumber", "0")!!.toInt(),
                categoryName = (spinner.selectedItem as RequestsManager.RequestType).name,
                comment = textEdit.text?.length?.let { if (it > 0) textEdit.text.toString() else null }), object : RequestsManager.RequestCreationListener {
                override fun onCreated() {
                    viewModel.setRequests(RequestsManager.requests)
                    viewModel.setSubmitEnabled(true)
                }

                override fun onError() {
                    viewModel.setSubmitEnabled(true)
                }
            })
        }

        val recyclerView: RecyclerView = inflated.findViewById(R.id.requests_now_list)
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, insets ->
            val innerPadding = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(
                innerPadding.left,
                0,
                innerPadding.right,
                innerPadding.bottom)
            insets
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RequestsAdapter()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable{
            override fun run() {
                val runnable: Runnable = this

                RequestsManager.updateRequestsList(object : RequestsManager.RequestRequestsReturnable {
                    override fun onReady(requests: ArrayList<RequestsManager.RequestObject>) {
                        viewModel.setRequests(requests)
                        handler.postDelayed(runnable, 1000)
                    }

                    override fun onError() {

                    }
                })
            }
        }, 1000)

        return inflated
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.dropdownItems.value == null)
            viewModel.setSubmitEnabled(false)

        RequestsManager.openRequestsWebsocket()
        RequestsManager.requestTypesList(object : RequestsManager.RequestTypesReturnable {
            override fun onReady(types: ArrayList<RequestsManager.RequestType>) {
                viewModel.setRequestTypes(types)
                viewModel.setSubmitEnabled(true)
            }

            override fun onError() {
                // TODO("Stub!")
            }
        })
        RequestsManager.requestRequestsList(object : RequestsManager.RequestRequestsReturnable {
            override fun onReady(requests: ArrayList<RequestsManager.RequestObject>) {
                viewModel.setRequests(requests)
            }

            override fun onError() {
                // TODO("Stub!")
            }
        })
    }

    override fun onResume() {
        super.onResume()

    }

    fun setSpinnerAdapter(data: ArrayList<RequestsManager.RequestType>) {
        val spinner: Spinner? = view?.findViewById(R.id.requests_add_type_selection)
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
                val view = p1 ?: layoutInflater.inflate(R.layout.spinner_item, null)
                view.findViewById<TextView>(R.id.spinner_item_text).text = data[p0].name
                return view
            }
        }

        spinner?.adapter = adapter
    }
}