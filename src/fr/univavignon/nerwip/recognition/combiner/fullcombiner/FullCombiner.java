package fr.univavignon.nerwip.recognition.combiner.fullcombiner;

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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.univavignon.nerwip.data.article.Article;
import fr.univavignon.nerwip.data.article.ArticleLanguage;
import fr.univavignon.nerwip.data.entity.EntityType;
import fr.univavignon.nerwip.data.entity.mention.AbstractMention;
import fr.univavignon.nerwip.data.entity.mention.Mentions;
import fr.univavignon.nerwip.recognition.AbstractProcessor;
import fr.univavignon.nerwip.recognition.ProcessorException;
import fr.univavignon.nerwip.recognition.ProcessorName;
import fr.univavignon.nerwip.recognition.combiner.AbstractCombiner;
import fr.univavignon.nerwip.recognition.combiner.svmbased.SvmCombiner;
import fr.univavignon.nerwip.recognition.combiner.svmbased.SvmCombiner.CombineMode;
import fr.univavignon.nerwip.recognition.combiner.votebased.VoteCombiner;
import fr.univavignon.nerwip.recognition.combiner.votebased.VoteCombiner.VoteMode;
import fr.univavignon.nerwip.recognition.internal.modelless.wikipediadater.WikipediaDater;

/**
 * This combiner is very basic: it first applies our date
 * detector to identify mentions which are quasi-certainly
 * dates, then uses one of the other combiners for the other
 * types of entities. There is no training for this combiner,
 * the training is performed at the level of the combiner
 * this one is built upon. 
 * <br/>
 * The recognizers handled by this combiner are:
 * <ul>
 * 		<li>WikipediaDater to detect dates (see {@link WikipediaDater})</li>
 * 		<li>Either SvmCombiner or VoteCombiner to detect locations, organizations 
 * 			and persons (see {@link SvmCombiner} and {@link VoteCombiner})</li>
 * </ul>
 * Various options allow changing the behavior of this combiner:
 * <ul>
 * 		<li>{@code combiner}: combiner used for the location, organization
 * 			and person mentions (i.e. either the SVM- or vote-based combiner).</li>
 * </ul>
 * 
 * @author Vincent Labatut
 */
public class FullCombiner extends AbstractCombiner
{	
	/**
	 * Builds a new full combiner.
	 *
	 * @param combiner
	 * 		Combiner used to handle locations, organizations and persons
	 * 		(either SVM- or vote-based).
	 *  
	 * @throws ProcessorException
	 * 		Problem while loading some combiner or tokenizer.
	 */
	public FullCombiner(Combiner combiner) throws ProcessorException
	{	super();
		
		this.combiner = combiner;
		
		initRecognizers();
		setSubCacheEnabled(cache);

		initConverter();
	}
	
	/////////////////////////////////////////////////////////////////
	// NAME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public ProcessorName getName()
	{	return ProcessorName.FULLCOMBINER;
	}

	/////////////////////////////////////////////////////////////////
	// FOLDER			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override	
	public String getFolder()
	{	String result = getName().toString();
		
		result = result + "_" + "combi="+combiner.toString();
		
//		result = result + "_" + "trim=" + trim;
//		result = result + "_" + "ignPro=" + ignorePronouns;
//		result = result + "_" + "exclude=" + exclusionOn;
		
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// ENTITY TYPES		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of entity types recognized by this combiner */
	private static final List<EntityType> HANDLED_TYPES = Arrays.asList(
		EntityType.DATE,
		EntityType.LOCATION,
		EntityType.ORGANIZATION,
		EntityType.PERSON
	);
	
	@Override
	public List<EntityType> getHandledEntityTypes()
	{	return HANDLED_TYPES;
	}

	/////////////////////////////////////////////////////////////////
	// LANGUAGES	 		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of languages recognized by this combiner */
	private static final List<ArticleLanguage> HANDLED_LANGUAGES = Arrays.asList(
		ArticleLanguage.EN
//		ArticleLanguage.FR
	);
	
	@Override
	public boolean canHandleLanguage(ArticleLanguage language)
	{	boolean result = HANDLED_LANGUAGES.contains(language);
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// TOOLS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Combiner used for locations, organizations and persons */
	private Combiner combiner;
	
	/**
	 * Represents the combiner used to process locations,
	 * organizations and persons.
	 * 
	 * @author Vincent Labatut
	 */
	public enum Combiner
	{	/** Use the best SVM-based combiner configuration */
		SVM("svm"),
		/** Use the best vote-based combiner configuration */
		VOTE("vote");
		
		/** String representing the parameter value */
		private String name;
		
		/**
		 * Builds a new Combiner value
		 * to be used as a parameter.
		 * 
		 * @param name
		 * 		String representing the parameter value.
		 */
		Combiner(String name)
		{	this.name = name;
		}
		
		@Override
		public String toString()
		{	return name;
		}
	}
	
	@Override
	protected void initRecognizers() throws ProcessorException
	{	logger.increaseOffset();
		boolean loadModelOnDemand = true;
	
		// Wikipedia Dater
		{	logger.log("Init Wikipedia Dater (Dates only)");
			WikipediaDater wikipediaDater = new WikipediaDater();
			recognizers.add(wikipediaDater);
		}
		
		// other combiner
		logger.log("Init the other combiner (Loc+Org+Per)");
		if(combiner==Combiner.SVM)
		{	logger.log("SVM-based combiner selected");
			boolean specific = true;
			boolean useCategories = true;
			CombineMode combineMode = CombineMode.CHUNK_PREVIOUS;
			SubeeMode subeeMode = SubeeMode.ALL;
			SvmCombiner svmCombiner = new SvmCombiner(loadModelOnDemand, specific, useCategories, combineMode, subeeMode);
			recognizers.add(svmCombiner);
		}
		else
		{	logger.log("Vote-based combiner selected");
			boolean specific = true;
			VoteMode voteMode = VoteMode.WEIGHTED_CATEGORY;
			boolean useRecall = true;
			boolean existVote = true;
			SubeeMode subeeMode = SubeeMode.ALL;
			VoteCombiner voteCombiner = new VoteCombiner(loadModelOnDemand, specific, voteMode, useRecall, existVote, subeeMode);
			recognizers.add(voteCombiner);
		}
		
		logger.decreaseOffset();		
	}

	/////////////////////////////////////////////////////////////////
	// GENERAL MODEL	 	/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public String getModelPath()
	{	return null; // no model here
	}
	
	/////////////////////////////////////////////////////////////////
	// PROCESSING	 		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected Mentions combineMentions(Article article, Map<AbstractProcessor,Mentions> mentions, StringBuffer rawOutput) throws ProcessorException
	{	logger.increaseOffset();
		Mentions result = new Mentions(getName());
		Iterator<AbstractProcessor> it = recognizers.iterator();
		
		// first get the dates
		AbstractProcessor wikipediaDater = it.next();
		Mentions dates = mentions.get(wikipediaDater);
		result.addMentions(dates);
		
		// then add the rest of the (non-overlapping) mentions
		AbstractProcessor combiner = it.next();
		Mentions ents = mentions.get(combiner);
		List<AbstractMention<?>> mentList = ents.getMentions();
		for(AbstractMention<?> mention: mentList)
		{	if(!result.isMentionOverlapping(mention))
				result.addMention(mention);
		}
		
		logger.decreaseOffset();
		return result;
	}
}
