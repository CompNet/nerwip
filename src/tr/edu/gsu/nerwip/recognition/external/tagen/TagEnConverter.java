package tr.edu.gsu.nerwip.recognition.external.tagen;

/*
 * Nerwip - Named Entity Extraction in Wikipedia Pages
 * Copyright 2011 Yasa Akbulut, Burcu Küpelioğlu & Vincent Labatut
 * Copyright 2012 Burcu Küpelioğlu, Samet Atdağ & Vincent Labatut
 * Copyright 2013 Samet Atdağ & Vincent Labatut
 * Copyright 2014-15 Vincent Labatut
 * 
 * This file is part of Nerwip - Named Entity Extraction in Wikipedia Pages.
 * 
 * Nerwip - Named Entity Extraction in Wikipedia Pages is free software: you can 
 * redistribute it and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * Nerwip - Named Entity Extraction in Wikipedia Pages is distributed in the hope 
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public 
 * License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Nerwip - Named Entity Extraction in Wikipedia Pages.  
 * If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import tr.edu.gsu.nerwip.data.article.Article;
import tr.edu.gsu.nerwip.data.entity.AbstractEntity;
import tr.edu.gsu.nerwip.data.entity.Entities;
import tr.edu.gsu.nerwip.data.entity.EntityType;
import tr.edu.gsu.nerwip.recognition.ConverterException;
import tr.edu.gsu.nerwip.recognition.RecognizerName;
import tr.edu.gsu.nerwip.recognition.external.AbstractExternalConverter;
import tr.edu.gsu.nerwip.tools.file.FileNames;

/**
 * This class is the converter associated to TagEN.
 * It is able to convert the text outputed by this NER tool
 * into objects compatible with Nerwip.
 * <br/>
 * It can also read/write these results using raw text
 * and our XML format.
 * 
 * @author Sabrine Ayachi
 * @author Vincent Labatut
 */
public class TagEnConverter extends AbstractExternalConverter
{
	/**
	 * Builds a new converter using the specified info.
	 * 
	 * @param nerFolder
	 * 		Folder used to stored the results of the NER tool.
	 */
	public TagEnConverter(String nerFolder)
	{	super(RecognizerName.TAGEN, nerFolder, FileNames.FI_OUTPUT_TEXT);
	}
	
	/////////////////////////////////////////////////////////////////
	// TYPE CONVERSION MAP	/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Header added to the file output by TagEn */
	private final static String NEW_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	/** Fake root element name */
	private final static String ELT_ROOT = "root";
	/** Enumerated entity value */
	private final static String ELT_ENAMEX = "enamex";	// location, organization, person...
	/** Temporal entity value */
	private final static String ELT_TIMEX = "timex";	// date... 
	/** Numerical entity value */
	private final static String ELT_NUMEX = "numex";	// percent...
	/** Use to represent range of values, inside entity elements */
	private final static String ELT_RANGE = "range";	// ...
	/** Map of String to entity type conversion */
	private final static Map<String, EntityType> CONVERSION_MAP = new HashMap<String, EntityType>();
	static
	{	CONVERSION_MAP.put("date", EntityType.DATE);
		CONVERSION_MAP.put("location", EntityType.LOCATION);
		CONVERSION_MAP.put("organization", EntityType.ORGANIZATION);
		CONVERSION_MAP.put("person", EntityType.PERSON);
	}
	/** List of ignored XML elements */
	private final static List<String> ELEMENT_BLACKLIST = Arrays.asList
	(	ELT_NUMEX,
		ELT_RANGE
	);
	
	/////////////////////////////////////////////////////////////////
	// PROCESS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
    public Entities convert(Article article, String data) throws ConverterException
	{	logger.increaseOffset();
		Entities result = new Entities(recognizerName);
		
		// the result file is basically an XML file, only the header is missing
		// so add a fake header and parse it like an xml file
		String xmlSource = NEW_HEADER
				+ "<" + ELT_ROOT + ">" 
				+ data 
				+ "</" + ELT_ROOT + ">";
		
		// parse the xml source
		logger.log("Completing TagEn result to get an XML file, then parsing it");
		Element root;
		try
		{	SAXBuilder sb = new SAXBuilder();
			Document doc = sb.build(new StringReader(xmlSource));
			root = doc.getRootElement();
		}
		catch (JDOMException e)
		{	//e.printStackTrace();
			System.err.println(data);
			throw new ConverterException(e.getMessage());
		}
		catch (IOException e)
		{	//e.printStackTrace();
			throw new ConverterException(e.getMessage());
		}
	
		// process the xml document
		logger.log("Processing the resulting XML document");
		logger.increaseOffset();
		int index = 0; 
		XMLOutputter xo = new XMLOutputter();
		List<Content> children = root.getContent();
		for(Content child: children)
		{	// text content is just counted
			if(child instanceof Text)
			{	Text t = (Text)child;
				String str = t.getText();
				int length = str.length();
				logger.log("("+index+")"+str+ "[["+length+"]]");
				index = index + length;
			}
			
			// elements are processed individually
			else if(child instanceof Element)
			{	Element e = (Element)child;
				String str = e.getText();
				int length = str.length();
				logger.log("("+index+")"+xo.outputString(e)+ "[["+length+"]]");
				List<AbstractEntity<?>> entList = convertElement(e, index);
				result.addEntities(entList);
				for(AbstractEntity<?> entity: entList)
					logger.log(entity.toString());
				index = index + length;
			}
		}
		logger.decreaseOffset();
	
		logger.decreaseOffset();
		return result;
	}
	
