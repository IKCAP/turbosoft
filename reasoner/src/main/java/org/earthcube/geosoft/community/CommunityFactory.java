package org.earthcube.geosoft.community;

import java.util.Properties;

import org.earthcube.geosoft.community.api.*;
import org.earthcube.geosoft.community.api.impl.kb.*;

public class CommunityFactory {

  /**
   * @param props
   *            The properties should contain: lib.domain.data.url,
   *            ont.domain.data.url, ont.data.url tdb.repository.dir
   *            (optional)
   */
  public static CommunityAPI getAPI(Properties props) {
    return new CommunityKB(props);
  }
}
