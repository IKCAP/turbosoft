package org.earthcube.geosoft.data;

import java.util.Properties;

import org.earthcube.geosoft.data.api.*;
import org.earthcube.geosoft.data.api.impl.kb.*;

public class DataFactory {

	/**
	 * @param props
	 *            The properties should contain: lib.domain.data.url,
	 *            ont.domain.data.url, ont.data.url tdb.repository.dir
	 *            (optional)
	 */
	public static DataAPI getAPI(Properties props) {
		return new DataKB(props, true);
	}
}
