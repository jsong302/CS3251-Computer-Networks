import java.io.*;
import java.net.*;
import java.lang.StringBuffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.lang.NumberFormatException;

class UDPSensor
{

	// -------------- HELPER FUNCTIONS ------------------------
	public static byte[] toByteArray(double value) {
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putDouble(value);
		return bytes;
	}

	public static double toDouble(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getDouble();
	}
	
	public static double[] toDoubleArray(byte[] byteArray){
		int times = Double.SIZE / Byte.SIZE;
		double[] doubles = new double[byteArray.length / times];
		for(int i=0;i<doubles.length;i++){
			doubles[i] = ByteBuffer.wrap(byteArray, i*times, times).getDouble();
		}
		return doubles;
	}
	
	//---------------------END OF HELPER FUNCTIONS ---------------------
	
	public static void main(String args[]) throws Exception
	{
		String user = "";
		String pass = "";
		InetAddress IPAddress = null;
		int port = 0;
		double value = 0;
		byte[] receiveData = new byte[1024];
		
		for(int x = 0; x < args.length; x++) {
			if(x < args.length-1) {
				if(args[x].equals("-s") ) {
					IPAddress = InetAddress.getByName(args[x+1]);
				}
				if(args[x].equals("-p")) {
					port = Integer.parseInt(args[x+1]);
				}
				if(args[x].equals("-u")) {
					user = args[x+1];
				}
				if(args[x].equals("-c")) {
					pass = args[x+1];
				}
				if(args[x].equals("-r")) {
					value = Double.parseDouble(args[x+1]);
				}
			}
			
		}
		
		if(IPAddress == null | port == 0) {
			System.out.println("Invalid Inputs");
		} else {
			byte[] confirmData = new byte[1];
			DatagramSocket clientSocket = new DatagramSocket();
			DatagramPacket authPacket = new DatagramPacket(receiveData, receiveData.length, IPAddress, port);
			clientSocket.send(authPacket); //Sends the confirmation to send the random characters to the sensor
			byte[] challenge = new byte[64];
			DatagramPacket challengePacket = new DatagramPacket(challenge, challenge.length);
			clientSocket.receive(challengePacket); //Receives the random characters
			MessageDigest digest = MessageDigest.getInstance("MD5");
			String login = user + pass + new String(challengePacket.getData(), 0, challengePacket.getLength());
			byte[] hash = digest.digest(login.getBytes()); //Creates the MD5 hash
			DatagramPacket loginPacket = new DatagramPacket(user.getBytes(), user.getBytes().length, IPAddress, port);
			clientSocket.send(loginPacket); // Sends the name of the user
			DatagramPacket confirmPacket1 = new DatagramPacket(confirmData, confirmData.length);
			clientSocket.receive(confirmPacket1);
			Byte confirmation = (confirmPacket1.getData())[0]; //Receives the confirmation that the user is in the usertable
			if(confirmation == 1) {
				DatagramPacket hashPacket = new DatagramPacket(hash, hash.length, IPAddress, port);
				clientSocket.send(hashPacket); // Sends the hash of the user, password, and the random String
				
				DatagramPacket confirmPacket2 = new DatagramPacket(confirmData, confirmData.length);
				clientSocket.receive(confirmPacket2);
				confirmation = (confirmPacket2.getData())[0]; // Receives the confirmation that the hashes match
				if(confirmation == 1) {
					System.out.println("User Confirmed");
					byte[] valueData = toByteArray(value);
					DatagramPacket valuePacket = new DatagramPacket(valueData, valueData.length, IPAddress, port);
					clientSocket.send(valuePacket); // Sends the value to the server
					DatagramPacket outputPacket = new DatagramPacket(receiveData, receiveData.length);
					clientSocket.receive(outputPacket); // Receives the calculations from the server
					double[] output = toDoubleArray(outputPacket.getData());
					System.out.println("Min:" + output[0]);
					System.out.println("Max:" + output[1]);
					System.out.println("Average:" + output[2]);
					System.out.println("Average of All:" + output[3]);
				} else {
					System.out.println("Invalid Username or Password");
				}
			} else {
				System.out.println("Invalid Username or Password");
			}
			
			clientSocket.close();
		}
	}
}