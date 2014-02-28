package org.earthcube.geosoft.software.classes;

import org.earthcube.geosoft.util.URIEntity;

public class SoftwareRole extends URIEntity {
	private static final long serialVersionUID = 1L;

	String role;
	String type;

	public SoftwareRole(String id) {
		super(id);
	}

	public String getRoleName() {
		return role;
	}

	public void setRoleName(String role) {
		this.role = role;
	}

	public String getType() {
		return type;
	}

	public void setType(String typeid) {
		this.type = typeid;
	}
}
