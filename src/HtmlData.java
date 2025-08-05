

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

	HashMap<String, Integer> font_weights_map = new HashMap<String, Integer>()
	{{
		put("lighter",400);
		put("normal",400);
		put("bold",700);
		put("bolder",700);
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

	public String display="";//'block' or 'inline-block'.

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
	public int font_weight = 400;
	public String text_decoration = "";

	public String text_align = "left";
	public String white_space= "normal";

	public Color fg_color = Color.BLACK;
	public Color background_color=Color.WHITE;

	//Tables
	public int colspan=1;


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

	public HtmlData(int start, int end, HtmlData html_data, String sequence)
	{
		this.start_index=start;
		this.end_index=end;

		copyStyleProperties(html_data);

		this.matched_sequence=sequence;


		extractTag();
		checkIsOpening();
		if(tag.equals("a"))
		{extractHref();}
		else if(tag.equals("img"))
		{extractSrc();}
		//System.out.println(class_name+".constructor(): this:"+this.toString());//debug**
	}//copy constructor().

	private void copyStyleProperties(HtmlData html_data)
	{
		this.position = html_data.position;
		this.float_side = html_data.float_side;
		this.display = html_data.display;

		this.width = html_data.width;
		this.max_width = html_data.max_width;
		this.min_width = html_data.min_width;
		this.height = html_data.height;
		this.max_height = html_data.max_height;
		this.min_height = html_data.min_height;

		this.border_top_width = html_data.border_top_width;
		this.border_right_width = html_data.border_right_width;
		this.border_bottom_width = html_data.border_bottom_width;
		this.border_left_width = html_data.border_left_width;
		this.border_top_color = html_data.border_top_color;
		this.border_right_color = html_data.border_right_color;
		this.border_bottom_color = html_data.border_bottom_color;
		this.border_left_color = html_data.border_left_color;

		this.margins = html_data.margins;
		this.margin_top = html_data.margin_top;
		this.margin_right = html_data.margin_right;
		this.margin_bottom = html_data.margin_bottom;
		this.margin_left = html_data.margin_left;

		this.paddings = html_data.paddings;
		this.padding_top = html_data.padding_top;
		this.padding_right = html_data.padding_right;
		this.padding_bottom = html_data.padding_bottom;
		this.padding_left = html_data.padding_left;

		this.font_size = html_data.font_size;
		this.font_family = html_data.font_family;
		this.font_weight = html_data.font_weight;
		this.text_decoration = html_data.text_decoration;

		this.text_align = html_data.text_align;
		this.white_space= html_data.white_space;

		this.fg_color = html_data.fg_color;
		this.background_color = html_data.background_color;
	}//copyStyleProperties()

	private void extractTag()
	{
		String tag_match="";
		try
		{tag_match = StaticStuff.findFirstMatch(this.matched_sequence, "</?[^ ]+");}
		catch(HtmlConversionException ce)
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
		HtmlConversionException.log_level=HtmlConversionException.DEBUG;
		if(!this.is_opening)
		{
			System.out.println("Warning: "+class_name+".setParent(): Cannot set 'parent' on closing tag!");
			return;
		}//if.

		log.debug(class_name+".setParent(): this.tag="+this.tag);//debug**
	//	log.debug(class_name+".setParent(): parent="+parent);//debug**

		this.parent=parent;
		//copyParentStyleProperties();
		extractStyleString();
		extractStyleProperties();

		if(this.getTag().equals("th") || this.getTag().equals("td"))
		{extractColspan();}
		log.debug(class_name+".setParent(): matched_sequence: "+this.matched_sequence);//debug**
		log.debug(" "+this.printStyling());//debug**
		HtmlConversionException.log_level=HtmlConversionException.INFO;
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
		catch(HtmlConversionException ce)
		{ce.writeLog(log);}

		//System.out.println(class_name+".extractStyleString(): this:"+this.toString());//debug**
		//System.out.println("style_match="+style_match);//debug**

		if(style_match!="")
		{this.style_string = style_match.replaceAll("style *= *|\"","").replaceAll("\n"," ");}
	}//extractStyleString().

	private void extractStyleProperties()
	{
	//	HtmlConversionException.log_level=HtmlConversionException.DEBUG;
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

	//Font
		extractFontData();

		extractTextProperties();//wrap, alignment.


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

	//Foreground Color
		try
		{
			String style_fg_color = StaticStuff.findLastMatch(this.style_string, "(^|;)+ *color *: *[^;\"]+");
			style_fg_color = style_fg_color.replaceAll(".*color *:","");
			Color default_color = parent.fg_color;
			if(this.getTag().equals("a"))//link
			{default_color=Color.BLUE;}

	//		System.out.println();//debug**
	//		log.debug(class_name+".extractStyleProperties(): matched_sequence: "+this.matched_sequence);//debug**
	//		log.debug(" style_fg_color: "+style_fg_color+" parent.fg_color: "+parent.fg_color+" default_color: "+default_color);//debug**
			if(style_fg_color.trim().equals("inherit"))
			{this.fg_color=parent.fg_color;}
			else
			{this.fg_color = extractColor(style_fg_color, default_color);}
		}//try.
		catch(HtmlConversionException ce)
		{
			ce.setCodeDescription("Trying to extract 'color' from style_string");
			ce.writeLog(log);
			this.fg_color=parent.fg_color;
		}//catch().
	//	log.debug(" final fg_colour: "+this.fg_color.toString());//debug**

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
		catch(HtmlConversionException ce)
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
		//Table elements
		if(this.getTag().equals("tr") || this.getTag().equals("th") || this.getTag().equals("td"))
		{
			this.position="relative";
			this.float_side="left";
			return;
		}//if.


	//Style Position.	
		String match=parent.position;
		try
		{match = StaticStuff.findLastMatch(this.style_string, "position *:[^;\"]+");}
		catch(HtmlConversionException ce)
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
		match="";
		try
		{match = StaticStuff.findLastMatchWithDefaultValue(this.style_string, "float *:[^;\"]+","").replaceAll("float *: *","").trim();}
		catch(HtmlConversionException ce)
		{
			ce.setCodeDescription("Trying to get 'float' from style_string");
			ce.writeLog(log);
			match="";
		}//catch().

		if(!match.isEmpty())
		{
			match=match.toLowerCase();
			if(match.equals("inherit"))
			{match=this.parent.float_side;}

			if(match.equals("right"))
			{
				this.float_side="right";
				this.display="inline-block";
			}//if.
			else if(match.equals("left"))
			{
				this.float_side="left";
				this.display="inline-block";
			}//else if.
			else 
			{
				this.float_side="left";
				this.display="block";
			}//else.
		}//if.
	/*	else
		{
			this.float_side="left";
			this.display="block";
		}//else.*/

	}//extractStylePositionAndFloat().

	private void extractStyleDisplay()
	{
		if(!this.display.equals(""))//Already set in 'extractStylePositionAndFloat()' method.
		{return;}

		if(this.getTag().equals("tr"))
		{
			this.display="block";
			return;
		}//if.
		else if(this.getTag().equals("th") || this.getTag().equals("td"))
		{
			this.display="inline-block";
			return;
		}//if.

		String match="";
		try
		{match = StaticStuff.findLastMatchWithDefaultValue(this.style_string, "display *:[^;\"]+","").replaceAll("display *: *","").trim();}
		catch(HtmlConversionException ce)
		{
			ce.setCodeDescription("Trying to extract 'display' from style_string");
			ce.writeLog(log);
		}//catch().

		if(!match.isEmpty())
		{
			match=match.toLowerCase();
			if(match.equals("inherit"))
			{match=parent.display;}

			if(match.equals("inline-block"))
			{this.display="inline-block";}
			else
			{this.display="block";}
		}//if.
		else
		{this.display="block";}

	}//extractStyleDisplay().

	private void extractFontData()
	{
	//	HtmlConversionException.log_level=HtmlConversionException.DEBUG;
	//	log.debug(class_name+".extractFontData(): matched_sequence: "+this.matched_sequence);//debug**
	//	log.debug(" tag: "+this.getTag());//debug**

	//Font Size
		String font_size_str="";
		try
		{
			font_size_str = StaticStuff.findLastMatchWithDefaultValue(this.style_string, "font-size *: *[0-9.]+(em)?", String.valueOf(parent.font_size)).replaceAll("font-size *: *","");
	//		log.debug(" font_size_str: "+font_size_str);//debug**
			this.font_size=Double.parseDouble(font_size_str.replaceAll("[^0-9.]+",""));
			if(font_size_str.matches("[0-9]+\\.?[0-9]*em"))
			{
				this.font_size = StaticStuff.roundTo(parent.font_size*this.font_size,2);//Use 'this.font_size' as a multiplier instead.
			}//if.
	//		log.debug(" this.font_size: "+this.font_size);//debug**
		}//try.
		catch(HtmlConversionException ce)
		{
			ce.setCodeDescription("Trying to extract 'font-size' from style_string");
			ce.writeLog(log);
			this.font_size=parent.font_size;
		}//catch().
		catch(NumberFormatException nfe)
		{
			this.font_size=parent.font_size;
		}//catch().


	//Text Decoration
		String decoration="";
		try
		{
			decoration = StaticStuff.findLastMatchWithDefaultValue(this.style_string, "text-decoration *: *[^;\"]+","").replaceAll("text-decoration *: *","").trim();
		}//try.
		catch(HtmlConversionException ce)
		{
			ce.setCodeDescription("Trying to extract 'text-decoration' from style_string");
			ce.writeLog(log);
			decoration="";
		}//catch(ce).
		decoration=decoration.toLowerCase();
		if(this.text_decoration.isEmpty())
		{this.text_decoration=decoration;}

		if(this.text_decoration.isEmpty())
		{
			if(this.getTag().equals("i") && !this.text_decoration.contains("italic"))//italic
			{this.text_decoration+=" italic";}
			if(this.getTag().equals("b") && !this.text_decoration.contains("bold"))//italic
			{this.text_decoration+=" bold";}
			if(this.getTag().equals("a") && !this.text_decoration.contains("underline"))//italic
			{this.text_decoration+=" underline";}
		}//if.
	//	log.debug(" text_decoration: "+this.text_decoration);//debug**


	//Font Family.
		try
		{this.font_family = StaticStuff.findLastMatchWithDefaultValue(this.style_string, "font-family *: *[^;\"]+", parent.font_family).replaceAll("font-family *: *+","").trim();}
		catch(HtmlConversionException ce)
		{
			ce.setCodeDescription("Trying to extract 'font-family' from style_string");
			ce.writeLog(log);
		}//catch().
		if(this.font_family.trim().isEmpty())
		{this.font_family=parent.font_family;}
	//	log.debug(" parent.font_family: "+this.parent.font_family);//debug**
	//	log.debug(" font_family: "+this.font_family);//debug**


	//Font Weight.
		String font_weight_style="";
		try
		{font_weight_style = StaticStuff.findLastMatch(this.style_string, "font-weight *: *[^;\"]+").replaceAll("font-weight *: *","");}
		catch(HtmlConversionException ce)
		{
			ce.setCodeDescription("Trying to extract 'font-weight' from style_string");
			ce.writeLog(log);
		}//catch().

		if(font_weight_style.trim().isEmpty())
		{
			if(this.text_decoration.contains("bold"))
			{this.font_weight=700;}
			else
			{this.font_weight=parent.font_weight;}
			return;
		}//if.

		font_weight_style = font_weight_style.toLowerCase();

		if(font_weight_style.equals("none"))
		{
			this.font_weight=0;
			return;
		}//if.

		if(font_weights_map.containsKey(font_weight_style))
		{
			this.font_weight = font_weights_map.get(font_weight_style);
			return;
		}//if.

		try
		{
			this.font_weight = Integer.parseInt(font_weight_style);
			return;
		}//try.
		catch(NumberFormatException nfe)
		{log.severe(class_name+".extractFontData(): Number Format Exception:\n"+nfe);}

		this.font_weight=parent.font_weight;
	}//extractFontData().

	private void extractTextProperties()
	{
	//text-align
		try
		{
			this.text_align = StaticStuff.findLastMatchWithDefaultValue(this.style_string, "text-align *: *[^;\"]+", String.valueOf(parent.text_align)).replaceAll("text-align *: *","").trim();
			if(this.text_align.isEmpty())
			{this.text_align=parent.text_align;}
		}//try.
		catch(HtmlConversionException ce)
		{
			ce.setCodeDescription("trying to extract text-align property");
			ce.writeLog(log);
			this.text_align=parent.text_align;
		}//catch().

	////white_space;
		try
		{
			this.white_space = StaticStuff.findLastMatchWithDefaultValue(this.style_string, "white-space *: *[^;\"]+", String.valueOf(parent.white_space)).replaceAll("white-space *: *","").trim();
			//log.debug(class_name+".extractTextProperties(): this.white_space: "+this.white_space);//debug**
			if(this.white_space.isEmpty())
			{this.white_space=parent.white_space;}
		}//try.
		catch(HtmlConversionException ce)
		{
			ce.setCodeDescription("trying to extract white_space property");
			ce.writeLog(log);
			this.white_space=parent.white_space;
		}//catch().
	}//extractTextProperties().

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
		{
			value = StaticStuff.findLastMatch(this.style_string, property_name+" *:[^;\"]+");
			value = StaticStuff.findFirstMatch(value, "[0-9]+\\.?[0-9]*(%|em)?|-1");
		}//try.
		catch(HtmlConversionException ce)
		{
			ce.setCodeDescription("Trying to extract '"+property_name+"' from style_string");
			ce.writeLog(log);
			return default_value;
		}//catch().

		if(value.trim().equals("none"))
		{return none_value;}

		if(value.trim().equals("inherit"))
		{return parent_value;}

	/*	Commented 'cos extracting values from bad data seems like a bad idea. Just give a warning if the value isn't good.
		value = value.replaceAll("[^0-9.]+(%|em)?",""); //Remove anything that isn't a number or the '%' sign.
		value = value.replaceAll("%.+","%");//Remove anything after '%';
		value = value.replaceAll("em.+","em");//Remove anything after 'em';
	*/

		if(value.isEmpty() || value.equals("%") || value.equals("em"))
		{value=default_value;}
		else if(!value.matches("[0-9]+\\.?[0-9]*(%|em)?|-1"))
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
	//	log.debug("\nextractBorderData(): this.tag: "+this.getTag());

		String[] all_borders_data = extractBorderDataHelper("","0");//debug**

		//Turns out border data is not normally automatically inherited.
		String[] top_border_data = extractBorderDataHelper("-top", String.valueOf(parent.border_top_width));
	//	log.debug(" top_border_data: "+Arrays.toString(top_border_data));//debug**
		String[] right_border_data = extractBorderDataHelper("-right", String.valueOf(parent.border_right_width));
	//	log.debug(" right_border_data: "+Arrays.toString(right_border_data));//debug**
		String[] bottom_border_data = extractBorderDataHelper("-bottom", String.valueOf(parent.border_bottom_width));
	//	log.debug(" bottom_border_data: "+Arrays.toString(bottom_border_data));//debug**
		String[] left_border_data = extractBorderDataHelper("-left", String.valueOf(parent.border_left_width));
	//	log.debug(" left_border_data: "+Arrays.toString(left_border_data));//debug**

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
		//If this is a table element: check to see if borders are missing and set defaults if they are.
		if(this.getTag().equals("table") || this.getTag().equals("tr") || this.getTag().equals("th") || this.getTag().equals("td"))
		{border_values = getTableDefaultBorders(side);}

		String match_value="";
		try
		{match_value = StaticStuff.findLastMatch(this.style_string, "border"+side+" *:[^;\"]+").replaceAll("border"+side+" *:","").trim();}
		catch(HtmlConversionException ce)
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

	private String[] getTableDefaultBorders(String side)
	{
	//	log.debug(" getTableDefaultBorders(): side: "+side);
		if(this.getTag().equals("table") && (side.endsWith("top") || side.endsWith("left")))
		{return new String[] {"1", "solid", "black"};}

		if(this.getTag().equals("tr") && side.endsWith("bottom"))
		{return new String[] {"1", "solid", "black"};}

		if((this.getTag().equals("th") || this.getTag().equals("td")) && side.endsWith("right"))
		{return new String[] {"1", "solid", "black"};}

		return new String[] {"0", "solid", "black"};		
	}//getTableDefaultBorders().

	private void extractMargins()
	{
		//System.out.println("\nextractMargins(): matched_sequence:"+this.matched_sequence);//debug**

		//Table cells currently don't support margins.	
		if(this.getTag().equals("th") || this.getTag().equals("td"))
		{
			this.margin_top = "0";
			this.margin_right = "0";
			this.margin_bottom = "0";
			this.margin_left = "0";
			return;
		}//if.

		//this.margins = extractStyleDimension("margin", "0", parent.margins, "none");
		String style_all_margins="";
		try
		{style_all_margins = StaticStuff.findLastMatch(this.style_string, "margin *:[^;\"]+").replaceAll("margin *:","").trim();}
		catch(HtmlConversionException ce)
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
					String global_margin = numbersAndPercentAndMSizing(all_margins[0]);
					this.margins=new String[]{global_margin, global_margin, global_margin, global_margin};
				}//if.
				else if(all_margins.length==2)
				{
					String top_bottom = numbersAndPercentAndMSizing(all_margins[0]);
					String left_right = numbersAndPercentAndMSizing(all_margins[1]);
					this.margins=new String[]{top_bottom, left_right, top_bottom, left_right};
				}//else if.
				else if(all_margins.length==4)
				{this.margins=new String[]{numbersAndPercentAndMSizing(all_margins[0]),numbersAndPercentAndMSizing(all_margins[1]),numbersAndPercentAndMSizing(all_margins[2]),numbersAndPercentAndMSizing(all_margins[3])};}
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
		catch(HtmlConversionException ce)
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
					String global_padding = numbersAndPercentAndMSizing(all_paddings[0]);
					this.paddings=new String[]{global_padding, global_padding, global_padding, global_padding};
				}//if.
				else if(all_paddings.length==2)
				{
					String top_bottom = numbersAndPercentAndMSizing(all_paddings[0]);
					String left_right = numbersAndPercentAndMSizing(all_paddings[1]);
					this.paddings=new String[]{top_bottom, left_right, top_bottom, left_right};
				}//else if.
				else if(all_paddings.length==4)
				{this.paddings=new String[]{numbersAndPercentAndMSizing(all_paddings[0]),numbersAndPercentAndMSizing(all_paddings[1]),numbersAndPercentAndMSizing(all_paddings[2]),numbersAndPercentAndMSizing(all_paddings[3])};}
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


	//Table cells only. 'colspan' is and attribute that specifies how many 'columns-to-span'.
	private void extractColspan()
	{
		try
		{
			String colspan_str = StaticStuff.findLastMatch(this.matched_sequence, "colspan *= *\" *[0-9]+\" *");
			if(colspan_str!=null && !colspan_str.trim().isEmpty())
			{
				int colspan_int = Integer.parseInt(StaticStuff.findFirstMatch(colspan_str, "[0-9]+"));
				if(colspan_int>0)
				{this.colspan=colspan_int;}
			}//if.
		}//try.
		catch(HtmlConversionException ce)
		{
			ce.setCodeDescription("Trying to extractColspan");
			ce.writeLog(log);
		}//catch().
	}//extractColspan().

	public String numbersAndPercentAndMSizing(String input)
	{
		try
		{
			return StaticStuff.findFirstMatchWithDefaultValue(input, "^[0-9]+\\.?[0-9]*(%|em)?","0");
		}//try.
		catch(HtmlConversionException ce)
		{
			ce.setCodeDescription("numbersAndPercent()");
			ce.severity=HtmlConversionException.SEVERE;
			ce.writeLog(log);
			return "0";
		}//catch().
	}//numbersAndPercent().


	private Color extractColor(String colour_str, Color default_colour)
	{

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

		return colour;
	}//extractColor().


	public String getStyle()
	{return this.style_string;}

	private void extractHref()
	{
		String href_match="";
		try
		{href_match = StaticStuff.findLastMatch(this.matched_sequence, "href *= *\"[^\"]+", Pattern.MULTILINE);}
		catch(HtmlConversionException ce)
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
		catch(HtmlConversionException ce)
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
		styling.append("\n\tfg_color: "+this.fg_color);
		styling.append("\n\tbackground_color: "+this.background_color);

		return styling.toString();
	}//printStyling().

}//class HtmlData.