package dev.rexe.hotelremote.managers

import android.util.Log
import dev.rexe.hotelremote.managers.HTTPManager.client
import dev.rexe.hotelremote.managers.HTTPManager.serverAddress
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object RequestsManager {
    data class RequestType(val id: Int, val name: String)
    data class RequestObject(
        var id: Int = 0, var roomNumber: Int, var categoryName: String, var comment: String? = null,
        var creationTime: String? = null)

    interface RequestTypesReturnable {
        fun onReady(types: ArrayList<RequestType>)
        fun onError()
    }

    interface RequestRequestsReturnable {
        fun onReady(requests: ArrayList<RequestObject>)
        fun onError()
    }

    interface RequestListener {
        fun onError()
    }

    interface RequestCreationListener : RequestListener {
        fun onCreated()
    }

    class RequestWebSocket : WebSocketListener() {
        fun run() {
            val request: Request = Request.Builder()
                .url("ws://${HTTPManager.serverAddress}/ws/alert")
                .build()
            requestsWebsocket = HTTPManager.client.newWebSocket(request, this)
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocket", "onOpen")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocket", "onMessage: $text")
            val json = JSONObject(text)
            when (json.getString("action")) {
                "create_alert" -> {
                    val parentItem: JSONObject = JSONObject(text)
                    val item = parentItem.getJSONObject("data")

                    val request = RequestObject(
                        comment = item.getString("comment"),
                        id = item.getInt("id"),
                        categoryName = item.getString("category_name"),
                        creationTime = item.getString("created_at"),
                        roomNumber = item.getInt("room_number")
                    )

                    requests.add(request)
                    lastCreationListener?.onCreated()
                }
                "get_alerts_status" -> {
                    val arr = json.getJSONArray("data")

                    for (i in 0..<arr.length()) {
                        val item: JSONObject = arr.getJSONObject(i)

                        if (i >= requests.size)
                            requests.add(RequestObject(
                                comment = item.getString("comment"),
                                id = item.getInt("id"),
                                categoryName = item.getString("category_name"),
                                creationTime = item.getString("created_at"),
                                roomNumber = item.getInt("room_number")
                            ))
                        else if (!item.has("completed_at") or item.getString("completed_at").equals("null")) {
                            requests[i].id = item.getInt("id")
                            requests[i].comment = item.getString("comment")
                            requests[i].creationTime = item.getString("created_at")
                            requests[i].roomNumber = item.getInt("room_number")
                            requests[i].categoryName = item.getString("category_name")
                        } else requests.removeAt(i)
                    }

                    lastUpdateRequestsListener?.onReady(requests)
                }
            }
        }

        public override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            println("MESSAGE: " + bytes.hex())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            println("CLOSE: $code $reason")
            requestsWebsocket = null
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (response != null) {
                val json = JSONObject(response.body.toString())
                when (json.getString("action")) {
                    "create_alert" -> lastCreationListener?.onError()
                    "get_alerts_status" -> lastUpdateRequestsListener?.onError()
                }
            }
        }

        companion object {
            @JvmStatic
            fun main(args: Array<String>) {
                requestsWebsocketEcho = RequestWebSocket()
                requestsWebsocketEcho?.run()
            }
        }
    }

    var requestsWebsocket: WebSocket? = null
    var requestsWebsocketEcho: RequestWebSocket? = null
    var lastCreationListener: RequestCreationListener? = null
    var lastUpdateRequestsListener: RequestRequestsReturnable? = null

    var requests: ArrayList<RequestObject> = arrayListOf()

    fun sendNewRequest(request: RequestObject, listener: RequestCreationListener? = null) {
        if (requestsWebsocket != null) {
            val obj: JSONObject = JSONObject()
            obj.put("action", "create_alert")
            val dataObj = JSONObject()
            dataObj.put("room_number", request.roomNumber)
            dataObj.put("category_name", request.categoryName)
            if (request.comment != null)
                dataObj.put("comment", request.comment)
            obj.put("data", dataObj)
            requestsWebsocket?.send(obj.toString())

            lastCreationListener = listener
        }
    }

    fun openRequestsWebsocket() {
        if (requestsWebsocket == null) {
            requestsWebsocketEcho = RequestWebSocket()
            requestsWebsocketEcho?.run()
        }
    }

    @JvmStatic
    fun requestTypesList(listener: RequestTypesReturnable) {
        val request = Request.Builder()
            .url("http://${serverAddress}/api/category")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                listener.onError()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val types: JSONArray = JSONArray(response.body?.string())
                        val readyList: ArrayList<RequestType> = arrayListOf()

                        for (i in 0..<types.length()) {
                            val type: JSONObject = types.getJSONObject(i)
                            val readyItem = RequestType(type.getInt("id"), type.getString("name"))

                            readyList.add(readyItem)
                        }

                        listener.onReady(readyList)
                    } else listener.onError()
                }
            }
        })
    }

    @JvmStatic
    fun updateRequestsList(listener: RequestRequestsReturnable?) {
        if (requestsWebsocket != null) {
            val obj: JSONObject = JSONObject()
            obj.put("action", "get_alerts_status")
            val dataObj = JSONObject()
            val arr: JSONArray = JSONArray()
            for (request in requests)
                arr.put(request.id)
            dataObj.put("alert_ids", arr)
            obj.put("data", dataObj)
            requestsWebsocket?.send(obj.toString())

            lastUpdateRequestsListener = listener
        }
    }

    @JvmStatic
    fun requestRequestsList(listener: RequestRequestsReturnable) {
        val request = Request.Builder()
            .url("http://${serverAddress}/api/alert")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                listener.onError()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val types = JSONArray(response.body?.string())
                        requests.clear()

                        for (i in 0..<types.length()) {
                            val obj: JSONObject = types.getJSONObject(i)
                            if (!obj.has("completed_at") or obj.getString("completed_at").equals("null")) {
                                val readyItem = RequestObject(
                                    comment = obj.getString("comment"),
                                    id = obj.getInt("id"),
                                    categoryName = obj.getString("category_name"),
                                    creationTime = obj.getString("created_at"),
                                    roomNumber = obj.getInt("room_number")
                                )

                                requests.add(readyItem)
                            }
                        }

                        listener.onReady(requests)
                    } else listener.onError()
                }
            }
        })
    }
}