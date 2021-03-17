package com.flywinter.udpsr

import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.Charset
import kotlin.concurrent.thread

/**
 * @author Zhang Xingkun
 *
 * @note 注意发送时的IP设置,如果是255.255.255.255 那么该局域网内所有设备都能收到消息以及对方的IP
 * 如果是192.168.31.255,那么192.168.31.xxx都能收到消息
 * 必须端口一致
 * 其他的格式是无效的
 */

class MainActivity : AppCompatActivity() {


    companion object {
        private const val UDP_RECEIVE_HANDLER_MESSAGE = 123
        private const val UDP_RECEIVE_HANDLER_BUNDLE = "UDPReceive"
        private const val HANDLER_UDP_RECEIVE_FAILED_MSG = 14
        private const val HANDLER_UDP_RECEIVE_FAILED_BUNDLE = "UDPReceiveFailed"
    }

    private val stringBuffer = StringBuffer()
    private var mUDPSendIp = String()
    private var mUDPSendPort = 8080
    var datagramSocket = DatagramSocket()
    private var encodingFormat = "GBK"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //设置接收消息框上下滚动，如果设置了多个，只会有一个起作用
        txt_udp_receive.movementMethod = ScrollingMovementMethod.getInstance()

        txt_udp_local_ip.text = funGetLocalIp()

        //清除UDP接收的内容
        btn_udp_receive_clear.setOnClickListener {
            stringBuffer.delete(0,stringBuffer.length)
            txt_udp_receive.text = stringBuffer
        }
        //开启或关闭UDP接收
        switch_udp_receive_status.setOnClickListener {
            txt_udp_local_ip.text = funGetLocalIp()

            val udpReceivePort = edit_udp_receive_port.text.toString().toInt()

            if (switch_udp_receive_status.isChecked) {
                switch_udp_receive_status.isChecked = true
                thread {
                    funUDPReceive(udpReceivePort)
                }
            } else {
                switch_udp_receive_status.isChecked = false
                funCloseUDPReceive()
            }
        }
        //UDP发送消息
        btn_udp_send.setOnClickListener {
            txt_udp_local_ip.text = funGetLocalIp()

            mUDPSendIp = edit_udp_send_ip.text.toString()
            mUDPSendPort = edit_udp_send_port.text.toString().toInt()
            var sendMessage = edit_udp_send.text.toString()
            if (check_udp_add_newline.isChecked) {
                sendMessage += "\r\n"
            }
            if (check_udp_add_renew.isChecked) {
                stringBuffer.append(sendMessage)
                txt_udp_receive.text = stringBuffer.toString()
            }
            thread {
                funUDPSend(mUDPSendIp, mUDPSendPort, sendMessage)
            }

        }
    }

    //UDP发送
    //需要添加子线程
    private fun funUDPSend(targetIp: String, targetPort: Int, msg: String) {
        val toByteArray = msg.toByteArray(Charset.forName(encodingFormat))
        val outPacket = DatagramPacket(ByteArray(0), 0, InetAddress.getByName(targetIp), targetPort)
        val datagramSocketSend = DatagramSocket()
        try {
            //注意，这里每次新建一个UDP，并且发送完毕后关闭，也可以新建一个全局UDP
            //退出时再关闭，不要和接收用同一个UDP，否则可能会信息错乱
            outPacket.data = toByteArray
            datagramSocketSend.send(outPacket)
            Log.e("UDP Send Message:", msg)
            datagramSocketSend.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("UDP Send Error!", e.toString())
        }
    }

    /**
     * 这里需要注意，UDP发送，可以是每次新建一个DatagramSocket，
     * 发送完消息后接着关闭，这样比较节省资源，
     * 也可以新建一个全局可变量datagramSocket，
     * 直到Activity销毁时顺便关闭，或者自己主动关闭，
     * 缺点是比较耗资源，优点是由于某些软件UDP接收时，如果收到来自
     * 不同端口的数据，会自动加上端口号及IP，这样由于每次发送的
     * 端口和IP都相同，就不会每次都出现前缀
     */
 /*   //UDP发送
    //需要添加子线程
    val datagramSocketSend = DatagramSocket()
    private fun funUDPSend(targetIp: String, targetPort: Int, msg: String) {
        val toByteArray = msg.toByteArray(Charset.forName(encodingFormat))
        val outPacket = DatagramPacket(ByteArray(0), 0, InetAddress.getByName(targetIp), targetPort)
        try {
            //注意，这里每次新建一个UDP，并且发送完毕后关闭，也可以新建一个全局UDP
            //退出时再关闭，不要和接收用同一个UDP，否则可能会信息错乱
            outPacket.data = toByteArray
            datagramSocketSend.send(outPacket)
            Log.e("UDP Send Message:", msg)
          //  datagramSocketSend.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("UDP Send Error!", e.toString())
        }
    }
*/
    //UDP接收
    //需要添加子线程
    private fun funUDPReceive(port: Int) {
        val byteArray = ByteArray(4096)
        val inPacket = DatagramPacket(byteArray, byteArray.size)
        try {
            datagramSocket = DatagramSocket(port)
            Log.e("UDP", "成功开启UDP接收")
            while (true) {
                datagramSocket.receive(inPacket)
                val string = String(byteArray, 0, inPacket.length, Charset.forName(encodingFormat))
                Log.e("UDPReceive", string)
                val message = Message()
                val bundle = Bundle()
                bundle.putString(
                    UDP_RECEIVE_HANDLER_BUNDLE,
                    inPacket.socketAddress.toString().substring(1) + ":" + string
                )
                message.what = UDP_RECEIVE_HANDLER_MESSAGE
                message.data = bundle
                handle.sendMessage(message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("error", e.toString())
            val message = Message()
            val bundle = Bundle()
            bundle.putString(HANDLER_UDP_RECEIVE_FAILED_BUNDLE,e.toString())
            message.what = HANDLER_UDP_RECEIVE_FAILED_MSG
            message.data = bundle
            handle.sendMessage(message)
            datagramSocket.close()
        }
    }


    //关闭UDP接收
    private fun funCloseUDPReceive() {
        try {
            if (datagramSocket.isConnected) {
                datagramSocket.close()
                Toast.makeText(this, "关闭UDP接收", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TAG", "closeUDPReceive: UDP接收关闭失败")

        }
    }

    //获取本地网络IP
    private fun funGetLocalIp(): String {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        val localIP =
            (ipAddress and 0xff).toString() + "." + (ipAddress shr 8 and 0xff) + "." + (ipAddress shr 16 and 0xff) + "." + (ipAddress shr 24 and 0xff)
        return localIP
    }

    private val handle = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                UDP_RECEIVE_HANDLER_MESSAGE -> {
                    val string = msg.data.getString(UDP_RECEIVE_HANDLER_BUNDLE)
                        stringBuffer.append(string)

                    txt_udp_receive.text = stringBuffer.toString()
                }
                HANDLER_UDP_RECEIVE_FAILED_MSG -> {
                    switch_udp_receive_status.isChecked = false
                    val string = msg.data.getString(HANDLER_UDP_RECEIVE_FAILED_BUNDLE)
                    Toast.makeText(this@MainActivity, string, Toast.LENGTH_SHORT).show()

                }
            }
        }

    }

}