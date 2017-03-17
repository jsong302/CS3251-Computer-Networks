
import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.lang.StringBuilder;
import java.lang.StringBuffer;
import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;

class UDPServer
{
	static int port;
	static String csvFile;
	static Hashtable<String, String> users; // Stores the list of users and passwords
	static DatagramSocket serverSocket;
	static byte[] receiveData;
	static Hashtable<String, ArrayList<Double>> values; //Stores the values pertaining to each user
	static Hashtable<SocketAddress, String> current; // Stores the sensors currently sending data
	static Hashtable<SocketAddress, String> hashes; // Stores the salt values for each instance
	static Hashtable<SocketAddress, Integer> states; // Stores the state of each sensor
	static ZoneId zone = ZoneId.of("-05:00");
	
	// -------------- HELPER FUNCTIONS ------------------------
	private static double toDouble(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getDouble();
	}
	
	private static byte[] toByteArray(double[] doubleArray){
		int times = Double.SIZE / Byte.SIZE;
		byte[] bytes = new byte[doubleArray.length * times];
		for(int i=0;i<doubleArray.length;i++){
			ByteBuffer.wrap(bytes, i*times, times).putDouble(doubleArray[i]);
		}
		return bytes;
	}
	
	private static byte[] toByteArray(double value) {
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putDouble(value);
		return bytes;
	}
	
