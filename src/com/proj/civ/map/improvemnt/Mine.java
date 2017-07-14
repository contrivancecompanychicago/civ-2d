package com.proj.civ.map.improvemnt;

import java.util.ArrayList;
import java.util.List;

import com.proj.civ.map.terrain.Feature;

public class Mine extends Improvement {
	private List<Feature> validFeatures = new ArrayList<Feature>();
	
	public Mine() {
		super(0, 1, 0, 0);
		
		validFeatures.add(Feature.WOODS);
		super.addAllValidFeatures(validFeatures);
	}

}
