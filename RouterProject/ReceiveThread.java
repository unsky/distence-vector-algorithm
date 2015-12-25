//*******************ReceiveThread class*******************//

// Java core packages
import java.net.*;
import java.util.*;
import java.awt.*;
import java.io.*;

// Java extention packages
import javax.swing.*;
import java.awt.event.*;

class ReceiveThread extends Thread
{
	private DatagramSocket socket;
	public DatagramPacket receivePacket;
    
    private byte data[]=new byte[200];
    
    private String tableReceive[][]=new String[6][2];
    private String tableUpdate[][]=new String[6][2];
    private boolean updateTableIdle=true;
    
 
    private byte fromRouter;
    private byte routerNum;
    private byte packetType;
    	

    TimeThread timer;              //���ö�ʱ��
    
    	
	public ReceiveThread(String name,String tableUpdate[][],boolean updateTableIdle,
	                     Integer routerNum,DatagramSocket socket,boolean neighborRouter[],boolean neighborIdle)
	{   
		super(name);
		
		int j;
			
		this.tableUpdate=tableUpdate;
		this.updateTableIdle=updateTableIdle;
		this.routerNum=routerNum.byteValue();					//ע�� byteValue�����ǽ�Integer��ת��Ϊbyte
		this.socket=socket;
		
		receivePacket=new DatagramPacket(data,data.length);
		
		for(j=0;j<6;j++)
		{
			tableReceive[j][0]=Integer.toString(j+1);
		}
		
		timer=new TimeThread(routerNum,neighborRouter,neighborIdle);
		timer.start();
				
	}
	
	
	public void run()
	{
		 int num;
		 
		 while(true)
		 {
		    try{
		    	socket.receive(receivePacket);
		    			    
			    //��Ӽ�ʱ���ĳ�ʼ������
					
				num = data[1]-'0'-1;
				timer.updateTime(num);
		    	
		       }
		    catch(IOException ioException){
				
				ioException.printStackTrace();
			}
			
			packetType = data[0];             //�������շ����ʽ�����ֽڣ����б��Ƿ�Ϊ·�ɱ���Ϣ
			
			if('T'==packetType)
				modifyUpdateTable();
		}
	}
	
	
	
	public synchronized void modifyUpdateTable()
	{
		
		int i,j = -1;
		
		while(!updateTableIdle)
		{
			try{
				wait();
			}
			catch(InterruptedException exception)
			{
				
			}
		}
		
		updateTableIdle=false;
		
	    //����Ϊ�ֽڱ任Ϊ�ַ�������Ĳ���
	  
		fromRouter = data[1];
		//DEBUG
		//System.err.println("routerNum:"+routerNum+"  fromRouter:"+(fromRouter-'0'));
		
	  	for(i=2;i<receivePacket.getLength();i++)
		{
			if(':'==data[i+1]) 
			{	
				j++;
			}
			
			if('\n'==data[i]||':'==data[i])  
		    	continue;
     		else {
				 	   if(':'==data[i-1])
				 	   {
				 	   	   if('0'==data[i])
				 	   	   	    tableReceive[j][1]="0"; 
				 	   	   else {
				 	   	   	     tableReceive[j][1]=Integer.toString(fromRouter-'0');          //��ÿ���յ���·����ǰ���Ϸ���·�����ı��
				 	   	   	     if(fromRouter!=data[i])                                         //�����Լ���·���������ײ�
				 	   	   	    	 tableReceive[j][1]=tableReceive[j][1]+Integer.toString(data[i]-'0');
				 	   	   	     
				 	   	   }
					 	      
				 	   }	
				 	   else
				 	   		tableReceive[j][1]=tableReceive[j][1]+Integer.toString(data[i]-'0');
				}
		}    
		//ת�����ֽ���
		
		//DEBUG
		//System.err.println("This is router :"+routerNum);
		//System.err.println("the Receive Table is :  \n"+new String(receivePacket.getData(),2,(receivePacket.getLength()-2)));
		
		//·�ɱ����ʵ��
		for(i=0;i<6;i++)
		{
			if(routerNum-1==i)
				continue;
						
			if("0"==tableReceive[i][1])
			{
				if(fromRouter==tableUpdate[i][1].charAt(0))     //ͬһ����һ����������Ϣ����Ҫ����
					tableUpdate[i][1]="0";
			}
			else{
				
					if("0"==tableUpdate[i][1])
					{			
						if(tableReceive[i][1].length()>1&&routerNum==tableReceive[i][1].charAt(1)-'0')         //֮ǰ��·�ɷ��ͳ�ȥ����Ϣ�ֱ�����������
						{
							continue;
						}
						else								                                       
							tableUpdate[i][1]=tableReceive[i][1];
					}
					else{
						if(tableReceive[i][1].length()>1&&routerNum==tableReceive[i][1].charAt(1)-'0') 
						{
							continue;
						}      	
						else{
					    	if(fromRouter==tableUpdate[i][1].charAt(0)&&!tableUpdate[i][1].equals(tableReceive[i][1]))
					    		tableUpdate[i][1]=tableReceive[i][1];
					    	else{
					    		    if(tableUpdate[i][1].length()+1>=tableReceive[i][1].length()+1)
					    				tableUpdate[i][1]=tableReceive[i][1];
					    	    }
					    	}	 
				    	}
				}	
				
		}
		//·�ɸ��½���
		
		updateTableIdle=true;
	
		notify();
	}
}


