
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;

import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.Paths;

import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.StringReader;

import java.awt.Color;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Anchor;
import com.lowagie.text.Chunk;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.RectangleReadOnly;
import com.lowagie.text.Font;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.Image;


public class HtmlToPdfConverter
{
	public static final String class_name = "HtmlToPdfConverter";
	public static final Logger log = Logger.getLogger(class_name);

	//A4 Width = 595.0
	//A4 height = 842.0
	//Landscape mode:
	private static double global_page_width=842;
	private static double global_page_height=595;

	private static String[] builtinFonts = new String[] {"Courier","Courier-Bold","Courier-Oblique","Courier-BoldOblique","Helvetica","Helvetica-Bold","Helvetica-Oblique","Helvetica-BoldOblique","Symbol","Times-Roman","Times-Bold","Times-Italic","Times-BoldItalic","ZapfDingbats"};

	public static void main(String[] args)
	{
		CustomException.log_level=CustomException.INFO;

		if(args.length<=0 || args[0]==null || args[0].trim().length()<=0)
		{
			System.out.println("SEVERE: "+"Please specify a XHTML input file. Usage:\n\tjava HtmlToPdfConverter 'path/to/file.html' ['path/to/css.file']");
			return;
		}//if.
		String xhtml_path = args[0];
		String pdf_path = xhtml_path.replaceAll("\\.x?html",".pdf");
		String xhtml_string = readFileToString(xhtml_path);

		if(args.length>=2)//css file specified
		{
	//		CustomException.log_level=CustomException.DEBUG;

			System.out.println("Inlining css...");//debug**
			String css_path = args[1];
			CSSInliner css_inliner = new CSSInliner(css_path);
			xhtml_string = css_inliner.inline(xhtml_string);

			CustomException.log_level=CustomException.INFO;
		}//if.

		PDFElementProperties.setPageSize(global_page_width, global_page_height);

	//	CustomException.log_level=CustomException.DEBUG;
		HashMap<String, BaseFont> custom_fonts = loadFonts();
		CustomException.log_level=CustomException.INFO;

	//	CustomException.log_level=CustomException.DEBUG;
		LinkedList<PDFElementProperties> flattened_elements = new LinkedList<PDFElementProperties>();
		try
		{
			PDFElementProperties top_element=readXHTML(xhtml_string, custom_fonts);
			flattenElements(flattened_elements, top_element);
		}//try.
		catch(CustomException ce)
		{
			ce.writeLog(log);
			if(ce.severity<CustomException.WARNING)
			{return;}
		}//catch().
		CustomException.log_level=CustomException.INFO;


	//	CustomException.log_level=CustomException.DEBUG;
		try (OutputStream os = new FileOutputStream(pdf_path))
		{
			generatePdf(flattened_elements, os, custom_fonts);
		}//try.
		catch(IOException ioe)
		{
			System.out.println("SEVERE: "+class_name+"IO Exception while trying to generate PDF:\n"+ioe);
		}//catch().*/
		CustomException.log_level=CustomException.INFO;


		/*try (OutputStream os = new FileOutputStream("example.pdf"))
		{
			generateExamplePdf(xhtml_string, os);
		}//try.
		catch(IOException ioe)
		{
			System.out.println("SEVERE: "+class_name+"IO Exception while trying to generate PDF:\n"+ioe);
		}//catch().*/
	}//main().

	public static HashMap<String, BaseFont> loadFonts()
	{
		List<String[]> font_files = getFontConfigs();

		HashMap<String, BaseFont> custom_fonts = new HashMap<String, BaseFont>();
		for(String[] name_and_file: font_files)
		{
			createBaseFont(name_and_file[1], name_and_file[0], custom_fonts);
		}//for(name_and_file).
		for(String font_name: builtinFonts)
		{
			createBaseFont(font_name, font_name, custom_fonts);
		}//for(font_name);

		return custom_fonts;
	}//loadFonts().
	private static void createBaseFont(String font_file_name, String font_name, HashMap<String, BaseFont> custom_fonts)
	{
		CustomException.debug(class_name+".createBaseFont(): ");//debug**
		String font_encoding=BaseFont.IDENTITY_H;
		if(font_file_name.equals(font_name))//inbuilt font.
		{font_encoding=BaseFont.CP1252;};
		try
		{
			CustomException.debug(" creating font '"+font_name+"' from '"+font_file_name+"'...");//debug**
			BaseFont base_font = BaseFont.createFont(
	            font_file_name, // e.g., "fonts/Roboto-Regular.ttf"
	            font_encoding,     // for full Unicode support
	            BaseFont.EMBEDDED        // embed font in the PDF
	        );
	        custom_fonts.put(font_name, base_font);
	        CustomException.debug(" base_font:"
	        	+ "\n\tencoding: "+base_font.getEncoding() 
	        	+ "\n\tfontType: "+base_font.getFontType() 
	        	+ "\n\tembedded: "+base_font.isEmbedded());//debug**
		}//try.
		catch(IOException ioe)
		{
			log.severe(class_name+" IO Exception while trying to createBaseFont '"+font_file_name+"':\n"+ioe);
		}//catch().
	}//createBaseFont().
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
			String font_file = parts[1].replaceAll("[\"+,]","").trim();
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


