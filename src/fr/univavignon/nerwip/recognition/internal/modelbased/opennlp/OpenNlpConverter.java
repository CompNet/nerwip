package fr.univavignon.nerwip.recognition.internal.modelbased.opennlp;

/*
 * Nerwip - Named Entity Extraction in Wikipedia Pages
 * Copyright 2011-16 Vincent Labatut et al.
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.univavignon.nerwip.data.article.Article;
import fr.univavignon.nerwip.data.entity.EntityType;
import fr.univavignon.nerwip.data.entity.mention.AbstractMention;
import fr.univavignon.nerwip.data.entity.mention.Mentions;
import fr.univavignon.nerwip.recognition.ConverterException;
import fr.univavignon.nerwip.recognition.ProcessorName;
import fr.univavignon.nerwip.recognition.internal.AbstractInternalConverter;
import fr.univavignon.nerwip.tools.file.FileNames;
import opennlp.tools.util.Span;

/**
 * This class is the converter associated to OpenNLP.
 * It is able to convert the text outputed by this recognizer
 * into objects compatible with Nerwip.
 * <br/>
 * It can also read/write these results using raw text
 * and our XML format.
 * 
 * @author Vincent Labatut
 */
public class OpenNlpConverter extends AbstractInternalConverter<Map<EntityType,List<Span>>>
{	
	/**
	 * Builds a new converter using the specified info.
	 * 
	 * @param nerFolder
	 * 		Folder used to stored the results of the recognizer.
	 */
	public OpenNlpConverter(String nerFolder)
	{	super(ProcessorName.OPENNLP, nerFolder, FileNames.FI_OUTPUT_TEXT);
	}

	/////////////////////////////////////////////////////////////////
	// PROCESS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public Mentions convert(Article article, Map<EntityType,List<Span>> data) throws ConverterException
	{	Mentions result = new Mentions(processorName);
		
		String rawText = article.getRawText();
		for(Entry<EntityType,List<Span>> entry: data.entrySet())
		{	EntityType type = entry.getKey();
			List<Span> spans = entry.getValue();
			for(Span span: spans)
			{	// build internal representation of the mention
				int startPos = span.getStart();
				int endPos = span.getEnd();
				String valueStr = rawText.substring(startPos,endPos);
				AbstractMention<?> mention = AbstractMention.build(type, startPos, endPos, processorName, valueStr);
				
				// ignore overlapping mentions
//				if(!result.hasMention(mention))	//TODO don't remember if i'm supposed to change that, or what?
					result.addMention(mention);
			}
		}	

		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// RAW				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected void writeRawResults(Article article, Map<EntityType,List<Span>> intRes) throws IOException
	{	StringBuffer string = new StringBuffer();
		
		String rawText = article.getRawText();
		for(Entry<EntityType,List<Span>> entry: intRes.entrySet())
		{	EntityType type = entry.getKey();
			List<Span> spans = entry.getValue();
			for(Span span: spans)
			{	// build internal representation of the mention
				int startPos = span.getStart();
				int endPos = span.getEnd();
				String valueStr = rawText.substring(startPos,endPos);
				string.append("["+type.toString()+" '"+valueStr+"' ("+startPos+","+endPos+")]\n");
			}
		}
		
		writeRawResultsStr(article, string.toString());
	}
}
