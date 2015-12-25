//*******************TimerThread class****************//

// Java core packages
import java.net.*;
import java.util.*;
import java.awt.*;
import java.io.*;
import java.text.*;

// Java extention packages
import javax.swing.*;
import java.awt.event.*;

class TimeThread extends Thread
{
	private SimpleDateFormat formatter;  // Formats the date displayed
    private String timeMetre[][]=new String[6][2];
    private String lastTime,presentTime;
    private Date currentDate; 
    
    private boolean neighborRouter[]=new boolean[6];
	private boolean neighborIdle;
	
	private int routerNum;
	private int disparity;

            
    public TimeThread(int routerNum,boolean neighborRouter[],boolean neighborIdle)
    {
    	int i;
    	
    	this.routerNum=routerNum;
    	this.neighborRouter=neighborRouter;
    	this.neighborIdle=neighborIdle;
    	    	
    	formatter = new SimpleDateFormat ("ss", 
                                          Locale.getDefault());
        currentDate = new Date();
        presentTime = formatter.format(currentDate);
        lastTime=presentTime;
        
        //初始化计时器二维数组……
        for(i=0;i<6;i++)
        {
        	
        	timeMetre[i][0]=lastTime;
        	timeMetre[i][1]=presentTime;
        }
    }
    
    public synchronized void updateTime(int fromRouterNum)
    {
    	while(!neighborIdle)
		{
			try{
				wait();
			}
			catch(InterruptedException exception)
			{
				
			}
		}
		
		neighborIdle=false;
		
		//debug
		
		//System.err.println("whoes timer: "+(fromRouterNum+1));
    	
    	if(!neighborRouter[fromRouterNum])              
    	{
    		neighborRouter[fromRouterNum]=true;
    		
    		currentDate = new Date();                              
	        presentTime = formatter.format(currentDate);
	        lastTime = presentTime;
	        
	        timeMetre[fromRouterNum][0]=lastTime;
        	timeMetre[fromRouterNum][1]=presentTime;
    	}
    	else
    	{
    		lastTime = timeMetre[fromRouterNum][1];
    		currentDate = new Date();                              
	        presentTime = formatter.format(currentDate);
	        
	        timeMetre[fromRouterNum][0]=lastTime;
        	timeMetre[fromRouterNum][1]=presentTime;
	    }
	    
    	neighborIdle=true;
    	notify();
    }
    
    public void run()
    {
    	int i;
    	    	
    	while(true)
    	{
    		try{
				Thread.sleep(500);             		//每隔1秒钟查看相邻路由是否可达。。。
			}
			catch(InterruptedException exception)
			{
				
			}
			for(i=0;i<6;i++)
			{
				if(i==routerNum-1)
        			continue;
        		
				disparity = Integer.valueOf(timeMetre[i][1])-Integer.valueOf(timeMetre[i][0]);
				disparity = (disparity+60)%60;
				
				//System.err.println(disparity);
				
				if(disparity>8)
				{
					setTopology(i);
					//debug
					//System.err.println("this is router "+routerNum+"  "+"router "+(i+1)+" too long time not send tabel to me....");
				}
	    		else 
	    		{
	    			currentDate = new Date();                              
	       			presentTime = formatter.format(currentDate);
	       			timeMetre[i][1]=presentTime;
	    		}
			}
			
			//debug
			//for(i=0;i<6;i++)
			//	System.err.println("this is router "+routerNum+"  "+"timeMetre"+"   "+timeMetre[i][0]+"   "+timeMetre[i][1]);
		}
    }
    
    public synchronized void setTopology(int modifyNum)
    {
    	while(!neighborIdle){
			try{
				wait();
			}
			catch(InterruptedException exception)
			{
				
			}
		}
		
		neighborIdle=false;
		
		neighborRouter[modifyNum]=false;
   
    	neighborIdle=true;
    	notify();
    }
}