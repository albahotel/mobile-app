package dev.rexe.hotelremote.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import dev.rexe.hotelremote.R
import dev.rexe.hotelremote.managers.RequestsManager
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

class RequestsAdapter : RecyclerView.Adapter<RequestsAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val requestTitle: MaterialTextView
        val requestCreationTime: MaterialTextView
        val requestComment: MaterialTextView

        init {
            requestTitle = view.findViewById(R.id.request_item_title)
            requestCreationTime = view.findViewById(R.id.request_item_creation_time)
            requestComment = view.findViewById(R.id.request_item_comment)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.requests_list_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val request = RequestsManager.requests[position]
        viewHolder.requestTitle.text = request.categoryName

        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
        try {
            val date: Date? = format.parse(request.creationTime!!)
            viewHolder.requestCreationTime.text = viewHolder.requestComment.context.getString(
                R.string.ttl_requests_now_time, date?.toLocaleString()
            )
            viewHolder.requestCreationTime.visibility = View.VISIBLE
        } catch (e: ParseException) {
            e.printStackTrace()
            viewHolder.requestCreationTime.visibility = View.GONE
        }

        viewHolder.requestComment.visibility = if ((request.comment != null) and (!request.comment.equals("null"))) {
            viewHolder.requestComment.text = viewHolder.requestComment.context.getString(R.string.ttl_requests_now_comment, request.comment)
            View.VISIBLE
        } else View.GONE
    }

    override fun getItemCount() = RequestsManager.requests.size
}