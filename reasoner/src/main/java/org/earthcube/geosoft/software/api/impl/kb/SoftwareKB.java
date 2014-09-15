package org.earthcube.geosoft.software.api.impl.kb;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.earthcube.geosoft.software.api.SoftwareAPI;
import org.earthcube.geosoft.software.api.impl.kb.SoftwareKB;
import org.earthcube.geosoft.software.classes.SWProperty;
import org.earthcube.geosoft.software.classes.SWPropertyValue;
import org.earthcube.geosoft.software.classes.Software;
import org.earthcube.geosoft.software.classes.SoftwareRole;
import org.earthcube.geosoft.software.classes.SoftwareType;
import org.earthcube.geosoft.software.classes.audit.DRAT;
import org.earthcube.geosoft.software.classes.sn.SNAssumption;
import org.earthcube.geosoft.software.classes.sn.SNObject;
import org.earthcube.geosoft.software.classes.sn.SNOperator;
import org.earthcube.geosoft.software.classes.sn.SNQuantity;
import org.earthcube.geosoft.software.classes.sn.StandardName;
import org.earthcube.geosoft.util.KBUtils;
import org.earthcube.geosoft.util.URIEntity;

import com.google.gson.internal.LinkedTreeMap;

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
  protected String d_onturl;
  protected String liburl;
  protected String rulesurl;
  protected String dataurl;

  protected OntFactory ontologyFactory;

  protected HashMap<String, KBObject> propMap;
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
    this.d_onturl = props.getProperty("ont.data.url");
    this.liburl = props.getProperty("lib.software.url");
    this.rulesurl = props.getProperty("rules.software.url");
    this.dataurl = props.getProperty("ont.domain.data.url");
    
    String tdbRepository = props.getProperty("tdb.repository.dir");
    this.ontologyFactory = new OntFactory(OntFactory.JENA, tdbRepository);
    
    this.initializeAPI();
  }
  
  protected void initializeAPI() {
    try {
      this.kb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN, true);
      this.kb.importFrom(this.ontologyFactory.getKB(onturl, OntSpec.PLAIN, false, true));
      this.kb.importFrom(this.ontologyFactory.getKB(d_onturl, OntSpec.PLAIN, false, true));
      this.kb.importFrom(this.ontologyFactory.getKB(dataurl, OntSpec.PLAIN, false));
      
      this.writerkb = this.ontologyFactory.getKB(liburl, OntSpec.PLAIN);
      this.writerkb.importFrom(this.ontologyFactory.getKB(onturl, OntSpec.PLAIN, false, true));
      
      this.skbs = new HashMap<String, KBAPI>();
      this.surls = new HashMap<String, String>();
      
      //this.createEntailments(this.kb);
      this.initializeMaps(this.kb);
      //this.initDomainKnowledge();
      //this.setRuleMappings(this.kb);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void initializeMaps(KBAPI kb) {
    this.propMap = new HashMap<String, KBObject>();
    this.conceptMap = new HashMap<String, KBObject>();
    this.provProps = new HashMap<String, SWProperty>();

    for (KBObject con : kb.getAllClasses()) {
      if(con.getID() != null) {
        this.conceptMap.put(con.getID(), con);
        this.conceptMap.put(con.getName(), con);
      }
    }
    for (KBObject prop : kb.getAllObjectProperties()) {
      this.propMap.put(prop.getID(), prop);
      this.propMap.put(prop.getName(), prop);
    }
    for (KBObject prop : kb.getAllDatatypeProperties()) {
      this.propMap.put(prop.getID(), prop);
      this.propMap.put(prop.getName(), prop);
    }
    KBObject propTopProp = propMap.get("hasProvenance");
    for(KBObject pp : kb.getSubPropertiesOf(propTopProp , false)) {
      this.provProps.put(pp.getID(), this.getSWProperty(pp, false));
    }
  }
  
  private ArrayList<KBTriple> createEntailments(KBAPI kb, KBObject obj, 
      KBObject cls, KBObject typeProp, ArrayList<KBTriple> triples) {
    for(KBObject supercls : kb.getSuperClasses(cls, false)) {
      KBTriple triple = this.ontologyFactory.getTriple(obj, typeProp, supercls);
      triples.add(triple);
      kb.addTriple(triple);
      triples = this.createEntailments(kb, obj, supercls, typeProp, triples);
    }
    return triples;
  }
  
  private ArrayList<KBTriple> createEntailments(KBAPI kb) {
    KBObject typeProp = kb.getProperty(KBUtils.RDF+"type");
    ArrayList<KBTriple> addedTriples = new ArrayList<KBTriple>();
    for(KBObject cls : kb.getAllClasses()) {
      if(cls.isAnonymous()) continue;
      if(!cls.getNamespace().equals(this.ontns) &&
          !cls.getNamespace().equals(this.liburl+"#"))
        continue;
      for(KBObject obj : kb.getInstancesOfClass(cls, true))
        addedTriples = this.createEntailments(kb, obj, cls, typeProp, addedTriples);
    }
    return addedTriples;
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
    //ArrayList<KBTriple> entailments = this.createEntailments(this.kb);
    SoftwareType rootType = this.getSoftwareType(this.ontns + "Software", true);
    //this.removeEntailments(this.kb, entailments);
    return rootType;
  }
  
  @Override
  public SoftwareType getSoftwareType(String id, boolean getSubtypes) {
    SoftwareType type = new SoftwareType(id);
    KBObject cls = conceptMap.get(id);
    type.setAnnotation(this.kb.getComment(cls));
    if(getSubtypes) {
      for(KBObject obj : this.kb.getSubClasses(cls, true)) {
        type.addSubtype(this.getSoftwareType(obj.getID(), getSubtypes));
      }
    }
    return type;
  }
  
  @Override
  public ArrayList<Software> getSoftwares(SoftwareType root, boolean details) {
    ArrayList<Software> softwares = new ArrayList<Software>();
    ArrayList<SoftwareType> queue = new ArrayList<SoftwareType>();
    queue.add(root);
    while(queue.size() > 0) {
      SoftwareType type = queue.remove(0);
      KBObject softwarecls = conceptMap.get(type.getId());
      ArrayList<KBObject> softwareobjs = this.kb.getInstancesOfClass(softwarecls, true);
      for (KBObject softwareobj : softwareobjs) {
        softwares.add(getSoftware(softwareobj.getID(), details));
      }
      queue.addAll(type.getSubtypes());
    }
    return softwares;
  }
  
  @Override
  public Software getSoftware(String softwareid, boolean details) {
    KBObject softwareobj = kb.getIndividual(softwareid);
    if(softwareobj == null) return null;

    KBObject holderprop = propMap.get("hasValueHolder");
    
    KBObject softwarecls = kb.getClassOfInstance(softwareobj);    
    Software software = new Software(softwareobj.getID(), softwarecls.getID());
    if(details) {
      
      KBObject noteProp = this.propMap.get("Note");
      KBObject ivarProp = this.propMap.get("InternalVariable");
      
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
          if(t.getPredicate().getName().equals("hasStandardName")) {
            // Get Standard Name
            StandardName sname = this.getStandardName(obj);
            // Get Extra information 
            KBObject noteobj = this.kb.getPropertyValue(holder, noteProp);
            KBObject ivarobj = this.kb.getPropertyValue(holder, ivarProp);
            if(noteobj != null && noteobj.getValue() != null)
              sname.setNote(noteobj.getValue().toString());
            if(ivarobj != null && ivarobj.getValue() != null)
              sname.setInternalVariable(ivarobj.getValue().toString());
            
            this.setProvenance(obj, sname);
            software.addStandardName(sname);
          }
          else if(t.getPredicate().getName().equals("hasAssumption")) {
            // Get Assumption
            SNAssumption ass = this.getAssumption(obj);
            // Get Extra information 
            KBObject noteobj = this.kb.getDatatypePropertyValue(holder, noteProp);
            if(noteobj != null && noteobj.getValue() != null)
              ass.setNote(noteobj.getValue().toString());
            
            this.setProvenance(obj, ass);
            software.addAssumption(ass);
          }
          else if(t.getPredicate().getName().equals("Note")) {
            continue;
          }
          else if(t.getPredicate().getName().equals("InternalVariable")) {
            continue;
          }
          else {
            Object objval;
            if(obj.isAnonymous()) {
              HashMap<String, Object> objmap = new HashMap<String, Object>();
              for(KBTriple ot : kb.genericTripleQuery(obj, null, null)) {
                objmap.put(ot.getPredicate().getName(), ot.getObject().getValue());
              }
              objval = objmap;
              if(prop.getId().equals(this.ontns + "CodeFile")) {
                if(objmap.containsKey("hasFileLocation")) {
                  String res = DRAT.RESULTS.get(objmap.get("hasFileLocation"));
                  if(res != null)
                    software.setAuditResults(res);
                }
              }
            }
            else if(obj.isLiteral())
              objval = obj.getValue();
            else
              objval = obj.getID();
            
            SWPropertyValue pv = new SWPropertyValue(prop.getId(), objval);
            if(this.provProps.containsKey(prop.getId()))
              provenance.add(pv);
            else
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
  public boolean setSoftwareType(String softwareid, String typeid) {
    KBObject mobj = this.kb.getIndividual(softwareid);
    if(mobj == null) return false;
    KBObject cls = conceptMap.get(typeid);
    if(cls == null) return false;
    this.writerkb.setClassForInstance(mobj, cls);
    return true;
  }
  
  @Override
  public boolean addSoftware(Software software) {
    String softwareid = software.getID();
    KBObject cls = conceptMap.get(software.getClassId());
    KBObject mobj = this.writerkb.createObjectOfClass(softwareid, cls);
    
    KBObject holdercls = conceptMap.get("ValueHolder");
    KBObject holderprop = propMap.get("hasValueHolder");
    
    HashMap<String, SWProperty> props = new HashMap<String, SWProperty>(); 
    for(SWProperty prop : this.getAllSoftwareProperties(false))
      props.put(prop.getId(), prop);
    
    for(SWPropertyValue mpv : software.getPropertyValues()) {
      if(mpv.getValue() == null || mpv.getValue().equals(""))
        continue;
      SWProperty prop = props.get(mpv.getPropertyId());
      KBObject propobj = propMap.get(prop.getId());
      KBObject valobj = null;
      if(mpv.getValue() instanceof LinkedTreeMap) {
        // If value is a complex gson object
        @SuppressWarnings("rawtypes")
        LinkedTreeMap map = (LinkedTreeMap) mpv.getValue();
        if(map.containsKey("type")) {
          String typeid = (String) map.get("type");
          KBObject typecls = conceptMap.get(typeid);
          if(typecls == null) continue;
          
          valobj = this.writerkb.createObjectOfClass(null, typecls);
          for(Object key : map.keySet()) {
            if(key.equals("type"))
              continue;
            KBObject typeprop = propMap.get(key);
            KBObject typevalobj = this.kb.createLiteral(map.get(key));
            this.writerkb.addPropertyValue(valobj, typeprop, typevalobj);
          }
        }
      }
      else if(prop.isObjectProperty())
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
      if(valobj == null)
        continue;
      
      KBObject valholder = this.writerkb.createObjectOfClass(null, holdercls);
      this.writerkb.addPropertyValue(mobj, holderprop, valholder);
      this.writerkb.setPropertyValue(valholder, propobj, valobj);
      
      // Set provenance information for valholder
      for(SWPropertyValue proppv : mpv.getProvenance())
        this.addProvenance(valholder, proppv.getPropertyId(), proppv.getValue());
    }
    
    // Store I/O
    KBObject inProp = propMap.get("hasInput");
    KBObject outProp = propMap.get("hasOutput");

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

    KBObject noteProp = propMap.get("Note");
    KBObject ivarProp = propMap.get("InternalVariable");
    
    // Store Assumptions
    KBObject asProp = propMap.get("hasAssumption");
    for(SNAssumption assumption: software.getAssumptions()) {
      KBObject asobj = kb.getIndividual(assumption.getID());
      if(asobj == null) {
        asobj = this.createAssumption(assumption, software.getLabels());
      }
      if(asobj != null) {
        KBObject valholder = this.writerkb.createObjectOfClass(null, holdercls);
        this.writerkb.addPropertyValue(mobj, holderprop, valholder);
        this.writerkb.setPropertyValue(valholder, asProp, asobj);
        //this.writerkb.addTriple(mobj, asProp, asobj);
        if(assumption.getNote() != null)
          this.writerkb.setPropertyValue(valholder, noteProp, 
              this.ontologyFactory.getDataObject(assumption.getNote().toString()));
      }
    }
    
    // Store StandardNames
    KBObject snProp = propMap.get("hasStandardName");
    for(StandardName sname: software.getStandardNames()) {
      KBObject snameobj = this.createStandardName(sname, software.getLabels());
      KBObject valholder = this.writerkb.createObjectOfClass(null, holdercls);
      this.writerkb.addPropertyValue(mobj, holderprop, valholder);
      this.writerkb.setPropertyValue(valholder, snProp, snameobj);
      //this.writerkb.addTriple(mobj, snProp, snameobj);
      if(sname.getNote() != null)
        this.writerkb.setPropertyValue(valholder, noteProp, 
            this.ontologyFactory.getDataObject(sname.getNote().toString()));
      if(sname.getInternalVariable() != null)
        this.writerkb.setPropertyValue(valholder, ivarProp, 
            this.ontologyFactory.getDataObject(sname.getInternalVariable().toString()));        
    }
    return true;
  }

  @Override
  public ArrayList<SWProperty> getAllSoftwareProperties(boolean rangeValues) {
    ArrayList<SWProperty> props = new ArrayList<SWProperty>();
    
    List<String> blacklist = Arrays.asList(
        "hasInput", "hasOutput","hasParameterName", "hasParameter", 
        "hasValueHolder", "hasProvenance", "hasFileLocation",
        "hasObject", "hasOperator", "hasSecondOperator", "hasQuantity",
        "hasAssumption", "hasStandardName", "hasExtraInformation");
    
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
    KBObject holderprop = propMap.get("hasValueHolder");
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
  public boolean renameSoftwareType(String oldid, String newid) {
    KBUtils.renameAllTriplesWith(writerkb, oldid, newid, false);
    return true;
  }
  
  @Override
  public boolean addSoftwareType(String typeid, String parentid) {
    KBObject pcls = conceptMap.get(parentid);
    if(pcls == null)
      return false;
    KBObject cls = this.writerkb.createClass(typeid, parentid);
    if(cls == null)
      return false;
    return true;
  }
  
  @Override
  /**
   * This just updates the software type annotation & parent for now
   */
  public boolean updateSoftwareType(SoftwareType type) {
    KBObject cls = conceptMap.get(type.getId());
    // Add new information
    if(type.getParentid() != null)
      this.writerkb.setSuperClass(type.getId(), type.getParentid());
    if(type.getAnnotation() != null)
      this.writerkb.setComment(cls, type.getAnnotation());
    return true;
  }
  
  @Override
  public boolean removeSoftwareType(String typeid) {
    KBObject cls = conceptMap.get(typeid);
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
      tkb.importFrom(this.ontologyFactory.getKB(d_onturl, OntSpec.PLAIN, false, true));
      tkb.importFrom(this.ontologyFactory.getKB(dataurl, OntSpec.PLAIN, false));
      
      HashMap<String, String> rulePrefixes = new HashMap<String, String>();
      rulePrefixes.put("ts", this.ontns);
      rulePrefixes.put("ds", this.d_onturl+"#");
      rulePrefixes.put("data", this.dataurl+"#");
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
  
  @Override
  public Software getTikaInferredSoftware(Software software, String tikaUrl) {
    try {
      HashMap<String, Boolean> roleids = new HashMap<String, Boolean>();
      for(SoftwareRole role : software.getInputs())
        roleids.put(role.getID(), true);
      for(SoftwareRole role : software.getOutputs())
        roleids.put(role.getID(), true);
      
      // FIXME: This is a bit hackish right now
      // - Change to use a proper Tika API ?
      // - Check Data Catalog for FileType to Mime conversion ?
      for(SWPropertyValue mpv : software.getPropertyValues()) {
        if(mpv.getPropertyId().equals(this.ontns+"UnitTestFile")) {
          if(mpv.getValue() instanceof HashMap) {
            // If value is a complex gson object
            @SuppressWarnings("rawtypes")
            HashMap map = (HashMap) mpv.getValue();
            String identifier = (String) map.get("forParameter");
            String roleid = software.getID() + "_" + KBUtils.sanitizeID(identifier);
            if(roleids.containsKey(roleid))
              continue;
            
            String floc = (String) map.get("hasFileLocation");
            String note = (String) map.get("Note");
            String fname = floc.substring( floc.lastIndexOf('/')+1, floc.length() );
            
            URL tika = new URL(tikaUrl);
            URL furl = new URL(floc);
            
            HttpURLConnection tikaCon = (HttpURLConnection) tika.openConnection();
            HttpURLConnection fcon = (HttpURLConnection) furl.openConnection();
            
            tikaCon.setDoOutput(true);
            tikaCon.setRequestMethod("PUT");
            tikaCon.setRequestProperty("Content-Disposition", "attachment;filename="+fname);
            OutputStreamWriter tikaOut = new OutputStreamWriter(tikaCon.getOutputStream());
            
            BufferedReader freader = new BufferedReader(new InputStreamReader(fcon.getInputStream()));
            String line = "";
            while((line=freader.readLine()) != null) {
              tikaOut.write(line);
            }
            tikaOut.close();
            InputStream is = tikaCon.getInputStream();
            BufferedReader tikareader = new BufferedReader(new InputStreamReader(is));
            
            // Just use the last
            line = "";
            String mime = null;
            while((line = tikareader.readLine()) != null) {
              if(mime == null)
                mime = line;
            }
            tikareader.close();
            is.close();
            
            if(mime == null)
              return null;
            
            String iotype = mime.split("/")[1];
            // Check iotype id
            KBObject iocls = conceptMap.get(this.dataurl + "#" + iotype);
            if(iocls == null)
              iocls = conceptMap.get(this.d_onturl + "#" + iotype);
            if(iocls == null)
              continue;
            
            SoftwareRole role = new SoftwareRole(roleid);
            role.setRoleName(identifier);
            role.setType(iocls.getID());
            role.setLabel(identifier);
            
            if(note != null && note.toLowerCase().contains("output")) {
              software.addOutput(role);
              software.addExplanation("[TIKA] Adding output "+identifier+" for mime-type "+mime);
            }
            else {
              software.addInput(role);
              software.addExplanation("[TIKA] Adding input "+identifier+" for mime-type "+mime);
            }
          }
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return software;
  }
  
  @Override
  public boolean runAuditTool(Software software, String dratHome) {
    try {
      if(DRAT.BUSY)
        return false;
      
      // - Change to use a proper DRAT API ?
      for(SWPropertyValue mpv : software.getPropertyValues()) {
        if(mpv.getPropertyId().equals(this.ontns+"CodeFile")) {
          if(mpv.getValue() instanceof HashMap) {
            // If value is a complex gson object
            @SuppressWarnings("rawtypes")
            HashMap map = (HashMap) mpv.getValue();
            String floc = (String) map.get("hasFileLocation");
            String note = (String) map.get("Note");
            
            DRAT drat = new DRAT(floc, note, dratHome);
            drat.start();
            return true;
          }
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
  
  @Override
  public String checkCode(String id) {
    Software software = this.getSoftware(id, true);
    List<SWPropertyValue> list = software.getPropertyValues();
    String codeurl=null;
    Pattern numberPat = Pattern.compile("\\s*[\\w]+?\\s*=\\s*([\\d\\.]+)[;\\s]");
    Pattern stringPat = Pattern.compile("\\s*([\\w]+?)\\s*=\\s*([\"\'][\\w\\s]+[\"\'])\\s*");
    Pattern pathPat = Pattern.compile("[\"\'].*\\w*[\\\\/]\\w*(?:\\.\\w+)?[\"\']");
    Pattern commandPat = Pattern.compile(".*\\sls");
    for (SWPropertyValue prop : list) {
      if (prop.getPropertyId().contains("CodeFile")) {
        codeurl=prop.getValue().toString().split("=")[1].split(",")[0];
      }
    }
    String output="";
    if (codeurl!=null) {
      try {
        URL code = new URL(codeurl);
        BufferedReader in = new BufferedReader(new InputStreamReader(code.openStream()));
        String inputLine;
        Matcher numMat = null;
        Matcher strMat = null;
        Matcher pathMat = null;
        Matcher comMat = null;
        int counter=0;
        while ((inputLine = in.readLine()) != null) {
          //System.out.println(inputLine);
          numMat = numberPat.matcher(inputLine);
          strMat = stringPat.matcher(inputLine);
          pathMat = pathPat.matcher(inputLine);
          comMat = commandPat.matcher(inputLine);
          if (numMat!=null && numMat.find()) {
            output+="Hardcoded variable, "+Integer.toString(counter)+", "+numMat.group().trim()+
                ", "+"This might be better as an input parameter"+"|";
          }
          if (strMat != null && strMat.find()) {
            output+="Hardcoded variable, "+Integer.toString(counter)+", "+strMat.group().trim()+
                ", "+"This might be better as an input parameter"+"|";
          }
          if (pathMat != null && pathMat.find()) {
            if(!pathMat.group().trim().matches(".*\\\\[nt].*"))
              output+="Absolute path, "+Integer.toString(counter)+", "+pathMat.group().trim()+
                ", "+"Try creating a local path, or an input parameter for the output path"+"|";
          }
          if (comMat!=null && comMat.find()) {
            output+="Command Line, "+Integer.toString(counter)+", "+comMat.group().trim()+
                ", "+"Try using \"dir\" to enumerate files"+"|";
          }
          counter++;

        }
        in.close();
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        return null;
      }
      //Open file, analyze, return results
    }
    return output;
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
    KBObject objcls = conceptMap.get(this.ontns + "Object");
    for(KBObject obj : this.kb.getInstancesOfClass(objcls, true)) {
      objects.add(this.getSNObject(obj));
    }
    return objects;
  }

  @Override
  public ArrayList<SNQuantity> getQuantities() {
    ArrayList<SNQuantity> quantities = new ArrayList<SNQuantity>();
    KBObject quantitycls = conceptMap.get(this.ontns + "Quantity");
    for(KBObject obj : this.kb.getInstancesOfClass(quantitycls, true)) {
      quantities.add(this.getSNQuantity(obj));
    }
    return quantities;
  }

  @Override
  public ArrayList<SNOperator> getOperators() {
    ArrayList<SNOperator> ops = new ArrayList<SNOperator>();
    KBObject opcls = conceptMap.get(this.ontns + "Operator");
    for(KBObject obj : this.kb.getInstancesOfClass(opcls, true)) {
      ops.add(this.getSNOperator(obj));
    }
    return ops;
  }

  @Override
  public ArrayList<StandardName> getStandardNames() {
    ArrayList<StandardName> snames = new ArrayList<StandardName>();
    KBObject sncls = conceptMap.get("StandardName");
    KBObject oprop = propMap.get("hasObject");
    KBObject qprop = propMap.get("hasQuantity");
    KBObject opprop = propMap.get("hasOperator");
    KBObject opprop2 = propMap.get("hasSecondOperator");

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
  
  private StandardName getStandardName(KBObject snobj) {
    KBObject objProp = propMap.get("hasObject");
    KBObject qProp = propMap.get("hasQuantity");
    KBObject opProp = propMap.get("hasOperator");
    KBObject opProp2 = propMap.get("hasSecondOperator");
    
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
    
    return sname;
  }

  private SNAssumption getAssumption(KBObject assobj) {
    KBObject catobj = kb.getClassOfInstance(assobj);
    SNAssumption ass = new SNAssumption(assobj.getID());
    ass.setLabel(this.kb.getLabel(assobj));
    if(catobj != null)
      ass.setCategoryId(catobj.getID());
    return ass;
  }
  
  @Override
  public ArrayList<URIEntity> getAssumptionCategories() {
    ArrayList<URIEntity> categories = new ArrayList<URIEntity>();
    KBObject ascls = conceptMap.get(this.ontns + "Assumption");
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
    KBObject ascls = conceptMap.get(this.ontns + "Assumption");
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
      prop.addParentId(top_propobj.getID());
    }
    
    if(rangeValues && range != null && prop.isObjectProperty()) {
      // Get Range Values
      KBObject rangecls = conceptMap.get(range.getID());
      for (KBObject pvalobj : this.kb.getInstancesOfClass(rangecls, false)) {
        if(!pvalobj.isAnonymous())
          prop.addPossibleValue(pvalobj.getID());
      }
    }
    return prop;
  }
  
  private ArrayList<KBObject> getSoftwareInputs(KBObject compobj) {
    KBObject inProp = propMap.get("hasInput");
    return kb.getPropertyValues(compobj, inProp);
  }

  private ArrayList<KBObject> getSoftwareOutputs(KBObject compobj) {
    KBObject outProp = propMap.get("hasOutput");
    return kb.getPropertyValues(compobj, outProp);
  }
  
  private SoftwareRole getRole(KBObject argobj) {
    SoftwareRole arg = new SoftwareRole(argobj.getID());
    ArrayList<KBObject> alltypes = kb.getAllClassesOfInstance(argobj, true);
    for (KBObject type : alltypes) {
      if (!type.getNamespace().equals(this.ontns))
        arg.setType(type.getID());
    }
    KBObject argidProp = propMap.get("hasParameterName");
    KBObject role = kb.getPropertyValue(argobj, argidProp);
    if (role != null && role.getValue() != null)
      arg.setRoleName(role.getValue().toString());
    
    this.setProvenance(argobj, arg);
    return arg;
  }

  private KBObject createRole(SoftwareRole role) {
    KBObject argidProp = propMap.get("hasParameterName");
    KBObject roletypeobj = conceptMap.get("SoftwareParameter");
    KBObject roleobj = writerkb.createObjectOfClass(role.getID(), roletypeobj);
    writerkb.setPropertyValue(roleobj, argidProp,
        ontologyFactory.getDataObject(role.getRoleName()));
    // Write the role type
    KBObject typeobj = conceptMap.get(role.getType());
    if (typeobj != null)
      writerkb.addClassForInstance(roleobj, typeobj);
    this.addURIEntityProvenance(roleobj, role);
    return roleobj;
  }
  
  private void setProvenance(KBObject obj, URIEntity item) {
    for(String propId : this.provProps.keySet()) {
      KBObject propobj = propMap.get(propId);
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
    KBObject ppropobj = propMap.get(provprop.getId());
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
    KBObject catcls = conceptMap.get(assumption.getCategoryId());
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
    KBObject cls = conceptMap.get(this.ontns + "Object");
    KBObject itemobj = this.writerkb.createObjectOfClass(object.getID(), cls);
    if(labels.containsKey(itemobj.getID()))
      this.writerkb.setLabel(itemobj, labels.get(itemobj.getID()));
    this.addURIEntityProvenance(itemobj, object);
    return itemobj;
  }
  
  private KBObject createQuantity(SNQuantity quantity, HashMap<String, String> labels) {
    KBObject cls = conceptMap.get(this.ontns + "Quantity");
    KBObject itemobj = this.writerkb.createObjectOfClass(quantity.getID(), cls);
    if(labels.containsKey(itemobj.getID()))
      this.writerkb.setLabel(itemobj, labels.get(itemobj.getID()));
    this.addURIEntityProvenance(itemobj, quantity);
    return itemobj;
  }
  
  private KBObject createOperator(SNOperator operator, HashMap<String, String> labels) {
    KBObject cls = conceptMap.get(this.ontns + "Operator");
    KBObject itemobj = this.writerkb.createObjectOfClass(operator.getID(), cls);
    if(labels.containsKey(itemobj.getID()))
      this.writerkb.setLabel(itemobj, labels.get(itemobj.getID()));
    this.addURIEntityProvenance(itemobj, operator);
    return itemobj;
  }
  
  private KBObject createStandardName(StandardName sname, HashMap<String, String> labels) {
    KBObject objProp = propMap.get("hasObject");
    KBObject qProp = propMap.get("hasQuantity");
    KBObject opProp = propMap.get("hasOperator");
    KBObject opProp2 = propMap.get("hasSecondOperator");
    
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
      KBObject cls = conceptMap.get(this.ontns + "StandardName");
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
