import java.io.*;
import java.net.*;
import java.lang.StringBuffer;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.net.ConnectException;

class TCPSensor
{

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
	
	public static void main(String args[]) throws Exception
	{
		String user = "";
		String pass = "";
		String IPAddress = null;
		int port = 0;
		double value = 0;
		byte[] receiveData = new byte[1024];
		
		for(int x = 0; x < args.length; x++) {
			if(x < args.length-1) {
				if(args[x].equals("-s") ) {
					IPAddress = args[x+1];
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
			Socket clientSocket = null;
			try{
				clientSocket = new Socket(IPAddress, port);
			} catch(ConnectException e) {
				System.err.println("ConnectException: " + e.getMessage());
				return;
			}
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
			String challenge = inFromServer.readUTF(); // Receives the challenge String
			MessageDigest digest = MessageDigest.getInstance("MD5");
			String login = user + pass + new String(challenge);
			byte[] hash = digest.digest(login.getBytes()); 
			outToServer.writeUTF(user); // sends the username
			if(inFromServer.readBoolean()) { // Waits for the confirmation that the username is in the user table
				outToServer.write(hash, 0, hash.length); // Sends the hashed information
				if(inFromServer.readBoolean()) { // Waits for confirmation that the hashed strings match
					System.out.println("User Confirmed");
					outToServer.writeDouble(value); // Sends the value to server
					double min = inFromServer.readDouble();
					double max = inFromServer.readDouble();
					double avg = inFromServer.readDouble();
					double avgAll = inFromServer.readDouble();
					System.out.println("Min:" + min);
					System.out.println("Max:" + max);
					System.out.println("Average:" + avg);
					System.out.println("Average of All:" + avgAll);
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