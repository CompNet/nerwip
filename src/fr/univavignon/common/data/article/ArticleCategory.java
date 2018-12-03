package fr.univavignon.common.data.article;

/*
 * Nerwip - Named Entity Extraction in Wikipedia Pages
 * Copyright 2011-18 Vincent Labatut et al.
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

/**
 * Category of an article.
 * 
 * @author Vincent Labatut
 */
public enum ArticleCategory
{	/** Scientists and science-related activity, including humanities, letters, etc. */
	ACADEMIA,
	/** Architects or building engineers */
	ARCHITECTURE,
	/** Artists, including writters */
	ART,
	/** Businessmen, managers, etc. */
	BUSINESS,
	/** Lawyers, judges, etc. */
	LAW,
	/** Medical-related positions */
	MEDICINE,
	/** TV, cinema, press related positions */
	MEDIA,
	/** Soldiers, military leaders */
	MILITARY,
	/** Politicians, elected positions */
	POLITICS,
	/** Priests, religious leaders, etc. */
	RELIGION,
	/** Sportsmen */ 
	SPORT,
	
	/** Other categories */
	OTHER;
}