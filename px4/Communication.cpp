#include <drivers/drv_hrt.h>
#include <termios.h>
#include "Communication.hpp"
#include <pthread.h>
#include <unistd.h>
#include <sys/time.h>
#include "Constants.hpp"

extern "C" __EXPORT int communication_main(int argc, char* argv[]);

/**
 * Communication Constructor
*/
Communication::Communication() :
	m_SerialPort(CommConstants::TELEM_SERIAL_PORT, B38400),
	m_WorkerThread(-1),
	m_IsConnected(false),
	m_ThreadRunning(false),
	m_LastCommandReceived(0),
	m_LastTelemetryTransmission(0),
	m_LastReconnectCheck(0)
{
	m_CliftonCommand.course = 0;
	m_CliftonCommand.is_stop_route = true;
	m_CliftonCommand.is_sail_agent_connected = false;

	//Invalidate telemetry data struct
	invalidateTelemetryData(&m_TelemetryData);
}

/**
 * Communication Destructor.
 * Terminates the connection by closing the threads and the serial port.
*/
Communication::~Communication() {
	stop();
}

int Communication::task_spawn(int argc, char* argv[]) {
	Communication *instance = new Communication();
	if (instance) {
		_object.store(instance);
		_task_id = task_id_is_work_queue;

		if (instance->init()) {
			return PX4_OK;
		}

	} else {
		PX4_ERR("alloc failed");
	}

	delete instance;
	_object.store(nullptr);
	_task_id = -1;

	return PX4_ERROR;
}

int Communication::print_usage(const char *reason) {
	if (reason) {
		PX4_WARN("%s\n", reason);
	}

	PX4_INFO("Usage: communication {start|stop|information}");
	return 0;
}

void Communication::print_info() {
	PX4_INFO("Communication info:");
	PX4_INFO("  Is Connected: %s", m_IsConnected.load() ? "true" : "false");
	PX4_INFO("  Threads Running: %s", m_ThreadRunning.load() ? "true" : "false");
}

void Communication::stop() {
	if (m_ThreadRunning.load()) {
        	m_ThreadRunning.store(false);
        	pthread_join(m_WorkerThread, nullptr);
		m_IsConnected.store(false);
        	PX4_INFO("Communication stopped");
    	} else {
		PX4_INFO("Communication already stopped");
	}
}

int Communication::custom_command(int argc, char *argv[]) {
	if (argc <= 0) {
		PX4_ERR("Invalid usage for communication\nUsage: communication {start|stop|information}");
		return 0;
	}

	if (strcmp(argv[0], "information") == 0) {
		_object.load()->print_info();
	} else if (strcmp(argv[0], "stop") == 0) {
		_object.load()->stop();
	} else {
		PX4_ERR("Invalid usage for communication\nUsage: communication {start|stop|information}");
	}

	return 0;
}

/**
 * Initializes the communication by creating the-
 * serial port connection and two worker threads.
 *
 * @return bool - true if the initialization was successfully or false if not
*/
bool Communication::init() {
	PX4_INFO("Communication init");
	if (!m_SerialPort.create()) {
		PX4_ERR("Failed to open Xbee Serial Port");
		return false;
	}

	m_ThreadRunning.store(true);
	m_IsConnected.store(true);

	if (pthread_create(&m_WorkerThread, nullptr, &Communication::worker, this) != 0) {
		PX4_ERR("Failed to start transmitWorker thread");
		m_ThreadRunning.store(false);
		return false;
	}

	PX4_INFO("Communication initialized successfully");
	return true;
}

/*
* A method that returns the current system time in ms.
* Used for time measurements (e.g. transmission interval etc.)
*/
long Communication::getCurrentTimeInMs() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec * 1000L + tv.tv_usec / 1000L;
}

/*
* Method, that handles the receiving and publishing of commands
*/
void Communication::handleCommandReceive(long currentSysTime) {
	auto availableBytes = m_SerialPort.getAvailableBytes();
	uint8_t buffer[CommConstants::COMMAND_SIZE_BYTES] = {0xDE, 0xAD, 0xBE};

	if (!m_IsConnected.load() && currentSysTime - m_LastReconnectCheck
		< CommConstants::RECONNECT_INTERVAL) {
		return;
	}

	if ((availableBytes <= 0 && currentSysTime - m_LastCommandReceived
		>= CommConstants::CONNECTION_LOST_TIMEOUT)
		|| (availableBytes >= CommConstants::COMMAND_SIZE_BYTES
			&& !m_SerialPort.readData(buffer, CommConstants::COMMAND_SIZE_BYTES))) {

		PX4_ERR("Sail agent has no connection -> Attempting to connect...");

		//Check if connection already lost (if so do not publish)
		if (m_IsConnected.load()) {
			m_IsConnected.store(false);
			m_CliftonCommand.is_sail_agent_connected = false;
			m_CliftonCommandPub.publish(m_CliftonCommand);
		}

		m_LastReconnectCheck = currentSysTime;
		return;
	}

	//Avoid command parsing, if no command is actual received
	if (memcmp(buffer, "\xDE\xAD\xBE", sizeof(buffer)) == 0) return;

	//Check if connection was lost before
	if (!m_IsConnected.load()) {
		PX4_INFO("Sail agent connected");
		m_IsConnected.store(true);
	}

	m_CliftonCommand.is_sail_agent_connected = m_IsConnected.load();

	//File out command struct before publish
	switch (buffer[CommConstants::COMMAND_ID_IDX]) {
		case CLIFTON_COMMAND_ID::HEARTBEAT:
			PX4_INFO("Received HEARTBEAT");
			break;

		case CLIFTON_COMMAND_ID::SET_COURSE:
			memcpy(&m_CliftonCommand.course, buffer + 1, sizeof(uint16_t));
			PX4_INFO("Received SET_COURSE: %hu", m_CliftonCommand.course);
			break;

		case CLIFTON_COMMAND_ID::START_ROUTE:
			m_CliftonCommand.is_stop_route = false;
			PX4_INFO("Received START_ROUTE");
			break;

		case CLIFTON_COMMAND_ID::STOP_ROUTE:
			m_CliftonCommand.is_stop_route = true;
			PX4_INFO("Received STOP_ROUTE");
			break;

		default:
			uint16_t course = 0;
			memcpy(&course, buffer + 1, sizeof(uint16_t));
			PX4_ERR("Received invalid command: id (%hhu), data (%hu)",
					buffer[0], course);
			break;
	}

	m_LastCommandReceived = currentSysTime;

	//Publish clifton command topic
	m_CliftonCommandPub.publish(m_CliftonCommand);
}

