package socket;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import socket.SocketInMessage.SocketMessageType;

public class SocketController implements ISocketController {
	Set<ISocketObserver> observers = new HashSet<ISocketObserver>();
	//TODO Maybe add some way to keep track of multiple connections?
	private BufferedReader inStream;
	private DataOutputStream outStream;


	@Override
	public void registerObserver(ISocketObserver observer) {
		observers.add(observer);
	}

	@Override
	public void unRegisterObserver(ISocketObserver observer) {
		observers.remove(observer);
	}

	@Override
	public void sendMessage(SocketOutMessage message) {
		try {
			//TODO send something over the socket!
			outStream.writeUTF(message.getMessage());
		} catch (IOException e) {
			//TODO maybe tell someone that connection is closed?
			e.printStackTrace();
			notifyObservers(new SocketInMessage(SocketMessageType.Error, "No socket is connected!"));
		}
	}

	@Override
	public void run() {
		//TODO some logic for listening to a socket //(Using try with resources for auto-close of socket)
		try (ServerSocket listeningSocket = new ServerSocket(Port)){ 
			while (true){
				waitForConnections(listeningSocket); 	
			}		
		} catch (IOException e1) {
			notifyObservers(new SocketInMessage(SocketMessageType.Error, "Could not establish connection to the cloud!"));
			e1.printStackTrace();
		} 


	}

	private void waitForConnections(ServerSocket listeningSocket) {
		try {
			Socket activeSocket = listeningSocket.accept(); //Blocking call
			inStream = new BufferedReader(new InputStreamReader(activeSocket.getInputStream()));
			outStream = new DataOutputStream(activeSocket.getOutputStream());
			String inLine;
			//.readLine is a blocking call 
			//TODO How do you handle simultaneous input and output on socket?
			//TODO this only allows for one open connection - how would you handle multiple connections?
			while (true){
				inLine = inStream.readLine();
				System.out.println(inLine);
				if (inLine==null) break;
				switch (inLine.split(" ")[0]) {
				case "RM20": // Display a message in the secondary display and wait for response
					notifyObservers(new SocketInMessage(SocketMessageType.RM208, inLine.substring(7)));
					sendMessage(new SocketOutMessage("RM20 B"));
					//TODO implement logic for RM command
					break;
				case "D": // Display a message in the primary display
					try {
						notifyObservers(new SocketInMessage(SocketMessageType.D, inLine.substring(2)));
						sendMessage(new SocketOutMessage("D A"));
					} catch (IndexOutOfBoundsException e) {
						e.printStackTrace();
						sendMessage(new SocketOutMessage("ES"));
					}
					break;
				case "DW": //Clear primary display
					notifyObservers(new SocketInMessage(SocketMessageType.DW, null));
					sendMessage(new SocketOutMessage("DW A"));
					break;
				case "P111": //Show something in secondary display
					try {
						notifyObservers(new SocketInMessage(SocketMessageType.P111, inLine.substring(5)));
						sendMessage(new SocketOutMessage("P111 A"));
					} catch (IndexOutOfBoundsException e) {
						e.printStackTrace();
						sendMessage(new SocketOutMessage("ES"));
					}
					break;
				case "T": // Tare the weight
					notifyObservers(new SocketInMessage(SocketMessageType.T, null));
					break;
				case "S": // Request the current load
					notifyObservers(new SocketInMessage(SocketMessageType.S, null));
					break;
				case "K":
					if (inLine.split(" ").length>1){
						notifyObservers(new SocketInMessage(SocketMessageType.K, inLine.split(" ")[1]));
					}
					break;
				case "B": // Set the load
					try {
						notifyObservers(new SocketInMessage(SocketMessageType.B, inLine.substring(2)));
						sendMessage(new SocketOutMessage("DB"));
					} catch (IndexOutOfBoundsException e) {
						e.printStackTrace();
						sendMessage(new SocketOutMessage("ES"));
					}
					break;
				case "Q": // Quit
					notifyObservers(new SocketInMessage(SocketMessageType.Q, null));
					break;
				default: //Something went wrong?
					sendMessage(new SocketOutMessage("ES"));
					break;
				}
			}
		} catch (IOException e) {
			notifyObservers(new SocketInMessage(SocketMessageType.Error, "Problems with internet connection"));
			e.printStackTrace();
		}
	}

	private void notifyObservers(SocketInMessage message) {
		for (ISocketObserver socketObserver : observers) {
			socketObserver.notify(message);
		}
	}

}

