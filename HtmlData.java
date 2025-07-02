
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.HashMap;

import java.awt.Color;

public class HtmlData
{

//Static variables.
	private static final String class_name="HtmlData"; 
	private static final Logger log = Logger.getLogger(class_name);

	public static double font_width_multiplier=0.45;

	HashMap<String, Color> colour_names = new HashMap<String,Color>()
	{{
		put("black", Color.BLACK);
		put("blue", Color.BLUE);
		put("cyan", Color.CYAN);
		put("darkGray", Color.DARK_GRAY);
		put("gray", Color.GRAY);
		put("green", Color.GREEN);
		put("lightGray", Color.LIGHT_GRAY);
		put("magenta", Color.MAGENTA);
		put("orange", Color.ORANGE);
		put("pink", Color.PINK);
		put("red", Color.RED);
		put("white", Color.WHITE);
		put("yellow", Color.YELLOW);
	}};




//Object variables.
	public HtmlData parent=null;

	public int start_index=-1;
	public int end_index=-1;

	public String matched_sequence="";

	private String tag="";//Set to lower case when extracted.

	public boolean opening_and_closing=false;
	public boolean is_opening=true;

	private String style_string="";

	private String href="";

	private String src="";


//Style Properties
	public String position="relative";//'relative' or 'fixed'.
	public String float_side="left"; //'left' or 'right'.

	public String display="inline-block";//'block' or 'inline-block'.

	public String width="-1";//-1=='auto'.
	public String max_width="-1";//-1=='auto'.
	public String min_width="0";
	public String height="-1";//-1=='auto'.
	public String max_height="-1";//-1=='auto'.
	public String min_height="0";

	public double border_top_width=0;
	public double border_right_width=0;
	public double border_bottom_width=0;
	public double border_left_width=0;
	public Color border_top_color=Color.BLACK;
	public Color border_right_color=Color.BLACK;
	public Color border_bottom_color=Color.BLACK;
	public Color border_left_color=Color.BLACK;

	public String[] margins=new String[0];
	public String margin_top="0";
	public String margin_right="0";
	public String margin_bottom="0";
	public String margin_left="0";

	public String[] paddings=new String[0];
	public String padding_top="0";
	public String padding_right="0";
	public String padding_bottom="0";
	public String padding_left="0";

	public double font_size=12.0;
	public String font_family = "Times-Roman";

	public Color fg_color = Color.BLACK;
	public Color background_color=Color.WHITE;


	public HtmlData(int start, int end, String sequence)
	{
		this.start_index=start;
		this.end_index=end;
		this.matched_sequence=sequence;

		extractTag();
		checkIsOpening();
		if(tag.equals("a"))
		{extractHref();}
		else if(tag.equals("img"))
		{extractSrc();}
		//System.out.println(class_name+".constructor(): this:"+this.toString());//debug**
	}//constructor().

	private void extractTag()
	{
		String tag_match="";
		try
		{tag_match = StaticStuff.findFirstMatch(this.matched_sequence, "</?[^ ]+");}
		catch(CustomException ce)
		{ce.writeLog(log);}

		if(tag_match!="")
		{this.tag = tag_match.replaceAll("[</>]+","").toLowerCase();}
		else
		{System.out.println("SEVERE: "+class_name+".extractTag(): failed to find tag in '"+this.matched_sequence+"'!");}
		//System.out.println(class_name+" tag="+tag);//debug**
	}//extractTag().

	private void checkIsOpening()
	{
		if(this.matched_sequence.startsWith("</"))
		{
			this.is_opening=false;
			return;
		}//if.

		is_opening=true;

		if(this.matched_sequence.endsWith("/>"))
		{this.opening_and_closing=true;}
	}//checkIsOpening().

	public void setParent(HtmlData parent)
	{
		if(!this.is_opening)
		{
			System.out.println("Warning: "+class_name+".setParent(): Cannot set 'parent' on closing tag!");
			return;
		}//if.

		//System.out.println(class_name+".setParent(): this.tag="+this.tag);//debug**
		//System.out.println(class_name+".setParent(): parent="+parent);//debug**

		this.parent=parent;
		//copyParentStyleProperties();
		extractStyleString();
		extractStyleProperties();
		//System.out.println("\n"+class_name+".setParent(): matched_sequence: "+this.matched_sequence);//debug**
		//System.out.println(" "+this.printStyling());//debug**
	}//setParent().

