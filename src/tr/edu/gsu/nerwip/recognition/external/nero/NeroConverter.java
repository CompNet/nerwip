package tr.edu.gsu.nerwip.recognition.external.nero;

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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import tr.edu.gsu.nerwip.data.article.Article;
import tr.edu.gsu.nerwip.data.entity.AbstractEntity;
import tr.edu.gsu.nerwip.data.entity.Entities;
import tr.edu.gsu.nerwip.data.entity.EntityType;
import tr.edu.gsu.nerwip.recognition.ConverterException;
import tr.edu.gsu.nerwip.recognition.RecognizerName;
import tr.edu.gsu.nerwip.recognition.external.AbstractExternalConverter;
import tr.edu.gsu.nerwip.tools.file.FileNames;
import tr.edu.gsu.nerwip.tools.string.StringTools;

/**
 * This class is the converter associated to Nero. It is able to convert the
 * text outputed by this NER tool into objects compatible with Nerwip. 
 * <br/>
 * It can also read/write these results using raw text and our XML format.
 * 
 * @author Sabrine Ayachi
 * @author Vincent Labatut
 */
public class NeroConverter extends AbstractExternalConverter
{	
	/**
	 * Builds a new converter using the specified info.
	 * 
	 * @param nerFolder
	 *            Folder used to stored the results of the NER tool.
	 */
	public NeroConverter(String nerFolder)
	{	super(RecognizerName.NERO, nerFolder, FileNames.FI_OUTPUT_TEXT);
	}

	/////////////////////////////////////////////////////////////////
	// TYPE CONVERSION MAP 	/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Map of URI to entity type conversion */
	private final static Map<String, EntityType> CONVERSION_MAP = new HashMap<String, EntityType>();
	/** List of ignored entity types */
	private final static List<String> IGNORED_TYPES = Arrays.asList(
		"amount",
		"unk"
	);
	
	/** Initialization of the conversion map */
	static 
	{	CONVERSION_MAP.put("fonc", EntityType.FUNCTION);
		CONVERSION_MAP.put("loc", EntityType.LOCATION);
		CONVERSION_MAP.put("org", EntityType.ORGANIZATION);
		CONVERSION_MAP.put("pers", EntityType.PERSON);
		CONVERSION_MAP.put("prod", EntityType.PRODUCTION);
		CONVERSION_MAP.put("time", EntityType.DATE);
	}

