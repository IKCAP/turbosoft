package org.earthcube.geosoft.util;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;

public class URIEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	private URI id;
	private String label;
	
	HashMap<String, Object> provenance;
	
	public URIEntity(String id) {
		setID(id);
		this.provenance = new HashMap<String, Object>();
	}
	
	public URIEntity(String id, String label) {
	  setID(id);
	  this.label = label;
	  this.provenance = new HashMap<String, Object>();
	}

	public String getID() {
		if (id != null)
			return id.toString();
		else
			return null;
	}

	public void setID(String id) {
		try {
			this.id = new URI(id).normalize();
		} catch (Exception e) {
			System.err.println(id + " Not a URI. Only URIs allowed for IDs");
		}
	}
	
	public String getURL() {
		return this.getNamespace().replaceAll("#$", "");
	}

	public String getName() {
		if (id != null)
			return id.getFragment();
		else
			return null;
	}

	public String getNamespace() {
		if (id != null)
			return id.getScheme() + ":" + id.getSchemeSpecificPart() + "#";
		else
			return null;
	}

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }
  
	public HashMap<String, Object> getProvenance() {
    return provenance;
  }

  public void addProvenance(String propId, Object value) {
    this.provenance.put(propId, value);
  }

  public String toString() {
		return getName();
	}

	public int hashCode() {
		return id.hashCode();
	}

}
