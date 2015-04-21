package org.opentripplanner.routing.algorithm;

import java.util.Collection;
import java.util.Iterator;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

@SuppressWarnings("unchecked")
public class PathDiscardingRaptorStateStore implements RaptorStateStore {
	// suppressing warnings because generic arrays don't work in Java . . .
    @SuppressWarnings("rawtypes")
	private TObjectIntMap[] matrix;

    private TObjectIntMap<TransitStop> bestStops;

    public int maxTime;
    
    int current = 0;
    
    @Override
    public boolean put(TransitStop t, int time, boolean transfer) {
    	/*if (time > maxTime)
    		return false;*/

        // This does not store internal algorithm state as it used to, but rather only the output.
        // The reasoning is that, in dynamic programming/range RAPTOR mode, bestStops is carried over between runs of
        // the algorithm. But you still want to propagate a non-optimal time with fewer transfers, because the
        // optimal time at this stop might have already used up all of the transfers.
        if (!transfer && time < bestStops.get(t))
            bestStops.put(t, time);

        if (time < matrix[current].get(t)) {
             matrix[current].put(t, time);
            return true;
        }

        return false;
    }

    @Override
    public void proceed() {
    	for (TObjectIntIterator<TransitStop> it = matrix[current].iterator(); it.hasNext();) {
    		it.advance();
    		
    		if (it.value() < matrix[current + 1].get(it.key()))
    			matrix[current + 1].put(it.key(), it.value());
    	}
        current++;
    }
    
    public int getTime (TransitStop t) {
    	return bestStops.get(t);
    }
    
    public int getPrev (TransitStop t) {
    	return matrix[current - 1].get(t);
    }
    
    /**
     * Restart the search from the first round. Used when running repeated RAPTOR searches using the dynamic programming
     * algorithm.
     * 
     * TODO write up the dynamic programming algorithm.
     */
    public void restart () {
    	current = 0;
    }
    
    public TObjectIntIterator<TransitStop> iterator () {
    	return bestStops.iterator();
    }
    
    /** Create a new store with the given number of rounds. Remember to include the initial walk as a "round" */
    public PathDiscardingRaptorStateStore(int rounds) {
    	this(rounds, Integer.MAX_VALUE);
    }
    
    public PathDiscardingRaptorStateStore(int rounds, int maxTime) {
    	this.maxTime = maxTime;
    	
    	matrix = new TObjectIntMap[rounds];
    	
    	for (int i = 0; i < rounds; i++) {
    		matrix[i] = new TObjectIntHashMap<TransitStop>(1000, 0.75f, Integer.MAX_VALUE);
    	}
    	
    	bestStops = new TObjectIntHashMap<TransitStop>(1000, 0.75f, Integer.MAX_VALUE);
    }
    
    public Collection<TransitStop> getTouchedStopsIncludingTransfers () {
    	return matrix[current].keySet();
    }
}
