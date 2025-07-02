
import java.util.logging.Logger;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.regex.Pattern;



import java.io.File;
import java.io.IOException;
import java.net.URL;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;


public class PDFElementProperties
{
	public static final String class_name="PDFElementProperties";
	public static final Logger log = Logger.getLogger(class_name);


//STATIC
	private static double page_width=0;
	private static double page_height=0;

	private static double text_left_margin=0;
	private static double text_right_margin=0;
	private static double text_margin_b_fraction=0.25;
	private static double text_margin_t_fraction=0.0;

	private static LinkedList<PDFElementProperties> page_sized_elements = null;

	public static double internal_elements_height=0;//Total height of internal elements. Used to work out page height.

	public static void setPageSize(double width, double height)
	{
		page_width=width;
		page_height=height;
	}//setPageSize().

	public static void addPageSizedElement(PDFElementProperties element)
	{
		if(page_sized_elements==null)
		{page_sized_elements=new LinkedList<PDFElementProperties>();}

		page_sized_elements.add(element);
	}//addPageSizedElement().

	public static double getPageWidth()
	{return page_width;}

	public static double getPageHeight()
	{return page_height;}



//OBJECT
	public HtmlData html_data=null;

	public PDFElementProperties parent=null;
	public LinkedList<PDFElementProperties> fixed_children_elements = new LinkedList<PDFElementProperties>(); //Elements with 'position'='fixed'.
	//public LinkedList<PDFElementProperties> left_children_elements = new LinkedList<PDFElementProperties>(); //Elements with 'float_side'='left'.
	//public LinkedList<PDFElementProperties> right_children_elements = new LinkedList<PDFElementProperties>(); //Elements with 'float_side'='right'.
	public LinkedList<PDFElementProperties> text_elements = new LinkedList<PDFElementProperties>();
	public LinkedList<PDFElementProperties> children = new LinkedList<PDFElementProperties>();//All (not 'fixed') children in the order they were added.

	//public String box_sizing="border-box"; //For this version all sizing is expected to be 'border-box'.

	protected String tag=null;//div/span/a/etc... Always in lowercase.

	public boolean is_closed=false;

	public boolean width_calculated=false;

	public double absolute_top=-1;
	public double absolute_left=-1;
	public double absolute_right=-1;
	public double absolute_bottom=-1;


	//These are all relative to this.parent.getMinChildTop().
	protected double top=-1;//-1=='unset'
	protected double left=-1;//-1=='unset'
	protected double right=-1;//-1=='unset'
	protected double bottom=-1;//-1=='unset'

	public double lowest_child_bottom=0; //Keeps track of the lowest bottom edge of the children.
	public double greatest_width=0; //Keeps track of the widest row of children.
	

//Dimensions.
	//'width' is used to define how the 'box' is drawn, not the space for internal elements.
	// Use 'getMaxPossibleChildWidth()' for the space for internal elements.
	public double width=-1; //-1=='auto'.
	private double max_width=-1;//-1=='unset'.
	public double min_width=0;
	public double height=-1; //-1=='auto'.
	public double max_height=-1;//-1=='unset';
	public double min_height=0;

	public BufferedImage image=null;

	protected String[][] text_words=null;//These are the words as read from the xhtml file. Format:[["word1","word1_length(double)"],[...],...]
	protected String text_string=null;//This is the substring of text that's been put into it's own sub-element. 


//Child placement variables
	private Double internal_width = 0.0;
	private Double row_top = 0.0;
	private Double row_rightmost_left = 0.0;
	private Double row_leftmost_right = internal_width;
	private Double row_lowest_bottom = 0.0;//Keep track of the lowest bottom edge for this row. Defines entire row size.


	public PDFElementProperties()
	{}//null constructor().

	public PDFElementProperties(PDFElementProperties parent, HtmlData html_data) throws CustomException
	{
		//System.out.println("PDFElementProperties(): ");//debug**
		this.html_data=html_data;
		this.tag=html_data.getTag();

		if(parent!=null)
		{
			//copyParentVars(parent);
			setParent(parent);
		}//if.
		else
		{
			this.top=0;
			this.left=0;
		}//else.

		if(this.tag.equals("img"))
		{readImage(this.html_data.getSrc());}

		firstDownwardPass();
	}//constructor().

/*	I think this is handled better in the HtmlData class...
	private void copyParentVars(PDFElementProperties parent)
	{
		this.display=parent.getDisplay();
		this.float_side=parent.getFloatSide();
		this.right=parent.right;
		this.bottom=parent.bottom;
		this.left=parent.left;

		this.width=parent.width;
		this.height=parent.height;

		this.border_top_width=parent.border_top_width;
		this.border_right_width=parent.border_right_width;
		this.border_bottom_width=parent.border_bottom_width;
		this.border_left_width=parent.border_left_width;
		this.border_top_color=parent.border_top_color;
		this.border_right_color=parent.border_right_color;
		this.border_bottom_color=parent.border_bottom_color;
		this.border_left_color=parent.border_left_color;

		this.background_color=parent.background_color;

		this.getMargin("top")=parent.getMargin("top");
		this.getMargin("right")=parent.getMargin("right");
		this.getMargin("bottom")=parent.getMargin("bottom");
		this.getMargin("left")=parent.getMargin("left");

		this.getPadding("top")=parent.getPadding("top");
		this.getPadding("right")=parent.getPadding("right");
		this.getPadding("bottom")=parent.getPadding("bottom");
		this.getPadding("left")=parent.getPadding("left");

		this.font_size=parent.font_size;
		this.font_family=parent.font_family;

	}//copyParentVars().*/


