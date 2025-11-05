package com.app.emilockerapp.uilayer.views

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import com.app.emilockerapp.coordinator.BaseChildNavGraph
import com.app.emilockerapp.coordinator.BaseNavCoordinator
import com.app.emilockerapp.datalayer.viewmodels.MainViewmodel
import com.app.emilockerapp.services.DeviceAdminManager
import com.app.emilockerapp.services.LockService
import com.app.emilockerapp.ui.theme.EmiLockerAppTheme
import com.app.emilockerapp.uilayer.views.dashboard.HomeScreen
import com.app.emilockerapp.uilayer.views.emi.EmiScreen
import com.app.emilockerapp.uilayer.views.security.SecurityScreen
import com.app.emilockerapp.uilayer.views.auth.LoginScreen
import com.app.emilockerapp.uilayer.views.phoneRegisterScreen.PhoneRegisterScreen
import com.app.emilockerapp.uilayer.views.test.LockedScreen
import com.app.emilockerapp.uilayer.views.welcome.WelcomeScreenNavGraph
import com.app.emilockerapp.utils.getDeviceActive
import com.app.emilockerapp.utils.getMyRef
import com.app.emilockerapp.utils.getMyRefUninstall
import com.app.emilockerapp.utils.isRegistered
import com.app.emilockerapp.utils.startKioskMode
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private val settingsEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (getDeviceActive(this@MainActivity) == true){
                when (intent?.action) {
                    "com.app.emilockerapp.RESET_FLOW_ENTERED" -> {
                        val cls = intent.getStringExtra("cls") ?: ""
                        toast("Factory Reset screen opened! Your device locked now")
                        setLockState()
                    }

                    "com.app.emilockerapp.FACTORY_RESET_TAPPED" -> {
                        toast("⚠️ Factory Reset tapped! Your device locked now")
                        setLockState()
                    }

                    "com.app.emilockerapp.APP_INFO_OPENED" -> {
                        toast("App Info opened! Your device locked now")
                        setLockState()

                    }

                    "com.app.emilockerapp.UNINSTALL_TAPPED" -> {
                        toast("⚠️ Uninstall tapped! Your device locked now")
                        setLockState()
                    }
                }
            }
        }
    }

    private lateinit var viewModel: MainViewmodel
    private var isDeviceLocked = false
    private var isDeviceUninstall = false
    private var _appCoordinator: EmiLockerNavCoordinator? = null
    private val appCoordinator get() = _appCoordinator!!
    private var bottomPadding = 0.dp

    companion object {
        private const val CHANNEL_ID = "ForegroundServiceChannel"
        const val POST_NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewmodel::class.java]

        enableEdgeToEdge()

        DeviceAdminManager.init(applicationContext)
        dpm = DeviceAdminManager.getDpm()

        startBackgroundService()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContent {
            MainView(false)
        }

    }

    override fun onStart() {
        super.onStart()
        val f = IntentFilter().apply {
            addAction("com.app.emilockerapp.RESET_FLOW_ENTERED")
            addAction("com.app.emilockerapp.FACTORY_RESET_TAPPED")
            addAction("com.app.emilockerapp.APP_INFO_OPENED")
            addAction("com.app.emilockerapp.UNINSTALL_TAPPED")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(settingsEventsReceiver, f, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(settingsEventsReceiver, f)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isDeviceLocked) {
            dpm.lockNow()
        } else {

        }
    }

    override fun onBackPressed() {
        if (isDeviceLocked) {
            dpm.lockNow()
        } else {
            super.onBackPressed()
        }
    }

    fun setLockState() {
        if (!isDeviceUninstall){
            dpm.lockNow()
        }
    }

    private fun checkDb(myRef: DatabaseReference) {
        myRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue<Boolean>()

                isDeviceLocked = value ?: false

                setContent {
                    MainView(value == true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })

        getMyRefUninstall(context = this).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue<Boolean>()
                isDeviceUninstall = value ?: false
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    override fun onResume() {
        super.onResume()
        startKioskMode(this@MainActivity)

        checkDb(getMyRef(this))
    }

    private fun startBackgroundService() {
        createNotificationChannel()
        val intent = Intent(this, LockService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun makeEmergencyCall() {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:911")
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:911")
                }
                startActivity(dialIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Emergency call failed", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    fun MainView(isLockedScreen: Boolean) {
        EmiLockerAppTheme(darkTheme = false) {
            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                bottomPadding = innerPadding.calculateBottomPadding()
                val mainModifier = Modifier.padding(innerPadding)

                val mNavHostController = androidx.navigation.compose.rememberNavController()
                if (!isRegistered(this)) {
                    _appCoordinator = EmiLockerNavCoordinator(
                        mNavHostController,
                        mainModifier,
                        createListOfScreensInsideComposable(mNavHostController),
                        PhoneRegisterScreen.Routes.phoneRegister,
                        this
                    )
                } else {
                    if (isLockedScreen) {
                        _appCoordinator = EmiLockerNavCoordinator(
                            mNavHostController,
                            mainModifier,
                            createListOfScreensInsideComposable(mNavHostController),
                            //LoginScreen.Routes.emi_Login_Screen,
                            LockedScreen.Routes.emi_Test_Screen,
                            this
                        )
                    } else {
                        _appCoordinator = EmiLockerNavCoordinator(
                            mNavHostController,
                            mainModifier,
                            createListOfScreensInsideComposable(mNavHostController),
                            //LoginScreen.Routes.emi_Login_Screen,
                            WelcomeScreenNavGraph.Routes.welcomeScreen,
                            //LoginScreen.Routes.emi_Login_Screen,
                            this
                        )
                    }
                }
                appCoordinator.CatoPayNavHost()
            }
        }
    }

    private fun createListOfScreensInsideComposable(
        mNavHostController: NavHostController,
    ): List<BaseChildNavGraph> {

        val mList = mutableListOf(
            HomeScreen(mNavHostController),
            EmiScreen(mNavHostController),
            LoginScreen(mNavHostController),
            SecurityScreen(mNavHostController),
            WelcomeScreenNavGraph(mNavHostController),
            LockedScreen(mNavHostController),
            PhoneRegisterScreen(mNavHostController)
        )
        return mList
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

class EmiLockerNavCoordinator(
    mNavHostController: NavHostController,
    mainModifier: Modifier,
    listOfChildNavGraphs: List<BaseChildNavGraph>,
    mStartDestination: String,
    mActivity: AppCompatActivity
) : BaseNavCoordinator(
    mNavHostController, mainModifier, listOfChildNavGraphs, mStartDestination, mActivity
) {
    @Composable
    fun CatoPayNavHost() {
        BaseNavHost()
    }
}