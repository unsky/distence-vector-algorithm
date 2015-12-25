//********************class Router*******************//

// Java core packages
import java.net.*;
import java.util.*;
import java.awt.*;
import java.io.*;

// Java extention packages
import javax.swing.*;
import java.awt.event.*;

class RouterThread extends JFrame implements Runnable
{ 

	private JTextArea routerArea;                                //构造路由器窗口的组件。
	private JLabel commandPrompt;
	private JLabel suspended;
	private JTextField enterField;
	private JCheckBox selectSuspend;
	public boolean suspendFlag=false;
	private JPanel firstLine,secondLine;

	private DatagramSocket socket;
	SendThread send_Thread;
	ReceiveThread receive_Thread;
	ForwardThread forward_Thread;	
							  									  //注意： 在线程内不可以定义static变量，
																  //应为它仅仅是类的，而非具体到某个线程的

	private String tableOld[][]=new String[6][2];                  //定义路由表。old table 代表未更新时的路由表。。。
	public boolean oldTableIdle=true;
	
	private String tableUpdate[][]=new String[6][2];               //定义路由表。update table 代表更新过的路由表。。。
	public boolean updateTableIdle=true;
	
	public boolean neighborRouter[]=new boolean[6];
	public boolean neighborIdle=true;
		
	final Integer routerNum;                                    //定义一个整型常量，用来存储当前线程所表示的路由器标示ID
	public String dataToSend="D";
		
	public RouterThread(String name,Integer Num)
	{
		super(name);
	
		routerNum=Num;
		
		InitiateTable(routerNum);
		
		try{
			socket=new DatagramSocket(2000+routerNum);       // create the socket of port "2000+routerNum"
		   }
		catch(SocketException socketException)
		{
			socketException.printStackTrace();
			System.exit(1);
		}
		/////////////////////////initiate the frame///////////////////////
		
		Container vehicle=getContentPane();
		commandPrompt=new JLabel("Please input command: ");		
		suspended=new JLabel("Select to suspend router: ");
		enterField=new JTextField(30);
		selectSuspend=new JCheckBox("Suspend...");
		selectSuspend.addActionListener(
			new ActionListener(){
    		public void actionPerformed(ActionEvent event)
			{
				suspendFlag=!suspendFlag;
			
			    if(suspendFlag)
			    {
			    		send_Thread.suspend();
			    		receive_Thread.suspend();
			    	
			    } 
			             
			    else {
			    		InitiateTable(routerNum);
			    		routerArea.setText("");
			    		showTable();
			    		send_Thread.resume();
			    		receive_Thread.resume();
			    }      
			}
	     }
	    ); //end call to addActionListener
	    
	    enterField.addActionListener(
			new ActionListener(){
			public void actionPerformed(ActionEvent event)
			{
					//此处可以添加判断从输入框中输入的命令：N、T等等。。。
					
					String command=event.getActionCommand();
					
					if(command.equals("T"))						//此处有待于更改。。。
					{							
						routerArea.setText("");
						showTable();
					
					}
					else if(command.equals("N"))
					{	
						//首先，擦除文本区域的已有文字：
						routerArea.setText("");
						routerArea.append(
							"\n Show the neighbor current routers .... "+"\n");
							
						showNeighbor();
					}
					else if('D'==command.charAt(0))
						 { 
							dataToSend = dataToSend+routerNum+command.substring(1);
						    
							
							try{
								send_Thread.sendPacketFunction(dataToSend);            
							
							   }
							catch(IOException ioException)
							{
								ioException.printStackTrace();
							}
							
							dataToSend="D"; 
							routerArea.setText("");
							routerArea.append(
								"\n Send data...... :"+command.substring(2));
												
					    }
					else{
						routerArea.setText("");
						routerArea.append(
							"\n Input command is invalid...... "+"\n");
					}
					
					enterField.setText("");
			}
	     }
	    ); //end call to addActionListener
		
		routerArea=new JTextArea("");
		
		firstLine=new JPanel();
		secondLine=new JPanel();
		
		firstLine.setLayout(new GridLayout(1,2));
		secondLine.setLayout(new GridLayout(1,2));
		
		firstLine.add(commandPrompt);
		firstLine.add(enterField);
		secondLine.add(suspended);
		secondLine.add(selectSuspend);
			    
	    enterField.setCaretPosition(enterField.getText().length());
			
		vehicle.add(firstLine,BorderLayout.NORTH);
		vehicle.add(secondLine,BorderLayout.SOUTH);
		vehicle.add(new JScrollPane(routerArea),BorderLayout.CENTER);
		  
		setSize(400,300);
		setVisible(true);
	}
	