/*
* Method for logging the telemetry data
*/
void Communication::logTelemetryData(const TELEMETRY_DATA& data) {
    PX4_INFO("Telemetry Data:");
    PX4_INFO("  Wind Direction: %u", data.windDirection);
    PX4_INFO("  Wind Speed: %u", data.windSpeed);
    PX4_INFO("  Agent Speed: %u", data.agentSpeed);
    PX4_INFO("  Agent Position X: %d", data.agentPosX);
    PX4_INFO("  Agent Position Y: %d", data.agentPosY);
    PX4_INFO("  Battery Status: %u%%", data.batteryStatus);
    PX4_INFO("  Agent Direction: %u", data.agentDirection);
    PX4_INFO("  Autonomous Mode: %s", data.statusInfo.isAutonomous ? "Yes" : "No");
}

/*
* Method for polling the sensordata information and filling out the telemetry data
*/
void Communication::sensordataPoll(TELEMETRY_DATA* pTelemData) {
	struct sensordaten_s sensorData = {};

	if (m_Sensordaten.updated()) {
		m_Sensordaten.copy(&sensorData);

		//Fill out telemetry data
		pTelemData->agentDirection = sensorData.clifton_direction;
		pTelemData->agentPosX = sensorData.ned_x;
		pTelemData->agentPosY = sensorData.ned_y;
		pTelemData->agentSpeed = sensorData.clifton_speed;
		pTelemData->windSpeed = sensorData.wind_speed;
		pTelemData->batteryStatus = sensorData.battery_status;
		pTelemData->windDirection = sensorData.median_wind_direction;
	}
}

/**
 * This Method invalidates the telemetry data struct.
 * This is needed at the beginning of the lifecycle, because no real sensordata is available.
*/
void Communication::invalidateTelemetryData(TELEMETRY_DATA* pTelemData) {
	pTelemData->windDirection = UINT16_MAX;
	pTelemData->windSpeed = UINT16_MAX;
	pTelemData->agentSpeed = UINT16_MAX;
	pTelemData->agentPosX = INT16_MAX;
	pTelemData->agentPosY = INT16_MAX;
	pTelemData->batteryStatus = UINT8_MAX;
	pTelemData->agentDirection = UINT16_MAX;
	pTelemData->statusInfo.packedInfo = UINT8_MAX;
}

/*
* Method for polling the vehicle status information like the current mode (manuel or autonomous)
*/
void Communication::vehicleStatusPoll(TELEMETRY_DATA* pTelemData) {
	struct vehicle_status_s vechicleStatus = {};

	//Receive information, if sail agent is in manuel mode or not
	if (m_VehicleStatus.updated()) {
		m_VehicleStatus.copy(&vechicleStatus);
		pTelemData->statusInfo.isAutonomous
			= (vechicleStatus.nav_state == vehicle_status_s::NAVIGATION_STATE_ACRO);
	}
}

/*
* Method, that handles the transmission to the laptop
*/
void Communication::handleTelemetryTransmission(long currentSysTime) {
	if (!m_IsConnected.load() || currentSysTime - m_LastTelemetryTransmission
		< CommConstants::TELEMETRY_TRANSMISSION_INTERVAL) {
		return;
	}

	sensordataPoll(&m_TelemetryData);
	vehicleStatusPoll(&m_TelemetryData);

	//Send data to laptop
	if (!m_SerialPort.writeData(&m_TelemetryData, sizeof(TELEMETRY_DATA))) {
		PX4_ERR("transmitWorker failed to write telemetry data to serial port");
	} else {
		logTelemetryData(m_TelemetryData);
		m_LastTelemetryTransmission = currentSysTime;
	}
}

/*
* Worker thread that handle both, transmission and receiving
*/
void* Communication::worker(void* arg) {
	auto thisRef = static_cast<Communication*>(arg);
	auto currentSysTime = getCurrentTimeInMs();

	while (thisRef->m_ThreadRunning.load()) {
		thisRef->handleCommandReceive(currentSysTime);
		thisRef->handleTelemetryTransmission(currentSysTime);
		usleep(CommConstants::msToUs(1));
		currentSysTime = getCurrentTimeInMs();
	}

	return (void*)0;
}

/**
 * Entry point for the communication module.
 *
 * @return int
*/
int communication_main(int argc, char* argv[]) {
	return Communication::main(argc, argv);
}
