//Jon 13-12-2017
//This class takes a css file name (in the constructor) and combines
//it with an html string specified in the inline() method to return
//the html with css inlined.
//This inliner does NOT recognise '!important' tag. 
//Class styles are inputed in the order they called in the html.

//IMPORTANT!! if classes and style are both defined in the html then style must be called before classes!

//DOM element classes must be called in the form:
//	class="class_name1 class_name2"
//i.e. no spaces between 'class', '=' and '"', class names separated by a space.
//DOM element style must be called in the same form (class names=>style parameters)

//The css file must be of the form:
//body
//{
//		parameter1:value1;
//		parameter2:value2;
// ...
//}
//.class_name1
//{
//		parameter1:value1;
//		parameter2:value2;
//		...
//	}
//.class_name2
//	{
//	...


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

public class CssInliner
{
	private static final String class_name = "CssInliner";
	private static final Logger log = Logger.getLogger(class_name);

	private static final String default_styling_file = "./defaults.css";

	private HashMap<String, CssObject> css_tags = new HashMap<String, CssObject>();
	private HashMap<String, CssObject> css_classes = new HashMap<String, CssObject>();
	private HashMap<String, CssObject> css_ids = new HashMap<String, CssObject>();
	

	private CssObject inline_styling = null;//This gets reset for each HTML element.

	public CssInliner(String css_filename)
	{
		HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, class_name+".constructor():");//debug**

		if(default_styling_file!=null)
		{readCss(default_styling_file);}
		if(css_filename!=null && !css_filename.trim().isEmpty())
		{readCss(css_filename);}

		HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, "css_tags:\n"+StaticStuff.printHashMap(this.css_tags));//debug**
		HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, "css_classes:\n"+StaticStuff.printHashMap(this.css_classes));//debug**
		HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, "css_ids:\n"+StaticStuff.printHashMap(this.css_ids));//debug**
	}//constructor().

	private void readCss(String css_filename)
	{
		String css_string = StaticStuff.readFile(css_filename);
		if(css_string.trim().isEmpty())
		{
			log.severe(class_name+" Failed to read CSS from '"+css_filename+"'!");
			return;
		}//if.

		processCss(css_string);
	}//readCss().

	private void processCss(String css_string)
	{
	//	HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, class_name+".processCss():");//debug**

		//Remove all comments.
		css_string = Pattern.compile("/\\*.*\\*/", Pattern.DOTALL).matcher(css_string).replaceAll("");

		Pattern pattern = Pattern.compile("\\{[^{}]+\\}", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(css_string);

		int previous_end_index=0;
		while(matcher.find())
		{
			String name = css_string.substring(previous_end_index, matcher.start()).trim();
			if(name.trim().isEmpty())
			{
				log.warning(class_name+".processCss(): name is empty! Matched sequence: '"+matcher.group()+"'");
				return;
			}//if.

			previous_end_index=matcher.end();

			CssObject css_object = new CssObject(name);

			String[] css_parameters = matcher.group().replaceAll("[{}\n]+","").split(";");
			if(css_parameters.length<=0)
			{
				log.warning(class_name+".processCss(): body is blank? matched sequence: '"+matcher.group()+"'");
				return;
			}//if.
		//	HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, " name:"+name);//debug**
		//	HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, " css_parameters:"+Arrays.toString(css_parameters));//debug**

			for(String param_data: css_parameters)
			{
				if(!param_data.contains(":"))//Basic catchall to skip blank lines etc..
				{continue;}

				try
				{css_object.addParameter(param_data);}
				catch(HtmlConversionException ce)
				{ce.writeLog(log);}
			}//for(param_data).

			if(name.startsWith("."))
			{css_classes.put(name.substring(1), css_object);}
			else if(name.startsWith("#"))
			{css_ids.put(name.substring(1), css_object);}
			else if(name.equals("inline_styling"))//This is styling specified in the html. eg: <div style="display:block;">
			{inline_styling=css_object;}
			else
			{css_tags.put(name, css_object);}
		}//while.
	}//processCss().

	public String inline(String html)
	{
		HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, class_name+".inline():");//debug**
		StringBuilder css_html = new StringBuilder();

		html = html.replaceAll("\n", "");
		int head_finish_index = html.indexOf("</head>");
		if(head_finish_index>0)
		{head_finish_index+=7;}
		else
		{head_finish_index=0;}
		css_html.append(html.substring(0, head_finish_index));
		
		html = html.substring(head_finish_index);

		String[] html_lines = html.split(">");
		boolean finished = false;
		for(String line : html_lines)
		{
			line=line.trim();
			if (line.isEmpty())
			{continue;}

			if (line.equals("</body"))
			{finished = true;}
			HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, " line="+line);//debug**
			if (finished || line.charAt(1) == '/')
			{
				css_html.append(line + ">\n");
				continue;
			}//if.

			int[] style_indexes = null;
			int[] class_indexes = null;
			int class_end_index = -1;
			String style_insert = "";
			StringBuilder line_new = new StringBuilder();

			int open_index = line.indexOf("<");

			if(open_index==-1 || line.charAt(open_index+1)=='/')//If this 'line' doesn't contain an opening tag.
			{
				css_html.append(line + ">\n");
				continue;
			}//if.

			
			HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, " original line="+line);//debug**
			String tag="";
			try
			{tag = StaticStuff.findFirstMatch(line, "<[^ ]+").replace("<","");}
			catch(HtmlConversionException ce)
			{
				ce.setCodeDescription("Trying to get html element tag");
				ce.severity=HtmlConversionException.SEVERE;
				ce.writeLog(log);
			}//catch().

			String id = "";
			try
			{id = StaticStuff.findFirstMatchWithDefaultValue(line, "id *= *\"[^\"]+", "").replaceAll("id *= *\"","").trim();}
			catch(HtmlConversionException ce)
			{
				ce.setCodeDescription("Trying to get html element id");
				ce.severity=HtmlConversionException.DEBUG;
				ce.writeLog(null);
			}//catch().

			String classes = "";
			try
			{classes = StaticStuff.findFirstMatchWithDefaultValue(line, "class *= *\"[^\"]+", "").replaceAll("class *= *\"","").trim();}
			catch(HtmlConversionException ce)
			{
				ce.setCodeDescription("Trying to get html element class");
				ce.severity=HtmlConversionException.DEBUG;
				ce.writeLog(null);
			}//catch().

			HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, "tag: "+tag+" id: "+id+" classes: "+classes);//debug**

			inline_styling=null;
			style_indexes = null;
			String inline_style_string = "";
			Pattern style_pattern = Pattern.compile("style *= *\"[^\"]+\"");
			Matcher style_matcher = style_pattern.matcher(line);
			if(style_matcher.find())
			{
				style_indexes = new int[] {style_matcher.start(), style_matcher.end()};
				inline_style_string=style_matcher.group().replaceAll("(style *= *)|\"","").trim();
			}//if.

			if(!inline_style_string.isEmpty())
			{processCss("inline_styling\n{"+inline_style_string+"}");}

			String all_styling = getCssStyles(tag, id, classes);

			HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, " all_styling: "+all_styling);//debug**

			if(style_indexes!=null)
			{line_new.append(line.substring(0, style_indexes[0]));}
			else
			{line_new.append(line);}

			if(!all_styling.equals(" style=\"\""))
			{line_new.append(all_styling);}

			if(style_indexes!=null)
			{line_new.append(line.substring(style_indexes[1]));}
			line_new.append(">\n");

			css_html.append(line_new);

		}//for(line).
		//HtmlConversionException.writeLog(HtmlConversionException.DEBUG, null, class_name+".inline() css_html="+css_html.toString());//debug**
		return css_html.toString();
	}//inline().


	private String getCssStyles(String tag, String id, String classes_string)
	{
		HashMap<String, String> style_parameters = new HashMap<String, String>();
		HashMap<String, String> important_parameters = new HashMap<String, String>();//Just used to keep track of which styles can't be overwritten.

		//NOTE: order of operations is important here. Each level overrides the previous one.

		//TAG styling.	
		if(css_tags.containsKey(tag))
		{addStyles(style_parameters, important_parameters, css_tags.get(tag));}

		//CLASS styling.
		String[] class_names = classes_string.split(" ");
		for (String class_name : class_names)
		{
			class_name = class_name.trim();
			if(css_classes.containsKey(class_name))
			{addStyles(style_parameters, important_parameters, css_classes.get(class_name));}
		}//for.

		//ID styling.
		if(css_ids.containsKey(id))
		{addStyles(style_parameters, important_parameters, css_ids.get(id));}

		//INLINE styling.
		if(inline_styling!=null)
		{addStyles(style_parameters, important_parameters, inline_styling);}


		StringBuilder style_insert = new StringBuilder(" style=\"");
		for(String param_name: style_parameters.keySet())
		{
			style_insert.append(param_name+":"+style_parameters.get(param_name)+"; ");
		}//for(param_name).
		style_insert.append("\"");

		return style_insert.toString();
	}//getCssStyles().

	private void addStyles(HashMap<String, String> styles, HashMap<String, String> important_styles, CssObject new_styles)
	{
		for(String param_name: new_styles.getParamNames())
		{
			if(new_styles.isImportant(param_name))
			{
				styles.put(param_name, new_styles.getParamValue(param_name));
				important_styles.put(param_name, new_styles.getParamValue(param_name));
			}//if.
			else if(!important_styles.containsKey(param_name))//This style parameter hasn't been set to '!important' previously.
			{styles.put(param_name, new_styles.getParamValue(param_name));}

		}//for(param_name).
	}//addStyles().

	//Example: findFirstMatch("foobar bar foo", "fo+").
	private int[] getIndexesOf(String source, String regex)
	{
		int[] result = new int[] {-1, -1};
		if(source==null || regex==null || regex.trim().isEmpty())
		{return result;}
		if(source.trim().isEmpty())
		{return result;}

		Pattern pattern=null;
		pattern=Pattern.compile(regex);

		Matcher matcher = pattern.matcher(source);
		if(matcher.find())
		{
			result[0]=matcher.start();
			result[1]=matcher.end();
		}//while.

		return result;
	}//findFirstMatch();

}//class CssInliner().