	//This contains the list of tasks to be done on each element
	// on the first downward travers of all the elements.
	//Runs as elements are first created.
	private void firstDownwardPass()
	{
		calculateMaxWidth();

		calculateSetWidth();

		calculateMaxHeight();

	}//firstDownwardPass().

	public void calculateAbsolutePositions(PDFElementProperties parent)
	{
	//	System.out.println("\ncalculateAbsolutePositions(): matched_sequence: "+this.html_data.matched_sequence);//debug**
		double parent_top=0;
		double parent_left=0;
		if(parent!=null)
		{
			//System.out.println(" parent.matched_sequence: "+parent.html_data.matched_sequence);//debug**
			parent_top=parent.absolute_top+parent.getMinChildTop();
			parent_left=parent.absolute_left+parent.getMinChildLeft();
			//System.out.println(" parent.width: "+parent.width+" parent.padding_left: "+parent.getPadding("l")+" parent.border_right: "+parent.getBorderWidth("l"));//debug**
		}//if.
		//System.out.println(" parent_top: "+parent_top+" this.top: "+this.top);//debug**
		//System.out.println(" parent_left: "+parent_left+" this.left: "+this.left);//debug**
		this.absolute_top=parent_top+this.top;
		this.absolute_left=parent_left+this.left;
		this.absolute_right=this.absolute_left+this.width;
		this.absolute_bottom=this.absolute_top+this.height;
		
	//	System.out.println(this.printAbsoluteLocationData());//debug**
	//	System.out.println(this.printRelativeLocationData());//debug**

		if(this.absolute_bottom>page_height)
		{page_height=this.absolute_bottom;}

		for(PDFElementProperties child: this.children)
		{
			if(child.getTag().equals("temptext"))
			{
				for(PDFElementProperties text_child: child.children)
				{text_child.calculateAbsolutePositions(this);}
			}//if().
			else
			{child.calculateAbsolutePositions(this);}
		}//for(child).
	}//calculateAbsolutePositions().

	protected double getMinChildTop()
	{
		if(this.top==-1)
		{
			System.out.println("Warning: "+class_name+".getMinChildTop(): this.top is unset, can't calculate getMinChildTop!");
			return -1;
		}//if.

		return this.getBorderWidth("t")+this.getPadding("t");
	}//getMinChildTop().

	protected double getMinChildLeft()
	{
		if(this.left==-1)
		{
			System.out.println("Warning: "+class_name+".minChildLeft(): this.top is unset, can't calculate minChildLeft!");
			return -1;
		}//if.

		return this.getBorderWidth("l")+this.getPadding("l");
	}//minChildLeft().


	//Calculated based on the minimum of html_data.max_width and parent.max_width.
	protected void calculateMaxWidth()
	{
	//	System.out.println("\ncalculateMaxWidth():");//debug**
	//	System.out.println(" sequence: "+this.html_data.matched_sequence);//debug**

		if(this.parent==null)//Top level element.
		{this.max_width=page_width;}
		else
		{
			this.max_width = this.parent.getMaxPossibleChildWidth();
			this.max_width = this.max_width-this.getMargin("l",this.max_width)-this.getMargin("r",this.max_width);
		}//else.


		String style_max_width_str = this.html_data.max_width;
		double style_max_width = 0;
		if(style_max_width_str.contains("%"))//Percentage.
		{
			style_max_width_str=style_max_width_str.replaceAll("%","");
			style_max_width = (this.max_width*Double.parseDouble(style_max_width_str))/100;
		}//if.
		else if(style_max_width_str.equals("-1"))//Unset.
		{style_max_width=this.max_width;}
		else//Static
		{style_max_width=Double.parseDouble(style_max_width_str);}

		this.max_width=Math.min(this.max_width, style_max_width);

	//	System.out.println(" this.max_width: "+this.max_width);//debug**

		//this.max_width=this.max_width-this.getMargin("l",this.max_width)-(this.getBorderWidth("l")/2)-(this.getBorderWidth("r")/2)-this.getMargin("r",this.max_width);
	}//calculateMaxWidth().

