package org.earthcube.geosoft.data.api.impl.kb;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.earthcube.geosoft.data.api.DataAPI;
import org.earthcube.geosoft.util.KBUtils;
import org.earthcube.geosoft.data.classes.DataItem;
import org.earthcube.geosoft.data.classes.DataTree;
import org.earthcube.geosoft.data.classes.DataTreeNode;
import org.earthcube.geosoft.data.classes.MetadataProperty;
import org.earthcube.geosoft.data.classes.MetadataValue;

import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.KBTriple;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;

public class DataKB implements DataAPI {
	protected KBAPI kb;
	protected KBAPI ontkb;
	protected KBAPI libkb;

	protected String dcns;
	protected String dclibns;
	protected String dcdomns;

	protected String liburl;
	protected String onturl;
	protected String dcurl;
	
	protected String datadir;

	protected HashMap<String, KBObject> objPropMap;
	protected HashMap<String, KBObject> dataPropMap;
	protected HashMap<String, KBObject> conceptMap;

	protected HashMap<String, String> conceptNameFormat;

	protected OntFactory ontologyFactory;
	protected String tdbRepository;

	private String topclass;
	private String topmetric;
	
	public DataKB(Properties props, boolean create_writers) {
		this.dcurl = props.getProperty("ont.data.url");
		this.onturl = props.getProperty("ont.domain.data.url");
		this.liburl = props.getProperty("lib.domain.data.url");
		this.datadir = props.getProperty("lib.domain.data.storage");

		String hash = "#";
		this.dcns = dcurl + hash;
		this.dcdomns = onturl + hash;
		this.dclibns = liburl + hash;

		this.tdbRepository = props.getProperty("tdb.repository.dir");
		this.ontologyFactory = new OntFactory(OntFactory.JENA, this.tdbRepository);
		
    this.topclass = this.dcns + "FileType";
    this.topmetric = this.dcns + "Metrics";
    
		this.initializeAPI(create_writers, false);
	}
	
