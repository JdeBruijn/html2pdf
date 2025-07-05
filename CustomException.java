/*
Jon de Bruijn
2020-11-27
This class provides a method of either:
	a) Wrapping an exception and throwing it with details about where it originally occurred and what the code was trying to do at the time
	b) Throwing a custom exception message
*/

import java.util.logging.Logger;
import java.io.StringWriter;
import java.io.PrintWriter;

public class CustomException extends Exception
{
	private static final String class_name="CustomException";
	private static final Logger log = Logger.getLogger(class_name);

	public static final int SEVERE=1;
	public static final int WARNING=2;
	public static final int INFO=3;
	public static final int DEBUG=4;//Actually uses 'log.info' but will only be logged when 'log_level' (below) is set>=4.

	//Which logs to actually print.
	public static int log_level=3;//1=severe only. 2=warning & severe. 3=info, warning and severe.

	private static final long serialVersionUID = 5332434849283211406L;

	//These values are passed in through the constructor.
	//Only 'class_name' and 'code_description' are mandatory.
	private String error_class_name="";//Class in which the error occurred.
	private String code_description="";//Name of method in which the error occurred AND/OR description of what the code was trying to do when the error occurred.
	private String error_message="";
	private Throwable original_exception=null;//The original exception that occurred and was caught if there was one.

	public int severity = 1;

	public CustomException(int severity, String error_class_name, String code_description, Throwable exception)
	{
		super(exception.getClass()+" in "+error_class_name+". Code description= "+code_description, exception);
		this.severity=severity;
		this.error_class_name=error_class_name;
		this.code_description=code_description;
		this.original_exception=exception;
		this.error_message=exception.getClass().toString();
	}//constructor().

	public CustomException(int severity, String error_class_name, String error_message, String code_description)
	{
		super(error_message+" in "+error_class_name+". Code description= "+code_description);
		this.severity=severity;
		this.error_class_name=error_class_name;
		this.code_description=code_description;
		this.error_message=error_message;
	}//constructor().

	public void setCodeDescription(String description)
	{
		this.code_description=description;
	}//setCodeDescription().

	
	public String getErrorSummary()
	{
		return this.error_message;
	}//getErrorSummary().

	@Override
	public String toString()
	{
		StringBuilder message = new StringBuilder();
		message.append(class_name);

		message.append("\n\tClass in which error occurred: "+this.error_class_name);
		message.append("\n\tCode description: "+this.code_description);
		message.append("\n\tError message: "+this.error_message);
		if(this.original_exception!=null)
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			this.original_exception.printStackTrace(pw);
			message.append("\n\tOriginal Exception:\n"+sw.toString());
		}//if.

		return message.toString();
	}//toString().

	public void writeLog(Logger logger)
	{
		writeLog(this.severity, logger, this.toString());
	}//writeLog().

	public static void writeLog(int severity, Logger logger, String log_string)
	{
		if(severity>log_level)
		{return;}

		if(severity==SEVERE)
		{
			if(logger!=null)
			{logger.severe(log_string);}
			else
			{System.out.println("SEVERE "+log_string);}
		}
		else if(severity==WARNING)
		{
			if(logger!=null)
			{logger.warning(log_string);}
			else
			{System.out.println("WARNING "+log_string);}
		}
		else if(severity==INFO)
		{
			if(logger!=null)
			{logger.info(log_string);}
			else
			{System.out.println("INFO "+log_string);}
		}
		else
		{
			if(logger!=null)
			{logger.info(log_string);}
			else
			{System.out.println("DEBUG "+log_string);}
		}
	}//writeLog().


}//class CustomException.