	public String getTag()
	{return this.tag;}

	public void extractStyleString()
	{
		if(style_string!="")//If it's already been extracted then don't do it again.
		{return;}

		String style_match="";
		try
		{style_match = StaticStuff.findLastMatch(this.matched_sequence, "style *= *\"[^\"]+\"", Pattern.MULTILINE);}
		catch(CustomException ce)
		{ce.writeLog(log);}

		//System.out.println(class_name+".extractStyleString(): this:"+this.toString());//debug**
		//System.out.println("style_match="+style_match);//debug**

		if(style_match!="")
		{this.style_string = style_match.replaceAll("style *= *|\"","").replaceAll("\n"," ");}
	}//extractStyleString().

/*	This is a bad implementation. Built the same idea into 'extractStyleProperties()' and associated sub-methods.
	private void copyParentStyleProperties()
	{
		this.width = parent.width;
		this.max_width = parent.max_width;
		this.min_width = parent.min_width;
		this.height = parent.height;
		this.max_height = parent.max_height;
		this.min_height = parent.min_height;

		this.border_top_width = parent.border_top_width;
		this.border_right_width = parent.border_right_width;
		this.border_bottom_width = parent.border_bottom_width;
		this.border_left_width = parent.border_left_width;
		this.border_top_color = parent.border_top_color;
		this.border_right_color = parent.border_right_color;
		this.border_bottom_color = parent.border_bottom_color;
		this.border_left_color = parent.border_left_color;

		this.background_color = parent.background_color;

		this.margin_top = parent.margin_top;
		this.margin_right = parent.margin_right;
		this.margin_bottom = parent.margin_bottom;
		this.margin_left = parent.margin_left;

		this.padding_top = parent.padding_top;
		this.padding_right = parent.padding_right;
		this.padding_bottom = parent.padding_bottom;
		this.padding_left = parent.padding_left;

		this.font_size = parent.font_size;
		this.font_family = parent.font_family;
	}//copyParentStyleProperties() */

	private void extractStyleProperties()
	{
		if(this.style_string==null)
		{this.style_string="";}//If no styling defined for this element then still copy parent styling.

		String default_width="-1";
		boolean parent_was_null=false;
		if(this.parent==null)
		{
			parent_was_null=true;
			this.parent=this;
			default_width="100%";
		}//if.

	//Position and Display.
		extractStylePositionAndFloat();

		extractStyleDisplay();


	//Width and Height.
		this.width = extractStyleDimension("width", default_width, parent.width, "-1");
	//	if(this.width.contains("%") && parent.width.equals("-1"))//Can't have % width if parent's width is 'auto'.
	//	{this.width="-1";}

		this.max_width = extractStyleDimension("max-width", "-1", parent.max_width, "-1");
	//	if(this.max_width.contains("%") && parent.width.equals("-1"))
	//	{this.max_width="-1";}

		this.min_width = extractStyleDimension("min-width", "0", parent.min_width, "0");

		this.height = extractStyleDimension("height", "-1", parent.height, "-1");
		if(this.height.contains("%") && parent.height.equals("-1"))
		{this.height="-1";}

		this.max_height = extractStyleDimension("max-height", "-1", parent.max_height, "-1");
		if(this.max_height.contains("%") && parent.height.equals("-1"))
		{this.height="-1";}

		this.min_height = extractStyleDimension("min-height", "0", parent.min_height, "0");

	//Borders, Margins and Padding.
		extractBorderData();

		extractMargins();

		extractPadding();

	//Font
		extractFontData();

	//Foreground Color
		try
		{
			String style_fg_color = StaticStuff.findLastMatch(this.style_string, "^|; *color *:[^;\"]+");
			style_fg_color = style_fg_color.replaceAll(".*color *:","");
			if(style_fg_color.trim().equals("inherit"))
			{this.fg_color=parent.fg_color;}
			else
			{this.fg_color = extractColor(style_fg_color, parent.fg_color);}
		}//try.
		catch(CustomException ce)
		{
			ce.setCodeDescription("Trying to extract 'color' from style_string");
			ce.writeLog(log);
			this.fg_color=parent.fg_color;
		}//catch().

	//Background Color
		try
		{
			String style_bg_color = StaticStuff.findLastMatch(this.style_string, "background(-color)? *:[^;\"]+");
			style_bg_color = style_bg_color.replaceAll("background(-color)? *:","");
			if(style_bg_color.trim().equals("inherit"))
			{this.background_color=parent.background_color;}
			else
			{this.background_color = extractColor(style_bg_color, parent.background_color);}
		}//try.
		catch(CustomException ce)
		{
			ce.setCodeDescription("Trying to extract 'background-color' from style_string");
			ce.writeLog(log);
			this.background_color=parent.background_color;
		}//catch().


		if(parent_was_null)
		{this.parent=null;}
	}//extractStyleProperties().

