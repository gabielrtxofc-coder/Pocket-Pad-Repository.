#ifndef ADB_HPP
#define ADB_HPP

#ifdef _WIN32

#include <windows.h>
#include <string>
#include <vector>
#include <cstdio>

#include "logger.h"

namespace adb
{
	/*
	* Device status enumeration
	*/
	enum class DeviceStatus {
		NONE,           // No devices connected
		UNAUTHORIZED,   // Device connected but not authorized
		OFFLINE,        // Device offline
		CONNECTED,      // At least one device connected and authorized
		MULTIPLE        // Multiple devices connected
	};

	/*
	* Get the local directory of the program
	*/
	inline std::string dir()
	{
		char buffer[MAX_PATH];
		GetModuleFileNameA(NULL, buffer, MAX_PATH);

		std::string path(buffer);
		return path.substr(0, path.find_last_of("\\/"));
	}

	/*
	* Get the full path to adb.exe
	*/
	inline std::string adbPath()
	{
		return dir() + "\\adb.exe";
	}

	/*
	* Execute an adb command and capture output.
	* Returns the exit code. Output is stored in the output parameter.
	*/
	inline int exec(const std::string& args, std::string& output)
	{
		std::string command = "\"" + adbPath() + "\" " + args + " 2>&1";
		
		output.clear();
		
		FILE* pipe = _popen(command.c_str(), "r");
		if (!pipe) return -1;
		
		char buffer[256];
		while (fgets(buffer, sizeof(buffer), pipe) != NULL) {
			output += buffer;
		}
		
		return _pclose(pipe);
	}

	/*
	* Execute an adb command without capturing output.
	* Returns the exit code.
	*/
	inline int execSimple(const std::string& args)
	{
		std::string command = "\"" + adbPath() + "\" " + args + " 2>nul";
		return system(command.c_str());
	}

	/*
	* Check connected devices by parsing "adb devices" output.
	* Returns the overall status.
	*/
	inline DeviceStatus checkDevices()
	{
		std::string output;
		int result = exec("devices", output);
		
		if (result != 0) {
			LOG("[ADB] Failed to query devices");
			return DeviceStatus::NONE;
		}
		
		// Parse output: skip first line ("List of devices attached")
		// Each subsequent line: <serial>\t<state>
		int deviceCount = 0;
		bool hasUnauthorized = false;
		bool hasOffline = false;
		bool hasAuthorized = false;
		
		size_t pos = 0;
		bool firstLine = true;
		while (pos < output.size()) {
			size_t end = output.find('\n', pos);
			if (end == std::string::npos) end = output.size();
			
			std::string line = output.substr(pos, end - pos);
			pos = end + 1;
			
			// Skip empty lines
			if (line.empty() || line.find('\r') == 0) continue;
			
			// Skip the header line
			if (firstLine) {
				firstLine = false;
				continue;
			}
			
			// Parse "serial\tstate"
			size_t tabPos = line.find('\t');
			if (tabPos == std::string::npos) continue;
			
			std::string state = line.substr(tabPos + 1);
			// Trim trailing whitespace
			while (!state.empty() && (state.back() == '\r' || state.back() == ' ' || state.back() == '\n'))
				state.pop_back();
			
			deviceCount++;
			
			if (state == "unauthorized") {
				hasUnauthorized = true;
			} else if (state == "offline") {
				hasOffline = true;
			} else if (state == "device") {
				hasAuthorized = true;
			}
		}
		
		if (deviceCount == 0) {
			return DeviceStatus::NONE;
		}
		if (deviceCount > 1) {
			return DeviceStatus::MULTIPLE;
		}
		if (hasAuthorized) {
			return DeviceStatus::CONNECTED;
		}
		if (hasUnauthorized) {
			return DeviceStatus::UNAUTHORIZED;
		}
		if (hasOffline) {
			return DeviceStatus::OFFLINE;
		}
		
		return DeviceStatus::NONE;
	}

	/*
	* Get a human-readable status message for the device status.
	*/
	inline std::string statusMessage(DeviceStatus status)
	{
		switch (status) {
			case DeviceStatus::NONE:
				return "No Android device connected";
			case DeviceStatus::UNAUTHORIZED:
				return "Device connected - authorize USB debugging on phone";
			case DeviceStatus::OFFLINE:
				return "Device offline";
			case DeviceStatus::CONNECTED:
				return "Device ready";
			case DeviceStatus::MULTIPLE:
				return "Multiple devices detected - connect only one";
			default:
				return "Unknown state";
		}
	}
	
	/*
	* Reverse the given tcp port.
	* adb reverse tcp:<port> tcp:<port>
	* Returns true if successful.
	*/
	inline bool start(int port)
	{
		std::string path = adbPath();
		std::string command = "\"" + path + "\" reverse tcp:" + std::to_string(port) + " tcp:" + std::to_string(port) + " 2>&1";
		
		std::string output;
		int result = exec("reverse tcp:" + std::to_string(port) + " tcp:" + std::to_string(port), output);

		if (result == 0)
		{
			LOG("[ADB:", port, "] Reverse tunnel established");
			return true;
		}
		else
		{
			LOG("[ADB:", port, "] Failed to establish reverse tunnel: ", output);
			return false;
		}
	}

	/*
	* Removes the reversed tcp port and cleans up.
	* adb reverse --remove tcp:<port>
	*/
	inline bool stop(int port)
	{
		std::string path = adbPath();
		
		// First remove the specific reverse tunnel
		std::string command = "\"" + path + "\" reverse --remove tcp:" + std::to_string(port);
		execSimple(command);
		
		LOG("[ADB:", port, "] Reverse tunnel removed");
		return true;
	}

	// Backward compatibility alias
	inline bool kill(int port) { return stop(port); }
}

#endif // _WIN32
#endif // ADB_HPP
