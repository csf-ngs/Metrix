// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

import java.io.IOException;
import java.io.EOFException;
import java.lang.Exception;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.*;
import nki.objects.Command;
import nki.objects.Summary;
import nki.objects.SummaryCollection;
import nki.exceptions.EmptyResultSetCollection;
import nki.exceptions.MissingCommandDetailException;
import nki.exceptions.UnimplementedCommandException;
import nki.exceptions.InvalidCredentialsException;
import java.util.logging.*;
import java.util.*;
import java.io.*;
import java.util.HashMap;
import java.util.Properties;
import nki.objects.MutableInt;

public class MetrixClient {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        final Logger metrixLogger = Logger.getLogger(MetrixClient.class.getName());
	metrixLogger.log(Level.INFO, "[CLIENT] Initiated");

        Properties configFile = new Properties();

        // Use external properties file, outside of jar location.
        String externalFileName = System.getProperty("properties");
        String absFile = (new File(externalFileName)).getAbsolutePath();

        InputStream fin = new FileInputStream(new File(absFile));
        configFile.load(fin);

        int port = Integer.parseInt(configFile.getProperty("PORT", "10000"));
	String host = configFile.getProperty("HOST", "localhost");	



    	try{
	        SocketChannel sChannel = SocketChannel.open();
	        sChannel.configureBlocking(true);
	        
	        if(sChannel.connect(new InetSocketAddress(host, port))){

                // Create OutputStream for sending objects.
                ObjectOutputStream oos = new ObjectOutputStream(sChannel.socket().getOutputStream());

                // Cteate Inputstream for receiving objects.
		        ObjectInputStream ois = new ObjectInputStream(sChannel.socket().getInputStream());

				try{
					nki.objects.Command sendCommand = new nki.objects.Command();
					
					// Set a value for command
					sendCommand.setFormat("XML");
					sendCommand.setState(1); // Select run state (1 - running, 2 - finished, 3 - errors / halted, 4 - FC needs turn, 5 - init) || 12 - ALL
					sendCommand.setCommand("FETCH");
					sendCommand.setMode("CALL");
					sendCommand.setType("METRIC");
//					sendCommand.setRunId(""); // Use run directory path as string or if a State is desired, use setState and comment out setRunId() method.
					oos.writeObject(sendCommand);
					oos.flush();
					
					boolean listen = true;
	
					Object serverAnswer = new Object();
					serverAnswer = ois.readObject();
	
					while(listen){
						if(serverAnswer instanceof Command){	// Answer is a Command with info message.
							nki.objects.Command commandIn = (nki.objects.Command) serverAnswer;
							if(commandIn.getCommand() != null){
								System.out.println("[SERVER] " + commandIn.getCommand());
							}
						}
	
						if(serverAnswer instanceof SummaryCollection){
							SummaryCollection sc = (SummaryCollection) serverAnswer;
							ListIterator litr = sc.getSummaryIterator();

							while(litr.hasNext()){
								Summary sum = (Summary) litr.next();

								// The following is an example. You can use any 'get'-method described in the Summary object (nki/objects/Summary,java) to access the parsed information.
                	            System.out.println(sum.getRunId() + " - Current Cycle: " + sum.getCurrentCycle());
								listen = false;

							}
						}
	
						if(serverAnswer instanceof String){ 			// Server returned a XML String with results.
							String srvResp = (String) serverAnswer;
							System.out.println("response = " + srvResp );
							listen = false;
						}

						if(serverAnswer instanceof EmptyResultSetCollection){
							System.out.println(serverAnswer.toString());
							listen = false;
						}

						if(serverAnswer instanceof InvalidCredentialsException){
							System.out.println(serverAnswer.toString());
							listen = false;
						}

						if(serverAnswer instanceof MissingCommandDetailException){
							System.out.println(serverAnswer.toString());
							listen = false;
						}

						if(serverAnswer instanceof UnimplementedCommandException){
							System.out.println(serverAnswer.toString());
							listen = false;
						}
					}
				}catch(IOException Ex){
					System.out.println("Error" + Ex);
				}
	        }
	}catch(EOFException ex){
//		log.error("Server has shutdown.");
	}catch(NoConnectionPendingException NCPE){
//		log.error("Communication channel is not connection and no operation has been initiated.");
	}catch(AsynchronousCloseException ACE){
//		log.error("Another client has shutdown the server. Channel communication prohibited by issueing a direct command.");
	}

	
    }
}
