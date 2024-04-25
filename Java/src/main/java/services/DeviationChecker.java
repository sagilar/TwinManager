package services;

import management.TwinManager;
import model.Twin;

public class DeviationChecker {
	
	public static Object deviationChecking(TwinManager twinManager, String variable, String twinName1, String twinName2, double threshold) {
		if ( (double)twinManager.getAttributeValue(variable,twinName1).getValue() > (1.0+threshold) * (double)twinManager.getAttributeValue(variable,twinName2).getValue()) {
			return twinName1 + "'s " + variable + " is greater than " + twinName2 + "'s " + variable + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		} else if ((double)twinManager.getAttributeValue(variable,twinName1).getValue() < (1.0 - threshold) * (double)twinManager.getAttributeValue(variable,twinName2).getValue()) {
			return twinName1 + "'s " + variable + " is lower than " + twinName2 + "'s " + variable + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		} else {
			return twinName1 + "'s " + variable + " within the boundaries for " + twinName2 + "'s " + variable + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		}		
	}
	
	public static Object deviationChecking(TwinManager twinManager, String variable1, String variable2, String twinName1, String twinName2, double threshold) {
		if ( (double)twinManager.getAttributeValue(variable1,twinName1).getValue() > (1.0+threshold) * (double)twinManager.getAttributeValue(variable2,twinName2).getValue()) {
			return twinName1 + "'s " + variable1 + " is greater than " + twinName2 + "'s " + variable2 + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		} else if ((double)twinManager.getAttributeValue(variable1,twinName1).getValue() < (1.0 - threshold) * (double)twinManager.getAttributeValue(variable2,twinName2).getValue()) {
			return twinName1 + "'s " + variable1 + " is lower than " + twinName2 + "'s " + variable2 + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		} else {
			return twinName1 + "'s " + variable1 + " within the boundaries for " + twinName2 + "'s " + variable2 + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		}		
	}
}
