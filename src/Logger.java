
import java.util.logging.Handler;

public class Logger
{
	public static final int DEBUG	= 0;
	public static final int INFO	= 1;
	public static final int WARNING	= 2;
	public static final int SEVERE	= 3;
	
	private static final String[] levels = new String[] {"DEBUG","INFO","WARNING","SEVERE"};
	
	private static int log_level = 1;
	
	private String name;
	private java.util.logging.Logger log;
	
	public static Logger getLogger(String name)
	{
		return new Logger(name);
	}//getLogger().
	
	public static void setLogLevel(int level)
	{
		log_level = level;
	}//setLogLevel().
	
	public static int getLogLevel()
	{
		return log_level;
	}//getLogLevel()
	
	public static String getLogLevelString()
	{
		return levels[log_level];
	}//getLogLevelString().


	
	public Logger(String name)
	{
		this.name=name;
		this.log = java.util.logging.Logger.getLogger(name);
	}//constructor().


	public void debug(String log_string)
	{
		if(!log_string.startsWith(this.name))
		{log_string=this.name+" "+log_string;}
		if(log_level<=DEBUG)
		{
			System.out.println("DEBUG: "+log_string);
		}//if.
	}//debug().

	public void info(String log_string)
	{
		if(!log_string.startsWith(this.name))
		{log_string=this.name+" "+log_string;}
		if(log_level<=INFO)
		{this.log.info(log_string);}
	}//info().
	
	public void warning(String log_string)
	{
		if(!log_string.startsWith(this.name))
		{log_string=this.name+" "+log_string;}
		if(log_level<=WARNING)
		{this.log.warning(log_string);}
	}//warning().
	
	public void severe(String log_string)
	{
		if(!log_string.startsWith(this.name))
		{log_string=this.name+" "+log_string;}
		if(log_level<=SEVERE)
		{this.log.severe(log_string);}
	}//severe().
	

	private void writeLogOut(String log_message)
	{

	}//writLogOut().
	

}//class Logger().
