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
import okio.ByteString.Companion.decodeHex
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONStringer
import java.io.IOException

object RequestsManager {
    data class RequestType(val id: Int, val name: String)
    data class RequestObject(val roomNumber: Int, val categoryName: String, val note: String? = null)

    interface RequestTypesReturnable {
        fun onReady(types: ArrayList<RequestType>)
        fun onError()
    }

//    interface RequestCreationListener : RequestListener{
//        fun onCreated()
//        override fun onError()
//    }

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
            Log.d("WebSocket", "onMessage: " + text)
        }

        public override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            println("MESSAGE: " + bytes.hex())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            println("CLOSE: " + code + " " + reason)
            requestsWebsocket = null
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            t.printStackTrace()
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

    fun sendNewRequest(request: RequestObject) {
        if (requestsWebsocket != null) {
            val obj: JSONObject = JSONObject()
            obj.put("action", "create_alert")
            val dataObj = JSONObject()
            dataObj.put("room_number", request.roomNumber)
            dataObj.put("category_name", request.categoryName)
            if (request.note != null)
                dataObj.put("comment", request.note)
            obj.put("data", dataObj)
            requestsWebsocket?.send(obj.toString())
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
                e.printStackTrace()
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
}