/*
	Jon de Bruijn
	2025-07-19
	This class holds all the 'global' variables for a PDF document.
*/


import java.util.logging.Logger;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;


public class PdfDocVars
{
	private static final String class_name = "PdfDocVars";
	private static final Logger log = Logger.getLogger(class_name);


	public double page_width=0;
	public double page_height=0;

	public LinkedList<PDFElementProperties> page_sized_elements = null;

	public double internal_elements_height=0;//Total height of internal elements. Used to work out page height.

	public String base_path="./";

	public PDFElementProperties open_table = null;
	public List<Double> table_column_widths = null;
	public PDFElementProperties open_row = null;
	public int current_cell_index = 0;


	public PdfDocVars()
	{}//null constructor().

	public void setPageSize(double width, double height)
	{
		page_width=width;
		page_height=height;
	}//setPageSize().

	public void addPageSizedElement(PDFElementProperties element)
	{
		if(page_sized_elements==null)
		{page_sized_elements=new LinkedList<PDFElementProperties>();}

		page_sized_elements.add(element);
	}//addPageSizedElement().

	public double getPageWidth()
	{return page_width;}

	public double getPageHeight()
	{return page_height;}


}//class PdfDocVars.