	/////////////////////////////////////////////////////////////////
	// PROCESS 			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public Entities convert(Article article, String data) throws ConverterException
	{	Entities result = new Entities(recognizerName);
		String originalText = article.getRawText();

		LinkedList<EntityType> types = new LinkedList<EntityType>();
		LinkedList<Integer> startPos1 = new LinkedList<Integer>();
//		LinkedList<Integer> startPos2 = new LinkedList<Integer>();
		LinkedList<String> tags = new LinkedList<String>();
		
		int i1 = 0;
		int i2 = 0;
		int c1 = originalText.codePointAt(i1);
		int c2 = data.codePointAt(i2);
		
//if(c1==65279) //debug
//	System.out.print("");
		// possibly pass a starting newline character 
		if(c2=='\n' && c1!='\n')
		{	i2++;
			c2 = data.codePointAt(i2);
		}
		
		while(i1<originalText.length() && i2<data.length())
		{	c1 = originalText.codePointAt(i1);
			c2 = data.codePointAt(i2);
			
			// beginning of a tag
			if(c2=='<')
			{	int k2 = i2;
				i2++; 
				c2 = data.codePointAt(i2);
				
				// closing tag
				if(c2=='/')
				{	int j2 = data.indexOf('>', i2);
					String tag = data.substring(i2+1,j2);
					String tag0 = tags.pop();
					if(!tag.equalsIgnoreCase(tag0))
					{	String msg = StringTools.highlightPosition(i2, data, 20);
						logger.log("WARNING: opening tag ("+tag0+") different from closing tag ("+tag+"):\n"+msg);
					}
					i2 = j2 + 1;
					EntityType type = types.pop();
					int sp1 = startPos1.pop();
//					int sp2 = startPos2.pop();
					if(type!=null)
					{
//						String valueStr = data.substring(sp2,k2);
						String valueStr = originalText.substring(sp1,i1);
						AbstractEntity<?> entity = AbstractEntity.build(type, sp1, i1, RecognizerName.NERO, valueStr);
						entity.correctEntitySpan(); // to remove some spaces located at the end of entities
						result.addEntity(entity);
					}
				}
				
				// opening tag
				else
				{	int j2 = data.indexOf('>', i2);
					String tag = data.substring(i2,j2);
					i2 = j2 + 1;
					tags.push(tag);
					EntityType type = CONVERSION_MAP.get(tag);
					if(type==null && !IGNORED_TYPES.contains(tag))
					{	if(tag.isEmpty())
						{	int end = Math.min(j2+40, data.length());
							String msg = data.substring(k2, end);
							logger.log("WARNING: found an empty tag, settling for a date ("+msg+"[...])");
							type = EntityType.DATE;
						}
						else
						{	String msg = StringTools.highlightPosition(k2, data, 20);
							throw new ConverterException("Found an unknown tag : \""+tag+"\" at "+msg);
						}
					}
					types.push(type);
					startPos1.push(i1);
//					startPos2.push(i2);
				}
			}
			
			// other character (than '<')
			else
			{	
//if(c1=='[') // debug
//	System.out.print("");

				// similar characters
				if(StringTools.compareCharsRelaxed(c1,c2)==0)// || c2==65533)
				{	// everything's normal
					// >> go to next chars in both texts
					i1++; 
					i2++; 
				}
				
				else
				{	boolean moved = false;
				
					// pass all non-letter and non-digit characters
					if(!Character.isLetterOrDigit(c1))//c1==' ' || c1=='\n' || StringTools.isPunctuation(c1))
					{	i1++;
						moved = true;
					}
					
					// pass all non-letter and non-digit characters
					if(!Character.isLetterOrDigit(c2))//c2==' ' || c2=='\n' || StringTools.isPunctuation(c2))
					{	i2++;
						moved = true;
					}
					
					// if both are letters or digits (but different), we have a problem
					if(!moved)
					{	String msg1 = StringTools.highlightPosition(i1, originalText, 20);
						String msg2 = StringTools.highlightPosition(i2, data, 20);
						throw new ConverterException("Found an untreatable character:\n"+msg1+"\n"+msg2);
					}
				}
				
				
//				// different chars, but space in the original text
//				else if(c1==' ')
//				{	// Nero probably ate a space
//					// >> go to next char in the original text only
//					i1++; 
//				}
//				
//				// different chars, but space in the annotated text
//				else if(c2==' ')
//				{	// if the very end or beginning of the annotated text, 
//					// Nero probably added a space
//					// >> go to next char in the annotated text
//					if(i2==0 || i2==data.length()-1)
//					{	i2++;
//					}
//					else
//					{	// if right before or right after a tag (in the annotated text), 
//						// or right before a new line,
//						// or right before or after another space, 
//						// then Nero probably added a space
//						// >> go to next char in the annotated text
//						int before = data.codePointAt(i2-1);
//						int after = data.codePointAt(i2+1);
//						if(before=='>' || after=='<' 
//								|| after=='\n'
//								|| before=='\''
//								|| before==' ' || after==' ')
//						{	i2++; 
//						}
//						// otherwise, if punctuation in the original text, 
//						// Nero probably ate this punctuation mark
//						// >> go to next char in the original text
//						else if(StringTools.isPunctuation(c1))// && originalText.charAt(i1+1)==' ')
//						{	i1++;
//							if(c1=='-')
//								i2++;
//						}
//						// else, we have a problem!
//						else
//						{	String msg1 = StringTools.highlightPosition(i1, originalText, 20);
//							String msg2 = StringTools.highlightPosition(i2, data, 20);
//							throw new ConverterException("Problem at position :\n"+msg1+"\n"+msg2);
//						}
//					}
//				}
//				
//				// different chars, and punctuation in the original text
//				// Nero probably ate the punctuation
//				// >> go to next char in the original text
//				else if(StringTools.isPunctuation(c1))
//				{	i1++;
//					if(StringTools.isPunctuation(c2))
//						i2++;
//				}
//				
//				// different chars, and punctuation in the annotated text
//				// Nero probably moved some punctuation
//				// >> go to next char in the annotated text
//				else if(StringTools.isPunctuation(c2))
//				{	i2++;
//				}
//				
//				// problem : display a specific error message
//				else
//				{	String msg1 = StringTools.highlightPosition(i1, originalText, 20);
//					String msg2 = StringTools.highlightPosition(i2, data, 20);
//					throw new ConverterException("Found a untreatable supernumerary character:\n"+msg1+"\n"+msg2);
//				}
				
				
			}
		}
		
		// check if we actually processed the whole texts
		if(i1<originalText.length())
		{	// possibly consume the final newline chars
			do
			{	c1 = originalText.codePointAt(i1);
				i1++;
			}
			while(i1<originalText.length() && (c1=='\n' || c1==' '));
			if(i1<originalText.length())
			{	String msg1 = StringTools.highlightPosition(i1, originalText, 20);
				throw new ConverterException("Didn't reach the end of the original text\n"+msg1);
			}
		}
		else if(i2<data.length())
		{	String msg2 = StringTools.highlightPosition(i2, data, 20);
			throw new ConverterException("Didn't reach the end of the annotated text\n"+msg2);
		}
		
		return result;
	}
}
