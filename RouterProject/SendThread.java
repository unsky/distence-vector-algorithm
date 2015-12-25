//****************SendThread class**********************//

// Java core packages
import java.net.*;
import java.util.*;
import java.awt.*;
import java.io.*;

// Java extention packages
import javax.swing.*;
import java.awt.event.*;

class SendThread extends Thread
{
	private DatagramSocket socket;
	DatagramPacket sendPacket;
    private byte data[]=new byte[200];
    private boolean dataIdle=true;
    
    private String tableOld[][]=new String[6][2];
    private boolean oldTableIdle;
    
    private boolean neighborRouter[]=new boolean[6];
	private boolean neighborIdle;
    
    public String sendMessage;                    
    byte routerNum;
    
    		
	public SendThread(String name,String tableOld[][],boolean oldTableIdle,
					  Integer routerNum,DatagramSocket socket,boolean neighborRouter[],boolean neighborIdle)
	{  
		super(name);
	
		this.tableOld=tableOld;
		this.oldTableIdle=oldTableIdle;
		this.neighborRouter=neighborRouter;
		this.neighborIdle=neighborIdle;
		this.routerNum=routerNum.byteValue();
		this.socket=socket;
				  	
		sendMessage="T";             
	}
	
	
    public synchronized void sendPacketFunction(String dataMessage) throws IOException
    {
    	int destination,portNum;
    	
    	while(!oldTableIdle&&!dataIdle){
			try{
				wait();
			}
			catch(InterruptedException exception)
			{
				
			}
		}
		
		oldTableIdle = false;
		dataIdle = false;
		
		data = dataMessage.getBytes();
		destination = dataMessage.charAt(2)-'0';
	
		
		System.err.println("\n\n\ndestination :"+destination);
		
		try{
				if(0!=tableOld[destination-1][1].charAt(0)-'0')
				{
					portNum = Integer.valueOf(tableOld[destination-1][1].charAt(0)-'0')+3000;
		
			    	sendPacket=new DatagramPacket(data,data.length,InetAddress.getLocalHost(),portNum);
			    	
			    	//debug
			    	//System.err.println("data length: "+data.length);
			    	//System.err.println("send Data "+dataMessage);
				}
				else
					System.err.println("Data cannot be sent out, Destination is unreachable..........");
				
		    }
		catch(IOException ioException)
		{
				ioException.printStackTrace();
		}
			
		socket.send(sendPacket);
		
		oldTableIdle = true;
		dataIdle = true;
		
		notify();
    }
    
    
    public synchronized void sendPacketFunction() throws IOException
	{
		int i,portNum;
		
		while(!neighborIdle&&!dataIdle){
			try{
				wait();
			}
			catch(InterruptedException exception)
			{
				
			}
		}
		
		neighborIdle=false;
		dataIdle = false;
					
		data=sendMessage.getBytes();
		for(i=0;i<6;i++)
		{
			if(!neighborRouter[i])
				continue;
				
			try{
				portNum=2000+i+1;
			    sendPacket=new DatagramPacket(data,data.length,InetAddress.getLocalHost(),portNum);
		    }
			catch(IOException ioException)
			{
					ioException.printStackTrace();
			}
				
			socket.send(sendPacket);
			//debug
			//System.err.println("Here is router "+routerNum+"'s send Thread...");
		}
		
		neighborIdle=true;
		dataIdle=true;
		
		notify();
	}	


	public synchronized void tableToString()
	{
		int i,j;
		
		while(!oldTableIdle){
			try{
				wait();
			}
			catch(InterruptedException exception)
			{
				
			}
		}
					
		oldTableIdle=false;
		
		sendMessage = sendMessage+routerNum;
		
		for(i=0;i<6;i++)
		{
			for(j=0;j<2;j++)
			{
				sendMessage=sendMessage+tableOld[i][j];
				if(j==0)
					sendMessage=sendMessage+":";
			}
		        
		    sendMessage=sendMessage+"\n";
		}
		
		oldTableIdle=true;
		notify();
	}
	
	public void run()
	{
	 	while(true)
		{  	
			try{
				Thread.sleep(2000);              //每隔2秒钟发出路由表。。。
			}
			catch(InterruptedException exception)
			{
				
			}
			
			tableToString();
								
			try{
				sendPacketFunction();            //一次性向所有相邻的路由器发送自己路由表
				sendMessage="T"; 
			   }
			catch(IOException ioException)
			{
				ioException.printStackTrace();
			}
		}			
	}
	
}