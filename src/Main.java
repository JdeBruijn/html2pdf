
import java.util.logging.Logger;
import java.util.List;
import java.util.LinkedList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main
{
	private static final String class_name="Main";
	private static final Logger log = Logger.getLogger(class_name);

	//A4 Width = 595.0
	//A4 height = 842.0
	//Landscape mode:
	private static double global_page_width=842;
	private static double global_page_height=595;

	public static void main(String[] args)
	{
		if(args.length<=1 || args[1]==null || args[1].trim().length()<=0)
		{
			System.out.println("SEVERE: "+"Please specify an Output Format (eg: pdf, png, jpg) and a HTML input file. Usage:\n\tjava HtmlToImageConverter 'output_format(pdf,png,jpg)' 'path/to/file.html' ['path/to/css.file']");
			return;
		}//if.
		String output_format=args[0].toLowerCase();

		String html_path = args[1];
		String output_path = html_path.replaceAll("\\.x?html","."+output_format);
		String html_string = StaticStuff.readFile(html_path);
		String base_path = extractBasePath(html_path);

	//GET CSS FILE PATH
		String css_path=null;
		if(args.length>=3)//css file specified
		{css_path = args[2];}

	//READ FONTS CONFIG
		List<String[]> font_files = getFontConfigs(base_path+"fonts.json");

	//SETUP CONVERTER
		HtmlConversionException.log_level=HtmlConversionException.DEBUG;
		HtmlConverter converter=null;
		if(output_format.equals("pdf"))
		{converter = new HtmlToPdfConverter(base_path, css_path, font_files, null, global_page_width, global_page_height);}
		else
		{converter = new HtmlToImageConverter(base_path, css_path, font_files, null, global_page_width, global_page_height);}
		HtmlConversionException.log_level=HtmlConversionException.INFO;

	//CONVERT HTML AND WRITE TO OUTPUT
		try
		{
			File output_file = new File(output_path);
			FileOutputStream output_stream = new FileOutputStream(output_file);
			
			converter.convert(html_string, output_stream);

			output_stream.flush();
			output_stream.close();
		}//try.
		catch(IOException ioe)
		{log.severe(class_name+".main(): IO Exception while trying to convert html and write to output:\n"+ioe);}
	}//main().


	public static String extractBasePath(String file_path)
	{
		if(!file_path.contains("/"))
		{return "./";}

		return file_path.replaceAll("/[^/]+\\.x?html","/");//
	}//extractBasePath().

	private static List<String[]> getFontConfigs(String fonts_config_path)
	{
		LinkedList<String[]> font_files = new LinkedList<String[]>();
		List<String> file_lines  = StaticStuff.readFileLines(fonts_config_path);
		if(file_lines==null || file_lines.size()<=0)
		{
			log.info(class_name+".getFontConfigs(): no file found at '"+fonts_config_path+"'.");
			return font_files;
		}//if.

		for(String line: file_lines)
		{
			if(line.matches("^ *#.*"))//Comment line.
			{continue;}
			else if(!line.contains(":"))//Invalid line.
			{continue;}

			String[] parts = line.split(":");
			if(parts.length!=2)
			{continue;}

			String font_name = parts[0].replaceAll("[\",]+","").trim();
			String font_file = parts[1].replaceAll("[\",]+","").trim();
			font_files.add(new String[]{font_name, font_file});
		}//for(line).
		return font_files;
	}//getFontConfigs().


}//class Main.