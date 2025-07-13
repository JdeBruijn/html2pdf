import java.util.HashMap;
import java.util.logging.Logger;
import java.util.Set;

public class CssObject
{
	private static final String class_name="CssObject";
	private static final Logger log = Logger.getLogger(class_name);

	public String name = "";
	
	public HashMap<String, String> parameters = new HashMap<String, String>();
	public HashMap<String, String> important_parameters = new HashMap<String, String>();

	public CssObject(String class_name)
	{
		name = class_name.replaceAll("^(\\.|#)+","");

		// System.out.println("CssObject class_name="+class_name);//debug**
	}// constructor().

	public void addParameter(String parameter_line) throws CustomException
	{
		String[] param_data = parameter_line.split(":");
		if(param_data.length<=0)
		{throw new CustomException(CustomException.WARNING, class_name+".addParameter()","Invalid parameter_line '"+parameter_line+"'. Expected format: param_name: param_value;", "Trying to add css parameter to '"+this.name+"'");}

		String param_name = param_data[0].trim();
		
		String param_value = param_data[1];
		if(param_value.contains("!important"))
		{
			param_value.replaceAll("!important","");
			this.important_parameters.put(param_name, param_value);
		}//if.

		this.parameters.put(param_name, param_value);
	}//addParameter().

	public Set<String> getParamNames()
	{
		return this.parameters.keySet();
	}//getParamNames().

	public String getParamValue(String param_name)
	{
		return this.parameters.get(param_name);
	}//getParamValue().
 
	public boolean isImportant(String param_name)
	{return this.important_parameters.containsKey(param_name);}

	public String toString()
	{
		StringBuilder rep = new StringBuilder("{");
		for(String param_name: this.parameters.keySet())
		{
			rep.append(" "+param_name+": "+this.parameters.get(param_name));
			if(this.important_parameters.containsKey(param_name))
			{rep.append(" !important");}
			rep.append(";");
		}//for(param_name).
		rep.append(" }");

		return rep.toString();
	}//toString().

}//class CssObject.
