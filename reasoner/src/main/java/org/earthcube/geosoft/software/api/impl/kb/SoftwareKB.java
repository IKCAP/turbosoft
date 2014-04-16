package org.earthcube.geosoft.software.api.impl.kb;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.earthcube.geosoft.software.api.SoftwareAPI;
import org.earthcube.geosoft.software.api.impl.kb.SoftwareKB;
import org.earthcube.geosoft.software.classes.SWProperty;
import org.earthcube.geosoft.software.classes.SWPropertyValue;
import org.earthcube.geosoft.software.classes.Software;
import org.earthcube.geosoft.software.classes.SoftwareRole;
import org.earthcube.geosoft.software.classes.SoftwareType;
import org.earthcube.geosoft.software.classes.sn.SNAssumption;
import org.earthcube.geosoft.software.classes.sn.SNObject;
import org.earthcube.geosoft.software.classes.sn.SNOperator;
import org.earthcube.geosoft.software.classes.sn.SNQuantity;
import org.earthcube.geosoft.software.classes.sn.StandardName;
import org.earthcube.geosoft.util.KBUtils;
import org.earthcube.geosoft.util.URIEntity;

import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.KBTriple;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;
import edu.isi.wings.ontapi.rules.KBRuleList;

public class SoftwareKB implements SoftwareAPI {
  protected KBAPI kb;
  protected KBAPI writerkb;
  
  protected HashMap<String, KBAPI> skbs;
  protected HashMap<String, String> surls;
  
  protected String ontns;
  protected String onturl;
  protected String liburl;
  protected String rulesurl;
  protected String d_onturl;

  protected OntFactory ontologyFactory;

  protected HashMap<String, KBObject> objPropMap;
  protected HashMap<String, KBObject> dataPropMap;
  protected HashMap<String, KBObject> conceptMap;
  protected HashMap<String, SWProperty> provProps;

  protected ArrayList<KBTriple> domainKnowledge;
  protected HashMap<String, String> rulePrefixes;
  
  protected Properties props;
  
  /**
   * Constructor
   * 
   * @param props
   *            The properties should contain: ont.software.url, lib.software.url,
   *            rules.software.url, ont.domain.data.url, tdb.repository.dir
   */
  public SoftwareKB(Properties props) {
    this.props = props;
    
    String hash = "#";
    this.ontns = props.getProperty("ont.software.url") + hash;
    this.onturl = props.getProperty("ont.software.url");
    this.liburl = props.getProperty("lib.software.url");
    this.rulesurl = props.getProperty("rules.software.url");
    this.d_onturl = props.getProperty("ont.domain.data.url");
    
    String tdbRepository = props.getProperty("tdb.repository.dir");
    this.ontologyFactory = new OntFactory(OntFactory.JENA, tdbRepository);
    
    this.initializeAPI();
  }
  
  protected void initializeAPI() {
    try {
      this.kb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN, true);
      this.kb.importFrom(this.ontologyFactory.getKB(onturl, OntSpec.PLAIN, false, true));
      this.kb.importFrom(this.ontologyFactory.getKB(d_onturl, OntSpec.PLAIN, false));
      
      this.writerkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
      
      this.skbs = new HashMap<String, KBAPI>();
      this.surls = new HashMap<String, String>();
      
      this.createEntailments(this.kb);
      this.initializeMaps(this.kb);
      //this.initDomainKnowledge();
      //this.setRuleMappings(this.kb);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void initializeMaps(KBAPI kb) {
    this.objPropMap = new HashMap<String, KBObject>();
    this.dataPropMap = new HashMap<String, KBObject>();
    this.conceptMap = new HashMap<String, KBObject>();
    this.provProps = new HashMap<String, SWProperty>();

    for (KBObject prop : kb.getAllObjectProperties()) {
      this.objPropMap.put(prop.getName(), prop);
    }
    for (KBObject con : kb.getAllClasses()) {
      this.conceptMap.put(con.getName(), con);
    }
    for (KBObject odp : kb.getAllDatatypeProperties()) {
      this.dataPropMap.put(odp.getName(), odp);
    }
    KBObject propTopProp = kb.getProperty(this.ontns + "hasProvenance");
    for(KBObject pp : kb.getSubPropertiesOf(propTopProp , false)) {
      this.provProps.put(pp.getID(), this.getSWProperty(pp, false));
    }
  }
  
  private void createEntailments(KBAPI kb) {
    for(KBObject cls : kb.getAllClasses()) {
      if(!cls.getNamespace().equals(this.ontns))
        continue;
      for(KBObject supercls : kb.getSuperClasses(cls, true)) {
        for(KBObject obj : kb.getInstancesOfClass(cls, true)) {
          kb.addClassForInstance(obj, supercls);
        }
      }
    }
  }