	protected void initializeAPI(boolean create_writers, boolean create_if_empty) {
		try {
			this.kb = this.ontologyFactory.getKB(liburl, OntSpec.PELLET, create_if_empty);
			this.kb.importFrom(this.ontologyFactory.getKB(onturl, OntSpec.PLAIN, create_if_empty));
			this.kb.importFrom(this.ontologyFactory.getKB(dcurl, OntSpec.PLAIN, create_if_empty, true));
			if (create_writers) {
				this.ontkb = this.ontologyFactory.getKB(onturl, OntSpec.PLAIN);
				this.libkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
			}
			this.initializeMaps();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeMaps() {
		this.objPropMap = new HashMap<String, KBObject>();
		this.dataPropMap = new HashMap<String, KBObject>();
		this.conceptMap = new HashMap<String, KBObject>();
		this.conceptNameFormat = new HashMap<String, String>();

		for (KBObject prop : this.kb.getAllObjectProperties()) {
			this.objPropMap.put(prop.getName(), prop);
		}
		for (KBObject prop : this.kb.getAllDatatypeProperties()) {
			this.dataPropMap.put(prop.getName(), prop);
		}
		for (KBObject con : this.kb.getAllClasses()) {
			this.conceptMap.put(con.getName(), con);
			for (String comment : this.kb.getAllComments(con)) {
				if (comment.startsWith("NameFormat=")) {
					this.conceptNameFormat
							.put(con.getID(), comment.replaceFirst("NameFormat=", ""));
				}
			}
		}
	}
	
	public String getDataLocation(String dataid) {
		KBObject locprop = this.kb.getProperty(this.dcns + "hasLocation");
		KBObject dobj = this.kb.getIndividual(dataid);
		KBObject locobj = this.kb.getPropertyValue(dobj, locprop);
		if (locobj != null && locobj.getValue() != null)
			return locobj.getValue().toString();
		else {
			String location = this.getDefaultDataLocation(dataid);
			File f = new File(location);
			if(f.exists())
				return location;
		}
		return null;
	}
	
	public String getDefaultDataLocation(String dataid) {
		KBObject dobj = this.kb.getResource(dataid);
		return this.datadir + File.separator + dobj.getName();
	}

	  @Override
	  public boolean save() {
	    if(this.ontkb != null && this.libkb != null)
	      return this.ontkb.save() &&
	          this.libkb.save();
	    return false;
	  }
	  
	  @Override 
	  public void end() {
	    if(this.kb != null)
	      this.kb.end();
	    if(this.ontkb != null)
	      this.ontkb.end();
	    if(this.libkb != null)
	      this.libkb.end();
	  }

	  @Override
	  public DataTree getDataHierarchy() {
	    return this.createHierarchy(this.topclass, false);
	  }
	  
	  @Override
	  public DataTree getDatatypeHierarchy() {
	    return this.createHierarchy(this.topclass, true);
	  }

	  @Override
	  public DataTree getMetricsHierarchy() {
	    return this.createHierarchy(this.topmetric, false);
	  }

	  @Override
	  public ArrayList<String> getAllDatatypeIds() {
	    ArrayList<String> list = new ArrayList<String>();
	    KBObject top = this.kb.getConcept(this.topclass);
	    ArrayList<KBObject> types = this.kb.getSubClasses(top, false);
	    for (KBObject type : types) {
	      if (!type.getNamespace().equals(this.dcdomns) && !type.getNamespace().equals(this.dcns))
	        continue;
	      list.add(type.getID());
	    }
	    return list;
	  }

	  @Override
	  public ArrayList<MetadataProperty> getAllMetadataProperties() {
	    KBObject mprop = this.kb.getProperty(this.dcns + "hasMetrics");
	    KBObject dmprop = this.kb.getProperty(this.dcns + "hasDataMetrics");
	    ArrayList<KBObject> properties = this.kb.getSubPropertiesOf(mprop, false);
	    properties.addAll(this.kb.getSubPropertiesOf(dmprop, false));
	    return createMetadataProperties(properties);
	  }

	  @Override
	  public ArrayList<MetadataProperty> getMetadataProperties(String dtypeid, boolean direct) {
	    KBObject datatype = this.kb.getConcept(dtypeid);
	    ArrayList<KBObject> properties = this.kb.getPropertiesOfClass(datatype, direct);
	    return createMetadataProperties(properties);
	  }

	  @Override
	  public DataItem getDatatypeForData(String dataid) {
	    KBObject data = this.kb.getIndividual(dataid);
	    KBObject cls = this.kb.getClassOfInstance(data);
	    if (cls != null)
	      return new DataItem(cls.getID(), DataItem.DATATYPE);
	    return null;
	  }

	  @Override
	  public ArrayList<DataItem> getDataForDatatype(String dtypeid, boolean direct) {
	    KBObject datatype = this.kb.getConcept(dtypeid);
	    ArrayList<KBObject> datas = this.kb.getInstancesOfClass(datatype, direct);
	    ArrayList<DataItem> list = new ArrayList<DataItem>();
	    for (KBObject data : datas) {
	      list.add(new DataItem(data.getID(), DataItem.DATA));
	    }
	    return list;
	  }

	  @Override
	  public String getTypeNameFormat(String dtypeid) {
	    KBObject datatype = this.kb.getConcept(dtypeid);
	    Pattern pat = Pattern.compile("^NameFormat=(.+)$");
	    for (String comment : this.kb.getAllComments(datatype)) {
	      Matcher m = pat.matcher(comment);
	      if (m.find()) {
	        return m.group(1);
	      }
	    }
	    return null;
	  }

	  @Override
	  public ArrayList<MetadataValue> getMetadataValues(String dataid, ArrayList<String> propids) {
	    KBObject data = this.kb.getIndividual(dataid);
	    ArrayList<MetadataValue> values = new ArrayList<MetadataValue>();
	    for (String propid : propids) {
	      KBObject prop = this.kb.getProperty(propid);
	      ArrayList<KBObject> vals = this.kb.getPropertyValues(data, prop);
	      for (KBObject val : vals) {
	        if (val.isLiteral())
	          values.add(new MetadataValue(propid, val.getValue(), MetadataValue.DATATYPE));
	        else
	          values.add(new MetadataValue(propid, val.getID(), MetadataValue.OBJECT));
	      }
	    }
	    return values;
	  }

	  @Override
	  public MetadataProperty getMetadataProperty(String propid) {
	    if (!this.kb.containsResource(propid))
	      return null;
	    KBObject property = this.kb.getProperty(propid);
	    return this.createMetadataProperty(property);
	  }

	  @Override
	  public boolean addDatatype(String dtypeid, String parentid) {
	    KBObject dtype = this.ontkb.createClass(dtypeid, parentid);
	    return (dtype != null);
	  }

	  @Override
	  public boolean removeDatatype(String dtypeid) {
	    KBObject cls = this.kb.getConcept(dtypeid);
	    // Remove all files
	    ArrayList<KBObject> files = this.kb.getInstancesOfClass(cls, true);
	    for (KBObject file : files) {
	      this.removeData(file.getID());
	    }
	    // Remove metadata properties
	    ArrayList<KBObject> props = this.kb.getPropertiesOfClass(cls, false);
	    for (KBObject prop : props) {
	      MetadataProperty mprop = this.getMetadataProperty(prop.getID());
	      if(mprop.getDomains().contains(dtypeid)) {
	        if(mprop.getDomains().size() > 1)
	          this.removeMetadataPropertyDomain(prop.getID(), dtypeid);
	        else
	          this.removeMetadataProperty(prop.getID());
	      }
	    }
	    // Remove all subclasses (recursive call)
	    ArrayList<KBObject> subclses = this.kb.getSubClasses(cls, true);
	    for (KBObject subcls : subclses) {
	      if (!subcls.isNothing())
	        this.removeDatatype(subcls.getID());
	    }
	    // Finally remove the class itself
	    KBUtils.removeAllTriplesWith(this.ontkb, dtypeid, false);
	    return true;
	  }

	  @Override
	  public boolean renameDatatype(String newtypeid, String oldtypeid) {
	    KBUtils.renameAllTriplesWith(this.ontkb, oldtypeid, newtypeid, false);
	    KBUtils.renameAllTriplesWith(this.libkb, oldtypeid, newtypeid, false);
	    return true;
	  }

	  @Override
	  public boolean moveDatatypeParent(String dtypeid, String fromtypeid, String totypeid) {
	    KBObject cls = this.kb.getConcept(dtypeid);
	    KBObject fromcls = this.kb.getConcept(fromtypeid);
	    KBObject tocls = this.kb.getConcept(totypeid);
	    ArrayList<KBObject> oldprops = this.kb.getPropertiesOfClass(fromcls, false);
	    ArrayList<KBObject> newprops = this.kb.getPropertiesOfClass(tocls, false);
	    ArrayList<KBObject> removedProps = new ArrayList<KBObject>(); 
	    for(KBObject oldprop : oldprops) {
	      if(!newprops.contains(oldprop)) {
	        removedProps.add(oldprop);
	      }
	    }
	    
	    for(KBObject ind : this.kb.getInstancesOfClass(cls, false)) {
	      for(KBObject prop : removedProps) {
	        for(KBTriple triple : this.kb.genericTripleQuery(ind, prop, null)) 
	          this.libkb.removeTriple(triple);
	      }
	    }
	    
	    if(!this.ontkb.setSuperClass(dtypeid, totypeid))
	      return false;
	    
	    return true;
	  }

	  @Override
	  public boolean addData(String dataid, String dtypeid) {
	    KBObject dtypeobj = this.kb.getConcept(dtypeid);
	    this.libkb.createObjectOfClass(dataid, dtypeobj);
	    return true;
	  }

	  @Override
	  public boolean renameData(String newdataid, String olddataid) {
	    KBUtils.renameAllTriplesWith(this.libkb, olddataid, newdataid, false);
	    return true;
	  }

	  @Override
	  public boolean removeData(String dataid) {
	    // Remove data if it is in the catalog's data directory
	    String loc = this.getDataLocation(dataid);
	    if(loc != null) {
	      File f = new File(loc);
	      if(f.getParentFile().getAbsolutePath().equals(this.datadir))
	        f.delete();
	    }
	    KBUtils.removeAllTriplesWith(this.libkb, dataid, false);
	    return true;
	  }

	  @Override
	  public boolean setDataLocation(String dataid, String locuri) {
	    // What happens to existing file ?
	    KBObject locprop = this.kb.getProperty(this.dcns + "hasLocation");
	    KBObject dobj = this.libkb.getIndividual(dataid);
	    KBObject locobj = ontologyFactory.getDataObject(locuri);
	    this.libkb.setPropertyValue(dobj, locprop, locobj);
	    return true;
	  }

	  @Override
	  public boolean setTypeNameFormat(String dtypeid, String format) {
	    KBObject dtypeobj = this.ontkb.getConcept(dtypeid);
	    this.ontkb.setComment(dtypeobj, "NameFormat=" + format);
	    return true;
	  }

	  @Override
	  public boolean addDatatypePropertyValue(String dataid, String propid, Object val) {
	    KBObject dataobj = this.libkb.getIndividual(dataid);
	    KBObject pobj = this.kb.getProperty(propid);
	    KBObject valobj = this.ontologyFactory.getDataObject(val);
	    this.libkb.setPropertyValue(dataobj, pobj, valobj);
	    return true;
	  }

	  @Override
	  public boolean addDatatypePropertyValue(String dataid, String propid, String val, String xsdtype) {
	    KBObject dataobj = this.libkb.getIndividual(dataid);
	    KBObject pobj = this.kb.getProperty(propid);
	    KBObject valobj = this.kb.createXSDLiteral(val, xsdtype);
	    this.libkb.setPropertyValue(dataobj, pobj, valobj);
	    return true;
	  }

	  @Override
	  public boolean addObjectPropertyValue(String dataid, String propid, String valid) {
	    KBObject dataobj = this.libkb.getIndividual(dataid);
	    KBObject pobj = this.kb.getProperty(propid);
	    KBObject valobj = this.kb.getResource(valid);
	    this.libkb.setPropertyValue(dataobj, pobj, valobj);
	    return true;
	  }

	  @Override
	  public boolean removePropertyValue(String dataid, String propid, Object val) {
	    KBObject dataobj = this.libkb.getIndividual(dataid);
	    KBObject pobj = this.kb.getProperty(propid);
	    KBObject valobj = this.ontologyFactory.getDataObject(val);
	    this.libkb.removeTriple(dataobj, pobj, valobj);
	    return true;
	  }

	  @Override
	  public boolean removeAllPropertyValues(String dataid, ArrayList<String> propids) {
	    KBObject dataobj = this.libkb.getIndividual(dataid);
	    for (String propid : propids) {
	      KBObject pobj = this.kb.getProperty(propid);
	      ArrayList<KBObject> vals = this.kb.getPropertyValues(dataobj, pobj);
	      for (KBObject val : vals) {
	        this.libkb.removeTriple(dataobj, pobj, val);
	      }
	    }
	    return true;
	  }

	  @Override
	  public boolean addMetadataProperty(String propid, String domain, String range) {
	    if (range.contains(KBUtils.XSD)) {
	      this.ontkb.createDatatypeProperty(propid, this.dcns + "hasDataMetrics");
	    } else {
	      this.ontkb.createObjectProperty(propid, this.dcns + "hasMetrics");
	    }
	    this.ontkb.addPropertyDomainDisjunctive(propid, domain);
	    this.ontkb.setPropertyRange(propid, range);
	    return true;
	  }

	  @Override
	  public boolean addMetadataPropertyDomain(String propid, String domain) {
	    this.ontkb.addPropertyDomainDisjunctive(propid, domain);
	    return true;
	  }
	  
	  @Override
	  public boolean removeMetadataPropertyDomain(String propid, String domain) {
	    this.ontkb.removePropertyDomainDisjunctive(propid, domain);
	    return true;
	  }
	  
	  @Override
	  public boolean removeMetadataProperty(String propid) {
	    // Remove all domains manually
	    // - Due to bug in removing triples with union classes
	    MetadataProperty prop = this.getMetadataProperty(propid);
	    for(String domid : prop.getDomains())
	      this.ontkb.removePropertyDomainDisjunctive(propid, domid);
	    
	    // Remove all triples (this skips removing domain union classes)
	    KBUtils.removeAllTriplesWith(this.ontkb, propid, true);
	    KBUtils.removeAllTriplesWith(this.libkb, propid, true);
	    return true;
	  }

	  @Override
	  public boolean renameMetadataProperty(String oldid, String newid) {
	    // First remove all domains and then readd them later
	    // - Due to bug in renaming triples
	    MetadataProperty prop = this.getMetadataProperty(oldid);
	    for(String domid : prop.getDomains())
	      this.ontkb.removePropertyDomainDisjunctive(oldid, domid);
	    
	    // Rename all triples (this skips renaming domain union classes)
	    KBUtils.renameAllTriplesWith(this.ontkb, oldid, newid, true);
	    KBUtils.renameAllTriplesWith(this.libkb, oldid, newid, true);
	    
	    for(String domid : prop.getDomains())
	      this.ontkb.addPropertyDomainDisjunctive(newid, domid);
	    return true;
	  }
	  
	  @Override
	  public void delete() {
	    this.libkb.delete();
	    this.ontkb.delete();
	  }
	  
	  /*
	   * Private Helper functions below
	   */

	  private DataTree createHierarchy(String classid, boolean types_only) {
	    DataItem rootitem = new DataItem(classid, DataItem.DATATYPE);
	    DataTreeNode rootnode = new DataTreeNode(rootitem);
	    ArrayList<DataTreeNode> queue = new ArrayList<DataTreeNode>();
	    queue.add(rootnode);
	    while (!queue.isEmpty()) {
	      DataTreeNode node = queue.remove(0);
	      DataItem item = node.getItem();
	      if (item.getType() == DataItem.DATATYPE) {
	        KBObject cls = this.kb.getConcept(item.getID());
	        if (cls == null)
	          continue;
	        if (!types_only) {
	          ArrayList<KBObject> instances = this.kb.getInstancesOfClass(cls, true);
	          for (KBObject inst : instances) {
	            DataItem institem = new DataItem(inst.getID(), DataItem.DATA);
	            DataTreeNode childnode = new DataTreeNode(institem);
	            node.addChild(childnode);
	          }
	        }
	        ArrayList<KBObject> subclasses = this.kb.getSubClasses(cls, true);
	        for (KBObject subcls : subclasses) {
	          if (!subcls.getNamespace().equals(this.dcdomns)
	              && !subcls.getNamespace().equals(this.dcdomns))
	            continue;
	          DataItem institem = new DataItem(subcls.getID(), DataItem.DATATYPE);
	          DataTreeNode childnode = new DataTreeNode(institem);
	          node.addChild(childnode);
	          queue.add(childnode);
	        }
	      }
	    }
	    DataTree tree = new DataTree(rootnode);

	    return tree;
	  }
	  
	  private ArrayList<MetadataProperty> createMetadataProperties(ArrayList<KBObject> properties) {
	    ArrayList<MetadataProperty> list = new ArrayList<MetadataProperty>();
	    for (KBObject property : properties) {
	      // Ignore properties not declared in this domain
	      if (!property.getNamespace().equals(this.dcdomns))
	        continue;
	      MetadataProperty prop = this.createMetadataProperty(property);
	      if (prop != null)
	        list.add(prop);
	    }
	    return list;
	  }

	  private MetadataProperty createMetadataProperty(KBObject property) {
	    if (property == null)
	      return null;
	    int proptype = this.kb.isDatatypeProperty(property) ? MetadataProperty.DATATYPE
	        : MetadataProperty.OBJECT;
	    MetadataProperty prop = new MetadataProperty(property.getID(), proptype);
	    
	    // Query for domain and range from the non-inference ontkb model (otherwise we get inferenced domains as well)
	    ArrayList<KBObject> domains = this.ontkb.getPropertyDomainsDisjunctive(property);
	    KBObject range = this.ontkb.getPropertyRange(property);
	    for(KBObject domain : domains)
	      prop.addDomain(domain.getID());
	    if (range != null) {
	      prop.setRange(range.getID());
	    }
	    return prop;
	  }
}