	protected void calculateMaxHeight()
	{
		if(parent!=null)
		{this.max_height=parent.getMaxPossibleChildHeight();}
		//Else: default 'max_height' is '-1' (unset). See variable declaration up top.

		double style_max_height=0;
		String style_max_height_str=this.html_data.max_height;
		if(style_max_height_str!=null)
		{
			if(style_max_height_str.equals("-1"))//-1=='auto'.
			{style_max_height=this.max_height;}
			if(style_max_height_str.contains("%") && this.max_height>-1)//Percentage. No point finding percentage height if parent height is 'auto'.
			{
				double percentage_height=Double.parseDouble(style_max_height_str.replaceAll("%",""));
				style_max_height = (this.max_height*percentage_height)/100;
			}//else.
			else
			{style_max_height=Integer.parseInt(style_max_height_str);}
		}//if.

		if(this.max_height==-1 && style_max_height>-1)
		{this.max_height=style_max_height;}
		else
		{this.max_height=Math.min(this.max_height, style_max_height);}

		//this.max_height=this.max_height-this.getMargin("t",this.max_height)-(this.getBorderWidth("l")/2)-(this.getBorderWidth("r")/2)-this.getMargin("b",this.max_height);
		this.max_height=this.max_height;
	}//calculateMaxHeight().

	//If width is static or a percentage then calculate it here as the object is created.
	protected void calculateSetWidth()
	{
		if(this.width_calculated)
		{return;}

	//	System.out.println("calculateSetWidth(): ");//debug**
	//	System.out.println(" matched_sequence: "+this.html_data.matched_sequence);//debug**
	//	System.out.println(" style_width: "+this.html_data.width);//debug**
	//	System.out.println(" html_data: "+html_data.printStyling());//debug**

		String style_width = this.html_data.width;

		if(style_width.equals("-1"))//This will be calculated in 'calculateAutoWidth()' when the object is closed.
		{
			this.width=-1;
			//Get width from image.
			if(this.getTag().equals("img") && this.image!=null)//We grab the image on element creation so we know its width.
			{
				this.width = this.image.getWidth()+this.getBorderWidth("l")+this.getPadding("l")+this.getPadding("r")+this.getBorderWidth("r");
			}//if.
			//System.out.println(class_name+".calculateSetWidth(): style_width==-1, returning.");//debug**
		}//if.
		else if(style_width.endsWith("%"))//Percentage.
		{
			double percentage = Double.parseDouble(style_width.replaceAll("%",""));
			if(this.parent!=null)
			{
				this.parent.getMaxPossibleChildWidth();
				this.width = (this.parent.getMaxPossibleChildWidth()*percentage)/100;
			}//if.
			else
			{this.width = (page_width*percentage)/100;}
			//System.out.println(" width: "+this.width);//debug**
		}//else if.
		else//Fixed size.
		{this.width = Double.parseDouble(style_width);}


	//	System.out.println(" width: "+this.width);//debug**

		if(this.width!=-1)//Width is fixed, won't be changed from here on.
		{
			this.max_width = this.getMaxWidth();//Called method to ensure 'max_width' has been calculated.
			if(this.width>this.max_width && this.max_width>0)//'width' can't be greater than 'max_width'.
			{this.width = this.max_width;}
			this.max_width=this.width;//If width is fixed then max_width is effectively the same as width.
		}//if.

		this.width_calculated=true;
	}//calculateSetWidth().


	protected double getMaxPossibleChildWidth()
	{
		//System.out.println("getMaxPossibleChildWidth():");//debug**
		//System.out.println(" this.width="+this.width);//debug**
		double internal_width=this.width;
		if(internal_width==-1)//'auto'
		{internal_width=this.max_width;}

		internal_width = internal_width-this.getBorderWidth("l")-this.getPadding("l",internal_width)-this.getPadding("r",internal_width)-this.getBorderWidth("r");
		return internal_width;
	}//getMaxPossibleChildWidth().

	protected double getMaxPossibleChildHeight()
	{
		double internal_height = this.height;
		if(internal_height==-1)//'auto'
		{internal_height=this.max_height;}

		if(internal_height==-1)//'unset'
		{return -1;}

		internal_height = internal_height-this.getBorderWidth("t")-this.getPadding("t",internal_height)-this.getPadding("b",internal_height)-this.getBorderWidth("b");
		return internal_height;
	}//getMaxPossibleChildHeight().


	//Calculates and returns the width of the entire element, all borders and margins included.
	protected double getExternalWidth()
	{
		if(this.width==-1)
		{
			System.out.println("getExternalWidth(): matched_sequence: "+this.html_data.matched_sequence);//debug**
			System.out.println("SEVERE: "+class_name+".getExternalWidth(): this element's width has not been calculated yet!");
			return -1;
		}//if.

		return this.width+this.getMargin("l")+this.getMargin("r");
	}//getExternalWidth().

	protected double getExternalHeight()
	{
		//System.out.println("getExternalHeight(): ");//debug**
		//System.out.println(" matched_sequence: "+this.html_data.matched_sequence);//debug**

		if(this.height==-1)
		{
			System.out.println("SEVERE: "+class_name+".getExternalHeight(): this element's height has not been calculated yet!");
			return -1;
		}//if.

		return this.height+this.getMargin("t")+this.getMargin("b");
	}//getExternalHeight().

	protected double getFurthestInternalLeft()
	{
		return this.left+this.getMargin("l")+this.getBorderWidth("l")+this.getPadding("l");
	}//getFurthestLeft().

