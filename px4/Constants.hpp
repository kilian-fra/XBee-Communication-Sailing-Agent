#pragma once

namespace CommConstants {
	/**
	 * The maximum number of bytes for receiving a command
	*/
	static constexpr int COMMAND_SIZE_BYTES = 3;

	/**
	 * The byte-array index position for the command id
	 * The command id is described by the enum CLIFTON_COMMAND_ID.
	*/
	static constexpr auto COMMAND_ID_IDX = 0;

	/**
	 * The timeout for the serial port to block in ms
	*/
	static constexpr auto CONNECTION_LOST_TIMEOUT = 3000; //3s (3000ms)

	/**
	 * The time interval for the telemetry data transmission in milliseconds
	*/
	static constexpr long TELEMETRY_TRANSMISSION_INTERVAL = 1000;

	/**
	 * The serial port name for the telemetry channel that is used for the xbee device
	*/
	static constexpr auto TELEM_SERIAL_PORT = "/dev/ttyS1";

	static constexpr long RECONNECT_INTERVAL = 1000;

	/*
	* Macro function, to convert milliseconds to microseconds
	*/
	constexpr auto msToUs(int ms) {
		return ms * 1000;
	}
}
