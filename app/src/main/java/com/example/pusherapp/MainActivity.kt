package com.example.pusherapp

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.google.gson.Gson
import com.pusher.client.channel.SubscriptionEventListener
import com.google.gson.annotations.SerializedName
import com.pusher.client.channel.Channel
import com.pusher.client.channel.PrivateChannel
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.util.HttpAuthorizer
import java.lang.Exception
import com.github.florent37.rxgps.RxGps
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


class MainActivity : AppCompatActivity() {

    private var pusher: Pusher? = null
    private var channel: PrivateChannel? = null
    private val PUSHER_APP_KEY = "c2510f7c3b603f89c615"
    private val PUSHER_APP_CLUSTER = "eu"
    private val CHANNEL_NAME = "private-courier-channel-1"
    private val EVENT_NAME = "remaining-time-event"
    private val COURIER_EVENT_NAME = "client-courier-position"

    var eventListener: PrivateChannelEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()*/
            val data = Gson().toJson(CourierDataResponse(41.023189, 28.975160, Courier(1903)))
            channel!!.trigger(COURIER_EVENT_NAME, data)
        }
    }

    override fun onResume() {
        super.onResume()
        val authorizer = HttpAuthorizer("https://suiste.mobillium.com/pusher/auth")
        val options = PusherOptions()
        options.setCluster(PUSHER_APP_CLUSTER)
        options.authorizer = authorizer
        //options.isEncrypted = true
        pusher = Pusher(PUSHER_APP_KEY, options)
        channel = pusher!!.subscribePrivate(CHANNEL_NAME)

        eventListener = object : PrivateChannelEventListener {
            override fun onEvent(channelName: String?, eventName: String?, data: String?) {
                println("Received event with data: $data")
                val stat = Gson().fromJson(data, CourierDataResponse::class.java)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, stat.courier.id.toString(), Toast.LENGTH_LONG).show()
                }
            }

            override fun onAuthenticationFailure(message: String?, e: Exception?) {

            }

            override fun onSubscriptionSucceeded(channelName: String?) {

            }

        }

        channel!!.bind(COURIER_EVENT_NAME, eventListener)
        pusher!!.connect()

        RxGps(this).locationHight()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ location ->
                Toast.makeText(this, location.latitude.toString(), Toast.LENGTH_SHORT).show()
            }, { throwable ->
                if (throwable is RxGps.PermissionException) {
                    //the user does not allow the permission
                } else if (throwable is RxGps.PlayServicesNotAvailableException) {
                    //the user do not have play services
                }
            })
    }

    override fun onPause() {
        super.onPause()
        channel!!.unbind(COURIER_EVENT_NAME, eventListener)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}

class CourierDataResponse(
    @SerializedName("lat")
    var lat: Double,
    @SerializedName("lon")
    var lon: Double,
    @SerializedName("courier")
    var courier: Courier
)

class Courier(
    @SerializedName("id")
    var id: Int
)

class RemainingTime(
    @SerializedName("minute")
    var minute: Int
)

class MyLocation(
    @SerializedName("lat")
    var lat: Double,
    @SerializedName("lon")
    var lon: Double
)