	protected double getFurthestInternalRight()
	{
		return this.left+this.getMaxWidth()-this.getBorderWidth("r")-this.getPadding("r");
	}//getFurthestInternalRight().


	//All the data for the element should be here now.
	// Element final sizing and positioning can now be done.
	public void closeTag()
	{
		if(this.is_closed)
		{return;}

		this.is_closed=true;
		//System.out.println("\n\ncloseTag(): matched_sequence="+this.html_data.matched_sequence);//debug**

		if(this.getTag().equals("temptext"))
		{return;}

		calculateAutoWidth();
		calculateHeight();
		//System.out.println(this.printRelativeLocationData());//debug**
	}//closeTag().

	protected void calculateAutoWidth()
	{
		//This next section is for working out auto-width.
		if(!this.is_closed)//Can't calculate 'auto' width until we know what's inside this element.
		{
			System.out.println("SEVERE: "+class_name+".calculateAutoWidth(): method called before element was closed!");
			return;
		}//if.

		//System.out.println(" children.size(): "+this.children.size());//debug**
		if(this.children.size()>0)
		{
			placeChildren();

			double max_row_width = this.greatest_width+this.getBorderWidth("l")+this.getPadding("l",this.greatest_width)+this.getPadding("r",this.greatest_width)+this.getBorderWidth("r");
			//System.out.println(" max_row_width: "+max_row_width);//debug**
			if(this.width==-1)//Finalise 'auto' width.
			{
				this.width = max_row_width;
				this.max_width = this.getMaxWidth();//Called method to ensure 'max_width' has been calculated.
				if(this.width>this.max_width && this.max_width>0)//'width' can't be greater than 'max_width'.
				{this.width = this.max_width;}
			}//if.
	//		System.out.println(" calculated width: "+this.width);//debug**
		}//if.
		else if(this.width==-1 && this.text_string==null)
		{
			this.width=0;
			System.out.println("Warning: "+class_name+".calculateAutoWidth(): has no children. Will be removed.");
		}//else.

		width_calculated=true;
	}//calculateAutoWidth().
	protected void placeChildren()
	{
		System.out.println("\nplaceChildren(): ");//debug**
		System.out.println(" this.width: "+this.width);//debug**
		this.internal_width = getMaxPossibleChildWidth();
		this.row_top = 0.0;
		this.row_rightmost_left = 0.0;
		this.row_leftmost_right = internal_width;
		this.row_lowest_bottom = 0.0;//Keep track of the lowest bottom edge for this row. Defines entire row size.


		for(PDFElementProperties child: this.children)
		{
			if(child.width==0 || child.height==0)
			{
				//System.out.println(" child has width: "+child.width+" height: "+child.height+" therefore won't show up so not placing it..");
				continue;
			}//if.

			if(child.getTag().equals("temptext"))
			{
				PDFElementProperties temptext_element = child;
				double line_height = temptext_element.html_data.font_size;//Might need to alter the '-2', it's kindof a thumb-suck.

				StringBuilder text_line = new StringBuilder();
				double existing_line_length = text_left_margin;
				for(String[] word_data: temptext_element.text_words)
				{
					double max_text_width = this.row_leftmost_right-this.row_rightmost_left;
					//System.out.println("max_text_width="+max_text_width);//debug**

					String word = word_data[0];
					double word_length = Double.parseDouble(word_data[1]);

					if(text_line.length()<=0)
					{
						existing_line_length += word_length;
						text_line.append(word_data[0]);
						//System.out.println("text_line="+text_line.toString());//debug**
						continue;
					}//if.

					
					if((existing_line_length+word_length)>=max_text_width)
					{
						try
						{
							PDFElementProperties text_element = createInternalTextElement(temptext_element, text_line.toString(), (existing_line_length-text_left_margin), line_height);
							text_element.closeTag();
							placeChild(text_element);
						}//try.
						catch(CustomException ce)
						{
							ce.setCodeDescription("Trying to create text_element");
							ce.writeLog(log);
						}//catch().

						text_line=new StringBuilder("  "+word);
						max_text_width=internal_width;
					}//if.
					else
					{
						text_line.append(word);
						existing_line_length += word_length;
					}//else.

					//System.out.println("text_line="+text_line.toString());//debug**
				}//for(word).

				//Append the last line.
				if(text_line.length()>0)
				{
					try
					{
						PDFElementProperties text_element = createInternalTextElement(temptext_element, text_line.toString(), (existing_line_length-text_left_margin), line_height);
						text_element.closeTag();
						placeChild(text_element);
					}//try.
					catch(CustomException ce)
					{
						ce.setCodeDescription("Trying to create text_element");
						ce.writeLog(log);
					}//catch().
				}//if.
				
			}//if(temptext).
			else
			{

				double child_external_width = child.getExternalWidth();
				double child_external_height = child.getExternalHeight();
				if(child_external_width<=0 || child_external_height<=0)
				{
					System.out.println("Warning: "+class_name+".placeChildren(): child dimensions<=0, width:'"+child_external_width+"', height:'"+child_external_height+"', skipping...");
					continue;
				}//if.

				placeChild(child);
			}//else.
			
			//System.out.println("placeChildren(): this.lowest_child_bottom="+this.lowest_child_bottom);//debug**
		}//for(child).
	}//placeChildren().
	private PDFElementProperties createInternalTextElement(PDFElementProperties text_parent, String text, double line_length, double line_height) throws CustomException
	{
		double margin_bottom = line_height*text_margin_b_fraction;
		double margin_top = line_height*text_margin_t_fraction;
		String text_styling = "<text style=\"display:inline-block; float:left; margin-left:"+text_left_margin+"; margin-top:"+margin_top+"; margin-bottom:"+margin_bottom+"; margin-right:"+text_right_margin+"; border:none; width:"+line_length+"; height:"+line_height+"; \" >";
		HtmlData text_html_data = new HtmlData(0, 0, text_styling);
		text_html_data.setParent(text_parent.html_data);
		PDFElementProperties new_text = new PDFElementProperties(text_parent, text_html_data);
		new_text.text_string=text;
		return new_text;
	}//createInternalTextElement().
	protected void placeChild(PDFElementProperties child)
	{
		System.out.println(" child matched_sequence: "+child.html_data.matched_sequence);//debug**
		System.out.println(" child.width="+child.width+" child.height="+child.height);//debug**
		double child_external_width = StaticStuff.roundDownTo(child.getExternalWidth(),2);
		double child_external_height = StaticStuff.roundDownTo(child.getExternalHeight(),2);

		String child_float_side = child.getFloatSide();

		System.out.println(class_name+" row_rightmost_left: "+row_rightmost_left);//debug**
		System.out.println(class_name+" row_leftmost_right: "+row_leftmost_right);//debug**
		System.out.println(class_name+" child_external_width: "+child_external_width);//debug**

		if(child_float_side.startsWith("r"))
		{
			this.greatest_width=Math.max(this.greatest_width, this.internal_width);//If any child is float=right parent is automatically max width.
			
			//start a new row.
			if((this.row_leftmost_right-child_external_width)<this.row_rightmost_left)
			{
				child.top=this.lowest_child_bottom+child.getMargin("t");
				child.left=this.internal_width-child.width;
				child.calculateRight();
				child.calculateBottom();

				this.row_top=this.lowest_child_bottom;
				this.row_rightmost_left = 0.0;
				this.row_leftmost_right = this.internal_width-child_external_width;
				this.lowest_child_bottom=Math.max(this.lowest_child_bottom, child.bottom+child.getMargin("b"));
			}//if.
			else//Just add to existing row.
			{
				child.top=this.row_top+child.getMargin("t");
				this.row_leftmost_right -= child_external_width;
				child.left=this.row_leftmost_right+child.getMargin("l");
				child.calculateRight();
				child.calculateBottom();
				this.lowest_child_bottom=Math.max(this.lowest_child_bottom, child.bottom+child.getMargin("b"));
			}//else.
		}//if.
		else //if(child_float_side.startsWith("l")). Float=left is the default.
		{
			//start a new row.
			if((child_external_width+this.row_rightmost_left)>this.row_leftmost_right)
			{
				System.out.println(" cew+rrl="+(child_external_width+this.row_rightmost_left)+" rlr="+this.row_leftmost_right);//debug**
				child.top=this.lowest_child_bottom+child.getMargin("t");
				child.left=0+child.getMargin("l");
				child.calculateRight();
				child.calculateBottom();

				this.greatest_width=Math.max(this.greatest_width, this.row_rightmost_left);
				//System.out.println(" left child new row. greatest_width: "+this.greatest_width);//debug**

				this.row_top=this.lowest_child_bottom;
				this.row_rightmost_left = child_external_width;
				this.row_leftmost_right = this.internal_width;
				this.lowest_child_bottom=Math.max(this.lowest_child_bottom, child.bottom+child.getMargin("b"));
			}//if.
			else//Just add to existing row.
			{					
				child.top=this.row_top;
				child.left=this.row_rightmost_left+child.getMargin("l");
				this.row_rightmost_left += child_external_width;
				this.greatest_width=Math.max(this.greatest_width, this.row_rightmost_left);
				//System.out.println(" left child. greatest_width: "+this.greatest_width);//debug**
				child.calculateRight();
				child.calculateBottom();
				this.lowest_child_bottom=Math.max(this.lowest_child_bottom, child.bottom+child.getMargin("b"));
			}//else.
		}//else.
	}//placeChild().

