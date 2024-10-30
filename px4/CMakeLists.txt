px4_add_module(
	MODULE modules__communication
	MAIN communication
	STACK_MAIN 2000
	SRCS
		Communication.cpp
		Communication.hpp
		SerialPort.cpp
		SerialPort.hpp
		Constants.hpp
	DEPENDS
	)
