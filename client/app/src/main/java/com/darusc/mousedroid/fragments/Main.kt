package com.darusc.mousedroid.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.darusc.mousedroid.R
import com.darusc.mousedroid.databinding.FragmentMainBinding
import com.darusc.mousedroid.networking.Connection
import com.darusc.mousedroid.networking.hasUsbConnection
import com.darusc.mousedroid.viewmodels.ConnectionViewModel
import kotlinx.coroutines.launch

class Main : Fragment() {

    private lateinit var binding: FragmentMainBinding
    private lateinit var loadingPopup: PopupWindow

    private val viewModel: ConnectionViewModel by activityViewModels()

    @SuppressLint("MissingPermission")
    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.startBluetoothMode(requireContext(), true)
            } else {
                Toast.makeText(context, "Bluetooth is required for this mode.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        loadingPopup = PopupWindow(
            layoutInflater.inflate(R.layout.loading_fragment, null),
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT,
        )

        return binding.root
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConnectSV.setOnClickListener {
            viewModel.startServerMode(requireContext())
        }

        binding.btnConnectBT.setOnClickListener {
            viewModel.startBluetoothMode(requireContext())
        }

        binding.btnPairBT.setOnClickListener {
            viewModel.startBluetoothMode(requireContext())
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
            }
            startActivity(intent)
        }

        // Auto-connect if USB is available when the fragment is shown
        tryAutoConnectUsb()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        when (state) {
                            is ConnectionViewModel.State.Searching -> {
                                if (!loadingPopup.isShowing) {
                                    loadingPopup.contentView.findViewById<TextView>(R.id.loadingMessage).text = state.message
                                    loadingPopup.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
                                }
                                // Hide the buttons when searching/connecting automatically
                                binding.btnConnectSV.visibility = View.GONE
                                binding.btnConnectBT.visibility = View.GONE
                                binding.btnPairBT.visibility = View.GONE
                            }

                            is ConnectionViewModel.State.Connecting -> {
                                if (!loadingPopup.isShowing) {
                                    loadingPopup.contentView.findViewById<TextView>(R.id.loadingMessage).text = state.message
                                    loadingPopup.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
                                }
                            }

                            is ConnectionViewModel.State.Reconnecting -> {
                                if (!loadingPopup.isShowing) {
                                    loadingPopup.contentView.findViewById<TextView>(R.id.loadingMessage).text = "Reconnecting... (attempt ${state.attempt})"
                                    loadingPopup.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
                                }
                            }

                            is ConnectionViewModel.State.Idle -> {
                                loadingPopup.dismiss()
                                // Show buttons again
                                binding.btnConnectSV.visibility = View.VISIBLE
                                binding.btnConnectBT.visibility = View.VISIBLE
                                binding.btnPairBT.visibility = View.VISIBLE
                            }

                            is ConnectionViewModel.State.Connected -> {
                                loadingPopup.dismiss()
                            }

                            is ConnectionViewModel.State.Error -> {
                                loadingPopup.dismiss()
                                binding.btnConnectSV.visibility = View.VISIBLE
                                binding.btnConnectBT.visibility = View.VISIBLE
                                binding.btnPairBT.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    viewModel.events.collect {
                        when (it) {
                            is ConnectionViewModel.Event.NavigateToInput -> {
                                findNavController().navigate(R.id.action_main_to_touchpad)
                            }

                            is ConnectionViewModel.Event.NavigateToDeviceList -> {
                                findNavController().navigate(R.id.action_main_to_devicelist, bundleOf("CONNECTION_MODE" to it.mode))
                            }

                            is ConnectionViewModel.Event.NavigateToMain -> {}

                            is ConnectionViewModel.Event.ConnectionDisconnected -> {
                                val pview = showPopupDialog(R.layout.connection_disconnected_fragment)
                                pview?.apply {
                                    if (it.connectionMode == Connection.Mode.BLUETOOTH) {
                                        findViewById<TextView>(R.id.subtitle).text = "Bluetooth connection to ${it.hostName} was terminated"
                                        findViewById<TextView>(R.id.description).text = "Host device turned bluetooth off or disconnected this device"
                                    } else {
                                        findViewById<TextView>(R.id.subtitle).text = "USB connection was interrupted"
                                        findViewById<TextView>(R.id.description).text = "Reconnecting automatically... If this persists, check the USB cable and make sure the server is running."
                                    }
                                }
                            }

                            is ConnectionViewModel.Event.ConnectionFailed -> {
                                val pview = showPopupDialog(R.layout.connection_failed_fragment)
                                pview?.apply {
                                    if (it.connectionMode == Connection.Mode.BLUETOOTH) {
                                        findViewById<TextView>(R.id.subtitle).text = "Bluetooth connection failed"
                                        findViewById<TextView>(R.id.description).text = "Make sure the device is on"
                                    } else if (it.connectionMode == Connection.Mode.USB) {
                                        findViewById<TextView>(R.id.subtitle).text = "USB connection failed"
                                        findViewById<TextView>(R.id.description).text = "Make sure:\n• The server is running on your computer\n• USB debugging is enabled on this phone\n• You authorized this computer on your phone"
                                    } else {
                                        findViewById<TextView>(R.id.subtitle).text = "Connection failed"
                                        findViewById<TextView>(R.id.description).text = "To connect over WiFi make sure you are on the same network.\nTo connect over USB make sure the server is running and ADB is on."
                                    }
                                }
                            }

                            is ConnectionViewModel.Event.EnableBluetooth -> {
                                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                enableBluetoothLauncher.launch(intent)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    /**
     * Try to auto-connect via USB when the fragment becomes visible.
     */
    private fun tryAutoConnectUsb() {
        val context = context ?: return
        if (hasUsbConnection(context)) {
            viewModel.tryAutoConnect(context)
        }
    }

    override fun onResume() {
        super.onResume()
        // Try auto-connect again when returning from other fragments
        tryAutoConnectUsb()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnect()
    }
}