	protected double calculateImageHeight()
	{
		if(this.image==null)
		{
			log.severe(class_name+".calculateImageHeight(): this.image==null! Cannot calculate height.");
			return 0;
		}//if.

		double image_width = this.image.getWidth();
		double image_height = this.image.getHeight();

		double ratio = image_height/image_width;

		double placed_image_width = this.width-this.getBorderWidth("l")-this.getPadding("l")-this.getPadding("r")-this.getBorderWidth("r");
		placed_image_width = StaticStuff.roundDownTo(placed_image_width,2);

		double placed_image_height = StaticStuff.roundDownTo(ratio*placed_image_width, 2);

		return placed_image_height;

	}//calculateImageHeight().

	protected void calculateHeight()
	{
		//System.out.println("\ncalculateHeight(): ");//debug**
		//System.out.println(" matched_sequence: "+this.html_data.matched_sequence);//debug**
		//System.out.println(" html_data.height: "+this.html_data.height);//debug**

		String style_height = this.html_data.height;
		if(style_height.equals("-1"))
		{
			this.height=-1;
			if(this.getTag().equals("img") && this.image!=null)
			{
				this.height=calculateImageHeight();
				//System.out.println("IMAGE height: "+this.height);//debug**
			}//if.
		}//if.
		else if(style_height.endsWith("%"))
		{
			if(this.max_height<=-1)
			{
				System.out.println("WARNING: "+class_name+".calculateHeight(): Cannot calculate percentage height when parent height is not fixed!");
				this.height=-1;
			}//if.
			double percentage = Double.parseDouble(style_height.replaceAll("%",""));
			this.height = (percentage*this.max_height)/100;
		}//if.
		else
		{this.height = Double.parseDouble(style_height);}

		if(this.height!=-1)
		{
			this.height = this.height;
			this.max_height = this.getMaxHeight();//Called method to ensure 'max_height' has been calculated.
			if(this.height>this.max_height && this.max_height>0)//'height' can't be greater than 'max_height'.
			{this.height = this.max_height;}
			return;
		}//if.

		//This next section is for working out auto-height.
		if(!this.is_closed)//Can't calculate 'auto' height until we know what's inside this element.
		{
			System.out.println("Warning: "+class_name+".calculateHeight(): method called before element was closed!");
			return;
		}//if.

		//System.out.println(" lowest_child_bottom: "+this.lowest_child_bottom);//debug**

		double max_column_height = this.lowest_child_bottom+this.getBorderWidth("t")+this.getPadding("t")+this.getPadding("b")+this.getBorderWidth("b");
		//System.out.println(" this.max_height: "+this.max_height+" max_column_height: "+max_column_height);//debug**
		if(max_column_height>this.max_height && this.max_height>0)
		{this.height=this.max_height;}
		else
		{this.height=max_column_height;}

		//System.out.println(" height: "+this.height);//debug**

		//Global var
		internal_elements_height = Math.max(internal_elements_height, this.getExternalHeight());
	}//calculateHeight().

