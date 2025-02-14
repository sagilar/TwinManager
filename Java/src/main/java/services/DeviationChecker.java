package services;

import management.TwinManager;
import model.Twin;
import model.composition.Attribute;

public class DeviationChecker {
	
	public static Object deviationChecking(TwinManager twinManager, String variable, String twinName1, String twinName2, double threshold) {
		Attribute attrTwin1 = twinManager.getAttributeValue(variable,twinName1);
		Attribute attrTwin2 = twinManager.getAttributeValue(variable,twinName2);
		
		if (attrTwin1.getValue() == null) {
			System.out.println(twinName1 + "'s " + variable + " is null. Changing to 0.0");
			attrTwin1.setValue(0.0);
		}
		
		if (attrTwin2.getValue() == null) {
			System.out.println(twinName2 + "'s " + variable + " is null. Changing to 0.0");
			attrTwin2.setValue(0.0);
		}
		
		if ( (double)attrTwin1.getValue() > (1.0+threshold) * (double)attrTwin2.getValue()) {
			return twinName1 + "'s " + variable + " is greater than " + twinName2 + "'s " + variable + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		} else if ((double)attrTwin1.getValue() < (1.0 - threshold) * (double)attrTwin2.getValue()) {
			return twinName1 + "'s " + variable + " is lower than " + twinName2 + "'s " + variable + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		} else {
			return twinName1 + "'s " + variable + " within the boundaries for " + twinName2 + "'s " + variable + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		}		
	}
	
	public static Object deviationChecking(TwinManager twinManager, String variable1, String variable2, String twinName1, String twinName2, double threshold) {
		Attribute attrTwin1 = twinManager.getAttributeValue(variable1,twinName1);
		Attribute attrTwin2 = twinManager.getAttributeValue(variable2,twinName2);
		if (attrTwin1.getValue() == null) {
			System.out.println(twinName1 + "'s " + variable1 + " is null. Changing to 0.0");
			attrTwin1.setValue(0.0);
		}
		
		if (attrTwin2.getValue() == null) {
			System.out.println(twinName2 + "'s " + variable2 + " is null. Changing to 0.0");
			attrTwin2.setValue(0.0);
		}
		if ( (double)attrTwin1.getValue() > (1.0+threshold) * (double)attrTwin2.getValue()) {
			return twinName1 + "'s " + variable1 + " is greater than " + twinName2 + "'s " + variable2 + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		} else if ((double)attrTwin1.getValue() < (1.0 - threshold) * (double)attrTwin2.getValue()) {
			return twinName1 + "'s " + variable1 + " is lower than " + twinName2 + "'s " + variable2 + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		} else {
			return twinName1 + "'s " + variable1 + " within the boundaries for " + twinName2 + "'s " + variable2 + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		}		
	}

	public static Object deviationCheckingOnSystem(TwinManager twinManager, String variable1, String variable2, String twinSystemName1, String twinSystemName2, double threshold) {
		Attribute attrTwin1 = twinManager.getSystemAttributeValue(variable1,twinSystemName1);
		Attribute attrTwin2 = twinManager.getSystemAttributeValue(variable2,twinSystemName2);
		if (attrTwin1.getValue() == null) {
			System.out.println(twinSystemName1 + "'s " + variable1 + " is null. Changing to 0.0");
			attrTwin1.setValue(0.0);
		}
		
		if (attrTwin2.getValue() == null) {
			System.out.println(twinSystemName2 + "'s " + variable2 + " is null. Changing to 0.0");
			attrTwin2.setValue(0.0);
		}
		if ( (double)attrTwin1.getValue() > (1.0+threshold) * (double)attrTwin2.getValue()) {
			return twinSystemName1 + "'s " + variable1 + " is greater than " + twinSystemName2 + "'s " + variable2 + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		} else if ((double)attrTwin1.getValue() < (1.0 - threshold) * (double)attrTwin2.getValue()) {
			return twinSystemName1 + "'s " + variable1 + " is lower than " + twinSystemName2 + "'s " + variable2 + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		} else {
			return twinSystemName1 + "'s " + variable1 + " within the boundaries for " + twinSystemName2 + "'s " + variable2 + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		}		
	}

	public static Object deviationCheckingOnSystem(TwinManager twinManager, String variable1, String variable2, String twinSystemName1, String twinSystemName2, String twinName1, String twinName2, double threshold) {
		Attribute attrTwin1 = twinManager.getSystemAttributeValue(variable1,twinSystemName1, twinName1);
		Attribute attrTwin2 = twinManager.getSystemAttributeValue(variable2,twinSystemName2, twinName2);
		if (attrTwin1.getValue() == null) {
			System.out.println(twinName1 + "'s (" + twinSystemName1 + ")" + variable1 + " is null. Changing to 0.0");
			attrTwin1.setValue(0.0);
		}
		
		if (attrTwin2.getValue() == null) {
			System.out.println(twinName2 + "'s (" + twinSystemName2 + ")" + variable2 + " is null. Changing to 0.0");
			attrTwin2.setValue(0.0);
		}
		if ( (double)attrTwin1.getValue() > (1.0+threshold) * (double)attrTwin2.getValue()) {
			return twinName1 + "'s (" + twinSystemName1 + ")" + variable1 + " is greater than " + twinName2 + "'s (" + twinSystemName2 + ")" + variable2 + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		} else if ((double)attrTwin1.getValue() < (1.0 - threshold) * (double)attrTwin2.getValue()) {
			return twinName1 + "'s (" + twinSystemName1 + ")" + variable1 + " is lower than " + twinName2 + "'s (" + twinSystemName2 + ")" + variable2 + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		} else {
			return twinName1 + "'s (" + twinSystemName1 + ")" + variable1 + " within the boundaries for " + twinName2 + "'s (" + twinSystemName2 + ")" + variable2 + " for the threshold " + String.valueOf(threshold * 100.0) + "%";
		}		
	}
}
