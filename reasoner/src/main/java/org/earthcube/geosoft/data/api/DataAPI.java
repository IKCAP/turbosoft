package org.earthcube.geosoft.data.api;

import java.util.ArrayList;

import org.earthcube.geosoft.data.classes.DataItem;
import org.earthcube.geosoft.data.classes.DataTree;
import org.earthcube.geosoft.data.classes.MetadataProperty;
import org.earthcube.geosoft.data.classes.MetadataValue;

/**
 * The interface used by data catalog viewers to query the data. Read/Only
 * access
 */
public interface DataAPI {
	// Query
	DataTree getDataHierarchy(); // Tree List of Data and Datatypes
	
	DataTree getDatatypeHierarchy(); // Tree List of Datatypes

	DataTree getMetricsHierarchy(); // Tree List of Metrics and Metric
											// types

	ArrayList<String> getAllDatatypeIds();

	MetadataProperty getMetadataProperty(String propid);

	ArrayList<MetadataProperty> getMetadataProperties(String dtypeid, boolean direct);

	ArrayList<MetadataProperty> getAllMetadataProperties();

	DataItem getDatatypeForData(String dataid);

	ArrayList<DataItem> getDataForDatatype(String dtypeid, boolean direct);

	String getTypeNameFormat(String dtypeid);

	String getDataLocation(String dataid);

	ArrayList<MetadataValue> getMetadataValues(String dataid, ArrayList<String> propids);

	// Write
	boolean addDatatype(String dtypeid, String parentid);

	boolean removeDatatype(String dtypeid);

	boolean renameDatatype(String newtypeid, String oldtypeid);

	boolean moveDatatypeParent(String dtypeid, String fromtypeid, String totypeid);

	boolean addData(String dataid, String dtypeid);

	boolean renameData(String newdataid, String olddataid);

	boolean removeData(String dataid);

	boolean setDataLocation(String dataid, String locuri);

	boolean setTypeNameFormat(String dtypeid, String format);

	boolean addObjectPropertyValue(String dataid, String propid, String valid);

	boolean addDatatypePropertyValue(String dataid, String propid, Object val);

	boolean addDatatypePropertyValue(String dataid, String propid, String val, String xsdtype);

	boolean removePropertyValue(String dataid, String propid, Object val);

	boolean removeAllPropertyValues(String dataid, ArrayList<String> propids);

	boolean addMetadataProperty(String propid, String domain, String range);
	
	boolean addMetadataPropertyDomain(String propid, String domain);

	boolean removeMetadataProperty(String propid);
	
	boolean removeMetadataPropertyDomain(String propid, String domain);

	boolean renameMetadataProperty(String oldid, String newid);

	// Sync/Save
	boolean save();
	
	void end();
	
	void delete();
}