	protected void calculateRight()
	{
		if(this.left<=-1)
		{
			System.out.println("SEVERE: "+class_name+".calculateRight(): this.left not set yet!");
			return;
		}//if.
		if(this.width<=-1)
		{
			System.out.println("SEVERE: "+class_name+".calculateRight(): this.width not set yet!");
			return;
		}//if.

		this.right = this.left+this.width;
	}//calculateRight().

	protected void calculateBottom()
	{
		if(this.top<=-1)
		{
			System.out.println("SEVERE: "+class_name+".calculateBottom(): this.top not set yet!");
			return;
		}//if.
		if(this.height<=-1)
		{
			System.out.println("SEVERE: "+class_name+".calculateBottom(): this.height not set yet!");
			return;
		}//if.

		this.bottom = this.top+this.height;//+this.getPadding("t")+this.getPadding("b");
	//	System.out.println("calculateBottom(): matched_sequence="+this.html_data.matched_sequence);//debug**
	//	System.out.println("calculateBottom(): ");//debug**
	//	System.out.println(" location data: \n"+this.printRelativeLocationData());//debug**
	}//calculateBottom().

	private void readImage(String image_src) throws CustomException
	{
		if(this.image!=null)
		{
			System.out.println("Warning: "+class_name+".readImage(): this.image already defined, not overwriting.");
			return;
		}//if.

		if(this.image!=null)
		{throw new CustomException(CustomException.SEVERE, class_name, "Cannot set 'image' when 'text' is already defined.", "Trying to set image.");}

		if(image_src==null || image_src.trim().isEmpty())
		{throw new CustomException(CustomException.WARNING, class_name+".readImage()","Image src is not defined!", "Trying to read image");}
		
		if(image_src.startsWith("http"))
		{
			try
			{this.image = ImageIO.read(new URL(image_src));}
			catch(IOException ioe)
			{throw new CustomException(CustomException.SEVERE, class_name, "Trying to read image from URL", ioe);}
		}//if.
		else
		{
			try
			{this.image = ImageIO.read(new File(image_src));}
			catch(IOException ioe)
			{throw new CustomException(CustomException.SEVERE, class_name, "Trying to read image from File", ioe);}
		}//else.

	}//readImage().

