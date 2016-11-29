package fr.univavignon.nerwip.processing;

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

import fr.univavignon.nerwip.data.article.Article;
import fr.univavignon.nerwip.data.entity.Entities;
import fr.univavignon.nerwip.data.entity.mention.Mentions;

/**
 * TODO
 * 		 
 * @author Vincent Labatut
 */
public interface InterfaceLinker extends InterfaceProcessor
{	
	/////////////////////////////////////////////////////////////////
	// FOLDER			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Returns the name of the folder containing the results of this
	 * linker.
	 * <br/>
	 * This name takes into account the name of the tool, but also the 
	 * parameters it uses. It can also be used just whenever a string 
	 * representation of the tool and its parameters is needed.
	 * 
	 * @return 
	 * 		Name of the appropriate folder.
	 */
	public String getLinkerFolder();
	
	/////////////////////////////////////////////////////////////////
	// PROCESSING		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Applies this processor to the specified article,
	 * in order to link entities to unique identifiers in 
	 * databases such as DBpedia or Freelink.
	 * <br/>
	 * If {@code mentions} is {@code null}, the recognizer is applied to get
	 * the mentions. Similarly, if {@code entities} is {@code null}, the
	 * resolver is applied to get the entities. If {@code recognizer} and/or
	 * {@code resolver} is this object and must be applied, then Nerwip tries 
	 * to perform simultaneously the concerned tasks, provided this processor
	 * allows it. Otherwise, the same processor is applied separately for all
	 * tasks.
	 * <br/>
	 * Note that if the resolver is applied, the {@code Mention} object will be 
	 * completed so as to point towards their assigned entities. When the linker
	 * is applied, some entities can be completed (i.e. unique URI) and removed/added,
	 * whereas the  mentions can be modified (link towards their entities). 
	 * 
	 * @param article
	 * 		Article to be processed.
	 * @param mentions
	 * 		List of the previously recognized mentions.
	 * @param entities
	 * 		List of the entities associated to the mentions.
	 * @param recognizer
	 * 		Processor used to recognize the entity mentions.
	 * @param resolver
	 * 		Processor used to resolve the coreferences.
	 * 
	 * @throws ProcessorException
	 * 		Problem while resolving co-occurrences. 
	 */
	public void link(Article article, Mentions mentions, Entities entities, InterfaceRecognizer recognizer, InterfaceResolver resolver) throws ProcessorException;
}