  /*private void initDomainKnowledge() {
    // Create general domain knowledge data for use in rules
    domainKnowledge = new ArrayList<KBTriple>();
    KBObject rdfsSubProp = this.kb.getProperty(KBUtils.RDFS + "subPropertyOf");
    KBObject topObjProp = this.kb.getProperty(KBUtils.OWL + "topObjectProperty");
    KBObject topDataProp = this.kb.getProperty(KBUtils.OWL + "topDataProperty");
    domainKnowledge.addAll(kb.genericTripleQuery(null, rdfsSubProp, topObjProp));
    domainKnowledge.addAll(kb.genericTripleQuery(null, rdfsSubProp, topDataProp));
  }*/

  /**
   * Set Rule Prefix-Namespace Mappings Prefixes allowed in Rules: rdf, rdfs,
   * owl, xsd -- usual dc, dcdom -- data catalog pc, pcdom -- software
   * catalog ac, acdom -- synonyms for pc, pcdom
   */
  /*private void setRuleMappings(KBAPI kb) {
    rulePrefixes = new HashMap<String, String>();
    rulePrefixes.put("rdf", KBUtils.RDF);
    rulePrefixes.put("rdfs", KBUtils.RDFS);
    rulePrefixes.put("owl", KBUtils.OWL);
    rulePrefixes.put("xsd", KBUtils.XSD);
    rulePrefixes.put("ont", this.ontns);
    kb.setRulePrefixes(rulePrefixes);
  }*/
  
  @Override
  public SoftwareType getSoftwareTypesTree() {
    return this.getSoftwareType(this.ontns + "Software", true);
  }
  
  @Override
  public SoftwareType getSoftwareType(String id, boolean getSubtypes) {
    SoftwareType type = new SoftwareType(id);
    KBObject cls = this.kb.getConcept(id);
    type.setAnnotation(this.kb.getComment(cls));
    if(getSubtypes) {
      for(KBObject obj : this.kb.getSubClasses(cls, true)) {
        type.addSubtype(this.getSoftwareType(obj.getID(), getSubtypes));
      }
    }
    return type;
  }
  
  @Override
  public ArrayList<Software> getSoftwares(boolean details) {
    ArrayList<Software> softwares = new ArrayList<Software>();
    KBObject softwarecls = this.kb.getConcept(this.ontns + "Software");
    ArrayList<KBObject> softwareobjs = this.kb.getInstancesOfClass(softwarecls, false);
    for (KBObject softwareobj : softwareobjs) {
      softwares.add(getSoftware(softwareobj.getID(), details));
    }
    return softwares;
  }
  