	public void setText(String[][] words) throws CustomException
	{
		if(this.image!=null)
		{throw new CustomException(CustomException.SEVERE, class_name, "Cannot set 'text' when 'image' is already defined.", "Trying to set text.");}
		
		if(this.text_elements.size()>0)
		{System.out.println("Warning: "+class_name+" 'text' is already defined, overwriting...");}

		if(words.length<=0)
		{return;}

		//System.out.println(class_name+".setText(): words="+Arrays.deepToString(words));//debug**

		String temptext_styling = "<temptext style=\"display:inline-block; float:left; margin:none; border:none;\" >";	
		HtmlData temptext_html_data = new HtmlData(0, 0, temptext_styling);
		temptext_html_data.setParent(this.html_data);

		PDFElementProperties temptext_element = new PDFElementProperties(this, temptext_html_data);
		temptext_element.text_words = words;
		temptext_element.closeTag();
	}//setText().


	public void addChild(PDFElementProperties child_element)
	{
		if(child_element.parent!=null) //Remove previous Parent.
		{
			child_element.parent.removeChild(this);
		}//if.

		/*if(child_element.getTag().equals("text"))
		{this.text_elements.add(child_element);}
		else if(child_element.getPosition().equals("fixed"))
		{this.fixed_children_elements.add(child_element);}
		else if(child_element.getFloatSide().equals("left"))
		{this.left_children_elements.add(child_element);}
		else if(child_element.getFloatSide().equals("right"))
		{this.right_children_elements.add(child_element);}
		else
		{System.out.println("Warning: "+class_name+".addChild(): Failed to add child!");return;}*/
		if(child_element.getPosition().equals("fixed"))
		{this.fixed_children_elements.add(child_element);}
		else
		{
			this.children.add(child_element);

			if(child_element.getTag().equals("temptext"))
			{this.text_elements.add(child_element);}
		}//else.

		child_element.parent=this;
	}//addChild().

	public void removeChild(PDFElementProperties child)
	{
		/*int index = left_children_elements.indexOf(child);
		if(index>=0)
		{
			left_children_elements.remove(index);
			child.parent=null;
			return;
		}//if.

		index = right_children_elements.indexOf(child);
		if(index>=0)
		{
			right_children_elements.remove(index);
			child.parent=null;
			return;
		}//if.*/

		int index = children.indexOf(child);
		if(index>=0)
		{
			children.remove(index);
			child.parent=null;
			return;
		}//if.

		index = fixed_children_elements.indexOf(child);
		if(index>=0)
		{
			fixed_children_elements.remove(index);
			child.parent=null;
			return;
		}//if.
		
	}//removeChild().


	protected void setParent(PDFElementProperties parent)
	{
		parent.addChild(this);
	}//setParent().

	public String getPosition()
	{return this.html_data.position;}

	public String getFloatSide()
	{return this.html_data.float_side;}

	public String getDisplay()
	{return this.html_data.display;}

	public double getBorderWidth(String side)
	{
		if(side.startsWith("t"))
		{return this.html_data.border_top_width;}
		else if(side.startsWith("r"))
		{return this.html_data.border_right_width;}
		else if(side.startsWith("b"))
		{return this.html_data.border_bottom_width;}
		else if(side.startsWith("l"))
		{return this.html_data.border_left_width;}
		
		System.out.println("SEVERE: "+class_name+".getBorderWidth(): invalid side '"+side+"'!");
		return 0;
	}//getBorderWidth().

	public double getMargin(String side)
	{return getMargin(side, null);}
	public double getMargin(String side, Double length)
	{
		String value = "0";
		if(side.startsWith("t"))
		{value = this.html_data.margin_top;}
		else if(side.startsWith("r"))
		{value =  this.html_data.margin_right;}
		else if(side.startsWith("b"))
		{value =  this.html_data.margin_bottom;}
		else if(side.startsWith("l"))
		{value =  this.html_data.margin_left;}
		else
		{
			System.out.println("SEVERE: "+class_name+".getMargin(): invalid side '"+side+"'!");
			return 0;
		}//else.

		if(side.startsWith("t") || side.startsWith("b"))
		{
			if(!value.endsWith("%"))
			{return Double.parseDouble(value);}
			if(this.width==-1 && length==null)
			{
				System.out.println("SEVERE: "+class_name+".getMargin(): height is not defined! Can't retrieve margin-"+side+"="+value+"!");
				return 0;
			}//if.
			if(length==null)
			{length=this.width;}
			double percentage = Double.parseDouble(value.replaceAll("%",""));
			return (length*percentage)/100;
		}//if.
		else
		{
			if(!value.endsWith("%"))
			{return Double.parseDouble(value);}
			if(this.width==-1 && length==null)
			{
				System.out.println("SEVERE: "+class_name+".getMargin(): width is not defined! Can't retrieve margin-"+side+"="+value+"!");
				return 0;
			}//if.
			if(length==null)
			{length=this.width;}
			double percentage = Double.parseDouble(value.replaceAll("%",""));
			return (length*percentage)/100;
		}//else.
	}//getMargin().