	private static PDFElementProperties readXHTML(String xhtml_string, HashMap<String, BaseFont> custom_fonts) throws CustomException
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

			if(previous_match!=null)
			{lookForUnenclosedText(xhtml_string, Math.max(previous_match.end_index, comment_close_index), current_match.start_index, parent_element, custom_fonts);}

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

				PDFElementProperties current_element = new PDFElementProperties(parent_element, current_match);

				
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

	private static void lookForUnenclosedText(String xhtml_string, int start_index, int end_index, PDFElementProperties parent_element, HashMap<String, BaseFont>custom_fonts) throws CustomException
	{
		String contained_text = xhtml_string.substring(start_index, end_index);
		if(contained_text.trim().isEmpty())
		{return;}

		String font_family = parent_element.html_data.font_family;
		double font_size = parent_element.html_data.font_size;

	//	CustomException.log_level=CustomException.DEBUG;
		BaseFont base_font=null;
		Font element_font = getElementFont(parent_element, custom_fonts);
		if(element_font!=null && element_font.getBaseFont()!=null)
		{base_font = element_font.getBaseFont();}
		else
		{
			try
			{base_font=BaseFont.createFont();}//default font.
			catch(IOException ioe)
			{throw new CustomException(CustomException.SEVERE, class_name+".lookForUnenclosedText()", "Trying to create default font", ioe);}
		}//try.

	//	CustomException.log_level=CustomException.INFO;

    	String[] text_split = contained_text.trim().replaceAll(" {2,}"," ").split(" ");
    	String[][] text_words = new String[text_split.length][];
    	String word="";
    	for(int wi=0; wi<text_split.length; wi++)
    	{
    		if(wi<(text_split.length-1))
    		{word=text_split[wi]+" ";}
    		else
    		{word=text_split[wi];}

    		float width = base_font.getWidthPoint(word, (float)font_size);
    		text_words[wi] = new String[] {word, String.valueOf(width)};
    	}//for(word).

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

	private static void generatePdf(LinkedList<PDFElementProperties> flattened_elements, OutputStream pdf_output_stream, HashMap<String, BaseFont>custom_fonts) throws IOException
	{
		float page_width = (float)PDFElementProperties.getPageWidth();
		float page_height = (float)PDFElementProperties.getPageHeight();

		CustomException.writeLog(CustomException.INFO, null, class_name+".generatePdf(): page_width:"+page_width+" page_height:"+page_height);//INFO
		//Font font1 = FontFactory.getFont("Helvetica", 8, Font.BOLD, Color.BLACK);
		//Rectangle page_rectangle = PageSize.A4;
		Rectangle page_rectangle = new RectangleReadOnly(page_width, page_height);
		//System.out.println("page_rectangle:"
		//				+ "\n\t"+page_rectangle
		//				+ "\n\tborder_left="+page_rectangle.getBorderWidthLeft()
		//				+ "\n\tborder_top="+page_rectangle.getBorderWidthTop());//debug**

		Document document = new Document(page_rectangle, 0, 0, 0, 0);
		PdfWriter writer = PdfWriter.getInstance(document, pdf_output_stream);
		document.open();

		PdfContentByte cb = writer.getDirectContent();

		/*Rectangle random_box1 = new Rectangle(0.5f, 1f, 594.5f, 841.5f);
		random_box1.setBorder(15);
		random_box1.setBorderWidth(1);
		document.add(random_box1);*/
				
		//Phrase test_para1 = new Phrase("Test phrase M", default_font);
		//ColumnText.showTextAligned(cb, 0, test_para1, 2, 830, 0);

		for(PDFElementProperties element: flattened_elements)
		{
			if(element.getWidth()<=0 || element.getHeight()<=0)
			{continue;}

			float llx = (float)element.absolute_left;
			float lly = page_height-(float)element.absolute_bottom;
			float urx = (float)element.absolute_right;
			float ury = page_height-(float)element.absolute_top;

			String tag = element.getTag();
			if(tag.equals("img") && element.image!=null)
			{
				com.lowagie.text.Image image_element = com.lowagie.text.Image.getInstance(element.image, null);
				image_element.scaleToFit((float)element.width, (float)element.height);
				image_element.setAbsolutePosition(llx, lly);
				image_element.setBorderWidthTop((float)element.getBorderWidth("t"));
				image_element.setBorderWidthRight((float)element.getBorderWidth("r"));
				image_element.setBorderWidthBottom((float)element.getBorderWidth("b"));
				image_element.setBorderWidthLeft((float)element.getBorderWidth("l"));
				cb.addImage(image_element);
			}//if.
			else if(tag.equals("text"))
			{
				Font element_font = getElementFont(element, custom_fonts);
			//	CustomException.debug(" text element parent.tag: "+element.parent.parent.getTag());//debug**
				if(element.parent.parent.getTag().equals("a"))//'.parent.parent' because 'text' elements are wrapped in a 'temptext' element.
				{
					Anchor link = new Anchor(element.getText(), element_font);
					link.setReference(element.parent.getHref());
					ColumnText.showTextAligned(cb, 0, link, llx, lly, 0);
				}
				else
				{
					Phrase text_element = new Phrase(element.getText(), element_font);
					ColumnText.showTextAligned(cb, 0, text_element, llx, lly, 0);}
			}//else if.
			else
			{
				Rectangle rectangle = new Rectangle(llx, lly, urx, ury);
				rectangle.setBorderWidthTop((float)element.getBorderWidth("t"));
				rectangle.setBorderWidthRight((float)element.getBorderWidth("r"));
				rectangle.setBorderWidthBottom((float)element.getBorderWidth("b"));
				rectangle.setBorderWidthLeft((float)element.getBorderWidth("l"));
				rectangle.setBackgroundColor(element.getBackgroundColor());
				rectangle.setBorderColorTop(element.getBorderColor("t"));
				rectangle.setBorderColorRight(element.getBorderColor("r"));
				rectangle.setBorderColorBottom(element.getBorderColor("b"));
				rectangle.setBorderColorLeft(element.getBorderColor("l"));
				document.add(rectangle);
			}//else.

		}//for(element).


		document.close();

	}//generatePdf().

	private static Font getElementFont(PDFElementProperties element, HashMap<String, BaseFont>custom_fonts)
	{
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

		int font_style=0;
		if(font_weight>=1 && italic)
		{font_style+=Font.BOLDITALIC;}
		else if(font_weight>=1)
		{font_style+=Font.BOLD;}
		else if(italic)
		{font_style+=Font.ITALIC;}

		if(text_decoration.contains("underline"))
		{font_style+=Font.UNDERLINE;}

	//	CustomException.debug(" text_decoration: "+text_decoration+" color: "+element.getColor()+" style: "+font_style);//debug**
	//	CustomException.debug(" font_size:"+element.getFontSize()+" font_family: "+font_family+" font_weight: "+font_weight+" text:"+element.getText());//debug**

		BaseFont base_font = null;
		if(custom_fonts.containsKey(font_family))
		{
			base_font = custom_fonts.get(font_family);
	//		CustomException.debug(" using custom font '"+font_family+"'!");//debug**
		}//if.

		Font element_font = new Font(base_font, (float)element.getFontSize(), font_style, element.getColor());
		return element_font;
	}//getElementFont().

	private static int interpretFontWeight(int css_weight)
	{
		if(css_weight>=700)
		{return Font.BOLD;}

		return Font.NORMAL;
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

	private static void generateExamplePdf(String xhtml_string, OutputStream pdf_output_stream) throws IOException
	{
		Rectangle a4 = PageSize.A4;
		System.out.println("A4 height = "+a4.getHeight());//debug**
		System.out.println("A4 Width = "+a4.getWidth());//debug**
		//A4 height = 842.0
		//A4 Width = 595.0



		//Font font1 = FontFactory.getFont("Helvetica", 8, Font.BOLD, Color.BLACK);
		Rectangle page_rectangle = PageSize.A4;
		System.out.println("page_rectangle:"
						+ "\n\t"+page_rectangle
						+ "\n\tborder_left="+page_rectangle.getBorderWidthLeft()
						+ "\n\tborder_top="+page_rectangle.getBorderWidthTop());//debug**

		Document document = new Document(PageSize.A4, 0, 0, 0, 0);
		PdfWriter writer = PdfWriter.getInstance(document, pdf_output_stream);
		document.open();

		PdfContentByte cb = writer.getDirectContent();

		Rectangle random_box1 = new Rectangle(0.5f, 1f, 594.5f, 841.5f);
		random_box1.setBorder(15);
		random_box1.setBorderWidth(1);
		document.add(random_box1);
		Rectangle random_box2 = new Rectangle(20f,20f,574f, 821f);
		random_box2.setBorder(15);
		random_box2.setBorderWidth(2);
		//document.add(random_box2);
		
		//How to embed my own custom font.
		//BaseFont bf = BaseFont.createFont("fonts/YourFont.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
		//Font font = new Font(bf, 12);
		BaseFont base_font = BaseFont.createFont();
		Font default_font = new Font(base_font, 14, Font.NORMAL, Color.black);
				
		Phrase test_para1 = new Phrase("Test phrase M", default_font);// also a very long phrase that I am really hoping I can get to be longer than the width of the entire page in order to force a wrap-around");
				 //showTextAligned(PdfContentByte canvas, int alignment, Phrase phrase, float x, float y, float rotation)
		ColumnText.showTextAligned(cb, 0, test_para1, 2, 830, 0);
		
		Chunk test_para2 = new Chunk("Test Paragraph 2", default_font);
		//test_para2.setLeading(0);
		//ColumnText.showTextAligned(cb, 0f, test_para2, 0, 0, 0);


		//HTMLWorker htmlWorker = new HTMLWorker(document);
		//htmlWorker.parse(new StringReader(xhtml_string));
		document.close();

	}//generateExamplePdf().


}//class HtmlToPdfConverter.