	public void run()
	{
		send_Thread=new SendThread("sendThread",tableOld,oldTableIdle,routerNum,socket,neighborRouter,neighborIdle);
		receive_Thread=new ReceiveThread("receiveThread",tableUpdate,updateTableIdle,routerNum,socket,neighborRouter,neighborIdle);   
		forward_Thread=new ForwardThread("forward_Thread",tableOld,oldTableIdle,routerNum);
		
		send_Thread.start();
		receive_Thread.start();
		forward_Thread.start();
		
		showTable();
		while(true)
		{
			try{
				Thread.sleep(500);              //每隔0.5秒钟更新路由表。。。
			}
			catch(InterruptedException exception)
			{
				
			}
			
			updateOldTable();
		}
	}
	
	public synchronized void showTable()
	{
		int i;
		
		while(!oldTableIdle)
		{
			try{
				wait();
			}
			catch(InterruptedException exception){
			}
		}
		
		oldTableIdle=false;
		
		routerArea.append("This is router  "+routerNum+":\n" );	
		for(i=0;i<6;i++)
		{
			routerArea.append("Destination: "+tableOld[i][0]+"        "+"Path: "+tableOld[i][1]+"\n");
		}
		routerArea.setCaretPosition(routerArea.getText().length());
		
		oldTableIdle=true;
		notify();
	}
	
	public synchronized void showNeighbor()
	{
		int i;
		
		while(!neighborIdle)
		{
			try{
				wait();
			}
			catch(InterruptedException exception){
			}
		}
		
		neighborIdle=false;
		
		for(i=0;i<6;i++)
		{
			routerArea.append("Router"+(i+1)+" reachable :  "+neighborRouter[i]+"\n");
		}
		
		routerArea.setCaretPosition(routerArea.getText().length());
		
		neighborIdle=true;
		notify();
		
	}
	
