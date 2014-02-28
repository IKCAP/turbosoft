package org.earthcube.geosoft.data.classes;

import org.earthcube.geosoft.util.URIEntity;

public class DataItem extends URIEntity {
	private static final long serialVersionUID = 1L;
	
	public static int DATATYPE = 1;
	public static int DATA = 2;

	int type;

	public DataItem(String id, int type) {
		super(id);
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}
