package com.app.emilockerapp.uilayer.views

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import com.app.emilockerapp.coordinator.BaseChildNavGraph
import com.app.emilockerapp.coordinator.BaseNavCoordinator
import com.app.emilockerapp.datalayer.viewmodels.MainViewmodel
import com.app.emilockerapp.services.DeviceAdminReceiver
import com.app.emilockerapp.services.LockService
import com.app.emilockerapp.services.SettingsWatchService
import com.app.emilockerapp.ui.theme.EmiLockerAppTheme
import com.app.emilockerapp.uilayer.views.dashboard.HomeScreen
import com.app.emilockerapp.uilayer.views.emi.EmiScreen
import com.app.emilockerapp.uilayer.views.security.SecurityScreen
import com.app.emilockerapp.uilayer.views.auth.LoginScreen
import com.app.emilockerapp.uilayer.views.phoneRegisterScreen.PhoneRegisterScreen
import com.app.emilockerapp.uilayer.views.test.LockedScreen
import com.app.emilockerapp.uilayer.views.welcome.WelcomeScreenNavGraph
import com.app.emilockerapp.utils.getMyRef
import com.app.emilockerapp.utils.isRegistered
import com.app.emilockerapp.utils.startKioskMode
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue

class MainActivity : AppCompatActivity() {

    private val settingsEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.app.emilockerapp.RESET_FLOW_ENTERED" -> {
                    val cls = intent.getStringExtra("cls") ?: ""
                    toast("Factory Reset screen opened! Your device locked now")
                    setLockState(true, getMyRef(this@MainActivity))
                }
                "com.app.emilockerapp.FACTORY_RESET_TAPPED" -> {
                    toast("⚠️ Factory Reset tapped! Your device locked now")
                    setLockState(true, getMyRef(this@MainActivity))
                }
                "com.app.emilockerapp.APP_INFO_OPENED" -> {
                    toast("App Info opened! Your device locked now")
                    setLockState(true, getMyRef(this@MainActivity))

                }
                "com.app.emilockerapp.UNINSTALL_TAPPED" -> {
                    toast("⚠️ Uninstall tapped! Your device locked now")
                    setLockState(true, getMyRef(this@MainActivity))
                }
            }
        }
    }

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private lateinit var viewModel: MainViewmodel
    private lateinit var mNavHostController: NavHostController

    private val adminRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            //Toast.makeText(this, "Device admin enabled", Toast.LENGTH_SHORT).show()
            //setupKioskMode()
        } else {
            //Toast.makeText(this, "Device admin not enabled", Toast.LENGTH_SHORT).show()
        }
    }

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

