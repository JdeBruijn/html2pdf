
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;

import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.Paths;

import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.File;

import java.awt.Color;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.awt.font.FontRenderContext;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;

import javax.imageio.ImageIO;



public class HtmlToImageConverter
{
	public static final String class_name = "HtmlToImageConverter";
	public static final Logger log = Logger.getLogger(class_name);

	//A4 Width = 595.0
	//A4 height = 842.0
	//Landscape mode:
	private static final double global_page_width=842;
	private static final double global_page_height=595;

	private static final BufferedImage dummy_image = new BufferedImage((int)global_page_width, (int)global_page_height, BufferedImage.TYPE_INT_ARGB);
	private static final Graphics2D dummy_graphics = dummy_image.createGraphics();
	//private static FontRenderContext dummy_frc = new FontRenderContext(null, true, true);
	private static FontRenderContext dummy_frc = dummy_graphics.getFontRenderContext();

	private static final double space_m_sizing = 0.33;

	public static void main(String[] args)
	{
		CustomException.log_level=CustomException.DEBUG;

		if(args.length<=0 || args[0]==null || args[0].trim().length()<=0)
		{
			System.out.println("SEVERE: "+"Please specify a XHTML input file. Usage:\n\tjava HtmlToImageConverter 'path/to/file.html' ['path/to/css.file']");
			return;
		}//if.
		String xhtml_path = args[0];
		String image_path = xhtml_path.replaceAll("\\.x?html",".png");
		String xhtml_string = readFileToString(xhtml_path);

		GlobalDocVars doc_vars = new GlobalDocVars();

		doc_vars.base_path=getBasePath(xhtml_path);//Used for finding location of things like images.
		CustomException.debug(class_name+".main(): base_path: "+doc_vars.base_path);//debug**
		CustomException.log_level=CustomException.INFO;

//INLINE CSS
		String css_path=null;
		if(args.length>=2)//css file specified
		{css_path = args[1];}

	//	CustomException.log_level=CustomException.DEBUG;

		System.out.println("Inlining css...");//debug**
		CssInliner css_inliner = new CssInliner(css_path);
		xhtml_string = css_inliner.inline(xhtml_string);

		CustomException.writeLog(CustomException.DEBUG, null, "html with inlined css:\n\t"+xhtml_string);//debug**

		CustomException.log_level=CustomException.INFO;

		doc_vars.setPageSize(global_page_width, global_page_height);

	//	CustomException.log_level=CustomException.DEBUG;
		HashMap<String, Font> custom_fonts = loadFonts();
		CustomException.log_level=CustomException.INFO;

//READ HTML FILE
	//	CustomException.log_level=CustomException.DEBUG;
		LinkedList<PDFElementProperties> flattened_elements = new LinkedList<PDFElementProperties>();
		try
		{
			PDFElementProperties top_element=readXHTML(xhtml_string, custom_fonts, doc_vars);
			flattenElements(flattened_elements, top_element);
		}//try.
		catch(CustomException ce)
		{
			ce.writeLog(log);
			if(ce.severity<CustomException.WARNING)
			{return;}
		}//catch().
		CustomException.log_level=CustomException.INFO;


//GENERATE IMAGE
	//	CustomException.log_level=CustomException.DEBUG;

		try
		{
			generateImage(flattened_elements, image_path, custom_fonts, doc_vars);
		}//try.
		catch(CustomException ce)
		{
			ce.writeLog(log);
		}//catch().*/
		CustomException.log_level=CustomException.INFO;
		
	/*	try
		{generateExampleImage();}
		catch(CustomException ce)
		{
			ce.writeLog(log);
		}//catch().*/

	}//main().

	public static String getBasePath(String file_path)
	{
		if(!file_path.contains("/"))
		{return "./";}

		return file_path.replaceAll("/[^/]+\\.x?html","/");//
	}//getBasePath().

