package com.gamearoosdevelopment.realistictrafficcontrol.util;

import com.gamearoosdevelopment.realistictrafficcontrol.oc.TrafficLightCardDriver;

import li.cil.oc.api.Driver;

public class OpenComputersHelper {

	public static void addOCDriver()
	{
		Driver.add(new TrafficLightCardDriver()	);
	}
}
