
import java.util.logging.Logger;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.regex.Pattern;



import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;


public class PDFElementProperties
{
	public static final String class_name="PDFElementProperties";
	public static final Logger log = Logger.getLogger(class_name);


//STATIC
	private static double page_width=0;
	private static double page_height=0;

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


	//These are all relative to this.parent.minChildTop().
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
	protected String text_string=null;


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

		calculateMaxHeight();

	}//firstDownwardPass().

	public void calculateAbsolutePositions()
	{
		//System.out.println("\ncalculateAbsolutePositions(): matched_sequence: "+this.html_data.matched_sequence);//debug**
		double parent_top=0;
		double parent_left=0;
		if(this.parent!=null)
		{
			//System.out.println(" parent.matched_sequence: "+this.parent.html_data.matched_sequence);//debug**
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
		
		//System.out.println(this.printAbsoluteLocationData());//debug**

		if(this.absolute_bottom>page_height)
		{page_height=this.absolute_bottom;}

		for(PDFElementProperties child: this.children)
		{
			child.calculateAbsolutePositions();
		}//for(child).
	}//calculateAbsolutePositions().

	protected double getMinChildTop()
	{
		if(this.top==-1)
		{
			System.out.println("Warning: "+class_name+".minChildTop(): this.top is unset, can't calculate minChildTop!");
			return -1;
		}//if.

		return this.getPadding("t")+(this.getBorderWidth("t")/2);
	}//minChildTop().

	protected double getMinChildLeft()
	{
		if(this.left==-1)
		{
			System.out.println("Warning: "+class_name+".minChildLeft(): this.top is unset, can't calculate minChildLeft!");
			return -1;
		}//if.

		return this.getPadding("l")+(this.getBorderWidth("l")/2);
	}//minChildLeft().


	//Calculated based on the minimum of html_data.max_width and parent.max_width.
	protected void calculateMaxWidth()
	{
		//System.out.println("calculateMaxWidth():");//debug**
		//System.out.println(" sequence: "+this.html_data.matched_sequence);//debug**

		if(this.parent==null)//Top level element.
		{this.max_width=page_width;}
		else
		{this.max_width = this.parent.getMaxPossibleChildWidth();}


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

	//	this.max_width=Math.min(this.max_width, style_max_width);


		//System.out.println(" this.max_width: "+this.max_width);//debug**

		//this.max_width=this.max_width-this.getMargin("l",this.max_width)-(this.getBorderWidth("l")/2)-(this.getBorderWidth("r")/2)-this.getMargin("r",this.max_width);
		this.max_width=this.max_width-(this.getBorderWidth("l")/2)-(this.getBorderWidth("r")/2);
	}//calculateMaxWidth().

	protected void calculateMaxHeight()
	{
		if(parent!=null)
		{this.max_height=parent.getMaxPossibleChildHeight();}
		//Else: default 'max_height' is '-1' (unset). See variable declaration up top.

		double style_max_height=0;
		String style_max_height_str=style_max_height_str = this.html_data.max_width;
		if(style_max_height_str!=null)
		{
			if(style_max_height_str.equals("-1"))//-1=='auto'.
			{style_max_height=this.max_height;}
			if(!style_max_height_str.contains("%"))//Static.
			{style_max_height=Integer.parseInt(style_max_height_str);}
			else if(this.max_height>-1)//No point finding percentage height if parent height is 'auto'.
			{
				double percentage_height=Double.parseDouble(style_max_height_str.replaceAll("%",""));
				style_max_height = (this.max_height*percentage_height)/100;
			}//else.
		}//if.

		if(this.max_height==-1 && style_max_height>-1)
		{this.max_height=style_max_height;}
		else
		{this.max_height=Math.min(this.max_height, style_max_height);}

		//this.max_height=this.max_height-this.getMargin("t",this.max_height)-(this.getBorderWidth("l")/2)-(this.getBorderWidth("r")/2)-this.getMargin("b",this.max_height);
		this.max_height=this.max_height-(this.getBorderWidth("l")/2)-(this.getBorderWidth("r")/2);
	}//calculateMaxHeight().

	protected double getMaxPossibleChildWidth()
	{
		//System.out.println("getMaxPossibleChildWidth():");//debug**
		//System.out.println(" this.width="+this.width);//debug**
		double internal_width=this.width;
		if(internal_width==-1)//'auto'
		{internal_width=this.max_width;}

		internal_width = internal_width-(this.getBorderWidth("l")/2)-this.getPadding("l",internal_width)-this.getPadding("r",internal_width)-(this.getBorderWidth("r")/2);
		return internal_width;
	}//getMaxPossibleChildWidth().

	//Calculates and returns the width of the entire element, all borders and margins included.
	protected double getExternalWidth()
	{
		if(this.width==-1)
		{
			System.out.println("SEVERE: "+class_name+".getExternalWidth(): this element's width has not been calculated yet!");
			return -1;
		}//if.

		return this.width+this.getMargin("l")+(this.getBorderWidth("l")/2)+(this.getBorderWidth("r")/2)+this.getMargin("r");
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

		return this.height+this.getMargin("t")+(this.getBorderWidth("t")/2)+(this.getBorderWidth("b")/2)+this.getMargin("b");
	}//getExternalHeight().

	protected double getMaxPossibleChildHeight()
	{
		double internal_height = this.height;
		if(internal_height==-1)//'auto'
		{internal_height=this.max_height;}

		if(internal_height==-1)//'unset'
		{return -1;}

		internal_height = internal_height-(this.getBorderWidth("t")/2)-this.getPadding("t",internal_height)-this.getPadding("b",internal_height)-(this.getBorderWidth("b")/2);
		return internal_height;
	}//getMaxPossibleChildHeight().

	protected double getFurthestInternalLeft()
	{
		return this.left+this.getMargin("l")+this.getBorderWidth("l")+this.getPadding("l");
	}//getFurthestLeft().

	protected double getFurthestInternalRight()
	{
		return this.left+this.getMaxWidth()-(this.getBorderWidth("r")/2)-this.getPadding("r");
	}//getFurthestInternalRight().


	//All the data for the element should be here now.
	// Element final sizing and positioning can now be done.
	public void closeTag()
	{
		if(this.is_closed)
		{return;}

		this.is_closed=true;
		//System.out.println("\n\ncloseTag(): matched_sequence="+this.html_data.matched_sequence);//debug**

		calculateWidth();
		calculateHeight();
		//System.out.println(this.printRelativeLocationData());//debug**
	}//closeTag().

	protected void calculateWidth()
	{
		if(this.width_calculated)//This method must only be called once per object.
		{return;}

	//	System.out.println("\ncalculateWidth(): ");//debug**
	//	System.out.println(" matched_sequence: "+this.html_data.matched_sequence);//debug**
	//	System.out.println(" style_width: "+this.html_data.width);//debug**

		String style_width = this.html_data.width;
		if(style_width.endsWith("%"))//Percentage.
		{
			double percentage = Double.parseDouble(style_width.replaceAll("%",""));
			if(this.parent!=null)
			{this.width = (this.parent.getMaxPossibleChildWidth()*percentage)/100;}
			else
			{this.width = (page_width*percentage)/100;}
		}//else if.
		else if(style_width.equals("-1"))//Auto
		{
			this.width=-1;
			if(this.getTag().equals("img") && this.image!=null)//We grab the image on element creation so we know its width.
			{
				this.width = this.image.getWidth();
				//System.out.println("IMAGE width: "+this.width);//debug**
			}//if.
		}//else if.
		else//Fixed size.
		{this.width = Double.parseDouble(style_width);}

		if(this.width!=-1)
		{
			//this.width = this.width-this.getMargin("l")-(this.getBorderWidth("l")/2)-(this.getBorderWidth("r")/2)-this.getMargin("r");
			this.width = this.width-(this.getBorderWidth("l")/2)-(this.getBorderWidth("r")/2);
		}//if.

		this.max_width = this.getMaxWidth();//Called method to ensure 'max_width' has been calculated.
		if(this.width>this.max_width && this.max_width>0)//'width' can't be greater than 'max_width'.
		{this.width = this.max_width;}

		//System.out.println(" width: "+this.width);//debug**

		//if(this.width!=-1)
		//{return;}

		//This next section is for working out auto-width.
		if(!this.is_closed)//Can't calculate 'auto' width until we know what's inside this element.
		{
			System.out.println("Warning: "+class_name+".calculateWidth(): method called before element was closed!");
			return;
		}//if.


		if(this.children.size()>0)
		{
			placeChildren();

			double max_row_width = this.greatest_width+(this.getBorderWidth("l")/2)+this.getPadding("l",this.greatest_width)+this.getPadding("r",this.greatest_width)+(this.getBorderWidth("r")/2);
			//System.out.println(" max_row_width: "+max_row_width);//debug**
			if(this.width==-1)
			{this.width = Math.min(this.max_width, max_row_width);}
		}//if.
		else if(this.text_string==null && this.image==null)
		{
			System.out.println("Warning: "+class_name+".calculateWidth(): this element width=-1 and has no children. Will be removed.");
		}//else.

		width_calculated=true;
	}//calculateWidth().
	protected void placeChildren()
	{
	//	System.out.println("placeChildren(): ");//debug**
		this.internal_width = getMaxPossibleChildWidth();
		this.row_top = 0.0;
		this.row_rightmost_left = 0.0;
		this.row_leftmost_right = internal_width;
		this.row_lowest_bottom = 0.0;//Keep track of the lowest bottom edge for this row. Defines entire row size.

	//	System.out.println(" internal_width: "+internal_width);//debug**
	/*	for(PDFElementProperties text_child: this.text_elements)
		{
			double child_external_width = text_child.getExternalWidth();
			double child_external_height = text_child.getExternalHeight();
			if(child_external_width<=0 || child_external_height<=0)
			{
				System.out.println("Warning: "+class_name+".placeChildren(): child dimensions<=0, width:'"+child_external_width+"', height:'"+child_external_height+"', skipping...");
				continue;
			}//if.

			placeChild(text_child);
			
			//System.out.println("placeChildren(): this.lowest_child_bottom="+this.lowest_child_bottom);//debug**
		}//for(text_child).*/

		for(PDFElementProperties child: this.children)
		{
		//	if(child.getTag().equals("text"))
		//	{continue;}

			double child_external_width = child.getExternalWidth();
			double child_external_height = child.getExternalHeight();
			if(child_external_width<=0 || child_external_height<=0)
			{
				System.out.println("Warning: "+class_name+".placeChildren(): child dimensions<=0, width:'"+child_external_width+"', height:'"+child_external_height+"', skipping...");
				continue;
			}//if.

			placeChild(child);
			
			//System.out.println("placeChildren(): this.lowest_child_bottom="+this.lowest_child_bottom);//debug**
		}//for(child).
	}//placeChildren()
	protected void placeChild(PDFElementProperties child)
	{
		//System.out.println("child matched_sequence: "+child.html_data.matched_sequence);//debug**
		//System.out.println(" child.width="+child.width+" child.height="+child.height);//debug**
		double child_external_width = child.getExternalWidth();
		double child_external_height = child.getExternalHeight();

		String child_float_side = child.getFloatSide();

//		System.out.println(class_name+" row_rightmost_left: "+row_rightmost_left);//debug**
//		System.out.println(class_name+" row_leftmost_right: "+row_leftmost_right);//debug**
//		System.out.println(class_name+" child_external_width: "+child_external_width);//debug**

		if(child_float_side.startsWith("r"))
		{
			this.greatest_width=Math.max(this.greatest_width, this.internal_width);//If any child is float=right parent is automatically max width.
			
			//start a new row.
			if((this.row_leftmost_right-child_external_width)<this.row_rightmost_left)
			{
				child.top=this.lowest_child_bottom+child.getMargin("t")+(child.getBorderWidth("t")/2);
				child.left=this.internal_width-child.width;
				child.calculateRight();
				child.calculateBottom();

				this.row_top=this.lowest_child_bottom;
				this.row_rightmost_left = 0.0;
				this.row_leftmost_right = this.internal_width-child_external_width;
				this.lowest_child_bottom=Math.max(this.lowest_child_bottom, child.bottom+child.getMargin("t")+(child.getBorderWidth("t")/2));
			}//if.
			else//Just add to existing row.
			{
				child.top=this.row_top+child.getMargin("t")+(child.getBorderWidth("t")/2);
				this.row_leftmost_right -= child_external_width;
				child.left=this.row_leftmost_right+child.getMargin("l")+(child.getBorderWidth("l")/2);
				child.calculateRight();
				child.calculateBottom();
				this.lowest_child_bottom=Math.max(this.lowest_child_bottom, child.bottom+child.getMargin("t")+(child.getBorderWidth("t")/2));
			}//else.
		}//if.
		else //if(child_float_side.startsWith("l")). Float=left is the default.
		{
			//start a new row.
			if((child_external_width+this.row_rightmost_left)>this.row_leftmost_right)
			{
				child.top=this.lowest_child_bottom+child.getMargin("t")+(child.getBorderWidth("t")/2);
				child.left=0+child.getMargin("l")+(child.getBorderWidth("l")/2);
				child.calculateRight();
				child.calculateBottom();

				this.greatest_width=Math.max(this.greatest_width, this.row_rightmost_left);
				//System.out.println(" left child new row. greatest_width: "+this.greatest_width);//debug**

				this.row_top=this.lowest_child_bottom;
				this.row_rightmost_left = child_external_width;
				this.row_leftmost_right = this.internal_width;
				this.lowest_child_bottom=Math.max(this.lowest_child_bottom, child.bottom+child.getMargin("t")+(child.getBorderWidth("t")/2));
			}//if.
			else//Just add to existing row.
			{					
				child.top=this.row_top;
				child.left=this.row_rightmost_left+child.getMargin("l")+(child.getBorderWidth("l")/2);
				this.row_rightmost_left += child_external_width;
				this.greatest_width=Math.max(this.greatest_width, this.row_rightmost_left);
				//System.out.println(" left child. greatest_width: "+this.greatest_width);//debug**
				child.calculateRight();
				child.calculateBottom();
				this.lowest_child_bottom=Math.max(this.lowest_child_bottom, child.bottom+child.getMargin("t")+(child.getBorderWidth("t")/2));
			}//else.
		}//else.
	}//placeChild().

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
				this.height=this.image.getHeight();
				System.out.println("IMAGE height: "+this.height);//debug**
			}//if.
		}//if.
		else if(style_height.endsWith("%"))
		{
			if(this.max_height<=-1)
			{
				System.out.println("SEVERE: "+class_name+".calculateHeight(): Cannot calculate percentage height when parent height is not fixed!");
				this.height=-1;
			}//if.
			double percentage = Double.parseDouble(style_height.replaceAll("%",""));
			this.height = (percentage*this.max_height)/100;
		}//if.
		else
		{this.height = Double.parseDouble(style_height);}

		if(this.height!=-1)
		{
			this.height = this.height-this.getMargin("t")-(this.getBorderWidth("t")/2)-(this.getBorderWidth("b")/2)-this.getMargin("b");
		}//if.

		this.max_height = this.getMaxHeight();//Called method to ensure 'max_height' has been calculated.
		if(this.height>this.max_height && this.max_height>0)//'height' can't be greater than 'max_height'.
		{this.height = this.max_height;}

		//System.out.println(" height: "+this.height);//debug**

		if(this.height!=-1)
		{return;}

		//This next section is for working out auto-height.
		if(!this.is_closed)//Can't calculate 'auto' height until we know what's inside this element.
		{
			System.out.println("Warning: "+class_name+".calculateHeight(): method called before element was closed!");
			return;
		}//if.

		//System.out.println(" lowest_child_bottom: "+this.lowest_child_bottom);//debug**

		double max_column_height = this.lowest_child_bottom+(this.getBorderWidth("t")/2)+this.getPadding("t")+this.getPadding("b")+(this.getBorderWidth("b")/2);
		//System.out.println(" this.max_height: "+this.max_height+" max_column_height: "+max_column_height);//debug**
		if(this.max_height>-1 && max_column_height>this.max_height)
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

		this.bottom = this.top+this.height+this.getPadding("t")+this.getPadding("b");
		//System.out.println("calculateBottom(): matched_sequence="+this.html_data.matched_sequence);//debug**
	//	System.out.println("calculateBottom(): ");//debug**
	//	System.out.println(this.printRelativeLocationData());//debug**
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

	public void setText(String text) throws CustomException
	{
		if(this.image!=null)
		{throw new CustomException(CustomException.SEVERE, class_name, "Cannot set 'text' when 'image' is already defined.", "Trying to set text.");}
		
		if(this.text_elements.size()>0)
		{System.out.println("Warning: "+class_name+" 'text' is already defined, overwriting...");}

		if(text==null || text.trim().isEmpty())
		{return;}
		text=text.trim();

		//System.out.println(class_name+".setText(): text="+text);//debug**


		double internal_width = this.getMaxPossibleChildWidth();
		//System.out.println("internal_width="+internal_width);//debug**
		double line_height = this.html_data.font_size-1;//Might need to alter the '-2', it's kindof a thumb-suck.
		double padding_bottom = line_height*30/100;

		String[] words = text.split(" ");
		//System.out.println("words="+Arrays.toString(words));//debug**
		StringBuilder text_line = new StringBuilder();
		for(String word: words)
		{

			if(text_line.length()<=0)
			{
				text_line.append(word);
				//System.out.println("text_line="+text_line.toString());//debug**
				continue;
			}//if.

			double existing_line_length = text_line.length()*this.html_data.char_width;
			double word_length = word.length()*this.html_data.char_width;
			if((existing_line_length+word_length)>=internal_width)
			{
				PDFElementProperties text_element = createInternalTextElement(text_line.toString(), existing_line_length, line_height, padding_bottom);
				//text_element.closeTag();
				//System.out.println("children.size()="+this.children.size());//debug**

				text_line=new StringBuilder(word);
			}//if.
			else
			{text_line.append(" "+word);}

			//System.out.println("text_line="+text_line.toString());//debug**
		}//for(word).

		//Append the last line.
		if(text_line.length()>0)
		{
			double existing_line_length = text_line.length()*this.html_data.char_width;
			PDFElementProperties text_element = createInternalTextElement(text_line.toString(), existing_line_length, line_height, padding_bottom);
			text_element.closeTag();
			//System.out.println("children.size()="+this.children.size());//debug**
		}//if.

	}//setText().

	//'text' is wrapped in an element to allow manipulating its placement, padding, etc...
	private PDFElementProperties createInternalTextElement(String text, double line_length, double line_height, double padding_bottom) throws CustomException
	{
		String text_styling = "<text style=\"display:inline-block; float:left; margin:none; padding-bottom:"+padding_bottom+"; border:none; width:"+line_length+"; height:"+line_height+"; \" >";
		HtmlData text_html_data = new HtmlData(0, 0, text_styling);
		text_html_data.setParent(this.html_data);
		PDFElementProperties new_text = new PDFElementProperties(this, text_html_data);
		new_text.text_string=text;
		return new_text;
	}//createInternalTextElement().


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

			if(child_element.getTag().equals("text"))
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

	public String getBorderColor(String side)
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
		return "";
	}//getBorderColor().

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

	public int getFontSize()
	{
		return this.html_data.font_size;
	}//getFontSize().

	public String getHref()
	{
		return this.html_data.getHref();
	}//getHref().

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
		return loc_data.toString();
	}//printAbsoluteLocationData().

}//PDFElementProperties().