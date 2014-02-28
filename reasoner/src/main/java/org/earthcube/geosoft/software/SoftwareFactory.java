package org.earthcube.geosoft.software;

import java.util.Properties;

import org.earthcube.geosoft.software.api.SoftwareAPI;
import org.earthcube.geosoft.software.api.impl.kb.SoftwareKB;

public class SoftwareFactory {

	public static SoftwareAPI getAPI(Properties props) {
		return new SoftwareKB(props);
	}
}
