
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


public class HtmlToPdfConverter extends HtmlConverter
{
	public static final String class_name = "HtmlToPdfConverter";
	public static final Logger log = Logger.getLogger(class_name);

	private static String[] builtinFonts = new String[] {"Courier","Courier-Bold","Courier-Oblique","Courier-BoldOblique","Helvetica","Helvetica-Bold","Helvetica-Oblique","Helvetica-BoldOblique","Symbol","Times-Roman","Times-Bold","Times-Italic","Times-BoldItalic","ZapfDingbats"};

	HashMap<String, BaseFont> custom_fonts = null;

	//Variables defined in parent class.
	//private GlobalDocVars doc_vars = new GlobalDocVars();
	//protected CssInliner css_inliner = null;
	//protected double space_m_sizing = 0.33;
	//protected LinkedList<PDFElementProperties> flattened_elements = null;


	public HtmlToPdfConverter(String base_path, String css_path, List<String[]> font_files, Double space_m_sizing, double output_width, double output_height)
	{
		setBasePath(base_path);//Used for finding location of things like images.
		log.debug(class_name+".main(): base_path: "+this.doc_vars.base_path);//debug**

		if(css_path!=null && !css_path.trim().isEmpty())
		{css_path = normalizePath(css_path);}

		log.info(class_name+" Reading css...");//debug**
		this.css_inliner = new CssInliner(css_path);

		this.doc_vars.setPageSize(output_width, output_height);

		loadFonts(font_files);

		if(space_m_sizing!=null)
		{this.space_m_sizing=space_m_sizing;}

	}//constructor().

	public void loadFonts(List<String[]> font_files)
	{
		if(this.custom_fonts==null)
		{this.custom_fonts = new HashMap<String, BaseFont>();}

		for(String[] name_and_file: font_files)
		{
			createBaseFont(name_and_file[0], name_and_file[1]);
		}//for(name_and_file).
		for(String font_name: builtinFonts)
		{
			createBaseFont(font_name, font_name);
		}//for(font_name);
	}//loadFonts().
	private void createBaseFont(String font_name, String font_file_name)
	{
	//	log.debug(class_name+".createBaseFont(): ");//debug**

		String font_encoding=BaseFont.IDENTITY_H;
		if(font_file_name.equals(font_name))//inbuilt font.
		{font_encoding=BaseFont.CP1252;}
		else
		{font_file_name = normalizePath(font_file_name);}

		try
		{
	//		log.debug(" creating font '"+font_name+"' from '"+font_file_name+"'...");//debug**
			BaseFont base_font = BaseFont.createFont(
	            font_file_name, // e.g., "fonts/Roboto-Regular.ttf"
	            font_encoding,     // for full Unicode support
	            BaseFont.EMBEDDED        // embed font in the PDF
	        );
	        this.custom_fonts.put(font_name, base_font);
	//        log.debug(" base_font:"
	//       	+ "\n\tencoding: "+base_font.getEncoding() 
	//        	+ "\n\tfontType: "+base_font.getFontType() 
	//        	+ "\n\tembedded: "+base_font.isEmbedded());//debug**
		}//try.
		catch(IOException ioe)
		{
			log.severe(class_name+" IO Exception while trying to createBaseFont '"+font_file_name+"':\n"+ioe);
		}//catch().
	}//createBaseFont().


	public void convert(String html_string, OutputStream output_stream)
	{

//INLINE CSS
		html_string = css_inliner.inline(html_string);		

//READ HTML
		this.flattened_elements = new LinkedList<PDFElementProperties>();
		try
		{
			PDFElementProperties top_element=readHTML(html_string);
			flattenElements(top_element);
		}//try.
		catch(HtmlConversionException ce)
		{
			ce.writeLog(log);
			if(ce.severity<HtmlConversionException.WARNING)
			{return;}
		}//catch().

//GENERATE PDF
		try
		{
			generatePdf(flattened_elements, output_stream);
		}//try.
		catch(HtmlConversionException hce)
		{
			hce.writeLog(log);
			if(hce.severity<HtmlConversionException.WARNING)
			{return;}
		}//catch().
	}//convert().