	public double getPadding(String side)
	{return getPadding(side, null);}
	public double getPadding(String side, Double length)
	{
		String value = "0";
		if(side.startsWith("t"))
		{value = this.html_data.padding_top;}
		else if(side.startsWith("r"))
		{value =  this.html_data.padding_right;}
		else if(side.startsWith("b"))
		{value =  this.html_data.padding_bottom;}
		else if(side.startsWith("l"))
		{value =  this.html_data.padding_left;}
		else
		{
			System.out.println("SEVERE: "+class_name+".getMargin(): invalid side '"+side+"'!");
			return 0;
		}//else.

		if(side.startsWith("t") || side.startsWith("b"))
		{
			if(!value.endsWith("%"))
			{return Double.parseDouble(value);}
			if(this.height==-1 && length==null)
			{
				System.out.println("SEVERE: "+class_name+".getMargin(): height is not defined! Can't retrieve padding-"+side+"="+value+"!");
				return 0;
			}//if.
			if(length==null)
			{length=this.height;}
			double percentage = Double.parseDouble(value.replaceAll("%",""));
			return (length*percentage)/100;
		}//if.
		else
		{
			if(!value.endsWith("%"))
			{return Double.parseDouble(value);}
			if(this.width==-1 && length==null)
			{
				System.out.println("SEVERE: "+class_name+".getMargin(): width is not defined! Can't retrieve padding-"+side+"="+value+"!");
				return 0;
			}//if.
			if(length==null)
			{length=this.width;}
			double percentage = Double.parseDouble(value.replaceAll("%",""));
			return (length*percentage)/100;
		}//else.
	}//getPadding().

	public Color getBorderColor(String side)
	{
		if(side.startsWith("t"))
		{return this.html_data.border_top_color;}
		else if(side.startsWith("r"))
		{return this.html_data.border_right_color;}
		else if(side.startsWith("b"))
		{return this.html_data.border_bottom_color;}
		else if(side.startsWith("l"))
		{return this.html_data.border_left_color;}

		System.out.println("SEVERE: "+class_name+".getBorderColor(): invalid side '"+side+"'!");
		return Color.BLACK;
	}//getBorderColor().

	public double getWidth()
	{return this.width;}

	public double getHeight()
	{return this.height;}

	public double getMaxWidth()
	{
		if(this.max_width==-1)
		{calculateMaxWidth();}

		return this.max_width;
	}//getMaxWidth().

	public double getMaxHeight()
	{
		if(this.max_height==-1)
		{calculateMaxHeight();}

		return this.max_height;
	}//getMaxHeight().

	public String getText()
	{
		return this.text_string;
	}//getText().

	public double getFontSize()
	{
		return this.html_data.font_size;
	}//getFontSize().

	public String getHref()
	{
		return this.html_data.getHref();
	}//getHref().

	public Color getColor()
	{
		return this.html_data.fg_color;
	}//getColor().

	public Color getBackgroundColor()
	{
		return this.html_data.background_color;
	}//getBackgroundColor().

	public String getTag()
	{return this.tag;}

	public String printRelativeLocationData()
	{
		StringBuilder loc_data = new StringBuilder();
		loc_data.append("\ttop: "+this.top);
		loc_data.append("\n\tmargin_top:"+this.getMargin("t"));
		loc_data.append("\n\tmargin_bottom:"+this.getMargin("b"));
		loc_data.append("\n\tmargin_left:"+this.getMargin("l"));
		loc_data.append("\n\tmargin_right:"+this.getMargin("r"));
		loc_data.append("\n\tborder_top:"+this.getBorderWidth("t"));
		loc_data.append("\n\tborder_bottom:"+this.getBorderWidth("b"));
		loc_data.append("\n\tborder_left:"+this.getBorderWidth("l"));
		loc_data.append("\n\tborder_right:"+this.getBorderWidth("r"));
		loc_data.append("\n\tpadding_top:"+this.getPadding("t"));
		loc_data.append("\n\tpadding_bottom:"+this.getPadding("b"));
		loc_data.append("\n\tpadding_left:"+this.getPadding("l"));
		loc_data.append("\n\tpadding_right:"+this.getPadding("r"));
		loc_data.append("\n\theight: "+this.height);
		loc_data.append("\n\tbottom: "+this.bottom);
		loc_data.append("\n\tleft: "+this.left);
		loc_data.append("\n\twidth: "+this.width);
		loc_data.append("\n\tright: "+this.right);
		loc_data.append("\n\tmax_width: "+this.max_width);
		return loc_data.toString();
	}//printRelativeLocationData().

	public String printAbsoluteLocationData()
	{
		StringBuilder loc_data = new StringBuilder();
		loc_data.append("\tabsolute_top: "+this.absolute_top);
		loc_data.append("\n\theight: "+this.height);
		loc_data.append("\n\tabsolute_bottom: "+this.absolute_bottom);
		loc_data.append("\n\tabsolute_left: "+this.absolute_left);
		loc_data.append("\n\twidth: "+this.width);
		loc_data.append("\n\tabsolute_right: "+this.absolute_right);
		loc_data.append("\n\tchildren.size(): "+this.children.size());
		//loc_data.append("\n"+this.html_data.printStyling());
		return loc_data.toString();
	}//printAbsoluteLocationData().

}//PDFElementProperties().