	public static HashMap<String, Font> loadFonts()
	{
		List<String[]> font_files = getFontConfigs();

		HashMap<String, Font> custom_fonts = new HashMap<String, Font>();
		for(String[] name_and_file: font_files)
		{
			createFont(name_and_file[1], name_and_file[0], custom_fonts);
		}//for(name_and_file).

		return custom_fonts;
	}//loadFonts().
	private static void createFont(String font_file_name, String font_name, HashMap<String, Font> custom_fonts)
	{
		CustomException.debug(class_name+".createFont(): ");//debug**
		try
		{
			File font_file = new File(font_file_name);
			CustomException.debug(" creating font '"+font_name+"' from '"+font_file_name+"'...");//debug**
			Font font = Font.createFont(Font.TRUETYPE_FONT, font_file);
			custom_fonts.put(font_name, font);
		}//try.
		catch(FontFormatException | IOException ioe)
		{
			log.severe(class_name+" IO Exception while trying to createFont '"+font_file_name+"':\n"+ioe);
		}//catch().
	}//createFont().
	private static List<String[]> getFontConfigs()
	{
		LinkedList<String[]> font_files = new LinkedList<String[]>();
		List<String> file_lines  = StaticStuff.readFileLines("./fonts.json");
		if(file_lines==null || file_lines.size()<=0)
		{return font_files;}

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

	public static String readFileToString(String file_path)
	{
		String xhtml_string="";
		try
		{
			xhtml_string = new String(Files.readAllBytes(Paths.get(file_path)), Charset.forName("UTF-8"));
		}//try.
		catch(IOException ioe)
		{
			System.out.println("SEVERE: "+class_name+" IO Exception while trying to read xhtml file:\n"+ioe);
			return null;
		}//catch().

		return xhtml_string;
	}//readFileToString().


	private static PDFElementProperties readXHTML(String xhtml_string, HashMap<String, Font> custom_fonts, GlobalDocVars doc_vars) throws CustomException
	{
		CustomException.debug(class_name+".readXHTML(): ");//debug**

		//Remove <head> since it's not relevant.
		xhtml_string = Pattern.compile("<head>.*</head>", Pattern.DOTALL).matcher(xhtml_string).replaceAll("");

		Pattern pattern = Pattern.compile("<[^<>]+>|<!--", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(xhtml_string);

		int matches_count=0;
		int open_tags_count=0;
		HtmlData previous_match=null;
		HtmlData current_match=null;
		PDFElementProperties parent_element=null;
		boolean previous_tag_was_closing=false; //Keeps track of whether the current element is a leaf child or is still a parent.
		boolean comment_open=false;
		int comment_close_index=0;
		while(matcher.find())
		{
			if(matcher.start()<comment_close_index)
			{continue;}

			//CustomException.debug(" matcher.group(): "+matcher.group());//debug**

			if(matcher.group().startsWith("<!--") || matcher.group().contains("<!--"))
			{
			//	CustomException.debug(class_name+" comment matcher.group()="+matcher.group());//debug**
				comment_close_index = xhtml_string.indexOf("-->",matcher.start())+3;
			//	CustomException.debug(class_name+" comment start index="+matcher.start()+" comment close index="+comment_close_index);//debug**
				continue;
			}//if.

			if(matcher.group().startsWith("<!DOCTYPE"))
			{continue;}

			matches_count++;

			previous_match=current_match;
			current_match = new HtmlData(matcher.start(), matcher.end(), matcher.group());

		//	CustomException.log_level=CustomException.DEBUG;
			if(previous_match!=null)
			{lookForUnenclosedText(xhtml_string, Math.max(previous_match.end_index, comment_close_index), current_match.start_index, parent_element, custom_fonts);}
		//	CustomException.log_level=CustomException.INFO;

			if(current_match.is_opening)
			{
				open_tags_count++;

				if(parent_element!=null)
				{
					current_match.setParent(parent_element.html_data);
					//System.out.println("parent_element.html_data = "+parent_element.html_data);//debug**
				}//if.
				else
				{current_match.setParent(null);}//Even if parent is null this needs to be called since this is when styling data is extracted.
				//System.out.println("\n\nMatch sequence: "+current_match.matched_sequence);//debug**
				//System.out.println("current_match.parent: "+current_match.parent);//debug**
				//System.out.println("opening match styling: "+current_match.printStyling());//debug**

				PDFElementProperties current_element = new PDFElementProperties(doc_vars, parent_element, current_match);

				
				if(current_match.opening_and_closing)
				{
					open_tags_count--;
					current_element.closeTag();
					//Don't update 'parent_element' if this element is already closed (can't be parent to anything).
				}//if.
				else
				{
					parent_element=current_element;
				}//else.
			}//if.
			else //closing tag.
			{
				open_tags_count--;

			//	System.out.println("\n");//debug**
			
				if(!parent_element.getTag().equals(current_match.getTag()))
				{throw new CustomException(CustomException.SEVERE, class_name, "Closing tag '"+current_match.getTag()+"' doesn't match current element tag '"+parent_element.getTag()+"'!","Trying to extract data from xhtml.");}

				if(parent_element.parent==null)//This closing tag closes the Top Level Element.
				{
					parent_element.closeTag();
					break;
				}//if.

				parent_element.closeTag();
				parent_element=parent_element.parent;
			}//else.
		}//while.

		if(open_tags_count>0)
		{
			throw new CustomException(CustomException.SEVERE, class_name, "XHTML tags mismatch! Found more opening tags than closing tags.", "Trying to extract data from xhtml.");
		}//if.

		if(matches_count<=0)
		{System.out.println("Warning: "+class_name+" no matches found");}

		parent_element.calculateAbsolutePositions(null);

		return parent_element;
	}//readXHTML().

	private static void lookForUnenclosedText(String xhtml_string, int start_index, int end_index, PDFElementProperties parent_element, HashMap<String, Font>custom_fonts) throws CustomException
	{
		CustomException.debug(" lookForUnenclosedText():");//debug**
		String contained_text = xhtml_string.substring(start_index, end_index);
		if(contained_text.trim().isEmpty())
		{return;}

		String font_family = parent_element.html_data.font_family;
		double font_size = parent_element.getFontSize();

		Font element_font = getElementFont(parent_element, custom_fonts);

		String[] text_split = contained_text.trim().replaceAll(" {2,}"," ").split(" ");
		String[][] text_words = new String[text_split.length][];
		String word="";
		double total_words_width = 0;//debug**
		for(int wi=0; wi<text_split.length; wi++)
		{
		//	if(wi<(text_split.length-1))
		//	{word=text_split[wi]+" ";}
		//	else
		//	{word=text_split[wi];}
			word=text_split[wi]+" ";


			double width = element_font.getStringBounds(word, dummy_frc).getWidth();
			if(wi==0 && parent_element.getTag().equals("li"))
			{
				width += font_size+(space_m_sizing*font_size);//Leave space for the bullet point
				word="&bull;"+word;
			}//if.
			text_words[wi] = new String[] {word, String.valueOf(width)};

			total_words_width+=width;//debug**
		}//for(word).

		CustomException.debug(" total_words_width: "+total_words_width);//debug**

		try
		{parent_element.setText(text_words);}
		catch(CustomException ce)
		{
			System.out.println("EXCEPTION while trying to setText");//debug**
			if(ce.severity==CustomException.SEVERE)
			{throw ce;}
			
			ce.writeLog(log);
		}//catch().

	}//lookForUnenclosedText().

	private static void generateImage(LinkedList<PDFElementProperties> flattened_elements, String image_name, HashMap<String, Font>custom_fonts, GlobalDocVars doc_vars) throws CustomException
	{
		int page_width = (int)doc_vars.getPageWidth();
		int page_height = (int)doc_vars.getPageHeight();

		CustomException.writeLog(CustomException.INFO, null, class_name+".generateImage(): page_width:"+page_width+" page_height:"+page_height);//INFO

		//Create a blank image
		BufferedImage output_image = new BufferedImage(page_width, page_height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = output_image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

		//Draw background
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, page_width, page_height);

		for(PDFElementProperties element: flattened_elements)
		{
			if(element.getWidth()<=0 || element.getHeight()<=0)
			{continue;}

			int lx = (int)element.absolute_left;
			int ty = (int)element.absolute_top;
			int rx = (int)element.absolute_right;
			int by = (int)element.absolute_bottom;

			int width = rx-lx;
			int height = by-ty;

			String tag = element.getTag();
			if(tag.equals("img") && element.image!=null)
			{
				graphics.drawImage(element.image, lx, ty, width, height, null);
				drawBorders(element, graphics);
			}//if.
			else if(tag.equals("text"))
			{
				graphics.setColor(element.getColor());
				Font element_font = getElementFont(element, custom_fonts);
				graphics.setFont(element_font);
				CustomException.debug(" text element parent.tag: "+element.parent.parent.getTag());//debug**
				int x_offset=0;
				if(element.getText().startsWith("&bull;"))//List items
				{
					drawBullet(element, graphics);
					x_offset=(int)(element.getFontSize()*0.75 + element.getFontSize()*space_m_sizing);//See 'drawBullet()' method for why it's like this.
				}//if.
				graphics.drawString(element.getText().replace("&bull;",""), lx+x_offset, by);
			}//else if.
			else
			{
				graphics.setColor(element.getBackgroundColor());
				graphics.fillRect(lx, ty, width, height);//x,y,width,height.
				drawBorders(element, graphics);
			}//else.

		}//for(element).
		graphics.dispose();

		try
		{ImageIO.write(output_image, "png", new File(image_name));}
		catch(IOException ioe)
		{throw new CustomException(CustomException.SEVERE, class_name, "Trying to write image to file", ioe);}

	}//generateImage().

	private static void drawBorders(PDFElementProperties element, Graphics2D graphics)
	{
	/*	int top = (int)(element.absolute_top + (int)element.getBorderWidth("t")/2);
		int left = (int)(element.absolute_left + (int)element.getBorderWidth("l")/2);
		int right = (int)(element.absolute_right - Math.ceil(element.getBorderWidth("r")/2));
		int bottom = (int)(element.absolute_bottom - Math.ceil(element.getBorderWidth("b")/2));*/

		if(element.getBorderWidth("t")>0)
		{
			double border_width = element.getBorderWidth("t");
			int top = (int)(element.absolute_top + (int)border_width/2);
			int right = (int)(element.absolute_right - Math.ceil(border_width/2));
			int left = (int)(element.absolute_left + (int)border_width/2);
			graphics.setColor(element.getBorderColor("t"));
			graphics.setStroke(new BasicStroke((float)border_width));
			graphics.drawLine(left, top, right, top);
		}//if.

		if(element.getBorderWidth("r")>0)
		{
			double border_width = element.getBorderWidth("r");
			int top = (int)(element.absolute_top + (int)border_width/2);
			int right = (int)(element.absolute_right - Math.ceil(border_width/2));
			int bottom = (int)(element.absolute_bottom - Math.ceil(border_width/2));
			graphics.setColor(element.getBorderColor("r"));
			graphics.setStroke(new BasicStroke((float)border_width));
			graphics.drawLine(right, top, right, bottom);
		}//if.

		if(element.getBorderWidth("b")>0)
		{
			double border_width = element.getBorderWidth("b");
			int right = (int)(element.absolute_right - Math.ceil(border_width/2));
			int bottom = (int)(element.absolute_bottom - Math.ceil(border_width/2));
			int left = (int)(element.absolute_left + (int)border_width/2);
			graphics.setColor(element.getBorderColor("b"));
			graphics.setStroke(new BasicStroke((float)border_width));
			graphics.drawLine(left, bottom, right, bottom);
		}//if.

		if(element.getBorderWidth("l")>0)
		{
			double border_width = element.getBorderWidth("l");
			int top = (int)(element.absolute_top + (int)border_width/2);
			int bottom = (int)(element.absolute_bottom - Math.ceil(border_width/2));
			int left = (int)(element.absolute_left + (int)border_width/2);
			graphics.setColor(element.getBorderColor("l"));
			graphics.setStroke(new BasicStroke((float)border_width));
			graphics.drawLine(left, bottom, left, top);
		}//if.
	}//drawBorders().

	private static void drawBullet(PDFElementProperties element, Graphics2D graphics)
	{
		int lx = (int)element.absolute_left;
		int ty = (int)element.absolute_top;
		
		double half_em = element.getFontSize()*0.5;
		double quarter_em = half_em*0.5;
		double bullet_x = lx+quarter_em;
		double bullet_y = ty+half_em;
		graphics.fillOval((int)bullet_x, (int)(bullet_y), (int)half_em, (int)half_em);
	}//drawBullet().

	private static Font getElementFont(PDFElementProperties element, HashMap<String, Font>custom_fonts)
	{
		CustomException.debug("getElementFont():");//debug**

		String font_family = element.getFontFamily();
		String lower_case_family = font_family.toLowerCase();
		int font_weight = interpretFontWeight(element.getFontWeight());
		String text_decoration = element.getTextDecoration();
		boolean italic = text_decoration.contains("italic");
		if(font_weight>=1 && !lower_case_family.contains("bold") && custom_fonts.containsKey(font_family+"-Bold"))
		{
			font_family+="-Bold";
			font_weight=0;//Might need to change this.
		}//if.
		if(italic && !lower_case_family.contains("italic") && custom_fonts.containsKey(font_family+"-Italic"))
		{
			font_family+="-Italic";
			italic=false;//Might need to change this.
		}//if.

	//	CustomException.debug(" text_decoration: "+text_decoration+" color: "+element.getColor()+" style: "+font_style);//debug**
		CustomException.debug(" font_size:"+element.getFontSize()+" font_family: "+font_family+" font_weight: "+font_weight+" text:"+element.getText());//debug**

		Font base_font = null;
		if(custom_fonts.containsKey(font_family))
		{
			base_font = custom_fonts.get(font_family);
			CustomException.debug(" using custom font '"+font_family+"'!");//debug**
		}//if.
		else
		{
			base_font = new Font("Dialog", Font.PLAIN, (int)element.getFontSize());//default font.
			CustomException.debug(" using default font!");//debug**
		}//else

		Map<TextAttribute, Object> attributes = new java.util.HashMap<>(base_font.getAttributes());

		if(font_weight>=1)
		{attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);}
		
		if(italic)
		{attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);}

		if(text_decoration.contains("underline"))
		{attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);}

