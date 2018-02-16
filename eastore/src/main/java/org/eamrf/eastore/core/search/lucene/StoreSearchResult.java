package org.eamrf.eastore.core.search.lucene;

import java.util.ArrayList;
import java.util.List;

import org.eamrf.core.util.CollectionUtil;

/**
 * Class which encapsulates search result data from lucene
 * 
 * @author slenzi
 *
 */
public class StoreSearchResult {

	private String searchValue = null;
	
	private int numResults = 0;
	
	private List<StoreSearchHit> hits = null;
	
	public StoreSearchResult() {
		
	}

	/**
	 * @return the searchValue
	 */
	public String getSearchValue() {
		return searchValue;
	}

	/**
	 * @param searchValue the searchValue to set
	 */
	public void setSearchValue(String searchValue) {
		this.searchValue = searchValue;
	}

	/**
	 * @return the numResults
	 */
	public int getNumResults() {
		return numResults;
	}

	/**
	 * @param numResults the numResults to set
	 */
	public void setNumResults(int numResults) {
		this.numResults = numResults;
	}

	/**
	 * @return the hits
	 */
	public List<StoreSearchHit> getHits() {
		return hits;
	}

	/**
	 * @param hits the hits to set
	 */
	public void setHits(List<StoreSearchHit> hits) {
		this.hits = hits;
	}

	/**
	 * Add a hit to the search result
	 * 
	 * @param hit
	 */
	public void addHit(StoreSearchHit hit) {
		if(hits == null) {
			hits = new ArrayList<StoreSearchHit>();
		}
		hits.add(hit);
	}
	
	/**
	 * Check if the search result returned any hits (i.e. found matches)
	 * 
	 * @return
	 */
	public boolean haveHits() {
		if(CollectionUtil.isEmpty(hits)) {
			return false;
		}
		return true;
	}
	
}