  @Override
  public Software getSoftware(String softwareid, boolean details) {
    KBObject softwareobj = kb.getIndividual(softwareid);
    if(softwareobj == null) return null;

    KBObject holderprop = this.writerkb.getProperty(this.ontns + "hasValueHolder");
    
    KBObject softwarecls = kb.getClassOfInstance(softwareobj);    
    Software software = new Software(softwareobj.getID(), softwarecls.getID());
    if(details) {
      
      // Get software property values
      for(KBObject holder : kb.getPropertyValues(softwareobj, holderprop)) {
        ArrayList<SWPropertyValue> mpvs = new ArrayList<SWPropertyValue>();
        ArrayList<SWPropertyValue> provenance = new ArrayList<SWPropertyValue>();
        
        for(KBTriple t : kb.genericTripleQuery(holder, null, null)) {
          if(!t.getPredicate().getNamespace().equals(this.ontns))
            continue;
          String label = this.kb.getLabel(t.getPredicate());
          SWProperty prop = new SWProperty(t.getPredicate().getID(), label);
          KBObject obj = t.getObject();
          SWPropertyValue pv = new SWPropertyValue(prop.getId(), obj.isLiteral() ? obj.getValue() : obj.getID());
          if(this.provProps.containsKey(prop.getId()))
            provenance.add(pv);
          else {
            mpvs.add(pv);
          }
        }
        for(SWPropertyValue mpv : mpvs) {
          mpv.setProvenance(provenance);
          software.addPropertyValue(mpv);
        }
      }

      // Get I/O
      ArrayList<KBObject> inobjs = this.getSoftwareInputs(softwareobj);
      for (KBObject inobj : inobjs) {
        software.addInput(this.getRole(inobj));
      }
      ArrayList<KBObject> outobjs = this.getSoftwareOutputs(softwareobj);
      for (KBObject outobj : outobjs) {
        software.addOutput(this.getRole(outobj));
      }
      
      // Get Assumptions
      KBObject asProp = kb.getProperty(this.ontns + "hasAssumption");
      for(KBObject assobj: kb.getPropertyValues(softwareobj, asProp)) {
        KBObject catobj = kb.getClassOfInstance(assobj);
        SNAssumption ass = new SNAssumption(assobj.getID());
        ass.setLabel(this.kb.getLabel(assobj));
        if(catobj != null)
          ass.setCategoryId(catobj.getID());
        
        this.setProvenance(assobj, ass);
        software.addAssumption(ass);
      }
      
      // Get StandardNames
      KBObject snProp = kb.getProperty(this.ontns + "hasStandardName");
      KBObject objProp = kb.getProperty(this.ontns + "hasObject");
      KBObject qProp = kb.getProperty(this.ontns + "hasQuantity");
      KBObject opProp = kb.getProperty(this.ontns + "hasOperator");
      KBObject opProp2 = kb.getProperty(this.ontns + "hasSecondOperator");
      
      for(KBObject snobj: kb.getPropertyValues(softwareobj, snProp)) {
        StandardName sname = new StandardName(snobj.getID());
        sname.setLabel(this.kb.getLabel(snobj));
        
        KBObject obj = kb.getPropertyValue(snobj, objProp);
        if(obj != null)
          sname.setObjectId(obj.getID());
        KBObject qobj = kb.getPropertyValue(snobj, qProp);
        if(qobj != null)
          sname.setQuantityId(qobj.getID());
        KBObject opobj = kb.getPropertyValue(snobj, opProp);
        if(opobj != null)
          sname.addOperatorId(opobj.getID());
        KBObject opobj2 = kb.getPropertyValue(snobj, opProp2);
        if(opobj2 != null)
          sname.addOperatorId(opobj2.getID());
        
        this.setProvenance(snobj, sname);
        software.addStandardName(sname);
      }
    }
    return software;
  }
  
  @Override
  public boolean updateSoftware(Software software) {
    if(software == null) return false;
    
    // Remove existing software assertions and re-add the new software details
    boolean ok1 = this.removeSoftware(software.getID());
    boolean ok2 = this.addSoftware(software);
    
    // TODO: If abstract, update all softwares defined in all libraries !
    return ok1 && ok2;
  }

  @Override
  public boolean addSoftware(Software software) {
    String softwareid = software.getID();
    KBObject cls = this.kb.getConcept(software.getClassId());
    KBObject mobj = this.writerkb.createObjectOfClass(softwareid, cls);
    
    KBObject holdercls = this.kb.getConcept(this.ontns + "ValueHolder");
    KBObject holderprop = this.kb.getProperty(this.ontns + "hasValueHolder");
    
    HashMap<String, SWProperty> props = new HashMap<String, SWProperty>(); 
    for(SWProperty prop : this.getAllSoftwareProperties(false))
      props.put(prop.getId(), prop);
    
    for(SWPropertyValue mpv : software.getPropertyValues()) {
      if(mpv.getValue() == null || mpv.getValue().equals(""))
        continue;
      SWProperty prop = props.get(mpv.getPropertyId());
      KBObject propobj = this.kb.getProperty(prop.getId());
      KBObject valobj;
      if(prop.isObjectProperty())
        valobj = this.kb.getIndividual(mpv.getValue().toString());
      else {
        // Json conversion is converting all numbers to Double. Convert them back here
        if(prop.getRange() != null && mpv.getValue() instanceof Double) {
          if(prop.getRange().startsWith(KBUtils.XSD+"int"))
            mpv.setValue(((Double)mpv.getValue()).intValue());
          else if(prop.getRange().equals(KBUtils.XSD+"long"))
            mpv.setValue(((Double)mpv.getValue()).longValue());
        }
        valobj = this.kb.createXSDLiteral(mpv.getValue().toString(), prop.getRange());
      }
      
      KBObject valholder = this.writerkb.createObjectOfClass(null, holdercls);
      this.writerkb.addPropertyValue(mobj, holderprop, valholder);
      this.writerkb.setPropertyValue(valholder, propobj, valobj);
      
      // Set provenance information for valholder
      for(SWPropertyValue proppv : mpv.getProvenance())
        this.addProvenance(valholder, proppv.getPropertyId(), proppv.getValue());
    }
    
    // Store I/O
    KBObject inProp = kb.getProperty(this.ontns + "hasInput");
    KBObject outProp = kb.getProperty(this.ontns + "hasOutput");

    for (SoftwareRole role : software.getInputs()) {
      role.setID(softwareid + "_" + role.getRoleName()); // HACK: role id is <swid>_<rolename>
      KBObject roleobj = this.createRole(role);
      if(roleobj == null)
        return false;
      this.writerkb.addTriple(mobj, inProp, roleobj);
    }
    for (SoftwareRole role : software.getOutputs()) {
      role.setID(softwareid + "_" + role.getRoleName());
      KBObject roleobj = this.createRole(role);
      if(roleobj == null)
        return false;
      this.writerkb.addTriple(mobj, outProp, roleobj);
    }

    // Store Assumptions
    KBObject asProp = kb.getProperty(this.ontns + "hasAssumption");
    for(SNAssumption assumption: software.getAssumptions()) {
      KBObject asobj = kb.getIndividual(assumption.getID());
      if(asobj == null) {
        asobj = this.createAssumption(assumption, software.getLabels());
      }
      if(asobj != null)
        this.writerkb.addTriple(mobj, asProp, asobj);
    }
    
    // Store StandardNames
    KBObject snProp = kb.getProperty(this.ontns + "hasStandardName");
    for(StandardName sname: software.getStandardNames()) {
      KBObject snameobj = this.createStandardName(sname, software.getLabels());
      this.writerkb.addTriple(mobj, snProp, snameobj);
    }
    return true;
  }

