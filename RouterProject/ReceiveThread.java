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
    	

    TimeThread timer;              //设置定时器
    
    	
	public ReceiveThread(String name,String tableUpdate[][],boolean updateTableIdle,
	                     Integer routerNum,DatagramSocket socket,boolean neighborRouter[],boolean neighborIdle)
	{   
		super(name);
		
		int j;
			
		this.tableUpdate=tableUpdate;
		this.updateTableIdle=updateTableIdle;
		this.routerNum=routerNum.byteValue();					//注： byteValue函数是将Integer型转化为byte
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
		    			    
			    //添加计时器的初始化……
					
				num = data[1]-'0'-1;
				timer.updateTime(num);
		    	
		       }
		    catch(IOException ioException){
				
				ioException.printStackTrace();
			}
			
			packetType = data[0];             //接收所收分组格式的首字节，来判别是否为路由表信息
			
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
		
	    //以下为字节变换为字符串数组的操作
	  
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
				 	   	   	     tableReceive[j][1]=Integer.toString(fromRouter-'0');          //在每个收到的路由项前加上发送路由器的编号
				 	   	   	     if(fromRouter!=data[i])                                         //对于自己的路由项不做添加首部
				 	   	   	    	 tableReceive[j][1]=tableReceive[j][1]+Integer.toString(data[i]-'0');
				 	   	   	     
				 	   	   }
					 	      
				 	   }	
				 	   else
				 	   		tableReceive[j][1]=tableReceive[j][1]+Integer.toString(data[i]-'0');
				}
		}    
		//转换部分结束
		
		//DEBUG
		//System.err.println("This is router :"+routerNum);
		//System.err.println("the Receive Table is :  \n"+new String(receivePacket.getData(),2,(receivePacket.getLength()-2)));
		
		//路由表更新实现
		for(i=0;i<6;i++)
		{
			if(routerNum-1==i)
				continue;
						
			if("0"==tableReceive[i][1])
			{
				if(fromRouter==tableUpdate[i][1].charAt(0))     //同一个下一跳发来的信息，需要更新
					tableUpdate[i][1]="0";
			}
			else{
				
					if("0"==tableUpdate[i][1])
					{			
						if(tableReceive[i][1].length()>1&&routerNum==tableReceive[i][1].charAt(1)-'0')         //之前我路由发送出去的信息又被反发回来。
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
		//路由更新结束
		
		updateTableIdle=true;
	
		notify();
	}
}