	/**
	 * Receives an XML element, and processes it to
	 * extract an entity.
	 * 
	 * @param element
	 * 		Element to process.
	 * @param index
	 * 		Current position in the original text (in characters).
	 * @return
	 * 		The created entity, or {@code null} if it was not
	 * 		possible to create it due to a lack of information.
	 */
	private List<AbstractEntity<?>> convertElement(Element element, int index)
	{	List<AbstractEntity<?>> result = new ArrayList<AbstractEntity<?>>();
		
		// retrieving the child(ren)
		String name = element.getName();
		List<Element> elts = element.getChildren();
		if(elts.size()>1)
			logger.log("WARNING: detected more than one child in a <"+name+"> element");
		else
		{	Element innerElt = elts.get(0);
			
			// enumerated entity
			if(name.equalsIgnoreCase(ELT_ENAMEX))
			{	AbstractEntity<?> entity = convertEnumElement(innerElt,index);
				result.add(entity);
			}
			
			// temporal entity
			else if(name.equalsIgnoreCase(ELT_TIMEX))
				result = convertTemporalElement(innerElt,index);
			
			// other entity
			else if(!ELEMENT_BLACKLIST.contains(name))
				logger.log("WARNING: detected an unknown element: <"+name+">");
		}
		
		return result;
	}

	/**
	 * Receives an XML element, and processes it to
	 * extract an enumerated entity.
	 * 
	 * @param element
	 * 		Element to process.
	 * @param index
	 * 		Current position in the original text (in characters).
	 * @return
	 * 		The created entity, or {@code null} if it was not
	 * 		possible to create it due to a lack of information.
	 */
	private AbstractEntity<?> convertEnumElement(Element element, int index)
	{	logger.increaseOffset();
		AbstractEntity<?> result = null;
		XMLOutputter xo = new XMLOutputter();
				
		// check if the element does not contain any lower element
		List<Element> children = element.getChildren();
		if(!children.isEmpty())
			logger.log("WARNING: detected some encapsulated elements in "+xo.outputString(element));
		
		else
		{	String name = element.getName();
			EntityType type = CONVERSION_MAP.get(name);
			if(type==null)
				logger.log("WARNING: could not find the entity type associated to tag "+xo.outputString(element));
			
			else
			{	String valueStr = element.getText();
				int length = valueStr.length();
				result = AbstractEntity.build(type, index, index+length, recognizerName, valueStr);
			}
		}
		
		logger.decreaseOffset();
		return result;
	}

	/**
	 * Receives an XML element, and processes it to
	 * extract a date entity.
	 * 
	 * @param element
	 * 		Element to process.
	 * @param index
	 * 		Current position in the original text (in characters).
	 * @return
	 * 		The created entity, or {@code null} if it was not
	 * 		possible to create it due to a lack of information.
	 */
	private List<AbstractEntity<?>> convertTemporalElement(Element element, int index)
	{	logger.increaseOffset();
		List<AbstractEntity<?>> result = new ArrayList<AbstractEntity<?>>();
		XMLOutputter xo = new XMLOutputter();
				
		String name = element.getName();
		String originalStr = element.getText();
		EntityType type = CONVERSION_MAP.get(name);
		if(type==null)
			logger.log("WARNING: could not find the entity type associated to tag "+xo.outputString(element));
//			logger.log("Element not describing a date/time (ignored): "+xo.outputString(element));
		
		// we only focus on dates and date-times
		else
		{	List<String> valuesStr = new ArrayList<String>();
			int offset = 0;
			
			// check if the element contains a <range> subelement
			List<Element> children = element.getChildren();
			if(children.size()==1)
			{	Element element2 = children.get(0);
				List<Element> children2 = element2.getChildren();
				if(!children2.isEmpty())
					logger.log("WARNING: detected some re-encapsulated elements in "+xo.outputString(element));
				else
				{	originalStr = element2.getText();
					String name2 = element2.getName();
					if(!name2.equalsIgnoreCase(ELT_RANGE))
						logger.log("WARNING: detected an unknown element: <"+name2+"> in "+xo.outputString(element));
					else
					{	String text = element2.getText();
						if(!text.contains("-"))
							logger.log("WARNING: detected an unknown form of <range> content in "+xo.outputString(element));
						else
						{	String tmp[] = text.split("-");
							for(String str: tmp)
								valuesStr.add(str);
						}
					}
				}
			}
			else if(children.size()>1)
				logger.log("WARNING: detected some encapsulated elements in "+xo.outputString(element));
			
			else
			{	String valueStr = element.getText();
				valuesStr.add(valueStr);
			}
			
			for(String valueStr: valuesStr)
			{	offset = originalStr.indexOf(valueStr, offset);
				// get the date value
//				Date date = parseTimex3Value(valueStr);
//				if(date==null)
//					logger.log("WARNING: could not parse the date/time in element "+xo.outputString(element)); 
//				else
				{	int length = valueStr.length();
					AbstractEntity<?> entity = AbstractEntity.build
					(	EntityType.DATE, 
						index+offset, index+offset+length, 
						recognizerName, valueStr
					);
//					result.setValue(date);
					result.add(entity);
				}
			}
		}
		
		logger.decreaseOffset();
		return result;
	}
}