	private void extractStylePositionAndFloat()
	{

	//Style Position.	
		String match=parent.position;
		try
		{match = StaticStuff.findLastMatch(this.style_string, "position *:[^;]+");}
		catch(CustomException ce)
		{
			ce.setCodeDescription("Trying to extract 'position' from style_string");
			ce.writeLog(log);
		}//catch().

		if(match!=null)
		{
			match = match.replaceAll("[^a-zA-Z]+","");
			if(match.equals("fixed"))
			{this.position="fixed";}
			else
			{this.position="relative";}
		}//if.

	//Style Float.
		match=parent.float_side;
		try
		{match = StaticStuff.findLastMatch(this.style_string, "float *:[^;]+");}
		catch(CustomException ce)
		{
			ce.setCodeDescription("Trying to get 'float' from style_string");
			ce.writeLog(log);
		}//catch().

		if(match!=null && match.contains("right"))
		{this.float_side="right";}
		else
		{this.float_side="left";}

	}//extractStylePositionAndFloat().

	private void extractStyleDisplay()
	{
		String match=parent.display;
		try
		{match = StaticStuff.findLastMatch(this.style_string, "display *:[^;]+");}
		catch(CustomException ce)
		{
			ce.setCodeDescription("Trying to extract 'display' from style_string");
			ce.writeLog(log);
		}//catch().

		//If 'float' is defined then 'display' must automatically be 'inline-block'.
		// This is how it works in browsers so best to mimic what they do.
		if(this.style_string.contains("float *:"))
		{
			this.display="inline-block";
			return;
		}//if.

		if(match!=null && match.contains("inline-block"))
		{this.display="inline-block";}
		else
		{this.display="block";}

	}//extractStyleDisplay().

	//Works for all properties that might be defined statically or as a percentage.
	// 'calc' or other values will be ignored and 'default_value' used instead.
	private String extractStyleDimension(String property_name, String default_value, String parent_value, String none_value)
	{
		//debug**
		/*if(property_name.equals("width"))
		{
			System.out.println("\nextractStyleDimension("+property_name+","+default_value+","+parent_value+","+none_value+"): ");
			System.out.println(" matched_sequence: "+this.matched_sequence);
		}//if.*/

		String value=default_value;
		try
		{value = StaticStuff.findLastMatch(this.style_string, property_name+" *:[^;\"]+");}
		catch(CustomException ce)
		{
			ce.setCodeDescription("Trying to extract '"+property_name+"' from style_string");
			ce.writeLog(log);
			return default_value;
		}//catch().

		if(value.trim().equals("none"))
		{return none_value;}

		if(value.trim().equals("inherit"))
		{return parent_value;}

		value = value.replaceAll("[^0-9.%]+",""); //Remove anything that isn't a number or the '%' sign.
		value = value.replaceAll("%.+","%");//Remove anything after '%';

		if(value.isEmpty() || value.equals("%"))
		{value=default_value;}
		else if(!value.matches("[0-9]+\\.?[0-9]*%?|-1"))
		{
			System.out.println("Warning: "+class_name+".extractStyleDimension(): invalid value '"+value+"' for property '"+property_name+"'. Using '"+default_value+"' instead.");
			value=default_value;
		}//else if.

		//debug**
		/*if(property_name.equals("width"))
		{
			System.out.println(" value="+value);
		}//if.*/

		return value;
	}//extractStyleDimension().