	private PDFElementProperties readHTML(String html_string) throws HtmlConversionException
	{
		log.debug(class_name+".readHTML(): ");//debug**

		//Remove <head> since it's not relevant.
		html_string = Pattern.compile("<head>.*</head>", Pattern.DOTALL).matcher(html_string).replaceAll("");

		Pattern pattern = Pattern.compile("<[^<>]+>|<!--", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(html_string);

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

			//log.debug(" matcher.group(): "+matcher.group());//debug**

			if(matcher.group().startsWith("<!--") || matcher.group().contains("<!--"))
			{
			//	log.debug(class_name+" comment matcher.group()="+matcher.group());//debug**
				comment_close_index = html_string.indexOf("-->",matcher.start())+3;
			//	log.debug(class_name+" comment start index="+matcher.start()+" comment close index="+comment_close_index);//debug**
				continue;
			}//if.

			if(matcher.group().startsWith("<!DOCTYPE"))
			{continue;}

			matches_count++;

			previous_match=current_match;
			current_match = new HtmlData(matcher.start(), matcher.end(), matcher.group());

			if(previous_match!=null)
			{lookForUnenclosedText(html_string, Math.max(previous_match.end_index, comment_close_index), current_match.start_index, parent_element);}

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

				PDFElementProperties current_element = new PDFElementProperties(this.doc_vars, parent_element, current_match);

				
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
				{throw new HtmlConversionException(HtmlConversionException.SEVERE, class_name, "Closing tag '"+current_match.getTag()+"' doesn't match current element tag '"+parent_element.getTag()+"'!","Trying to extract data from html.");}

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
			throw new HtmlConversionException(HtmlConversionException.SEVERE, class_name, "HTML tags mismatch! Found more opening tags than closing tags.", "Trying to extract data from html.");
		}//if.

		if(matches_count<=0)
		{System.out.println("Warning: "+class_name+" no matches found");}

		parent_element.calculateAbsolutePositions(null);

		return parent_element;
	}//readHTML().

	private void lookForUnenclosedText(String html_string, int start_index, int end_index, PDFElementProperties parent_element) throws HtmlConversionException
	{
		String contained_text = html_string.substring(start_index, end_index);
		if(contained_text.trim().isEmpty())
		{return;}

		String font_family = parent_element.html_data.font_family;
		double font_size = parent_element.html_data.font_size;

	//	HtmlConversionException.log_level=HtmlConversionException.DEBUG;
		BaseFont base_font=null;
		Font element_font = getElementFont(parent_element);
		if(element_font!=null && element_font.getBaseFont()!=null)
		{base_font = element_font.getBaseFont();}
		else
		{
			try
			{base_font=BaseFont.createFont();}//default font.
			catch(IOException ioe)
			{throw new HtmlConversionException(HtmlConversionException.SEVERE, class_name+".lookForUnenclosedText()", "Trying to create default font", ioe);}
		}//try.

	//	HtmlConversionException.log_level=HtmlConversionException.INFO;

    	String[] text_split = contained_text.trim().replaceAll(" {2,}"," ").split(" ");
    	String[][] text_words = new String[text_split.length][];
    	String word="";
    	for(int wi=0; wi<text_split.length; wi++)
    	{
    	//	if(wi<(text_split.length-1))
    	//	{word=text_split[wi]+" ";}
    	//	else
    	//	{word=text_split[wi];}
    		word=text_split[wi]+" ";

    		float width = base_font.getWidthPoint(word, (float)font_size);
    		if(wi==0 && parent_element.getTag().equals("li"))
    		{
    			width += base_font.getWidthPoint("\u2022 ", (float)font_size);
    			word="&bull;"+word;
    		}//if.
    		text_words[wi] = new String[] {word, String.valueOf(width)};
    	}//for(word).

		try
		{parent_element.setText(text_words);}
		catch(HtmlConversionException ce)
		{
			System.out.println("EXCEPTION while trying to setText");//debug**
			if(ce.severity==HtmlConversionException.SEVERE)
			{throw ce;}
			
			ce.writeLog(log);
		}//catch().
	}//lookForUnenclosedText().