//        startKioskMode(this)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewmodel::class.java]

        enableEdgeToEdge()
        setupDeviceAdmin()
        //disableDeviceAdmin()
        checkAndRequestPermissions()
        startBackgroundService()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContent {
            MainView(false)
        }
        blockDeviceRestrictions()

        // Check and request Accessibility permission
        Handler().postDelayed({
            ensureAccessibilityEnabled()
            openAppDetails(this)
        }, 5000)

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

    fun setLockState(lock: Boolean, myRef1: DatabaseReference) {
        myRef1.setValue(lock)
            .addOnSuccessListener {
                Toast.makeText(this, "Your Device is Locked", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update lock", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                checkDb(myRef1)
            }
    }

    private fun checkDb(myRef: DatabaseReference) {
        myRef.addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = snapshot.getValue<Boolean>()
                Log.d(TAG, "Value is: " + value)

                if (value == true) {
                    //setupKioskMode()
                    //disableFileSharing(this@MainActivity)

                    devicePolicyManager.lockNow()

                    Toast.makeText(this@MainActivity, "yes", Toast.LENGTH_SHORT).show()

                } else{
                    unlockApp()
                    //unblockDeviceRestrictions()
                    //enableFileSharing(this@MainActivity)
                }

                setContent {
                    MainView(value == true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }

        })
    }

    private fun ensureAccessibilityEnabled() {
        if (!isA11yServiceEnabled(this, SettingsWatchService::class.java)) {
            // Show instruction or open the settings page
            openAccessibilitySettings(this)
        } else {
            // Already enabled — safe to continue normal flow
            // e.g. start your overlay or background logic here
        }
    }

    fun isA11yServiceEnabled(context: Context, service: Class<*>): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val cn = ComponentName(context, service).flattenToString()
        return enabled.split(':').any { it.equals(cn, ignoreCase = true) }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openAppDetails(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }


    override fun onResume() {
        super.onResume()
        startKioskMode(this@MainActivity)

        checkDb(getMyRef(this))
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestPermissions() {
        // Check if SMS permissions are granted

        // Check if notification permission is granted
        val notificationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED


        // If permissions are not granted, request them
        val permissionsToRequest = mutableListOf<String>()


        if (!notificationPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }


        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                POST_NOTIFICATION_PERMISSION_REQUEST_CODE // permission @nahid Use the same request code for all permissions
            )
        }
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

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setupDeviceAdmin() {
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)

        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "This app requires device admin permissions to function as a kiosk."
                )
            }
            adminRequestLauncher.launch(intent)
        } else {
            //setupKioskMode()
        }
    }

    private fun disableDeviceAdmin() {
        devicePolicyManager.removeActiveAdmin(adminComponent)
        Toast.makeText(this, "Device Admin Disabled", Toast.LENGTH_SHORT).show()
    }

    private fun setupKioskMode() {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )

            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            lockApp()
            startLockTask()
            if (devicePolicyManager.isLockTaskPermitted(packageName)) {


            }
        }
    }

    private fun lockApp() {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            startLockTask()
            Toast.makeText(this, "Device Locked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unlockApp() {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            stopLockTask()
            //Toast.makeText(this, "Device Unlocked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun blockDeviceRestrictions() {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            try {
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)

                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)


                //Toast.makeText(this, "Factory reset is now blocked", Toast.LENGTH_SHORT).show()
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to block factory reset: ${e.message}")
                //Toast.makeText(this, "Blocking factory reset failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            //Toast.makeText(this, "Device admin not active", Toast.LENGTH_SHORT).show()
        }
    }

    fun disableFileSharing(context: Context) {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SHARE_LOCATION)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
        }
    }

    fun enableFileSharing(context: Context) {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_SHARE_LOCATION)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
        }
    }

    private fun unblockDeviceRestrictions() {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            try {
                devicePolicyManager.clearUserRestriction(
                    adminComponent,
                    UserManager.DISALLOW_FACTORY_RESET
                )
                devicePolicyManager.clearUserRestriction(
                    adminComponent,
                    UserManager.DISALLOW_SAFE_BOOT
                )
                devicePolicyManager.clearUserRestriction(
                    adminComponent,
                    UserManager.DISALLOW_USB_FILE_TRANSFER
                )

                Toast.makeText(this, "Device restrictions removed", Toast.LENGTH_SHORT).show()
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to unblock restrictions: ${e.message}")
                Toast.makeText(this, "Failed to remove restrictions", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Device admin not active", Toast.LENGTH_SHORT).show()
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
                if (!isRegistered(this)){
                    _appCoordinator = EmiLockerNavCoordinator(
                        mNavHostController,
                        mainModifier,
                        createListOfScreensInsideComposable(mNavHostController),
                        PhoneRegisterScreen.Routes.phoneRegister,
                        this
                    )
                } else{
                    if (isLockedScreen){
                        _appCoordinator = EmiLockerNavCoordinator(
                            mNavHostController,
                            mainModifier,
                            createListOfScreensInsideComposable(mNavHostController),
                            //LoginScreen.Routes.emi_Login_Screen,
                            LockedScreen.Routes.emi_Test_Screen,
                            this
                        )
                    } else{
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

        /*EmiLockerAppTheme(darkTheme = false) {
            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                bottomPadding = innerPadding.calculateBottomPadding()
                val mainModifier = Modifier.padding(innerPadding)

                val mNavHostController = androidx.navigation.compose.rememberNavController()

                // Example Button to call blockDeviceRestrictions()
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    androidx.compose.material3.Button(
                        onClick = {
                            val isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
                            if (!isDeviceOwner) {
                                Toast.makeText(this@MainActivity, "Not device owner", Toast.LENGTH_SHORT).show()
                                return@Button
                            } else {
                                devicePolicyManager.clearDeviceOwnerApp(componentName.packageName)
                                Toast.makeText(this@MainActivity, "Device owner clear", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            //blockDeviceRestrictions()
                            Toast.makeText(this@MainActivity, "Clicked", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        androidx.compose.material3.Text("Block Device Restrictions")
                    }

                    androidx.compose.material3.Button(
                        onClick = {
                            Toast.makeText(this@MainActivity, "Clearing permission", Toast.LENGTH_SHORT).show()
                            //devicePolicyManager.clearDeviceOwnerApp(componentName.packageName)
                            //devicePolicyManager.reboot(adminComponent)
                            blockDeviceRestrictions()
                        }
                    ) {
                        androidx.compose.material3.Text("Clear permission")
                    }
                }

                _appCoordinator = EmiLockerNavCoordinator(
                    mNavHostController,
                    mainModifier,
                    createListOfScreensInsideComposable(mNavHostController),
                    SecurityScreen.Routes.emiSecurityScreen,
                    this
                )
                appCoordinator.CatoPayNavHost()
            }
        }*/
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