package org.earthcube.geosoft.portal.classes.importer;

import java.util.Properties;

import org.earthcube.geosoft.portal.classes.importer.api.ImportAPI;
import org.earthcube.geosoft.portal.classes.importer.api.impl.csdms.ImportCSDMS;

public class ImporterFactory {

  public static ImportAPI getAPI(String repo_id, Properties props) {
    if(repo_id.equals("CSDMS")) {
      return new ImportCSDMS(props);
    }
    return null;
  }
}