	private void extractBorderData()
	{
		String[] all_borders_data = extractBorderDataHelper("","0");

		//Turns out border data is not normally automatically inherited.
		String[] top_border_data = extractBorderDataHelper("-top", String.valueOf(parent.border_top_width));
		String[] right_border_data = extractBorderDataHelper("-right", String.valueOf(parent.border_right_width));
		String[] bottom_border_data = extractBorderDataHelper("-bottom", String.valueOf(parent.border_bottom_width));
		String[] left_border_data = extractBorderDataHelper("-left", String.valueOf(parent.border_left_width));

		if(!all_borders_data[0].equals("0"))//'0' means no value was found when looking for 'border:'. 'none' means 'border:' was explicitly set to 'none' or '0'.
		{
			if(all_borders_data[0].equals("none"))//Done to prevent overriding by parent property.
			{all_borders_data[0]="0";}
			
			if(top_border_data[0].equals("0"))
			{top_border_data = all_borders_data;}
			if(right_border_data[0].equals("0"))
			{right_border_data = all_borders_data;}
			if(bottom_border_data[0].equals("0"))
			{bottom_border_data = all_borders_data;}
			if(left_border_data[0].equals("0"))
			{left_border_data = all_borders_data;}
		}//if.

		this.border_top_width=Integer.parseInt(top_border_data[0].replaceAll("none","0").replaceAll("[^0-9]+",""));
		this.border_right_width=Integer.parseInt(right_border_data[0].replaceAll("none","0").replaceAll("[^0-9]+",""));
		this.border_bottom_width=Integer.parseInt(bottom_border_data[0].replaceAll("none","0").replaceAll("[^0-9]+",""));
		this.border_left_width=Integer.parseInt(left_border_data[0].replaceAll("none","0").replaceAll("[^0-9]+",""));

		this.border_top_color=extractColor(top_border_data[2], Color.BLACK);
		this.border_right_color=extractColor(right_border_data[2], Color.BLACK);
		this.border_bottom_color=extractColor(bottom_border_data[2], Color.BLACK);
		this.border_left_color=extractColor(left_border_data[2], Color.BLACK);
	}//extractBorderData().
	private String[] extractBorderDataHelper(String side, String parent_value)
	{
		//border_values format: ["width(eg 1px)","style(eg solid)", "color(eg black)"]
		String[] border_values = new String[] {"0", "solid", "black"};
		String match_value="";
		try
		{match_value = StaticStuff.findLastMatch(this.style_string, "border"+side+" *:[^;\"]+").replaceAll("border"+side+" *:","").trim();}
		catch(CustomException ce)
		{
			ce.setCodeDescription("Trying to extract 'border"+side+"' from style_string");
			ce.writeLog(log);
		}//catch().

		if(match_value.trim().isEmpty())
		{return border_values;}

		//Prevent deliberate 'none' border being overwritten by global 'border'.
		if(match_value.trim().equals("none") || match_value.trim().equals("0"))
		{
			border_values[0]="none";
			return border_values;
		}//if.

		if(match_value.trim().equals("inherit"))
		{
			border_values[0]=parent_value;
			return border_values;
		}//if.

		match_value=match_value.replaceAll("[ ]{2,}"," ");//Remove extra spaces.
		String[] match_parts = match_value.split(" ");
		if(match_parts.length!=3 || !match_parts[0].matches("[0-9]+(px)?"))
		{
			System.out.println("Warning: "+class_name+".extractBorderDataHelper(): unknown value '"+match_value+"' for 'border"+side+"'! Using defaults instead.");
			return border_values;
		}//if.

		//Should probably do more validation on the other parts here leter.

		border_values=match_parts;

		return border_values;
	}//extractBorderDataHelper().

