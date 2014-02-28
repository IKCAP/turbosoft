package org.earthcube.geosoft.portal.classes.importer.api;

import org.earthcube.geosoft.software.classes.Software;

public interface ImportAPI {
  Software importSoftware(String softwareid, String typeid, String repository_softwareid);
}