	private void generatePdf(LinkedList<PDFElementProperties> flattened_elements, OutputStream pdf_output_stream) throws HtmlConversionException
	{
		float page_width = (float)this.doc_vars.getPageWidth();
		float page_height = (float)this.doc_vars.getPageHeight();

		log.info( class_name+".generatePdf(): page_width:"+page_width+" page_height:"+page_height);//INFO
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
				try
				{
					com.lowagie.text.Image image_element = com.lowagie.text.Image.getInstance(element.image, null);
					image_element.scaleToFit((float)element.width, (float)element.height);
					image_element.setAbsolutePosition(llx, lly);
					image_element.setBorderWidthTop((float)element.getBorderWidth("t"));
					image_element.setBorderWidthRight((float)element.getBorderWidth("r"));
					image_element.setBorderWidthBottom((float)element.getBorderWidth("b"));
					image_element.setBorderWidthLeft((float)element.getBorderWidth("l"));
					cb.addImage(image_element);
				}//try.
				catch(IOException ioe)
				{
					log.severe(class_name+".generatePdf(): IO Exception while trying to create Image Element:\n"+ioe);
					continue;
				}//catch().
			}//if.
			else if(tag.equals("text"))
			{
				Font element_font = getElementFont(element);
			//	log.debug(" text element parent.tag: "+element.parent.parent.getTag());//debug**
				if(element.parent.parent.getTag().equals("a"))//'.parent.parent' because 'text' elements are wrapped in a 'temptext' element.
				{
					Anchor link = new Anchor(element.getText(), element_font);
					link.setReference(element.parent.getHref());
					ColumnText.showTextAligned(cb, 0, link, llx, lly, 0);
				}
				else
				{
					Paragraph text_element = new Paragraph("", element_font);
					if(element.getText().startsWith("&bull;"))//List items
					{

						Chunk bullet = new Chunk("\u2022 ");
						text_element.add(bullet);
						text_element.add(element.getText().replace("&bull;",""));
					}//if.
					else
					{text_element.add(element.getText());}
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

	private Font getElementFont(PDFElementProperties element)
	{
		String font_family = element.getFontFamily();
		String lower_case_family = font_family.toLowerCase();
		int font_weight = interpretFontWeight(element.getFontWeight());
		String text_decoration = element.getTextDecoration();
		boolean italic = text_decoration.contains("italic");
		if(font_weight>=1 && !lower_case_family.contains("bold") && this.custom_fonts.containsKey(font_family+"-Bold"))
		{
			font_family+="-Bold";
			font_weight=0;//Might need to change this.
		}//if.
		if(italic && !lower_case_family.contains("italic") && this.custom_fonts.containsKey(font_family+"-Italic"))
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

	//	log.debug(" text_decoration: "+text_decoration+" color: "+element.getColor()+" style: "+font_style);//debug**
	//	log.debug(" font_size:"+element.getFontSize()+" font_family: "+font_family+" font_weight: "+font_weight+" text:"+element.getText());//debug**

		BaseFont base_font = null;
		if(this.custom_fonts.containsKey(font_family))
		{
			base_font = this.custom_fonts.get(font_family);
	//		log.debug(" using custom font '"+font_family+"'!");//debug**
		}//if.

		Font element_font = new Font(base_font, (float)element.getFontSize(), font_style, element.getColor());
		return element_font;
	}//getElementFont().

	private int interpretFontWeight(int css_weight)
	{
		if(css_weight>=700)
		{return Font.BOLD;}

		return Font.NORMAL;
	}//interpretFontWeight().


}//class HtmlToPdfConverter.