	private void extractMargins()
	{
		//System.out.println("\nextractMargins(): matched_sequence:"+this.matched_sequence);//debug**

		//this.margins = extractStyleDimension("margin", "0", parent.margins, "none");
		String style_all_margins="";
		try
		{style_all_margins = StaticStuff.findLastMatch(this.style_string, "margin *:[^;\"]+").replaceAll("margin *:","").trim();}
		catch(CustomException ce)
		{
			ce.setCodeDescription("Trying to get 'margin'");
			ce.writeLog(log);
		}//catch().
		if(style_all_margins.length()>0)
		{
			//System.out.println(" style_all_margins: "+style_all_margins);//debug**
			if(style_all_margins.equals("none"))
			{this.margins=new String[]{"0","0","0","0"};}
			else
			{
				String[] all_margins = style_all_margins.split(" ");
				if(all_margins.length==1)
				{
					String global_margin = numbersAndPercent(all_margins[0]);
					this.margins=new String[]{global_margin, global_margin, global_margin, global_margin};
				}//if.
				else if(all_margins.length==2)
				{
					String top_bottom = numbersAndPercent(all_margins[0]);
					String left_right = numbersAndPercent(all_margins[1]);
					this.margins=new String[]{top_bottom, left_right, top_bottom, left_right};
				}//else if.
				else if(all_margins.length==4)
				{this.margins=new String[]{numbersAndPercent(all_margins[0]),numbersAndPercent(all_margins[1]),numbersAndPercent(all_margins[2]),numbersAndPercent(all_margins[3])};}
			}//else.
		}//if.
		//System.out.println(" this.margins: "+Arrays.toString(this.margins));//debug**

		String top_margin = extractStyleDimension("margin-top", "0", String.valueOf(parent.margin_top), "none");
		String right_margin = extractStyleDimension("margin-right", "0", String.valueOf(parent.margin_right), "none");
		String bottom_margin = extractStyleDimension("margin-bottom", "0", String.valueOf(parent.margin_bottom), "none");
		String left_margin = extractStyleDimension("margin-left", "0", String.valueOf(parent.margin_left), "none");

		if(this.margins.length>0)//'0' means no value was found when looking for 'margin:'. 'none' means 'margin:' was explicitly set to 'none' or '0'.
		{
			if(top_margin.equals("0"))
			{top_margin = this.margins[0];}
			if(right_margin.equals("0"))
			{right_margin = this.margins[1];}
			if(bottom_margin.equals("0"))
			{bottom_margin = this.margins[2];}
			if(left_margin.equals("0"))
			{left_margin = this.margins[3];}
		}//if.

		this.margin_top = top_margin.replaceAll("none","0");
		this.margin_right = right_margin.replaceAll("none","0");
		this.margin_bottom = bottom_margin.replaceAll("none","0");
		this.margin_left = left_margin.replaceAll("none","0");
	}//extractMarginData().

