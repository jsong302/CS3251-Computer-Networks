Joshua Song
jsong302@gatech.edu
CS 3251-A
9/30/16
Sockets Programming Assignment 1

files:  TCPServer.java
		TCPSensor.java
		UDPServer.java
		UDPSensor.java

Compiling:
	javac TCPServer.java TCPSensor.java UDPServer.java UDPSensor.java

Running UDP:
	java UDPServer -p 8591 -f passwords.csv
	java UDPSensor -s localhost -p 8591 -u admin -c password -r 20

Running TCP:
	java TCPServer -p 8591 -f passwords.csv
	java TCPSensor -s localhost -p 8591 -u admin -c password -r 20



UDP SENSOR
--------------------------------
Name	

	UDPServer - creates a udp server for the sensors

Options

	-p
		the port number for the server.
	
	-f
		the location of the csv file that contains the usernames and passwords.

Example

	java UDPServer -p 8591 -f passwords.csv

Name

	UDPSensor - creates a udp sensor

Options

	-s
		the host name of the server
	
	-p
		the port number of the server
		
	-u
		the username of the sensor
		
	-c
		the password of the sensor
	
	-r
		the intended value

Example
	
	java UDPSensor -s localhost -p 8591 -u admin -c password -r 20
	java UDPSensor -s 127.0.0.1 -p 8591 -u john -c smith -r 100
		
TCP SENSOR
------------------------

Name	

	TCPServer - creates a tcp server for the sensors

Options

	-p
		the port number for the server.
	
	-f
		the location of the csv file that contains the usernames and passwords.

Example

	java TCPServer -p 8591 -f passwords.csv

Name

	TCPSensor - creates a tcp sensor

Options

	-s
		the host name of the server
	
	-p
		the port number of the server
		
	-u
		the username of the sensor
		
	-c
		the password of the sensor
	
	-r
		the intended value

Example
	
	TCPSensor -s localhost -p 8591 -u admin -c password -r 20
	TCPSensor -s 127.0.0.1 -p 8591 -u john -c smith -r 100

	
	
Protocol:


1. Sensor sends a packet to the server notifying that a connection has started. This is a boolean value in TCP 
   and a byte array of size 1 in UDP.
2. Server receives the packet and sends a 64 character randomized string. This is a String in TCP and a 64 byte value in UDP.
3. Sensor receives the string, stores it, and sends the inputed user to confirm if the user is in the user list. 
   This sends a String in TCP and a byte array in UDP.
4. Server receives the username, compares it to the userlist, and sends the confirmation back if it is.
   This is a boolean value in TCP and a byte array of size 1 in UDP.
5. Sensor receives confirmation and if it is true, then the sensor sends the md5 hashed username, password, and random string
   This is a byte array for both TCP and UDP
6. Server takes the hash and compared it with its own hash of the username password and random string 
   and sends back a confirmation if it is. This is a boolean value in TCP and a byte array of size 1 in UDP.
7. Sensor receives confirmation and sends the value. This is a double in TCP and a byte array of size 8 in UDP.
8. Server receives value, stores it, calculates the min, max, average of sensor, and average of all and sends to sensor.
   This is a byte array in UDP and multiple double sends in TCP
9. Sensor outputs the calculations.










