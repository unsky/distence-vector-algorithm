// Router_multiThreads.java
// to use multiThreads to simulate many routers

// Java core packages
import java.net.*;
import java.util.*;
import java.awt.*;
import java.io.*;

// Java extention packages
import javax.swing.*;
import java.awt.event.*;

/////////////////class Manage....////////////////////
public class RouterManage extends JFrame
{
	
	private JTextArea router_MainArea;
	private Thread routers[];
	
	
	
	// create and start threads
	public RouterManage()
	{
		super("router_MainProcess");
		
		routers=new Thread[6];
				
		router_MainArea=new JTextArea();
		getContentPane().add(router_MainArea,BorderLayout.CENTER);
		
		router_MainArea.append("\n\n\nThere was 6 routers originally in the network...");
		
		setSize(500,400);
		setVisible(true);
	}
	
	public void execute()
	{
		for(int i=0;i<routers.length;i++)      //此处为调试方便设为只有三个路由器。。。   
		{
		
				routers[i]=new Thread(new RouterThread("Router "+(i+1),i+1),
				                     "Router "+(i+1));
			    		    	
		    	routers[i].start();
		}

	}
	

	
	public static void main(String args[])
	{
		RouterManage application=new RouterManage();
		
		application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		application.execute();
	}
}  
  
////////////////// end class/////////////////////////
