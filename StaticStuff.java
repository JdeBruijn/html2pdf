/*
Jon de Bruijn
2022-01-02
Collection of useful generic methods to be imported into any project.
*/

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Calendar;
import java.util.Date;
import java.util.Collection;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.BufferedReader;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.lang.StringBuilder;

import java.text.SimpleDateFormat;

public class StaticStuff
{
	private static final String class_name = "StaticStuff";
	private static final Logger log = Logger.getLogger(class_name);

	public static final HashMap<String, Charset> text_encodings  = new HashMap<String, Charset>()
	{{
		put("UTF_8",StandardCharsets.UTF_8);
		put("ISO_8859_1",StandardCharsets.ISO_8859_1);
		put("US_ASCII",StandardCharsets.US_ASCII);
	}};

	public static final HashMap<String, String> type_to_extension = new HashMap<String,String>()
	{{
		put("image/jpeg","jpg");
		put("image/avif","avif");
		put("image/bmp","bmp");
		put("image/gif","gif");
		put("image/vnd.microsoft.icon","ico");
		put("image/png","png");
		put("image/svg+xml","svg");
		put("image/tiff","tiff");
		put("image/webp","webp");
	}};

	public static final String root_dir = "/var/lib/tomcat/webapps/pepper_fresh/";
	public static final String image_folder_path = root_dir+"uploaded_images/";

//Useful generic method for adding to a message.
	public static void appendMessage(String separator, StringBuilder message, String addition)
	{
		if(message.length()>0)
		{message.append(separator);}
		message.append(addition);
	}//appendErrorMessage().

//Use this method for all timestamps (created_date, modified_date).
	public static long getCurrentUTCEpoch()
	{
		Calendar calendar = Calendar.getInstance();
		TimeZone time_zone = TimeZone.getTimeZone("UTC");
		calendar.setTimeZone(time_zone);
		long utc_epoch = calendar.getTimeInMillis();
		return utc_epoch;
	}//getCurrentUTCEpoch().

//Returns int[] with format: [year(eg 1993), month (1-12), date (1-31)].
	public static int[] getUTCDate()
	{
		TimeZone utc_timezone = TimeZone.getTimeZone("GMT");
		Calendar calendar = Calendar.getInstance(utc_timezone);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH)+1;//Calendar class numbers January as 0.
		int date = calendar.get(Calendar.DATE);
		return new int[] {year, month, date};
	}//getUTCDate().

//Converts date int[] to  date int with format: yyyyMMdd.
	public static int convertToDateInt(int[] date_list)
	{
		if(date_list==null || date_list.length<3)
		{return 0;}
		int date_int = (date_list[0]*10000)+(date_list[1]*100)+date_list[2];
		return date_int;
	}//convertToDateInt().

//Uses the 2 above methods to return int with format: yyyyMMdd
	public static int getUTCDateInt()
	{
		return convertToDateInt(getUTCDate());
	}//getUTCDateInt().

//Converts date string with format yyyy-MM-dd to epoch.
	public static long convertDateStringToEpoch(String date_string)
	{
		String[] date_list = date_string.split("-");
		if(date_list.length!=3)
		{
			log.warning(class_name+" convertDateStringToEpoch(): invalid date_string: '"+date_string+"'");
			return 0;
		}//if.
		int year = Integer.parseInt(date_list[0]);
		int month = Integer.parseInt(date_list[1])-1;
		int day = Integer.parseInt(date_list[2]);
		
		Calendar calendar  = Calendar.getInstance();
		calendar.set(year, month, day);

		long epoch = calendar.getTimeInMillis();
		log.info(class_name+" date_string="+date_string+" epoch="+epoch);//debug**
		return epoch;
	}//convertDateStringToEpoch().

	public static String readFile(String file_path)
	{
		if(file_path==null || file_path.isEmpty())
		{return "";}

		StringBuilder result = new StringBuilder();
		try
		{
			File file = new File(file_path);
			Scanner scanner = new Scanner(file);
			while(scanner.hasNextLine())
			{
				if(result.length()>0)
				{result.append("\n");}
				result.append(scanner.nextLine());
			}//while.
			scanner.close();
    	}//try.
    	catch (FileNotFoundException fnfe)
    	{
    		log.severe(class_name+" File Not Found Exception while trying to read file '"+file_path+"':\n"+fnfe);
    	}//catch().

    	return result.toString();
	}//readFile().

	public static boolean deleteFile(String file_path)
	{
		if(file_path==null || file_path.trim().isEmpty())
		{return false;}
		log.info(class_name+" Deleting file ("+file_path+")...");//INFO.
		File file = new File(file_path);
		try
		{
			return file.delete();
		}//try.
		catch(SecurityException se)
		{
			log.severe(class_name+" Security Exception while trying to delete file ("+file_path+"):\n"+se);
			return false;
		}//catch().
	}//deleteFile().


