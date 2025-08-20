
import java.util.List;
import java.util.LinkedList;

import java.io.OutputStream;

public abstract class HtmlConverter
{

	protected GlobalDocVars doc_vars = new GlobalDocVars();

	protected CssInliner css_inliner = null;

	protected double space_m_sizing = 0.33;

	protected LinkedList<PDFElementProperties> flattened_elements = null;



	public abstract void convert(String html_string, OutputStream output_stream);


	public void setBasePath(String path)
	{
		if(path==null || path.trim().isEmpty())
		{path="./";}

		if(!path.endsWith("/"))
		{path = path+"/";}

		this.doc_vars.base_path=path;
	}//setBasePath().

	public String getBasePath()
	{return this.doc_vars.base_path;}

	protected String normalizePath(String path)
	{
		if(!path.startsWith("/") && !path.contains(this.doc_vars.base_path))//Absolute paths and paths that already specify the base_path can be skipped.
		{return this.doc_vars.base_path+path;}

		return path;
	}//normalizePath().

	protected void flattenElements(PDFElementProperties base_element)
	{
		if(this.flattened_elements==null)
		{this.flattened_elements = new LinkedList<PDFElementProperties>();}
		this.flattened_elements.add(base_element);
		for(PDFElementProperties child: base_element.children)
		{
			flattenElements(child);
		}//for(child).
		for(PDFElementProperties child: base_element.fixed_children_elements)
		{
			flattenElements(child);
		}//for(child.)

	}//flattenElements().

}//interface HtmlConverter.