	public synchronized void updateOldTable()
	{
		int i,j;
		
		while(!updateTableIdle&&!oldTableIdle&&!neighborIdle)
		{
			try{
				wait();
			}
			catch(InterruptedException exception){
			}
		}
		
		updateTableIdle=false;
		oldTableIdle=false;
		neighborIdle=false;
		
		//由于相邻路由发生变化而更新路由表。。。
		for(j=0;j<6;j++)
		{				
			for(i=0;i<6;i++)
			{
				if(routerNum-1==i)
					continue;
					
				if(j+1==tableUpdate[i][1].charAt(0)-'0'&&!neighborRouter[j])      
				{
					tableUpdate[i][1]="0";
				}
				
				if(tableUpdate[i][1].length()>5)	//&&i+1==routerNum-'0')      
				{
					tableUpdate[i][1]="0";
				}
				
			}
		}	
				
		for(i=0;i<6;i++)
	       tableOld[i][1]=tableUpdate[i][1];
	
		updateTableIdle=true;
		oldTableIdle=true;
		neighborIdle=true;
		
		notify();
	}
	
	
	public void InitiateTable(int routerNum) // set the origin router table by the topology structure...
	{		
	    switch(routerNum)
		{
			case 1:    
				{   
					tableOld[0][0]="1";   tableUpdate[0][0]="1";
					tableOld[0][1]="1";   tableUpdate[0][1]="1";
					
					tableOld[1][0]="2";   tableUpdate[1][0]="2";
					tableOld[1][1]="2";   tableUpdate[1][1]="2";
					
					tableOld[2][0]="3";   tableUpdate[2][0]="3";
					tableOld[2][1]="0";   tableUpdate[2][1]="0";
					
					tableOld[3][0]="4";   tableUpdate[3][0]="4";
					tableOld[3][1]="4";   tableUpdate[3][1]="4";
					
					tableOld[4][0]="5";   tableUpdate[4][0]="5";
					tableOld[4][1]="5";   tableUpdate[4][1]="5";
					
					tableOld[5][0]="6";   tableUpdate[5][0]="6";
					tableOld[5][1]="0";   tableUpdate[5][1]="0";
				   
				    neighborRouter[0]=false;    neighborRouter[1]=true;
				    neighborRouter[2]=false;	neighborRouter[3]=true;
				    neighborRouter[4]=true;	    neighborRouter[5]=false;
				
				}
			    break;  
			case 2:
			    {
			    	tableOld[0][0]="1";   tableUpdate[0][0]="1";
					tableOld[0][1]="1";   tableUpdate[0][1]="1";
					
					tableOld[1][0]="2";   tableUpdate[1][0]="2";
					tableOld[1][1]="2";   tableUpdate[1][1]="2";
					
					tableOld[2][0]="3";   tableUpdate[2][0]="3";
					tableOld[2][1]="3";   tableUpdate[2][1]="3";
					
					tableOld[3][0]="4";   tableUpdate[3][0]="4";
					tableOld[3][1]="0";   tableUpdate[3][1]="0";
					
					tableOld[4][0]="5";   tableUpdate[4][0]="5";
					tableOld[4][1]="0";   tableUpdate[4][1]="0";
					
					tableOld[5][0]="6";   tableUpdate[5][0]="6";
					tableOld[5][1]="0";   tableUpdate[5][1]="0";
			    
			    	neighborRouter[0]=true;     neighborRouter[1]=false;
				    neighborRouter[2]=true; 	neighborRouter[3]=false;
				    neighborRouter[4]=false;    neighborRouter[5]=false;
			    	
			    }
			    break;
			    
			case 3:
			    {
			    	tableOld[0][0]="1";   tableUpdate[0][0]="1";
					tableOld[0][1]="0";   tableUpdate[0][1]="0";
					
					tableOld[1][0]="2";   tableUpdate[1][0]="2";
					tableOld[1][1]="2";   tableUpdate[1][1]="2";
					
					tableOld[2][0]="3";   tableUpdate[2][0]="3";
					tableOld[2][1]="3";   tableUpdate[2][1]="3";
					
					tableOld[3][0]="4";   tableUpdate[3][0]="4";
					tableOld[3][1]="4";   tableUpdate[3][1]="4";
					
					tableOld[4][0]="5";   tableUpdate[4][0]="5";
					tableOld[4][1]="0";   tableUpdate[4][1]="0";
					
					tableOld[5][0]="6";   tableUpdate[5][0]="6";
					tableOld[5][1]="6";   tableUpdate[5][1]="6";
					
					neighborRouter[0]=false;    neighborRouter[1]=true;
				    neighborRouter[2]=false;	neighborRouter[3]=true;
				    neighborRouter[4]=false;    neighborRouter[5]=true;
		    	}
		    	break;
		    
		    case 4:
		    	{
		    		tableOld[0][0]="1";   tableUpdate[0][0]="1";
					tableOld[0][1]="1";   tableUpdate[0][1]="1";
					
					tableOld[1][0]="2";   tableUpdate[1][0]="2";
					tableOld[1][1]="0";   tableUpdate[1][1]="0";
					
					tableOld[2][0]="3";   tableUpdate[2][0]="3";
					tableOld[2][1]="3";   tableUpdate[2][1]="3";
					
					tableOld[3][0]="4";   tableUpdate[3][0]="4";
					tableOld[3][1]="4";   tableUpdate[3][1]="4";
					
					tableOld[4][0]="5";   tableUpdate[4][0]="5";
					tableOld[4][1]="0";   tableUpdate[4][1]="0";
					
					tableOld[5][0]="6";   tableUpdate[5][0]="6";
					tableOld[5][1]="0";   tableUpdate[5][1]="0";
					
					neighborRouter[0]=true;     neighborRouter[1]=false;
				    neighborRouter[2]=true; 	neighborRouter[3]=false;
				    neighborRouter[4]=false;    neighborRouter[5]=false;
		         }
		         break;
		    case 5:
		         {
		         	tableOld[0][0]="1";   tableUpdate[0][0]="1";
					tableOld[0][1]="1";   tableUpdate[0][1]="1";
					
					tableOld[1][0]="2";   tableUpdate[1][0]="2";
					tableOld[1][1]="0";   tableUpdate[1][1]="0";
					
					tableOld[2][0]="3";   tableUpdate[2][0]="3";
					tableOld[2][1]="0";   tableUpdate[2][1]="0";
					
					tableOld[3][0]="4";   tableUpdate[3][0]="4";
					tableOld[3][1]="0";   tableUpdate[3][1]="0";
					
					tableOld[4][0]="5";   tableUpdate[4][0]="5";
					tableOld[4][1]="5";   tableUpdate[4][1]="5";
					
					tableOld[5][0]="6";   tableUpdate[5][0]="6";
					tableOld[5][1]="6";   tableUpdate[5][1]="6";
					
					neighborRouter[0]=true;     neighborRouter[1]=false;
				    neighborRouter[2]=false;	neighborRouter[3]=false;
				    neighborRouter[4]=false;	neighborRouter[5]=true;
			   
		         }
		         break;
		         
		   case 6:
		   		{
		   			tableOld[0][0]="1";   tableUpdate[0][0]="1";
					tableOld[0][1]="0";   tableUpdate[0][1]="0";
					
					tableOld[1][0]="2";   tableUpdate[1][0]="2";
					tableOld[1][1]="0";   tableUpdate[1][1]="0";
					
					tableOld[2][0]="3";   tableUpdate[2][0]="3";
					tableOld[2][1]="3";   tableUpdate[2][1]="3";
					
					tableOld[3][0]="4";   tableUpdate[3][0]="4";
					tableOld[3][1]="0";   tableUpdate[3][1]="0";
					
					tableOld[4][0]="5";   tableUpdate[4][0]="5";
					tableOld[4][1]="5";   tableUpdate[4][1]="5";
					
					tableOld[5][0]="6";   tableUpdate[5][0]="6";
					tableOld[5][1]="6";   tableUpdate[5][1]="6";
					
					neighborRouter[0]=false;    neighborRouter[1]=false;
				    neighborRouter[2]=true;	    neighborRouter[3]=false;
				    neighborRouter[4]=true;	    neighborRouter[5]=false;
		        }
		        break;
		} // end switch
	} // end the initiate table method...
} 