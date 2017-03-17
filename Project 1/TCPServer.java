
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

class TCPServer
{
	static int port;
	static String csvFile;
	static Hashtable<String, String> users; // Stores the list of users and passwords
	static ServerSocket socket;
	static byte[] receiveData;
	static Hashtable<String, ArrayList<Double>> values; //Stores the values pertaining to each user
	static ZoneId zone = ZoneId.of("-05:00");
	
	// -------------- HELPER FUNCTIONS ------------------------
	
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
	
	/*
	 * Creates a thread for the socket and handles it
	 * 
	 */
	static class ServerThread implements Runnable {
		Socket client = null;
		String random;
		String user;
		DataOutputStream outToClient;
		DataInputStream inFromClient;
		public ServerThread(Socket c) {
			this.client = c;
			random = getSaltString();
			user = null;
			try {
				outToClient = new DataOutputStream(client.getOutputStream());
				inFromClient = new DataInputStream(client.getInputStream());
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
			
		}
		
		private boolean authUser() {
		
			try{
				MessageDigest digest = MessageDigest.getInstance("MD5");
				String login = user + users.get(user) + random;
				byte[] hash = digest.digest(login.getBytes());
				byte[] h = new byte[hash.length];
				inFromClient.read(h);
				
				return digest.isEqual(h, hash);
			} catch(GeneralSecurityException e) {
				System.err.println("GeneralSecurityException: " + e.getMessage());
				return false;
			} catch(IOException e) {
				System.err.println("IOException: " + e.getMessage());
				return false;
			}
		}
		//---------------------END OF HELPER FUNCTIONS ---------------------
		
		/*
		 * The authentication state where a salt is sent to the user and
		 * the salt is stored in the hash hashtable.
		 */
		private void authentication() throws IOException {
			outToClient.writeUTF(random);
		}
		
		/*
		 * The state that accepts the name of the user and stores
		 * it in the user hashtable
		 */
		private boolean getUser() throws IOException {
			user = inFromClient.readUTF();
			if(users.containsKey(user)) {
				outToClient.writeBoolean(true);
				return true;
			} else {
				System.out.println("User Authorization Failed");
				outToClient.writeBoolean(false);
				return false;
			}
		}
		
		/*
		 * The state that authenticates the user and sends the
		 * confirmation to the sensor
		 */
		private boolean login() throws IOException {
			
			if(authUser()) {
				System.out.println("User Confirmed");
				outToClient.writeBoolean(true);
				return true;
			} else {
				System.out.println("User authorization failed for client: " + client.getInetAddress().getHostAddress() + " port: " + client.getPort() + " user: " + user);
				outToClient.writeBoolean(false);
				return false;
			}
		}
		
		/*
		 * The state that receives the data, stores it in the values hashtable , and
		 * sends the calculated values back to the sensor.
		 */
		public void receiveData() throws IOException {
			double value = inFromClient.readDouble();
			values.get(user).add(value);
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
					if(u.equals(user)) {
						avg += num;
					}
					count++;
				}
			}
			avgAll /= count;
			avg /= values.get(user).size();
			System.out.println("Sensor: " + user);
			System.out.println("Value: " + value);
			System.out.println("Time: " + LocalDateTime.ofInstant(Instant.now(), zone));
			System.out.println("SensorMin: " + min);
			System.out.println("SensorAvg: " + avg);
			System.out.println("SensorMax: " + max);
			System.out.println("allAvg: " + avgAll);
			outToClient.writeDouble(min);
			outToClient.writeDouble(max);
			outToClient.writeDouble(avg);
			outToClient.writeDouble(avgAll);
		}
		
		public void run() {
			try {
				authentication();
				if(getUser()) {
					if(login()) {
						receiveData();
					}
				}
				client.close();
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
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
			socket = new ServerSocket(port);
			receiveData = new byte[1024];
			values = new Hashtable<>();
			
			for(String u : users.keySet()) {
				values.put(u, new ArrayList<Double>());
			}
			
			// END OF INITIALIZATION
			
			while(true)
			   {
					Socket connectionSocket = socket.accept();
					new Thread(new ServerThread(connectionSocket)).start(); // Creates Threads
			   }
	  }
}