		attributes.put(TextAttribute.SIZE, element.getFontSize());

		Font element_font = new Font(attributes);

		return element_font;
	}//getElementFont().

	private static int interpretFontWeight(int css_weight)
	{
		if(css_weight>=700)
		{return Font.BOLD;}

		return Font.PLAIN;
	}//interpretFontWeight().

	private static void flattenElements(LinkedList<PDFElementProperties> flattened_elements, PDFElementProperties base_element)
	{
		flattened_elements.add(base_element);
		for(PDFElementProperties child: base_element.children)
		{
			flattenElements(flattened_elements, child);
		}//for(child).
		for(PDFElementProperties child: base_element.fixed_children_elements)
		{
			flattenElements(flattened_elements, child);
		}//for(child.)

	}//flattenElements().

	private static void generateExampleImage() throws CustomException
	{
		int width = 400;
		int height = 300;

		// Create a blank image
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();

		// Draw background
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, width, height);

		//Draw bordered rectangle
		graphics.setColor(Color.BLACK);             // Border color
		graphics.setStroke(new BasicStroke(2));     // Optional: border thickness
		graphics.drawRect(200, 50, 100, 100);

		//Fill in rectangle
		graphics.setColor(Color.LIGHT_GRAY);         // Fill color
		graphics.fillRect(200, 40, 100, 100);         // x, y, width, height

		//setStroke options
	/*	graphics.setStroke(new BasicStroke(
		    2f,                             // thickness
		    BasicStroke.CAP_ROUND,         // end cap style
		    BasicStroke.JOIN_MITER,        // corner join style
		    10f,                            // miter limit
		    new float[] {10f, 5f},          // dash pattern: 10px dash, 5px gap
		    0f                              // dash phase offset
		));*/


		// Draw something (e.graphics. a red circle)
		graphics.setColor(Color.RED);
		graphics.fillOval(50, 50, 100, 100);

		// Draw some text
		graphics.setColor(Color.BLACK);
		graphics.setFont(new java.awt.Font("SansSerif", Font.BOLD, 20));
		graphics.drawString("Hello Bitmap!", 50, 200);

		graphics.dispose();

		try
		{ImageIO.write(image, "png", new File("example.png"));}
		catch(IOException ioe)
		{throw new CustomException(CustomException.SEVERE, class_name, "Trying to write image to file", ioe);}
	}//generateExampleImage().


}//class HtmlToImageConverter.