  @Override
  public ArrayList<SWProperty> getAllSoftwareProperties(boolean rangeValues) {
    ArrayList<SWProperty> props = new ArrayList<SWProperty>();
    
    List<String> blacklist = Arrays.asList(
        "hasInput", "hasOutput","hasParameterName", "hasParameter", 
        "hasValueHolder", "hasProvenance", 
        "hasObject", "hasOperator", "hasSecondOperator", "hasQuantity",
        "hasAssumption", "hasStandardName");
    
    for(KBObject propobj : this.kb.getAllProperties()) {
      // Ignore blacklisted properties (as these are encapsulated elsewhere)
      String pname = propobj.getName();
      if(blacklist.contains(pname))
        continue;
      
      if(!propobj.getNamespace().equals(this.ontns))
        continue;
      
      SWProperty prop = this.getSWProperty(propobj, rangeValues);
      props.add(prop);
    }
    return props;
  }

  @Override
  public boolean removeSoftware(String softwareid) {
    KBObject compobj = kb.getIndividual(softwareid);
    if(compobj == null) return false;
    
    ArrayList<KBObject> inputobjs = this.getSoftwareInputs(compobj);
    ArrayList<KBObject> outputobjs = this.getSoftwareOutputs(compobj);
    for (KBObject obj : inputobjs) {
      KBUtils.removeAllTriplesWith(writerkb, obj.getID(), false);
    }
    for (KBObject obj : outputobjs) {
      KBUtils.removeAllTriplesWith(writerkb, obj.getID(), false);
    }
    KBObject holderprop = this.writerkb.getProperty(this.ontns + "hasValueHolder");
    for (KBObject obj : kb.getPropertyValues(compobj, holderprop)) {
      for(KBTriple t : kb.genericTripleQuery(obj, null, null))
        writerkb.removeTriple(t);
    }
    KBUtils.removeAllTriplesWith(writerkb, softwareid, false);
    return true;
  }

  @Override
  public boolean renameSoftware(String oldid, String newid) {
    KBUtils.renameAllTriplesWith(writerkb, oldid, newid, false);
    return true;
  }
  
  @Override
  public boolean addSoftwareType(String typeid, String parentid) {
    KBObject pcls = this.kb.getConcept(parentid);
    if(pcls == null)
      return false;
    KBObject cls = this.writerkb.createClass(typeid, parentid);
    if(cls == null)
      return false;
    return true;
  }
  
  @Override
  /**
   * This just updates the software type annotation for now
   */
  public boolean updateSoftwareType(SoftwareType type) {
    KBObject cls = this.kb.getConcept(type.getId());
    this.writerkb.setComment(cls, type.getAnnotation());
    return true;
  }
  
  @Override
  public boolean removeSoftwareType(String typeid) {
    KBObject cls = this.kb.getConcept(typeid);
    // Remove all softwares
    ArrayList<KBObject> softwares = this.kb.getInstancesOfClass(cls, true);
    for (KBObject sw : softwares) {
      this.removeSoftware(sw.getID());
    }
    // Remove all subclasses (recursive call)
    ArrayList<KBObject> subclses = this.kb.getSubClasses(cls, true);
    for (KBObject subcls : subclses) {
      if (!subcls.isNothing())
        this.removeSoftwareType(subcls.getID());
    }
    // Finally remove the class itself
    KBUtils.removeAllTriplesWith(this.writerkb, typeid, false);
    return true;
  }

