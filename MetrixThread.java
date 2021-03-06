// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.net.*;
import java.io.*;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.logging.*;
import nki.objects.Command;
import nki.objects.Summary;
import nki.objects.SummaryCollection;
import nki.constants.Constants;
import nki.exceptions.CommandValidityException;
import nki.exceptions.InvalidCredentialsException;
import nki.exceptions.UnimplementedCommandException;
import nki.io.DataStore;
import nki.parsers.metrix.CommandProcessor;
import nki.constants.Constants;

public class MetrixThread extends Thread {
	private SocketChannel sChannel = null;
	private boolean timedBool = false;	

	// SERVER LOGGING HERE FOR INSTANTIATION OF CLIENT...
	final Logger metrixLogger = Logger.getLogger(MetrixThread.class.getName());

	public MetrixThread(SocketChannel sChannel){
		super("MetrixThread");
		this.sChannel = sChannel;
	}

	public void run(){
		
		try{
			String clientSocketDetails = sChannel.socket().getRemoteSocketAddress().toString();
			metrixLogger.log(Level.INFO, "[SERVER] Client connection accepted at: " + clientSocketDetails);

			// Create OutputStream for sending objects.
			ObjectOutputStream  oos = new ObjectOutputStream(sChannel.socket().getOutputStream());
			
			// Cteate Inputstream for receiving objects.
			ObjectInputStream ois = new ObjectInputStream(sChannel.socket().getInputStream());
			
			// DataStore instantiation
			DataStore ds = new DataStore();

			try{
				Command commandClient;

				while (( commandClient = (Command) ois.readObject()) != null){
					String mode = "";
					if(commandClient instanceof Command){
						mode = commandClient.getMode();
						
						CommandProcessor cp;
						
						try{	
							// Mode Check
							if(mode.equals(Constants.COM_MODE_TIMED)){	// Keep alive repetitive command
								timedBool = true;
								while(timedBool){
									cp = new CommandProcessor(commandClient, oos, ds);
									Thread.sleep(commandClient.getTimedInterval());	
								}
							}
	
							if(mode.equals(Constants.COM_MODE_CALL)){	// Single call
								cp = new CommandProcessor(commandClient, oos, ds);			
							}
		
						// Server Exceptions and important logging.
						}catch(CommandValidityException CVE){
							System.out.println("Command Validity Exception! " + CVE);
						}catch(InvalidCredentialsException ICE){
							System.out.println("Invalid Credentials Exception! " + ICE);
						}finally{
							// Close all channels and client streams.
							ds = null;
							sChannel.socket().close();
							sChannel.close();
							ois.close();
							oos.close();
						}
					}else{
						metrixLogger.log(Level.WARNING, "[SERVER] Command not understood [" + commandClient + "]");
					}
					
					metrixLogger.log(Level.INFO, "[SERVER] Finished processing command");
				}
			}catch(ClassNotFoundException CNFE){
				CNFE.printStackTrace();
			}catch(Exception Ex){
			//	metrixLogger.log(Level.INFO, "Disconnect from client. ");
			}

		}catch(IOException Ex){
			System.err.println("[Log] Client disconnected or IOException " + Ex.toString());
		}

	}
}	