	private void extractPadding()
	{
		//System.out.println("\nextractPaddings(): matched_sequence:"+this.matched_sequence);//debug**

		//this.paddings = extractStyleDimension("padding", "0", parent.paddings, "none");
		String style_all_paddings="";
		try
		{style_all_paddings = StaticStuff.findLastMatch(this.style_string, "padding *:[^;\"]+").replaceAll("padding *:","").trim();}
		catch(CustomException ce)
		{
			ce.setCodeDescription("Trying to get 'padding'");
			ce.writeLog(log);
		}//catch().
		if(style_all_paddings.length()>0)
		{
			//System.out.println(" style_all_paddings: "+style_all_paddings);//debug**
			if(style_all_paddings.equals("none"))
			{this.paddings=new String[]{"0","0","0","0"};}
			else
			{
				String[] all_paddings = style_all_paddings.split(" ");
				if(all_paddings.length==1)
				{
					String global_padding = numbersAndPercent(all_paddings[0]);
					this.paddings=new String[]{global_padding, global_padding, global_padding, global_padding};
				}//if.
				else if(all_paddings.length==2)
				{
					String top_bottom = numbersAndPercent(all_paddings[0]);
					String left_right = numbersAndPercent(all_paddings[1]);
					this.paddings=new String[]{top_bottom, left_right, top_bottom, left_right};
				}//else if.
				else if(all_paddings.length==4)
				{this.paddings=new String[]{numbersAndPercent(all_paddings[0]),numbersAndPercent(all_paddings[1]),numbersAndPercent(all_paddings[2]),numbersAndPercent(all_paddings[3])};}
			}//else.
		}//if.
		//System.out.println(" this.paddings: "+Arrays.toString(this.paddings));//debug**

		String top_padding = extractStyleDimension("padding-top", "0", String.valueOf(parent.padding_top), "none");
		String right_padding = extractStyleDimension("padding-right", "0", String.valueOf(parent.padding_right), "none");
		String bottom_padding = extractStyleDimension("padding-bottom", "0", String.valueOf(parent.padding_bottom), "none");
		String left_padding = extractStyleDimension("padding-left", "0", String.valueOf(parent.padding_left), "none");

		if(this.paddings.length>0)//'0' means no value was found when looking for 'padding:'. 'none' means 'padding:' was explicitly set to 'none' or '0'.
		{
			if(top_padding.equals("0"))
			{top_padding = this.paddings[0];}
			if(right_padding.equals("0"))
			{right_padding = this.paddings[1];}
			if(bottom_padding.equals("0"))
			{bottom_padding = this.paddings[2];}
			if(left_padding.equals("0"))
			{left_padding = this.paddings[3];}
		}//if.

		this.padding_top = top_padding.replaceAll("none","0");
		this.padding_right = right_padding.replaceAll("none","0");
		this.padding_bottom = bottom_padding.replaceAll("none","0");
		this.padding_left = left_padding.replaceAll("none","0");
	}//extractPadding().

	private void extractFontData()
	{
		String font_size_str="";
		try
		{
			font_size_str = StaticStuff.findLastMatchWithDefaultValue(this.style_string, "font-size *: *[0-9.]+", String.valueOf(parent.font_size));
			this.font_size=Double.parseDouble(font_size_str.replaceAll("[^0-9.]+",""));
		}//try.
		catch(CustomException ce)
		{
			ce.setCodeDescription("Trying to extract 'font-size' from style_string");
			ce.writeLog(log);
			this.font_size=parent.font_size;
		}//catch().
		catch(NumberFormatException nfe)
		{
			this.font_size=parent.font_size;
		}//catch().

		try
		{this.font_family = StaticStuff.findLastMatchWithDefaultValue(this.style_string, "font-family *: *[^;\"]", parent.font_family).trim();}
		catch(CustomException ce)
		{
			ce.setCodeDescription("Trying to extract 'font-family' from style_string");
			ce.writeLog(log);
			this.font_family=parent.font_family;
		}//catch().

	}//extractFontData().

	public String numbersAndPercent(String input)
	{
		try
		{
			return StaticStuff.findFirstMatchWithDefaultValue(input, "^[0-9]+\\.?[0-9]*%?","0");
		}//try.
		catch(CustomException ce)
		{
			ce.setCodeDescription("numbersAndPercent()");
			ce.severity=CustomException.SEVERE;
			ce.writeLog(log);
			return "0";
		}//catch().
	}//numbersAndPercent().


	private Color extractColor(String colour_str, Color default_colour)
	{
	//	System.out.println("extractColor(): colour_str: "+colour_str);//debug**
		if(colour_str==null || colour_str.trim().isEmpty())
		{return default_colour;}

		Color colour=null;

		colour_str=colour_str.trim();
		if(colour_str.matches("[a-zA-Z]+"))
		{colour = colour_names.get(colour_str.toLowerCase());}

		if(colour!=null)
		{return colour;}

		if(colour_str.matches("^#([a-fA-F0-9]){3}$"))
		{
			char r = colour_str.charAt(1);
			char g = colour_str.charAt(2);
			char b = colour_str.charAt(3);
			colour_str = "#"+r+r+g+g+b+b;
		}//if.

		try
		{colour = Color.decode(colour_str);}
		catch(NumberFormatException nfe)
		{
			log.warning(class_name+".extractColor(): Number Format Exception when trying to get Color from '"+colour_str+"':\n"+nfe);
			colour =  default_colour;
		}//catch().

	//	System.out.println(" colour: "+colour.toString());//debug**

		return colour;
	}//extractColor().


