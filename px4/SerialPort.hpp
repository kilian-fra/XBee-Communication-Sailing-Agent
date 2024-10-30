#pragma once

class SerialPort {

	int m_Fd;
	const char* m_PortName;
	speed_t m_BaudRate;
	bool m_ShouldBlock;
	int m_BlockingTimeout;

public:
	SerialPort(const char* portName, speed_t baudRate, bool shouldBlock = false, int blockTimeout = 0);
	~SerialPort();

	bool writeData(void* buffer, size_t size);
	bool readData(void* buffer, size_t size);
	bool create();
	bool flush();
	int getAvailableBytes();
};