  @Override
  public Software getInferredSoftware(Software software) {
    try {
      // Create a temporary kb
      KBAPI tkb = ontologyFactory.getKB(OntSpec.PLAIN);
      tkb.importFrom(this.ontologyFactory.getKB(onturl, OntSpec.PLAIN, false, true));
      tkb.importFrom(this.ontologyFactory.getKB(d_onturl, OntSpec.PLAIN, false));
      
      HashMap<String, String> rulePrefixes = new HashMap<String, String>();
      rulePrefixes.put("ts", this.ontns);
      rulePrefixes.put("data", this.d_onturl+"#");
      for(String repoid : this.surls.keySet()) {
        rulePrefixes.put(repoid.toLowerCase(),  this.surls.get(repoid) + "#");
        tkb.importFrom(this.skbs.get(repoid));
      }
      tkb.setRulePrefixes(rulePrefixes);
      
      // write the software to it
      KBAPI backup = this.writerkb;
      this.writerkb = tkb;
      this.addSoftware(software);
      this.writerkb = backup;
      
      this.createEntailments(tkb);
      
      // Redirect output (to catch rule printouts)
      ByteArrayOutputStream bost = new ByteArrayOutputStream();
      PrintStream oldout = System.out;
      System.setOut(new PrintStream(bost, true));
      
      // Run rules on the temporary kb
      KBRuleList rules = ontologyFactory.parseRules(IOUtils.toString(new URL(this.rulesurl)));
      tkb.applyRules(rules);

      ArrayList<String> explanations = new ArrayList<String>();
      // Get printouts from Rules and store as Explanations
      if (!bost.toString().equals("")) {
        for (String exp : bost.toString().split("\\n")) {
          if(!explanations.contains(exp))
            explanations.add(exp);
        }
      }
      // Set output back to original System.out
      System.setOut(oldout);

      // Read software back from temporary kb
      backup = this.kb;
      this.kb = tkb;
      Software nsoftware = this.getSoftware(software.getID(), true);
      this.kb = backup;
      
      nsoftware.setExplanations(explanations);
      
      return nsoftware;
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  
  /*
   * Standard Names part
   */
  @Override
  public void loadStandardNamesOntology(String id, String url) {
      try {
        if(!this.skbs.containsKey(id)) {
          this.surls.put(id, url);
          this.skbs.put(id, this.ontologyFactory.getKB(url, OntSpec.PLAIN, false, true));
          this.kb.importFrom(this.skbs.get(id));
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
  }
  
  @Override
  public ArrayList<SNObject> getObjects() {
    ArrayList<SNObject> objects = new ArrayList<SNObject>();
    KBObject objcls = this.kb.getConcept(this.ontns + "Object");
    for(KBObject obj : this.kb.getInstancesOfClass(objcls, true)) {
      objects.add(this.getSNObject(obj));
    }
    return objects;
  }

  @Override
  public ArrayList<SNQuantity> getQuantities() {
    ArrayList<SNQuantity> quantities = new ArrayList<SNQuantity>();
    KBObject quantitycls = this.kb.getConcept(this.ontns + "Quantity");
    for(KBObject obj : this.kb.getInstancesOfClass(quantitycls, true)) {
      quantities.add(this.getSNQuantity(obj));
    }
    return quantities;
  }

  @Override
  public ArrayList<SNOperator> getOperators() {
    ArrayList<SNOperator> ops = new ArrayList<SNOperator>();
    KBObject opcls = this.kb.getConcept(this.ontns + "Operator");
    for(KBObject obj : this.kb.getInstancesOfClass(opcls, true)) {
      ops.add(this.getSNOperator(obj));
    }
    return ops;
  }

  @Override
  public ArrayList<StandardName> getStandardNames() {
    ArrayList<StandardName> snames = new ArrayList<StandardName>();
    KBObject sncls = this.kb.getConcept(this.ontns + "StandardName");
    KBObject oprop = this.kb.getProperty(this.ontns + "hasObject");
    KBObject qprop = this.kb.getProperty(this.ontns + "hasQuantity");
    KBObject opprop = this.kb.getProperty(this.ontns + "hasOperator");
    KBObject opprop2 = this.kb.getProperty(this.ontns + "hasSecondOperator");

    for(KBObject obj : this.kb.getInstancesOfClass(sncls, true)) {
      StandardName sname = new StandardName(obj.getID());
      sname.setLabel(this.kb.getLabel(obj));
      sname.setObjectId(this.kb.getPropertyValue(obj, oprop).getID());
      sname.setQuantityId(this.kb.getPropertyValue(obj, qprop).getID());
      KBObject opobj = this.kb.getPropertyValue(obj, opprop);
      if(opobj != null)
        sname.addOperatorId(opobj.getID());
      KBObject opobj2 = this.kb.getPropertyValue(obj, opprop2);
      if(opobj2 != null)
        sname.addOperatorId(opobj2.getID());
      snames.add(sname);
    }
    return snames;
  }


  @Override
  public ArrayList<URIEntity> getAssumptionCategories() {
    ArrayList<URIEntity> categories = new ArrayList<URIEntity>();
    KBObject ascls = this.kb.getConcept(this.ontns + "Assumption");
    ArrayList<KBObject> astypes = this.kb.getSubClasses(ascls, true);
    for(KBObject astype : astypes) {
      URIEntity category = new URIEntity(astype.getID());
      category.setLabel(this.kb.getLabel(astype));
      categories.add(category);
    }
    return categories;
  }
  
  @Override
  public ArrayList<SNAssumption> getAssumptions() {
    ArrayList<SNAssumption> assumptions = new ArrayList<SNAssumption>();
    KBObject ascls = this.kb.getConcept(this.ontns + "Assumption");
    ArrayList<KBObject> astypes = this.kb.getSubClasses(ascls, true);
    for(KBObject astype : astypes) {
      for(KBObject obj : this.kb.getInstancesOfClass(astype, true)) {
        SNAssumption ass = new SNAssumption(obj.getID());
        ass.setLabel(this.kb.getLabel(obj));
        ass.setCategoryId(astype.getID());
        assumptions.add(ass);
      }
    }
    return assumptions;
  }

  @Override
  public boolean addObject(SNObject object) {
    KBObject itemobj = this.createObject(object, new HashMap<String, String>());
    return (itemobj != null);
  }

  @Override
  public boolean addQuantity(SNQuantity quantity) {
    KBObject itemobj = this.createQuantity(quantity, new HashMap<String, String>());
    return (itemobj != null);
  }

  @Override
  public boolean addOperator(SNOperator operator) {
    KBObject itemobj = this.createOperator(operator, new HashMap<String, String>());
    return (itemobj != null);
  }

  @Override
  public boolean addStandardName(StandardName sname) {
    KBObject itemobj = this.createStandardName(sname, new HashMap<String, String>());
    return (itemobj != null);
  }
  
  @Override
  public boolean addAssumption(SNAssumption assumption) {
    KBObject asobj = this.createAssumption(assumption, new HashMap<String, String>());
    return (asobj != null);
  }
  
  @Override
  public boolean save() {
    if(this.writerkb != null)
      return this.writerkb.save();
    return false;
  }
  
  @Override
  public void end() {
    if(this.kb != null)
      this.kb.end();
    if(this.writerkb != null)
      this.writerkb.end();
  }
  
  @Override
  public void delete() {
    this.writerkb.delete();
  }
  
  /*
   * Private helper functions
   */
  
  private SWProperty getSWProperty(KBObject propobj, boolean rangeValues) {
    SWProperty prop = new SWProperty(propobj.getID(), this.kb.getLabel(propobj));
    prop.setObjectProperty(this.kb.isObjectProperty(propobj));

    // Get range (from non-inference kb -- else we get indirect ranges as well)
    KBObject range = this.kb.getPropertyRange(propobj);
    if(range != null)
      prop.setRange(range.getID());
    
    // Get comment
    prop.setComment(this.kb.getComment(propobj));

    // Get Property Category (super-property)
    for (KBObject top_propobj : this.kb.getSuperPropertiesOf(propobj, true)) {
      prop.setCategory(top_propobj.getID());
      break;
    }
    
    if(rangeValues && range != null && prop.isObjectProperty()) {
      // Get Range Values
      KBObject rangecls = this.kb.getConcept(range.getID());
      for (KBObject pvalobj : this.kb.getInstancesOfClass(rangecls, false)) {
        prop.addPossibleValue(pvalobj.getID());
      }
    }
    return prop;
  }
  
  private ArrayList<KBObject> getSoftwareInputs(KBObject compobj) {
    KBObject inProp = kb.getProperty(this.ontns + "hasInput");
    return kb.getPropertyValues(compobj, inProp);
  }

  private ArrayList<KBObject> getSoftwareOutputs(KBObject compobj) {
    KBObject outProp = kb.getProperty(this.ontns + "hasOutput");
    return kb.getPropertyValues(compobj, outProp);
  }
  
  private SoftwareRole getRole(KBObject argobj) {
    SoftwareRole arg = new SoftwareRole(argobj.getID());
    ArrayList<KBObject> alltypes = kb.getAllClassesOfInstance(argobj, true);
    for (KBObject type : alltypes) {
      if (!type.getNamespace().equals(this.ontns))
        arg.setType(type.getID());
    }
    KBObject argidProp = kb.getProperty(this.ontns + "ParameterName");
    KBObject role = kb.getPropertyValue(argobj, argidProp);
    if (role != null && role.getValue() != null)
      arg.setRoleName(role.getValue().toString());
    
    this.setProvenance(argobj, arg);
    return arg;
  }

  private KBObject createRole(SoftwareRole role) {
    KBObject argidProp = kb.getProperty(this.ontns + "ParameterName");
    KBObject roletypeobj = this.kb.getConcept(this.ontns + "SoftwareParameter");
    KBObject roleobj = writerkb.createObjectOfClass(role.getID(), roletypeobj);
    writerkb.setPropertyValue(roleobj, argidProp,
        ontologyFactory.getDataObject(role.getRoleName()));
    // Write the role type
    KBObject typeobj = kb.getConcept(role.getType());
    if (typeobj != null)
      writerkb.addClassForInstance(roleobj, typeobj);
    this.addURIEntityProvenance(roleobj, role);
    return roleobj;
  }
  
  private void setProvenance(KBObject obj, URIEntity item) {
    for(String propId : this.provProps.keySet()) {
      KBObject propobj = this.kb.getProperty(propId);
      KBObject val = this.kb.getDatatypePropertyValue(obj, propobj);
      if(val != null && val.getValue() != null)
        item.addProvenance(propId, val.getValue());
    }
  }
  
  private void addProvenance(KBObject obj, String propId, Object value) {
    if(value == null)
      return;
    SWProperty provprop = this.provProps.get(propId);
    if(provprop == null)
      return;
    KBObject ppropobj = this.kb.getProperty(provprop.getId());
    KBObject pvalobj;
    if(provprop.isObjectProperty())
      pvalobj = this.kb.getIndividual(value.toString());
    else {
      // Json conversion is converting all numbers to Double. Convert them back here
      if(provprop.getRange() != null && value instanceof Double) {
        if(provprop.getRange().startsWith(KBUtils.XSD+"int"))
          value = ((Double)value).intValue();
        else if(provprop.getRange().equals(KBUtils.XSD+"long"))
          value = ((Double)value).longValue();
      }
      pvalobj = this.kb.createXSDLiteral(value.toString(), provprop.getRange());
    }
    this.writerkb.setPropertyValue(obj, ppropobj, pvalobj);
  }
  
  private void addURIEntityProvenance(KBObject obj, URIEntity entity) {
    HashMap<String, Object> pval = entity.getProvenance();
    if(pval == null)
      return;
    for(String propId : pval.keySet()) 
      this.addProvenance(obj, propId, pval.get(propId));
  }
  
  private KBObject createAssumption(SNAssumption assumption, HashMap<String, String> labels) {
    KBObject catcls = this.kb.getConcept(assumption.getCategoryId());
    if(catcls == null) {
      catcls = this.writerkb.createClass(assumption.getCategoryId(), 
          this.ontns + "Assumption");
      if(labels.containsKey(catcls.getID()))
        this.writerkb.setLabel(catcls, labels.get(catcls.getID()));
    }
    KBObject asobj = this.writerkb.createObjectOfClass(assumption.getID(), catcls);
    this.writerkb.setLabel(asobj, assumption.getLabel());
    this.addURIEntityProvenance(asobj, assumption);
    return asobj;
  }
  
  private KBObject createObject(SNObject object, HashMap<String, String> labels) {
    KBObject cls = this.kb.getConcept(this.ontns + "Object");
    KBObject itemobj = this.writerkb.createObjectOfClass(object.getID(), cls);
    if(labels.containsKey(itemobj.getID()))
      this.writerkb.setLabel(itemobj, labels.get(itemobj.getID()));
    this.addURIEntityProvenance(itemobj, object);
    return itemobj;
  }
  
  private KBObject createQuantity(SNQuantity quantity, HashMap<String, String> labels) {
    KBObject cls = this.kb.getConcept(this.ontns + "Quantity");
    KBObject itemobj = this.writerkb.createObjectOfClass(quantity.getID(), cls);
    if(labels.containsKey(itemobj.getID()))
      this.writerkb.setLabel(itemobj, labels.get(itemobj.getID()));
    this.addURIEntityProvenance(itemobj, quantity);
    return itemobj;
  }
  
  private KBObject createOperator(SNOperator operator, HashMap<String, String> labels) {
    KBObject cls = this.kb.getConcept(this.ontns + "Operator");
    KBObject itemobj = this.writerkb.createObjectOfClass(operator.getID(), cls);
    if(labels.containsKey(itemobj.getID()))
      this.writerkb.setLabel(itemobj, labels.get(itemobj.getID()));
    this.addURIEntityProvenance(itemobj, operator);
    return itemobj;
  }
  
  private KBObject createStandardName(StandardName sname, HashMap<String, String> labels) {
    KBObject objProp = kb.getProperty(this.ontns + "hasObject");
    KBObject qProp = kb.getProperty(this.ontns + "hasQuantity");
    KBObject opProp = kb.getProperty(this.ontns + "hasOperator");
    KBObject opProp2 = kb.getProperty(this.ontns + "hasSecondOperator");
    
    KBObject obj = kb.getIndividual(sname.getObjectId());
    if (obj == null) 
      obj = this.createObject(new SNObject(sname.getObjectId()), labels);
    
    KBObject qobj = kb.getIndividual(sname.getQuantityId());
    if (qobj == null)
      qobj = this.createQuantity(new SNQuantity(sname.getQuantityId()), labels);

    KBObject opobj1 = null;
    KBObject opobj2 = null;
    ArrayList<String> opids = sname.getOperatorIds();
    if (opids.size() > 0) {
      opobj1 = kb.getIndividual(opids.get(0));
      if (opobj1 == null) {
        opobj1 = this.createOperator(new SNOperator(opids.get(0)), labels);
      }
      if (opids.size() > 1) {
        opobj2 = kb.getIndividual(opids.get(1));
        if (opobj2 == null) 
          opobj2 = this.createOperator(new SNOperator(opids.get(1)), labels);
      }
    }
    
    // Check if a Standard name already exists with the combination above
    KBObject snameobj = null;
    for (KBTriple t : kb.genericTripleQuery(null, objProp, obj)) {
      KBObject snobj = t.getSubject();
      KBObject snqobj = kb.getPropertyValue(snobj, qProp);
      if (snqobj.getID().equals(qobj.getID())) {
        
        KBObject snopobj1 = kb.getPropertyValue(snobj, opProp);
        if(opobj1 != null && snopobj1 == null)
          continue;
        if(opobj1 == null && snopobj1 != null)
          continue;
        if (opobj1 != null && !opobj1.getID().equals(snopobj1.getID()))
            continue;
        
        KBObject snopobj2 = kb.getPropertyValue(snobj, opProp2);
        if(opobj2 != null && snopobj2 == null)
          continue;
        if(opobj2 == null && snopobj2 != null)
          continue;
        if (opobj2 != null && !opobj2.getID().equals(snopobj2.getID()))
          continue;
        
        // Everything matches !
        snameobj = snobj;
        break;
      }
    }
    
    // If it doesn't exist, then create one
    if(snameobj == null) {
      KBObject cls = this.kb.getConcept(this.ontns + "StandardName");
      snameobj = this.writerkb.createObjectOfClass(sname.getID(), cls);
      this.writerkb.setLabel(snameobj, sname.getLabel());
      
      this.writerkb.setPropertyValue(snameobj, objProp, obj);
      this.writerkb.setPropertyValue(snameobj, qProp, qobj);
      if(opobj1 != null)
        this.writerkb.setPropertyValue(snameobj, opProp, opobj1);
      if(opobj2 != null)
        this.writerkb.setPropertyValue(snameobj, opProp2, opobj1);
      this.addURIEntityProvenance(snameobj, sname);
    }
    
    return snameobj;
  }
  
  private SNObject getSNObject(KBObject obj) {
    if(obj == null)
      return null;
    SNObject item = new SNObject(obj.getID());
    item.setLabel(this.kb.getLabel(obj));
    return item;
  }
  
  private SNOperator getSNOperator(KBObject obj) {
    if(obj == null)
      return null;
    SNOperator item = new SNOperator(obj.getID());
    item.setLabel(this.kb.getLabel(obj));
    return item;
  }
  
  private SNQuantity getSNQuantity(KBObject obj) {
    if(obj == null)
      return null;
    SNQuantity item = new SNQuantity(obj.getID());
    item.setLabel(this.kb.getLabel(obj));
    return item;
  }
  
}