	private static boolean authUser(String u, byte[] h, SocketAddress a) {
		
		try{
			MessageDigest digest = MessageDigest.getInstance("MD5");
			String login = u + users.get(u) + hashes.get(a);
			byte[] hash = digest.digest(login.getBytes());
			return digest.isEqual(h, hash);
		} catch(GeneralSecurityException e) {
			System.err.println("GeneralSecurityException: " + e.getMessage());
			return false;
		}
	}
	
	
	private static String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 64) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }
	
	//---------------------END OF HELPER FUNCTIONS ---------------------
	
	/*
	 * The authentication state where a salt is sent to the user and
	 * the salt is stored in the hash hashtable.
	 */
	private static void authentication(DatagramPacket packet) throws IOException {
		String salt = getSaltString();
		InetAddress packetIPAddress = packet.getAddress();
		int packetPort = packet.getPort();
		states.put(packet.getSocketAddress(), 1);
		hashes.put(packet.getSocketAddress(), salt);
		DatagramPacket authPacket = new DatagramPacket(salt.getBytes(), salt.getBytes().length, packetIPAddress, packetPort);
		serverSocket.send(authPacket);
	}
	
	/*
	 * The state that accepts the name of the user and stores
	 * it in the user hashtable
	 */
	private static void getUser(DatagramPacket packet) throws IOException {
		String inputUser = new String(packet.getData(), 0, packet.getLength());
		InetAddress packetIPAddress = packet.getAddress();
		int packetPort = packet.getPort();
		if(users.containsKey(inputUser)) {
			current.put(packet.getSocketAddress(), inputUser);
			states.replace(packet.getSocketAddress(), 2);
			byte[] confirm = {1};
			DatagramPacket confirmPacket = new DatagramPacket(confirm, confirm.length, packetIPAddress, packetPort);
			serverSocket.send(confirmPacket);
		} else {
			System.out.println("User Authorization Failed");
			byte[] confirm = {0};
			DatagramPacket confirmPacket = new DatagramPacket(confirm, confirm.length, packetIPAddress, packetPort);
			serverSocket.send(confirmPacket);
			current.remove(packet.getSocketAddress());
			states.remove(packet.getSocketAddress());
			hashes.remove(packet.getSocketAddress());
		}
		
	}
	
	/*
	 * The state that authenticates the user and sends the
	 * confirmation to the sensor
	 */
	private static void login(DatagramPacket packet) throws IOException {
		String inputUser = current.get(packet.getSocketAddress());
		byte[] hash = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
		InetAddress packetIPAddress = packet.getAddress();
		int packetPort = packet.getPort();
		if(authUser(inputUser, hash, packet.getSocketAddress())) {
			System.out.println("User Confirmed");
			current.put(packet.getSocketAddress(), inputUser);
			states.replace(packet.getSocketAddress(), 3);
			byte[] confirm = {1};
			DatagramPacket confirmPacket = new DatagramPacket(confirm, confirm.length, packetIPAddress, packetPort);
			serverSocket.send(confirmPacket);
		} else {
			System.out.println("User authorization failed for client: " + packetIPAddress.getHostAddress() + " port: " + packetPort + " user: " + inputUser);
			byte[] confirm = {0};
			DatagramPacket confirmPacket = new DatagramPacket(confirm, confirm.length, packetIPAddress, packetPort);
			serverSocket.send(confirmPacket);
			current.remove(packet.getSocketAddress());
			states.remove(packet.getSocketAddress());
		}
		hashes.remove(packet.getSocketAddress());
	}
	/*
	 * The state that receives the data, stores it in the values hashtable , and
	 * sends the calculated values back to the sensor.
	 */
	public static void receiveData(DatagramPacket packet) throws IOException {
		byte[] valueData = new byte[8];
		String inputUser = current.get(packet.getSocketAddress());
		values.get(inputUser).add(toDouble(packet.getData()));
		double min = 0;
		double max = 0;
		double avg = 0;
		double avgAll = 0;
		int count = 0;
		for(String u : values.keySet()) {
			for(double num : values.get(u)) {
				if(min == 0 || min > num) {
					min = num;
				}
				if(max == 0 || max < num) {
				  max = num;
				}
				avgAll += num;
				if(u.equals(inputUser)) {
					avg += num;
				}
				count++;
			}
		}
		avgAll /= count;
		avg /= values.get(inputUser).size();
		System.out.println("Sensor: " + inputUser);
		System.out.println("Time: " + LocalDateTime.ofInstant(Instant.now(), zone));
		System.out.println("Value: " + toDouble(packet.getData()));
		System.out.println("SensorMin: " + min);
		System.out.println("SensorAvg: " + avg);
		System.out.println("SensorMax: " + max);
		System.out.println("allAvg: " + avgAll);
		InetAddress packetIPAddress = packet.getAddress();
		int packetPort = packet.getPort();
		double[] output = {min, max, avg, avgAll};
		DatagramPacket outputPacket = new DatagramPacket(toByteArray(output), toByteArray(output).length, packetIPAddress, packetPort);
		serverSocket.send(outputPacket);
		current.remove(packet.getSocketAddress());
		states.remove(packet.getSocketAddress());
	}
	
	public static void main(String args[]) throws Exception
	  {
			// READING OF CSV FILES AND INITIALIZATION
			port = 0;
			csvFile = null;
			users = new Hashtable<>();
			
			
			for(int x = 0; x < args.length; x++) {
				if(x < args.length-1) {
					if(args[x].equals("-p")) {
						port = Integer.parseInt(args[x+1]);
					}
					if(args[x].equals("-f")) {
						csvFile = args[x+1]; 
					}
				}
			}
			if(port == 0 || csvFile == null) {
				System.out.println("Invalid input");
				return;
			}
			BufferedReader br = new BufferedReader(new FileReader(csvFile));
			String line = br.readLine();
			while (line != null) {
				String[] info = line.split(",");
				users.put(info[0], info[1]);
				line = br.readLine();
			}
			serverSocket = new DatagramSocket(port);
			receiveData = new byte[1024];
			values = new Hashtable<>();
			states = new Hashtable<>();
			current = new Hashtable<>();
			hashes = new Hashtable<>();
			
			for(String u : users.keySet()) {
				values.put(u, new ArrayList<Double>());
			}
			
			// END OF INITIALIZATION
			
			while(true)
			   {
				   //Receives the packets and depending on the state, sends it to the currect function.
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					serverSocket.receive(receivePacket);
					
					if(!states.containsKey(receivePacket.getSocketAddress())){
						try {
							authentication(receivePacket);
						} catch(IOException e) {
							System.err.println("IOException: " + e.getMessage());
						}
					} else if(states.get(receivePacket.getSocketAddress()) == 1) {
						try {
							getUser(receivePacket);
						} catch(IOException e) {
							System.err.println("IOException: " + e.getMessage());
						}
					} else if(states.get(receivePacket.getSocketAddress()) == 2) {
						try {
							login(receivePacket);
						} catch(IOException e) {
							System.err.println("IOException: " + e.getMessage());
						}
					} else if(states.get(receivePacket.getSocketAddress()) == 3) {
						try {
							receiveData(receivePacket);
						} catch(IOException e) {
							System.err.println("IOException: " + e.getMessage());
						}
					}
			   }
	  }
}
