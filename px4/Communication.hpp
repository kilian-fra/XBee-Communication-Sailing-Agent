#pragma once

#include <drivers/drv_hrt.h>
#include <px4_platform_common/px4_config.h>
#include <px4_platform_common/defines.h>
#include <px4_platform_common/posix.h>
#include <px4_platform_common/tasks.h>
#include <px4_platform_common/module.h>
#include <px4_platform_common/log.h>
#include <px4_platform_common/atomic.h>
#include <uORB/topics/sensordaten.h>
#include <uORB/topics/clifton_command.h>
#include <uORB/topics/vehicle_status.h>
#include <uORB/uORB.h>
#include <uORB/Subscription.hpp>
#include <uORB/SubscriptionCallback.hpp>
#include <uORB/Publication.hpp>

#include "SerialPort.hpp"

/**
 * This enum represents all available commands the sail agent needs to execute.
*/
typedef enum : uint8_t {
	HEARTBEAT,
	START_ROUTE,
	STOP_ROUTE,
	SET_COURSE
} CLIFTON_COMMAND_ID;

/**
 * Status flags for the laptop encoded as singl byte
*/
typedef union {
	struct {
		uint8_t isAutonomous : 1;
		uint8_t reserved : 7;
	};

	uint8_t packedInfo;
} STATUS_INFO;

/**
 * Struct that holds the telemetry data, that gets transmited to the laptop.
*/
#pragma pack(push, 1)
typedef struct {
        uint16_t windDirection;
	uint16_t windSpeed;
	uint16_t agentSpeed;
	int16_t agentPosX;
	int16_t agentPosY;
	uint8_t batteryStatus;
	uint16_t agentDirection;
        STATUS_INFO statusInfo;
} TELEMETRY_DATA;
#pragma pack(pop)


class Communication final
	: public ModuleBase<Communication>
{
	SerialPort m_SerialPort;
	pthread_t m_WorkerThread;
	px4::atomic<bool> m_IsConnected;
	px4::atomic<bool> m_ThreadRunning;
	long m_LastCommandReceived;
	long m_LastTelemetryTransmission;
	long m_LastReconnectCheck;
	struct clifton_command_s m_CliftonCommand;
	TELEMETRY_DATA m_TelemetryData;

	static void* worker(void* arg);
	static long getCurrentTimeInMs();
	void print_info();
	void stop();
	void handleCommandReceive(long currentSysTime);
	void handleTelemetryTransmission(long currentSysTime);
	void logTelemetryData(const TELEMETRY_DATA& data);
	void sensordataPoll(TELEMETRY_DATA* pTelemData);
	void vehicleStatusPoll(TELEMETRY_DATA* pTelemData);
	void invalidateTelemetryData(TELEMETRY_DATA* pTelemData);

	uORB::Subscription m_VehicleStatus {ORB_ID(vehicle_status)};
	uORB::Subscription m_Sensordaten {ORB_ID(sensordaten)};
	uORB::Publication<clifton_command_s> m_CliftonCommandPub {ORB_ID(clifton_command)};

public:

	Communication();
	~Communication();
	Communication(const Communication&) = delete;
	Communication operator=(const Communication&) = delete;

	static int task_spawn(int argc, char* argv[]);
	static int print_usage(const char *reason = nullptr);
	static int custom_command(int argc, char *argv[]);

	bool init();
};
