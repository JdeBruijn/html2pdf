
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
		if(args.length<=0 || args[0]==null || args[0].trim().length()<=0)
		{
			System.out.println("SEVERE: "+"Please specify a XHTML input file. Usage:\n\tjava HtmlToPdfConverter 'path/to/file.xhtml'");
			return;
		}//if.
		String xhtml_path = args[0];
		String pdf_path = xhtml_path.replaceAll("\\.x?html",".pdf");
		String xhtml_string = readFileToString(xhtml_path);

		PDFElementProperties.setPageSize(global_page_width-10, global_page_height);

		String[][] custom_font_files = new String[][] {{"Open Sans","open-sans.regular.ttf"}};
		HashMap<String, BaseFont> custom_fonts = loadFonts(custom_font_files);

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


		try (OutputStream os = new FileOutputStream(pdf_path))
		{
			generatePdf(flattened_elements, os, custom_fonts);
		}//try.
		catch(IOException ioe)
		{
			System.out.println("SEVERE: "+class_name+"IO Exception while trying to generate PDF:\n"+ioe);
		}//catch().


		/*try (OutputStream os = new FileOutputStream("example.pdf"))
		{
			generateExamplePdf(xhtml_string, os);
		}//try.
		catch(IOException ioe)
		{
			System.out.println("SEVERE: "+class_name+"IO Exception while trying to generate PDF:\n"+ioe);
		}//catch().*/
	}//main().

	public static HashMap<String, BaseFont> loadFonts(String[][] font_files)
	{
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
		String font_encoding=BaseFont.IDENTITY_H;
		if(font_file_name.equals(font_name))//inbuilt font.
		{font_encoding=BaseFont.CP1252;};
		try
		{
			BaseFont base_font = BaseFont.createFont(
	            font_file_name, // e.g., "fonts/Roboto-Regular.ttf"
	            font_encoding,     // for full Unicode support
	            BaseFont.EMBEDDED        // embed font in the PDF
	        );
	        custom_fonts.put(font_name, base_font);
		}//try.
		catch(IOException ioe)
		{
			log.severe(class_name+" IO Exception while trying to createBaseFont '"+font_file_name+"':\n"+ioe);
		}//catch().
	}//createBaseFont().


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
		//System.out.println(class_name+".readXHTML(): xhtml_string="+xhtml_string);//debug**

		Pattern pattern = Pattern.compile("<[^<>]+>", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(xhtml_string);

		int matches_count=0;
		int open_tags_count=0;
		HtmlData previous_match=null;
		HtmlData current_match=null;
		PDFElementProperties parent_element=null;
		boolean previous_tag_was_closing=false; //Keeps track of whether the current element is a leaf child or is still a parent.
		while(matcher.find())
		{
			matches_count++;

			if(matcher.group().startsWith("<!DOCTYPE"))
			{continue;}

			previous_match=current_match;
			current_match = new HtmlData(matcher.start(), matcher.end(), matcher.group());

			if(previous_match!=null)
			{lookForUnenclosedText(xhtml_string, previous_match.end_index, current_match.start_index, parent_element, custom_fonts);}

			if(current_match.is_opening)
			{
				open_tags_count++;

				PDFElementProperties current_element = new PDFElementProperties(parent_element, current_match);

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

				System.out.println("\n");//debug**
			
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

		BaseFont base_font = null;
		if(custom_fonts.containsKey(font_family))
		{base_font=custom_fonts.get(font_family);}
        else
        {
			try
			{base_font=BaseFont.createFont();}//default font.
			catch(IOException ioe)
			{throw new CustomException(CustomException.SEVERE, class_name+".lookForUnenclosedText()", "Trying to create default font", ioe);}
		}//else.

    	String[] text_split = contained_text.trim().split(" ");
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

		//Font font1 = FontFactory.getFont("Helvetica", 8, Font.BOLD, Color.BLACK);
		//Rectangle page_rectangle = PageSize.A4;
		Rectangle page_rectangle = new RectangleReadOnly(page_width, page_height);
		System.out.println("page_rectangle:"
						+ "\n\t"+page_rectangle
						+ "\n\tborder_left="+page_rectangle.getBorderWidthLeft()
						+ "\n\tborder_top="+page_rectangle.getBorderWidthTop());//debug**

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
			float llx = (float)element.absolute_left;
			float lly = page_height-(float)element.absolute_bottom;
			float urx = (float)element.absolute_right;
			float ury = page_height-(float)element.absolute_top;

			String tag = element.getTag();
			if(tag.equals("img"))
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
				BaseFont base_font = BaseFont.createFont();
				Font default_font = new Font(base_font, (float)element.getFontSize(), Font.NORMAL, Color.black);
				if(element.parent.getTag().equals("a"))
				{
					default_font.setStyle(Font.UNDERLINE);
					default_font.setColor(Color.blue);
					Anchor link = new Anchor(element.getText(), default_font);
					link.setReference(element.parent.getHref());
					ColumnText.showTextAligned(cb, 0, link, llx, lly, 0);
				}
				else
				{
					Phrase text_element = new Phrase(element.getText(), default_font);
					ColumnText.showTextAligned(cb, 0, text_element, llx, lly, 0);}
			}//else if.
			else
			{
				Rectangle rectangle = new Rectangle(llx, lly, urx, ury);
				rectangle.setBorderWidthTop((float)element.getBorderWidth("t"));
				rectangle.setBorderWidthRight((float)element.getBorderWidth("r"));
				rectangle.setBorderWidthBottom((float)element.getBorderWidth("b"));
				rectangle.setBorderWidthLeft((float)element.getBorderWidth("l"));
				document.add(rectangle);
			}//else.

		}//for(element).


		document.close();

	}//generatePdf().

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

	public static <T>T[] copyArray(T[] original_arr)
	{
		T[] new_arr = (T[])new Object[original_arr.length];
		for(int index=0; index<original_arr.length; index++)
		{new_arr[index]=original_arr[index];}

		return new_arr;
	}//copyArray().
	

}//class HtmlToPdfConverter.