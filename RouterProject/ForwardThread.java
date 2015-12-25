//*******************ForwardThread class************************//

// Java core packages
import java.net.*;
import java.util.*;
import java.awt.*;
import java.io.*;

// Java extention packages
import javax.swing.*;
import java.awt.event.*;

class ForwardThread extends Thread
{
	private DatagramPacket receivePacket;
	private DatagramPacket sendPacket;
	private DatagramSocket socket;
	private String tableOld[][]=new String[6][2];                 
	private boolean oldTableIdle;
	private String message;
	
	private int routerNum;
	private Byte destination;
	private byte fromRouter;
	private byte packetType;
	private byte dataReceive[]=new byte[200];
	private byte dataForward[]=new byte[200];
	
	
	public ForwardThread(String name,String tableOld[][],boolean oldTableIdle,int routerNum)
	{
		super(name);
		
		this.tableOld=tableOld;
		this.oldTableIdle=oldTableIdle;
		this.routerNum=routerNum;					      //注意：端口号共更改了三次
		
		//DEBUG...
	
		try{
			socket=new DatagramSocket(3000+routerNum);       // create the socket of port "3000+routerNum"
		   }
		catch(SocketException socketException)
		{
			socketException.printStackTrace();
			System.exit(1);
		}
	
		receivePacket=new DatagramPacket(dataReceive,dataReceive.length);
	}
	
	public void run()
	{
		while(true)
		{
			try{
				receivePacket=new DatagramPacket(dataReceive,dataReceive.length);
		    	socket.receive(receivePacket);
		    	
		       }
		    catch(IOException ioException){
				
				ioException.printStackTrace();
			}
			
			packetType = dataReceive[0];
			
			if('D'==packetType)
				ForWardOrReceive();
		}
	}
	
	public synchronized void ForWardOrReceive()
	{
		int portNum;
		
		while(!oldTableIdle)
		{
			try{
				wait();
			}
			catch(InterruptedException exception)
			{
				
			}
		}
		
		oldTableIdle=false;
		
		message=new String(receivePacket.getData(),0,receivePacket.getLength());
		
		//DEBUG
		//System.err.println("receivePacketLenght :"+receivePacket.getLength());
	
		destination=dataReceive[2];
	    fromRouter=dataReceive[1];
	    
		dataForward=message.getBytes();
	
		System.err.println("\n\nThis is router "+routerNum);

		if(routerNum==destination-'0')
			System.err.println("Receive Data from Router "+(fromRouter-'0')+"  :"
											+new String(receivePacket.getData(),3,(receivePacket.getLength()-3)));
		else {
			if(0!=tableOld[destination-'0'-1][1].charAt(0)-'0')
			{
				try{
					portNum = tableOld[destination-'0'-1][1].charAt(0)-'0'+3000;	 //发送与接收数据传输的端口号统一设为3000起	
					sendPacket = new DatagramPacket(dataForward,dataForward.length,InetAddress.getLocalHost(),portNum);
					
					socket.send(sendPacket);
				}
				catch(IOException ioException)
				{
					ioException.printStackTrace();
				}
				
				System.err.println("Forward Data from Router "+(fromRouter-'0')+" to Router "+(destination-'0')+"  :"
				            			      +new String(receivePacket.getData(),3,(receivePacket.getLength()-3)));
			}
			else
				System.err.println("Data cannot be forwarded..........");
					
		}
						          
		oldTableIdle=true;	
		
		notify();	
	}
}