/*	public static boolean validSession(HttpServletRequest req)
	{
		String session_token=null;
		HttpSession sess = req.getSession();
		if(sess.getAttribute("session_token")!=null)
		{session_token = (String)sess.getAttribute("session_token");}
		else
		{session_token = extractParameter(req,"session_token");}
		log.info(class_name+" session_token="+session_token);//debug**
		if(session_token==null || session_token.trim().isEmpty() || !AccessCheck.checkToken(req, session_token))
		{
			return false;
		}//if.
		log.info(class_name+" validSession(): true");//debug**
		return true;
	}//validSession().*/

	public static String inputStreamToString(InputStream input) throws CustomException
	{
		String text = new BufferedReader(new InputStreamReader(input)).lines().collect(Collectors.joining("\n"));
		return text;
	}//inputStreamToString().

	public static String toHexString(byte[] hash)
	{
		StringBuilder hex_string = new StringBuilder();
		for(byte h: hash)
		{
			String hex = Integer.toHexString(0xFF & h);
			if(hex.length()<=1)
			{hex_string.append("0");}
			hex_string.append(hex);
		}//for(h).
		return hex_string.toString();
	}//toHexString().

	public static void addAll(ArrayList add_to, String[] add_from, int start_index)
	{
		for(int index=start_index; index<add_from.length; index++)
		{add_to.add(add_from[index]);}
	}//addAll().

	public static <T>T[] copyArray(T[] original_arr)
	{
		T[] new_arr = (T[])new Object[original_arr.length];
		for(int index=0; index<original_arr.length; index++)
		{new_arr[index]=original_arr[index];}

		return new_arr;
	}//copyArray().	

	public static String getSigFigInt(int number, int sig_figs)
	{
		StringBuilder formatted = new StringBuilder();
		sig_figs--;
		while((sig_figs*10)>number)
		{
			formatted.append("0");
			sig_figs--;
		}//while.
		formatted.append(number);
		return formatted.toString();
	}//getSigFigInt().

	public static double roundTo(double number, int places)
	{
		double multiplier = (int)Math.pow(10, places);
		number = Math.round(number*multiplier);
		number = number/multiplier;
		return number;
	}//roundTo().

	public static double roundDownTo(double number, int places)
	{
		double multiplier = Math.pow(10, places);
		number = (int)Math.round(number*multiplier);
		number = number/multiplier;
		return number;
	}//roundDownTo().

	public static double doubleFromString(String string)
	{
		double result = 0;
		try
		{
			result = Double.parseDouble(string);
		}//try
		catch(NullPointerException | NumberFormatException nfe)
		{
			log.info(class_name+" Exception while trying to get double from '"+string+"':\n"+nfe);
			result=0;
		}//catch.
		return result;
	}//doubleFromString

	public static String replaceNullString(String value)
	{
		if(value==null)
		{return "";}
		return value;
	}//replaceNullString().

	public static String getFormattedDate(String pattern, String epoch_string)
	{return getFormattedDate(pattern, Long.parseLong(epoch_string));}
	public static String getFormattedDate(String pattern, long epoch)
	{
		if(epoch<=0)
		{return "";}
		return getFormattedDate(pattern, new Date(epoch));
	}//getFormattedDate().
	public static String getFormattedDate(String pattern, Calendar calendar)
	{return getFormattedDate(pattern, calendar.getTime());}
	public static String getFormattedDate(String pattern, Date date)
	{
		if(pattern==null || pattern.trim().isEmpty())
		{pattern="dd/MM/yyyy";}
		SimpleDateFormat formatter = new SimpleDateFormat(pattern);
		log.info(class_name+" pattern="+pattern+" formatted_date="+formatter.format(date));//debug**
		return formatter.format(date);
	}//getFormattedDate().

	//Example: findFirstMatch("foobar bar foo", "fo+", Pattern.MULTILINE).
	public static String findFirstMatch(String source, String regex, Integer pattern_option) throws CustomException
	{
		if(source==null || regex==null || regex.trim().isEmpty())
		{
			throw new CustomException(CustomException.SEVERE, class_name+".findFirstMatch()","'source' and 'regex' must both be defined!", "Trying to get last match");
		}//if.
		if(source.trim().isEmpty())
		{return "";}

		Pattern pattern=null;
		if(pattern_option!=null)
		{pattern = Pattern.compile(regex, pattern_option);}
		else
		{pattern=Pattern.compile(regex);}

		String result="";
		Matcher matcher = pattern.matcher(source);
		if(matcher.find())
		{
			result=matcher.group();
		}//while.
		return result;
	}//findFirstMatch();

	public static String findFirstMatch(String source, String regex) throws CustomException
	{
		return findFirstMatch(source, regex, null);
	}//findFirstMatch().

	public static String findFirstMatchWithDefaultValue(String source, String regex, String default_value) throws CustomException
	{
		String match = findFirstMatch(source, regex, null);
		if(match.isEmpty())
		{match=default_value;}

		return match;
	}//findFirstMatchWithDefaultValue().

	//Example: findMatches("foobar bar foo", "fo+", Pattern.MULTILINE).
	public static LinkedList<String> findMatches(String source, String regex, Integer pattern_option) throws CustomException
	{
		LinkedList<String> results = new LinkedList<String>();

		if(source==null || regex==null || regex.trim().isEmpty())
		{
			throw new CustomException(CustomException.SEVERE, class_name+".findLastMatchWithDefaultValue()","'source' and 'regex' must both be defined!", "Trying to get last match");
		}//if.
		if(source.trim().isEmpty())
		{return results;}

		Pattern pattern=null;
		if(pattern_option!=null)
		{pattern = Pattern.compile(regex, pattern_option);}
		else
		{pattern=Pattern.compile(regex);}

		Matcher matcher = pattern.matcher(source);
		while(matcher.find())
		{
			results.add(matcher.group());
		}//while.
		return results;
	}//findMatches().

	public static LinkedList<String> findMatches(String source, String regex) throws CustomException
	{
		return findMatches(source, regex, null);
	}//findMatches().

	public static String findLastMatch(String source, String regex, Integer pattern_option) throws CustomException
	{
		if(source==null || regex==null || regex.trim().isEmpty())
		{
			throw new CustomException(CustomException.SEVERE, class_name,"'source' and 'regex' must both be defined!", "Trying to get last match");
		}//if.
		if(source.trim().isEmpty())
		{return "";}

		LinkedList<String> results = findMatches(source, regex, pattern_option);
		if(results.size()<=0)
		{return "";}

		return results.get(results.size()-1);
	}//findLastMatchWithDefaultValue().

	public static String findLastMatch(String source, String regex) throws CustomException
	{return findLastMatch(source, regex, null);}

	public static String findLastMatchWithDefaultValue(String source, String regex, String default_value, Integer pattern_option) throws CustomException
	{
		if(source==null || source.trim().isEmpty())
		{return default_value;}

		String last_match = findLastMatch(source, regex, pattern_option);
		if(last_match==null)
		{last_match=default_value;}

		return last_match;
	}//findLastMatchWithDefaultValue().

	public static String findLastMatchWithDefaultValue(String source, String regex, String default_value) throws CustomException
	{return findLastMatchWithDefaultValue(source, regex, default_value, null);}



}//class StaticStuff.