	public String getStyle()
	{return this.style_string;}

	private void extractHref()
	{
		String href_match="";
		try
		{href_match = StaticStuff.findLastMatch(this.matched_sequence, "href *= *\"[^\"]+", Pattern.MULTILINE);}
		catch(CustomException ce)
		{ce.writeLog(log);}

		if(href_match!="")
		{this.href = href_match.replaceAll("href *= *\"","").replaceAll("\n","").trim();}
		//System.out.println(class_name+" href="+this.href);//debug**
	}//extractHref().

	public String getHref()
	{return this.href;}

	private void extractSrc()
	{
		String src_match="";
		try
		{src_match = StaticStuff.findLastMatch(this.matched_sequence, "src *= *\"[^\"]+", Pattern.MULTILINE);}
		catch(CustomException ce)
		{ce.writeLog(log);}

		if(src_match!="")
		{this.src = src_match.replaceAll("src *= *\"","").replaceAll("\n","").trim();}
		//System.out.println(class_name+" src="+this.src);//debug**
	}//extractSrc().

	public String getSrc()
	{return this.src;}

	public String toString()
	{
		StringBuilder rep = new StringBuilder(""
				+ "\n\tstart_index: "+this.start_index
				+ "\n\tend_index: "+this.end_index
				+ "\n\tis_opening: "+this.is_opening
				+ "\n\ttag: "+this.tag
				+ "\n\thref: "+this.href
				+ "\n\tsrc: "+this.src
				+ "\n\tmatched_sequence: "+this.matched_sequence);
		if(this.is_opening)
		{rep.append("\n\tstyle_string: "+this.style_string);}

		return rep.toString();
	}//toString().

	//Debug method. Prints out all the style values that have been extracted.
	public String printStyling()
	{
		if(!this.is_opening)
		{return "This is a closing tag, it doesn't have any styling data.";}

		StringBuilder styling = new StringBuilder();
		styling.append("style_string: "+this.style_string);
		styling.append("\n\tposition: "+this.position);
		styling.append("\n\tfloat_side: "+this.float_side); 
		styling.append("\n\tdisplay: "+this.display);
		styling.append("\n\twidth: "+this.width);
		styling.append("\n\tmax_width: "+this.max_width);
		styling.append("\n\tmin_width: "+this.min_width);
		styling.append("\n\theight: "+this.height);
		styling.append("\n\tmax_height: "+this.max_height);
		styling.append("\n\tmin_height: "+this.min_height);
		styling.append("\n\tborder_top_width: "+this.border_top_width);
		styling.append("\n\tborder_right_width: "+this.border_right_width);
		styling.append("\n\tborder_bottom_width: "+this.border_bottom_width);
		styling.append("\n\tborder_left_width: "+this.border_left_width);
		styling.append("\n\tborder_top_color: "+this.border_top_color);
		styling.append("\n\tborder_right_color: "+this.border_right_color);
		styling.append("\n\tborder_bottom_color: "+this.border_bottom_color);
		styling.append("\n\tborder_left_color: "+this.border_left_color);
		styling.append("\n\tmargins: "+Arrays.toString(this.margins));
		styling.append("\n\tmargin_top: "+this.margin_top);
		styling.append("\n\tmargin_right: "+this.margin_right);
		styling.append("\n\tmargin_bottom: "+this.margin_bottom);
		styling.append("\n\tmargin_left: "+this.margin_left);
		styling.append("\n\tpaddings: "+Arrays.toString(this.paddings));
		styling.append("\n\tpadding_top: "+this.padding_top);
		styling.append("\n\tpadding_right: "+this.padding_right);
		styling.append("\n\tpadding_bottom: "+this.padding_bottom);
		styling.append("\n\tpadding_left: "+this.padding_left);
		styling.append("\n\tfont_size: "+this.font_size);
		styling.append("\n\tfont_family : "+this.font_family);
		styling.append("\n\tbackground_color: "+this.background_color);

		return styling.toString();
	}//printStyling().

}//class HtmlData.