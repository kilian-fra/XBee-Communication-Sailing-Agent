#include <fcntl.h>
#include <termios.h>
#include <unistd.h>
#include <cstring>
#include <stdint.h>
#include <sys/ioctl.h>
#include "SerialPort.hpp"

SerialPort::SerialPort(const char* portName, speed_t baudRate, bool shouldBlock, int blockTimeout)
        : m_Fd(-1), m_PortName(portName), m_BaudRate(baudRate), m_ShouldBlock(shouldBlock), m_BlockingTimeout(blockTimeout)
{}

SerialPort::~SerialPort() {
        if (m_Fd > 0) close(m_Fd);
}

bool SerialPort::create() {
	int oflags = m_ShouldBlock ? O_RDWR | O_NOCTTY | O_SYNC
				   : O_RDWR | O_NOCTTY | O_NONBLOCK;

    	m_Fd = open(m_PortName, oflags);
	if (m_Fd == -1) return false;

	struct termios tty;
	memset(&tty, 0, sizeof(tty));
	if (tcgetattr(m_Fd, &tty) != 0) {
		close(m_Fd);
		return false;
	}

	cfsetospeed(&tty, m_BaudRate);
	cfsetispeed(&tty, m_BaudRate);

	tty.c_cflag = (tty.c_cflag & ~CSIZE) | CS8; // 8 data bits
	tty.c_lflag = 0;                            // no signaling chars, no echo, no canonical processing
	tty.c_oflag = 0;                            // no remapping, no delays

	//Set blocking mode for read (atleast 1 byte)
	tty.c_cc[VMIN] = m_ShouldBlock ? 1 : 0;
	tty.c_cc[VTIME] = m_BlockingTimeout; //timeout

	tty.c_iflag &= ~(IXON | IXOFF | IXANY);     // shut off xon/xoff ctrl
	tty.c_iflag &= ~(IGNBRK|BRKINT|PARMRK|ISTRIP|INLCR|IGNCR|ICRNL); // Disable any special handling of received bytes

	tty.c_cflag |= (CLOCAL | CREAD);            // Enable receiver and set local mode
	tty.c_cflag &= ~(PARENB | PARODD);          // shut off parity
	tty.c_cflag &= ~CSTOPB;                     // 1 stop bit
	tty.c_cflag &= ~CRTSCTS;                    // no hardware flow control

	if (tcsetattr(m_Fd, TCSANOW, &tty) != 0) {
		close(m_Fd);
		return false;
	}

	//Clear input and output
	flush();

	return true;
}

bool SerialPort::writeData(void* buffer, size_t size) {
        size_t bytesWrite = 0;
        while (bytesWrite < size) {
            ssize_t result = write(m_Fd, (uint8_t*)buffer + bytesWrite, size - bytesWrite);
            if (result < 0) return false;
            bytesWrite += result;
        }

        return true;
}

bool SerialPort::readData(void* buffer, size_t size) {
        size_t bytesRead = 0;
        while (bytesRead < size) {
            ssize_t result = read(m_Fd, (uint8_t*)buffer + bytesRead, size - bytesRead);
            if (result < 0) return false;
            bytesRead += result;
        }

        return true;
}

/**
 * This method flushes both, the output and input
*/
bool SerialPort::flush() {
	return tcflush(m_Fd, TCIOFLUSH) != -1;
}

int SerialPort::getAvailableBytes() {
	int availableBytes = 0;
	ioctl(m_Fd, FIONREAD, &availableBytes);
	return availableBytes;
}
