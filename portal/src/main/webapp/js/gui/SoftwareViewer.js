function SoftwareViewer(guid, store, op_url, upload_url, ontns, liburl, advanced_user) {
    this.guid = guid;
    this.store = store;
    this.op_url = op_url;
    this.upload_url = upload_url;
    this.liburl = liburl;
    this.libname = liburl.replace(/.+\//, '').replace(/\.owl$/, '');
    this.advanced_user = advanced_user;
    
    this.ns = {};
    this.ns[''] = ontns;
    this.ns['lib'] = liburl + "#";
    this.ns['xsd'] = "http://www.w3.org/2001/XMLSchema#";

    this.treePanel = null;
    this.tabPanel = null;
    this.leftPanel = null;
    this.mainPanel = null;
    
    this.propcomments = {};
    this.properties = {};
    for(var i=0; i<store.properties.length; i++) {
    	var prop = store.properties[i];
    	this.properties[prop.id] = store.properties[i];
    	this.propcomments[prop.id] = {};
    	if(store.properties[i].comment) {
    		try {
    		    // decode property comment json
    			this.propcomments[prop.id] = Ext.decode(store.properties[i].comment);
    		}
    		catch (e) {}
    	}
    }

    // Create id to item mappings for all standard-name items
    this.idmap = {};
    for(var i=0; i<store.sninfo.categories.length; i++)
    	this.idmap[store.sninfo.categories[i].id] = store.sninfo.categories[i];
    for(var i=0; i<store.sninfo.objects.length; i++)
    	this.idmap[store.sninfo.objects[i].id] = store.sninfo.objects[i];
    for(var i=0; i<store.sninfo.quantities.length; i++)
    	this.idmap[store.sninfo.quantities[i].id] = store.sninfo.quantities[i];
    for(var i=0; i<store.sninfo.operators.length; i++)
    	this.idmap[store.sninfo.operators[i].id] = store.sninfo.operators[i];
    for(var i=0; i<store.sninfo.assumptions.length; i++)
    	this.idmap[store.sninfo.assumptions[i].id] = store.sninfo.assumptions[i];
    for(var i=0; i<store.sninfo.standardnames.length; i++)
    	this.idmap[store.sninfo.standardnames[i].id] = store.sninfo.standardnames[i];
};

SoftwareViewer.prototype.getSoftwareTreePanel = function(root, title, iconCls, enableDrag) {
    var This = this;
    if (!Ext.ModelManager.isRegistered('compTreeRecord'))
        Ext.define('compTreeRecord', {
	        extend: 'Ext.data.Model',
	        fields: ['text', 'software']
        });

    var treeStore = Ext.create('Ext.data.TreeStore', {
        model: 'compTreeRecord',
        root: root,
        sorters: ['text']
    });
    var treePanel = new Ext.tree.TreePanel({
        width: '100%',
        border: false,
        autoScroll: true,
        hideHeaders: true,
        rootVisible: false,
        iconCls: iconCls,
        bodyCls: 'x-docked-noborder-top',
        title: title,
        store: treeStore,
        url: This.op_url
    });
    return treePanel;
};

SoftwareViewer.prototype.getSoftwareTree = function(list) {
	var root =  {
        text: "Software",
        id: "Software",
        leaf: false,
        iconCls: 'ctypeIcon',
        expanded: true,
        children: []
    };
	var nodes = {};
	for (var i = 0; i < this.store.software_types.length; i++) {
		var typeid = this.store.software_types[i];
		nodes[typeid] = {
			id: typeid,
			text: getLocalName(typeid),
			leaf: false,
			iconCls: 'dtypeIcon',
			expanded: true,
			children: []
		};
		root.children.push(nodes[typeid]);
	}
	
    for (var i = 0; i < list.length; i++) {
    	var software = list[i];
    	var node = nodes[software.classId];
    	if(!node)
    		continue;
    	node.children.push({
    		id: software.id,
    		text: getLocalName(software.id),
    		leaf: true,
    		iconCls: 'compIcon'
    	});
    }
    return root;
};

SoftwareViewer.prototype.getSoftwareListTree = function(enableDrag) {
    var tmp = this.getSoftwareTree(this.store.softwares);
    return this.getSoftwareTreePanel(tmp, 'Softwares', 'compIcon', enableDrag);
};

SoftwareViewer.prototype.addSoftware = function() {
    var This = this;
    var cTree = this.treePanel;
    
    var nodes = This.treePanel.getSelectionModel().getSelection();
    if (!nodes || !nodes.length || nodes[0].data.leaf) {
    	Ext.MessageBox.show({
            icon: Ext.MessageBox.ERROR,
            buttons: Ext.MessageBox.OK,
            msg: "First select a Software type (Software, Visualization, etc)"
        });
        return;
    }
    var pNode = nodes[0];
    var mtype = getLocalName(pNode.data.id);
    
    Ext.Msg.prompt("Add Software", "Enter name for the new Software:", function(btn, text) {
        if (btn == 'ok' && text) {
            text = getRDFID(text);
            var softwareid = This.ns['lib'] + text;
            var enode = cTree.getStore().getNodeById(softwareid);
            if (enode) {
                showError(mtype+' ' + text + ' already exists');
                return;
            }
            var url = This.op_url + '/addSoftware';
            Ext.get(cTree.getId()).mask("Creating..");
            Ext.Ajax.request({
                url: url,
                params: {
                    softwareid: softwareid,
                    software_typeid: pNode.data.id
                },
                success: function(response) {
                    Ext.get(cTree.getId()).unmask();
                    if (response.responseText == "OK") {
                    	pNode.appendChild({
                    		id: softwareid,
                    		text: getLocalName(softwareid),
                    		leaf: true,
                    		iconCls: 'compIcon'
                    	});
                        This.treePanel.getStore().sort('text', 'ASC');
                    } else {
                        _console(response.responseText);
                    }
                },
                failure: function(response) {
                    Ext.get(cTree.getId()).unmask();
                    _console(response.responseText);
                }
            });
        }
    }, window, false);
};

SoftwareViewer.prototype.importSoftware = function(repo_id) {
    var This = this;
    var cTree = this.treePanel;

    var nodes = This.treePanel.getSelectionModel().getSelection();
    if (!nodes || !nodes.length || nodes[0].data.leaf) {
    	Ext.MessageBox.show({
            icon: Ext.MessageBox.ERROR,
            buttons: Ext.MessageBox.OK,
            msg: "First select a Software type (Software, Visualization, etc)"
        });
        return;
    }
    var pNode = nodes[0];
    var mtype = getLocalName(pNode.data.id);
    
    Ext.Msg.prompt("Import "+repo_id+" Software Description", 
    		"Enter "+repo_id+" Software name", function(btn, text) {
        if (btn == 'ok' && text) {
            var sanetext = getRDFID(text);
            var softwareid = This.ns['lib'] + sanetext;
            var enode = cTree.getStore().getNodeById(softwareid);
            if (enode) {
                showError(mtype+' ' + sanetext + ' already exists');
                return;
            }
            var url = This.op_url + '/importSoftware';
            Ext.get(cTree.getId()).mask("Importing..");
            Ext.Ajax.request({
                url: url,
                params: {
                    softwareid: softwareid,
                    software_typeid: pNode.data.id,
                    repo_softwareid: text,
                    repo_id: repo_id
                },
                success: function(response) {
                    Ext.get(cTree.getId()).unmask();
                    if (response.responseText == "OK") {
                    	pNode.appendChild({
                    		id: softwareid,
                    		text: getLocalName(softwareid),
                    		leaf: true,
                    		iconCls: 'compIcon'
                    	});
                        This.treePanel.getStore().sort('text', 'ASC');
                    } else {
                        _console(response.responseText);
                    }
                },
                failure: function(response) {
                    Ext.get(cTree.getId()).unmask();
                    _console(response.responseText);
                }
            });
        }
    }, window, false);
};

SoftwareViewer.prototype.prepareRoleRecord = function(arg) {
    var narg = {};
    for (var key in arg)
        narg[key] = arg[key];
    if(!narg.provenance)
    	narg.provenance = {};
    return narg;
};

SoftwareViewer.prototype.openSoftwareEditor = function(args) {
    var tab = args[0];
    var id = args[1];
    var compStore = args[2];
    var mainPanel;
    var This = this;
    
    var savebtn = new Ext.Button({
        text: 'Save Software',
        iconCls: 'saveIcon',
        disabled: true,
        handler: function() {
        	var form = tab.down('form');
        	var software = This.createSoftwareFromForm(form, id, compStore.classId);
        	if(!software)
        		return;
        	
        	Ext.get(This.tabPanel.getId()).mask("Saving..");
        	Ext.Ajax.request({
                url: This.op_url + '/saveSoftwareJSON',
                params: {
                    softwareid: id,
                    software_json: Ext.encode(software)
                },
                success: function(response) {
                    Ext.get(This.tabPanel.getId()).unmask();
                    if (response.responseText == "OK") {
                    	// Reset added flag in idmap
                    	for(var key in This.idmap) {
                    		if(This.idmap[key].added)
                    			This.idmap[key].added = false;
                    	}
						// Reset dirty bit
						form.getForm().getFields().each(function(field) {
							field.resetOriginalValue();
						});
						var grids = form.query('grid');
						for(var i=0; i<grids.length; i++) {
							grids[i].store.commitChanges();
						}
                        savebtn.setDisabled(true);
                        tab.setTitle(tab.title.replace(/^\*/, ''));
                    } else {
                        Ext.MessageBox.show({
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.MessageBox.OK,
                            msg: "Could not save:<br/>" + response.responseText.replace(/\n/, '<br/>')
                        });
                    }
                },
                failure: function(response) {
                    Ext.get(This.tabPanel.getId()).unmask();
                    _console(response.responseText);
                }
            });
        }
    });
    
    var inferbtn = new Ext.Button({
        text: 'Make suggestions',
        iconCls: 'inferIcon',
        handler: function() {
        	var form = tab.down('form');
        	Ext.get(This.tabPanel.getId()).mask("Getting Suggestions..");
        	var software = This.createSoftwareFromForm(form, id, compStore.classId);
        	Ext.Ajax.request({
                url: This.op_url + '/getInferredSoftware',
                params: {
                    softwareid: id,
                    software_json: Ext.encode(software)
                },
                success: function(response) {
                    Ext.get(This.tabPanel.getId()).unmask();
                    if(response.responseText) {
                    	var newsoftware = Ext.decode(response.responseText);
                    	// Compare software with newsoftware (get changes)
                    	// and inform user of changes to accept/reject
                    	This.showSuggestions(software, newsoftware, form);
                    }
                },
                failure: function(response) {
                    Ext.get(This.tabPanel.getId()).unmask();
                    _console(response.responseText);
                }
            });
        }
    });

    tab.softwareEditor = This.getSoftwareEditor(id, compStore, This.store.properties, tab, savebtn, true);
    
	var editable = true;
    var mainPanelItems = [ tab.softwareEditor ];
    
    var mainPanel = new Ext.Panel({
        region: 'center',
        border: false,
        tbar: editable ? [ inferbtn, savebtn ] : null,
        layout: 'fit',
        //tbar: tbar,
        bodyStyle: editable ? '' : 'background-color:#ddd',
        items: mainPanelItems
    });
    tab.add(mainPanel);
};


SoftwareViewer.prototype.createSoftwareFromForm = function (form, softwareid, classId) {
	var software = {id: softwareid, classId: classId, propertyValues: [], 
			inputs: [], outputs: [],
			assumptions: [], standardnames: [],
			labels: {}};
	
	var fields = form.getForm().getFields();
	fields.each(function(field) {
		var prop = this.properties[field.name];
		if(!prop) 
			return;
		var propid = prop.id;
		
		var prov = field.provenance;
		var provarr = [];
		for(var ppropid in prov) {
			if(prov[ppropid])
				provarr.push({propertyId: ppropid, value: prov[ppropid]});
		}
		
		var val = field.getValue();
		if(Array.isArray(val)) {
			for(var i=0; i<val.length; i++) {
				software.propertyValues.push({propertyId: propid, value: val[i], 
					provenance: provarr});
			}
		}
		else {
			if(prop.range != (this.ns['xsd'] + "string") && val == "")
				val = null;
			software.propertyValues.push({propertyId: propid, value: val,
				provenance: provarr});
		}
	}, this);
	
	var names = {};
	var message = "";
	var notok = false;
	var This = this;
	var inputGrid = form.down('grid[type=input]');
    inputGrid.getStore().each(function(rec) {
        if (!rec.data.role || !rec.data.type) {
        	if(!rec.data.role) message += "Input Name not specified.. ";
        	if(!rec.data.type) message += "Input Type not specified.. ";
            notok = true;
        }
        else if(names[rec.data.role]) {
        	message += "Duplicate role name found: "+rec.data.role;
        	notok = true;
        }
        else {
        	names[rec.data.role] = 1;
        	software.inputs.push(This.prepareRoleRecord(rec.data, false));
        }
    });
    
	var outputGrid = form.down('grid[type=output]');
    outputGrid.getStore().each(function(rec) {
        if (!rec.data.role || !rec.data.type) {
        	if(!rec.data.role) message += "Output Name not specified.. ";
        	if(!rec.data.type) message += "Output Type not specified.. ";
            notok = true;
        }
        else if(names[rec.data.role]) {
        	message += "Duplicate role name found: "+rec.data.role;
        	notok = true;
        }
        else {
        	names[rec.data.role] = 1;
        	software.outputs.push(This.prepareRoleRecord(rec.data, false));
        }
    });
    
	var assGrids = form.query('grid[type=assumptions]');
	for(var i=0; i<assGrids.length; i++) {
		var assGrid = assGrids[i];
		assGrid.getStore().each(function(rec) {
	        if (!rec.data.id || !rec.data.categoryId) {
	        	if(!rec.data.id) message += "Assumption name not specified.. ";
	        	if(!rec.data.categoryId) message += "Assumption category not specified.. ";
	            notok = true;
	        }
	        else {
	        	software.assumptions.push(This.prepareRoleRecord(rec.data));
	        }
	    });
	}

	var snGrids = form.query('grid[type=standardnames]');
	for(var i=0; i<snGrids.length; i++) {
		var snGrid = snGrids[i];
		snGrid.getStore().each(function(rec) {
	        if (!rec.data.objectId || !rec.data.quantityId) {
	        	if(!rec.data.objectId) message += "StandardName Object not specified.. ";
	        	if(!rec.data.quantityId) message += "StandardName Quantity not specified.. ";
	            notok = true;
	        }
	        else {
	            if(!rec.data.operatorIds)
	            	rec.data.operatorIds = [];
	            
	        	// create id and label for the standard names here
	            var snlabel = This.getLabel(rec.data.objectId) + "__";
	            var opids = rec.data.operatorIds.filter( function fn(el){return el;} );
	            rec.data.operatorIds = opids;
	            if (opids.length > 0 && opids[0])
	              snlabel += This.getLabel(opids[0]) + "_of_";
	            if (opids.length > 1 && opids[1])
	              snlabel += This.getLabel(opids[1]) + "_of_";
	            snlabel += This.getLabel(rec.data.quantityId);
	            
	            rec.data.label = snlabel;
	            rec.data.id = snGrid.repons + camelCase(snlabel);
	        	software.standardnames.push(This.prepareRoleRecord(rec.data));
	        }
	    });
	}
	
	// Set labels for new ids
	for(var key in this.idmap) {
		if(this.idmap[key].added) {
			software.labels[key] = this.idmap[key].label;
		}
	}
    
    if (notok) {
        Ext.MessageBox.show({
            icon: Ext.MessageBox.ERROR,
            buttons: Ext.MessageBox.OK,
            msg: message
        });
        return;
    }
	return software;
};

SoftwareViewer.prototype.getLabel = function(itemId) {
	if(!itemId) return itemId;
	if(this.idmap[itemId] && this.idmap[itemId].label)
		return this.idmap[itemId].label;
	return getLocalName(itemId);
};

SoftwareViewer.prototype.coalesceSoftwarePropertyValues = function(software) {
	var pvs = {};
	for(var i=0; i<software.propertyValues.length; i++) {
		var pval = software.propertyValues[i];
		if(!pval.value) continue;
		var prop = this.properties[pval.propertyId];
		if(pvs[prop.id] && pvs[prop.id] != pval.value) {
			if(!Array.isArray(pvs[prop.id]))
				pvs[prop.id] = [pvs[prop.id]];
			pvs[prop.id].push(pval.value);
			pvs[prop.id].sort();
		}
		else {
			pvs[prop.id] = pval.value;
		}
	}
	return pvs;
};

SoftwareViewer.prototype.compareSoftwares = function(software, newsoftware) {
	var changes = [];
	var pvs = this.coalesceSoftwarePropertyValues(software);
	var npvs = this.coalesceSoftwarePropertyValues(newsoftware);
    for(var i=0; i<this.store.properties.length; i++) {
    	var prop = this.store.properties[i];
    	if(!prop.category)
    		continue;
		var pval = pvs[prop.id];
		var npval = npvs[prop.id];
		if (pval != npval && (pval+"" != npval+"")) {
			changes.push({
				propid : prop.id,
				oldval : pval,
				newval : npval
			});
		}
	}
	return changes;
};

SoftwareViewer.prototype.showSuggestions = function(software, newsoftware, form) {
	var changes = this.compareSoftwares(software, newsoftware);
	var This = this;
	
	var grid = {
		xtype: 'grid',
		region: 'center',
		forceFit: true,
        border: false,
        columnLines: true,
        autoScroll: true,
        selModel: Ext.create('Ext.selection.CheckboxModel', {
            checkOnly: true,
        }),
		columns: [
		    { text: 'Documentation field', dataIndex: 'propid', 
		    	renderer: function(v) {
		    		return getLocalName(v);
		    	}
		    },
		    { text: 'Current content', dataIndex: 'oldval',
		    	renderer: function(v) {
		    		if(v == "") return "-";
		    		return v;
		    	}
		    },
		    { text: 'Suggested content', dataIndex: 'newval',
		    	renderer: function(v) {
		    		if(v == "") return "-";
		    		return v;
		    	}
		    }
		],
		store: {
			fields: ['propid', 'oldval', 'newval'],
			data: changes
		},
        tbar: [{
            text: 'Accept Suggestions',
            iconCls: 'selectIcon',
            handler: function() {
            	var mygrid = this.up('grid');
                var recs = mygrid.getSelectionModel().getSelection();
                if (!recs || !recs.length) {
                    Ext.Msg.show({
                        msg: 'Select from below and then press button',
                        modal: false,
                        buttons: Ext.Msg.OK
                    });
                    return;
                }
                for (var i = 0; i < recs.length; i++) {
                    var rec = recs[i];
                    var propid = rec.get('propid');
                    var newval = rec.get('newval');
                    var field = form.getForm().findField(propid);
                    field.setValue(newval);
                    field.provenance = {};
                    field.provenance[This.ns[''] + "isInferred"] = true;
                    field.provenance[This.ns[''] + "timestamp"] = (new Date()).getTime();
                    field.setFieldStyle(This.getProvenanceStyle(field.provenance));
                    
					var infolabel = field.nextSibling('label');
					infolabel.setText(This.getProvenanceHtml(field.provenance), false);
					
                    mygrid.getStore().remove(rec);
                }
                if(mygrid.getStore().getTotalCount() == 0)
                	this.up('window').close();
            }
        }]
	};
	
	var explanations = this.getExplanationGrid(newsoftware);
	Ext.create('Ext.window.Window', {
		title: 'Suggestions',
		layout: 'border',
        constrain: true,
        maximizable: true,
        autoScroll: true,
        tools: [{
			type : 'help',
			tooltip : 'Get Help',
			handler : function(event, toolEl, panel) {
				This.showSuggestionsFAQ();
			}
		}],
        width: 600,
        height: 450,
		items: [ grid, explanations ]
	}).show();
	
};

SoftwareViewer.prototype.getExplanationGrid = function(data) {
    for (var i = 0; i < data.explanations.length; i++) {
        // Replace urls with local names
        data.explanations[i] = data.explanations[i].replace(/http.*?#/g, '');
        data.explanations[i] = data.explanations[i].replace(/\^\^\w+/g, '');
    }

    var exps = Ext.Array.map(data.explanations, function(a) {
        return {
            text: a
        };
    });
    var exp = new Ext.grid.Panel({
        columns: [{
            dataIndex: 'text',
            menuDisabled: true
        }],
        hideHeaders: true,
        region: 'south',
        title: 'Explanation',
        collapsible: false,
        animCollapse: false,
        preventHeader: true,
        hideCollapseTool: true,
        //hidden: true,
        forceFit: true,
        viewConfig: {
            getRowClass: function(record, rowIndex, rp, ds) {
                if (record.data.text.match(/ERROR/i)) {
                    return "errorCls";
                }
                if (record.data.text.match(/INFO/i)) {
                    return "infoCls";
                }
                if (record.data.text.match(/Suggest/i)) {
                    return "suggestCls";
                }
            }
        },
        bodyCls: 'multi-line-grid',
        height: 200,
        // border: false,
        split: true,
        autoScroll: true,
        store: new Ext.data.Store({
            fields: ['text'],
            data: exps
        }),
        });
    return exp;
};


SoftwareViewer.prototype.getSoftwareEditor = function (id, store, props, maintab, savebtn, editable) {
	var editorPanel = {
		xtype: 'form',
        fieldDefaults: {
            msgTarget: 'side',
            labelWidth: 150
        },
        frame: false,
        border: false,
        layout: 'border',
		items: [],
		listeners: {
			dirtychange: function(item, dirty, opts) {
				if(dirty) {
					savebtn.setDisabled(false);
					maintab.setTitle("*" + maintab.title.replace(/^\*/, ''));
				}
			}
		}
	};
	
	var editorTabPanel = {
		xtype : 'tabpanel',
		region: 'center',
        border: false,
        plain: true,
		activetab : 0,
		items : []
	};
	editorPanel.items.push(editorTabPanel);
    
	var ioEditor = this.getIOListEditor(id, store, this.store.data_types, maintab, savebtn, editable);
	editorTabPanel.items.push(ioEditor);
	
	var asEditor = this.getAssumptionsEditor(id, store, this.store.sninfo, maintab, savebtn, editable);
	editorTabPanel.items.push(asEditor);

	var snEditor = this.getStandardNamesEditor(id, store, this.store.sninfo, maintab, savebtn, editable);
	editorTabPanel.items.push(snEditor);
	
	var propContainer = {};
	var sectionById = {};
	var propLabels = {};
	
	var propValues = {};
	var propProv = {};
	for(var i=0; i<store.propertyValues.length; i++) {
		var pv = store.propertyValues[i];
		var prop = pv.propertyId;
		var curval = propValues[prop];
		if(curval) {
			if(Array.isArray(curval)) 
				propValues[prop].push(pv.value);
			else
				propValues[prop] = [curval, pv.value];
		}
		else
			propValues[prop] = pv.value;
		
		if(!propProv[prop])
			propProv[prop] = {};
		// Store provenance
		var prov = pv.provenance;
		for(var j=0; j<prov.length; j++) {
			var ppv = prov[j];
			var pprop = ppv.propertyId;
			propProv[prop][pprop] = ppv.value;
		}
	}
	
	var This = this;
	
	props = props.sort(function(x, y) {
		var xc = This.propcomments[x.id];
		var yc = This.propcomments[y.id];
		var posx = (xc && xc.ui && xc.ui.position) ? xc.ui.position : 0;
		var posy = (yc && yc.ui && yc.ui.position) ? yc.ui.position : 0;
		var secx = (xc && xc.ui && xc.ui.section) ? xc.ui.section : 0;
		var secy = (yc && yc.ui && yc.ui.section) ? yc.ui.section : 0;
		return ((secx*100 + posx) - (secy*100 + posy));
	});
	
	// Create tabs
	for(var i=0; i<props.length; i++) {
		var prop = props[i];
		propLabels[prop.id] = prop.label;
		if(!prop.category) {
			var label = propLabels[prop.id];
			var tab = propContainer[label];
			if(!tab) {
				tab = {
					xtype: 'panel',
					title: prop.label,
			        frame:true,
			        layout: 'form',
			        bodyStyle:'padding:5px',
			        margin: 5,
					autoScroll: true,
					items: []
				};
				propContainer[prop.label] = tab;
				editorTabPanel.items.push(tab);
			}
		}
	}
	
	// Create sections (if needed)
	for(var i=0; i<props.length; i++) {
		var prop = props[i];
		if(prop.category) {
			var catlabel = propLabels[prop.category];
			var tab = propContainer[catlabel];
			var pcui = This.propcomments[prop.id].ui;
			if( pcui && pcui.section) {
				var fset = propContainer[catlabel+":"+pcui.section];
				if(!fset) {
					fset = {
						xtype: 'fieldset',
						title: 'Section '+pcui.section,
						layout: 'anchor',
						items:[]
					};
					tab.items.push(fset);
					propContainer[catlabel+":"+pcui.section] = fset;
				}
			}
		}
	}
	
	// Create editors within the tabs
	for(var i=0; i<props.length; i++) {
		var prop = props[i];
		if(prop.category) {
			var pcui = This.propcomments[prop.id].ui;
			var catlabel = propLabels[prop.category];
			var comp = null;
			if(pcui && pcui.section)
				comp = propContainer[catlabel + ":" + pcui.section];
			if(!comp)
				comp = propContainer[catlabel];
			if(!comp) continue;
			var provenance = propProv[prop.id];
			
			var item = {
				name: prop.id,
				fieldLabel: prop.label ? prop.label : getLocalName(prop.id),
				value: propValues[prop.id],
				flex: 1,
				anchor: '100%',
				provenance: provenance,
				fieldStyle: this.getProvenanceStyle(provenance),
				listeners: {
					change: function (item, newv, oldv, opts) {
						item.provenance = {};
						item.provenance[This.ns[''] + "editedBy"] = USER_ID;
						item.provenance[This.ns[''] + "timestamp"] = (new Date()).getTime();
						item.setFieldStyle(This.getProvenanceStyle(item.provenance));
						
						var infolabel = item.nextSibling('label');
						infolabel.setText(This.getProvenanceHtml(item.provenance), false);
					}
				}
			};
			if(prop.isObjectProperty) {
				var store = new Ext.data.Store({
					fields: ['value'],
					sorters: ['value'],
					data: Ext.Array.map (prop.possibleValues, function (x) {
						return {id: x, value: getLocalName(x)};
					}, this)
				});
				item.xtype = 'combo';
				item.store = store;
				item.multiSelect = true;
				item.displayField = 'value';
				item.valueField = 'id';
				item.forceSelection = true;
			}
			else if(prop.range == this.ns['xsd'] + "int") {
				item.xtype = 'numberfield';
				item.allowDecimals = false;
			}
			else if(prop.range == this.ns['xsd'] + "integer") {
				item.xtype = 'numberfield';
				item.allowDecimals = false;
			}
			else if(prop.range == this.ns['xsd'] + "boolean") {
				item.xtype = 'checkbox';
				item.checked = Boolean(item.value);
			}
			else if(prop.range == this.ns['xsd'] + "date")
				item.xtype = 'datefield';
			else if(prop.label && prop.label.match(/descri/i))
				item.xtype = 'textareafield';
			else 
				item.xtype = 'textfield';
			
			var info = this.getProvenanceHtml(provenance);
			var infoitem = {
				xtype : 'label',
				cls : 'info',
				html : info,
				width: 150,
				style: 'padding-left:10px; font-size: 10px; font-style:italic'
			};
			
			comp.items.push({
				xtype: 'panel',
				layout: 'hbox',
				padding: 3,
				border: false,
				bodyStyle: 'background:transparent',
				items: [item, infoitem]
			});
		}
	}
	return editorPanel;
};

SoftwareViewer.prototype.getProvenanceHtml = function(provenance) {
	var info = "";
	if(provenance) {
		var xns = this.ns[''];
		var eby = provenance[xns+'editedBy'];
		var inf = provenance[xns+'isInferred'];
		var imp = provenance[xns+'importedFrom'];
		var ts = provenance[xns+'timestamp'];
		if(eby)
			info += "<b style='color:blue'>Edited by "+eby+"</b>";
		else if(inf)
			info += "<b style='color:brown'>Turbosoft Suggestion"+"</b>";
		else if(imp)
			info += "<b style='color:green'>Imported from "+imp+"</b>";
		if(ts)
			info += "<div style='color:grey'>(" + Ext.Date.format(new Date(ts), 
					'F j Y, g:ia') + ")</div>";
	}
	else {
		info = "<span style='color:grey'>No value</span>";
	}
	return info;
};

SoftwareViewer.prototype.getProvenanceCreationHtml = function(provenance) {
	var info = "";
	if(provenance) {
		var xns = this.ns[''];
		var eby = provenance[xns+'editedBy'];
		var ts = provenance[xns+'timestamp'];
		if(eby)
			info += "By <b>"+eby+"</b>";
		if(ts)
			info += " on " + Ext.Date.format(new Date(ts), 'F j Y, g:ia');
	}
	if(!info)
		info = "<i style='color:#999'>From original ontology</i>";
	return info;
};

SoftwareViewer.prototype.getProvenanceStyle = function(provenance) {
	if(provenance) {
		var xns = this.ns[''];
		if(provenance[xns+'editedBy'])
			return "color:blue";
		if(provenance[xns+'isInferred'])
			return "color:brown";
		if(provenance[xns+'importedFrom'])
			return "color:green";
	}
	return "color:grey";
};

SoftwareViewer.prototype.getIOListEditor = function(c, iostore, data_types, tab, savebtn, editable) {
    var This = this;

    var mainPanel = new Ext.Panel({
        region: 'center',
        title: 'I/O',
        iconCls: 'paramIcon',
        border: false,
        defaults: {
            border: false,
            padding: 0
        },
        autoScroll: true
    });

    // Register store models
    if (!Ext.ModelManager.isRegistered('DataRole'))
        Ext.define('DataRole', {
	        extend: 'Ext.data.Model',
	        fields: ['role', 'type', 'provenance']
        });
    if (!Ext.ModelManager.isRegistered('dataPropRangeTypes'))
        Ext.define('dataPropRangeTypes', {
	        extend: 'Ext.data.Model',
	        fields: ['id', 'type']
        });

    // Create stores for Inputs, Params and Outputs
    var ipStore = new Ext.data.Store({
        model: 'DataRole',
        data: iostore.inputs
    });
    var opStore = new Ext.data.Store({
        model: 'DataRole',
        data: iostore.outputs
    });

    var iDataGrid, oDataGrid;

    // Create editors
    for (var i = 0; i < 2; i++) {
        var typeEditor = new Ext.form.ComboBox({
            store: {
                model: 'dataPropRangeTypes',
                data: Ext.Array.map(data_types, function(typeid) {
                    return {
                        id: typeid,
                        type: getPrefixedUrl(typeid, This.ns)
                    };
                }),
                sorters: ['type']
            },
            displayField: 'type',
            valueField: 'id',
            queryMode: 'local',
            typeAhead: true,
            forceSelection: true,
            allowBlank: false
        });
        var txtEditor = new Ext.form.field.Text({
            allowBlank: false
        });

        var columns = [{
            dataIndex: 'role',
            header: 'Identifier',
            flex: 1,
            editor: txtEditor,
            menuDisabled: true
        }, {
            dataIndex: 'type',
            header: 'Type',
            flex: 1,
            renderer: function(url) {
                return getPrefixedUrl(url, This.ns);
            },
            editor: typeEditor,
            menuDisabled: true
        }, {
        	dataIndex: 'provenance',
        	header: 'Provenance',
        	flex: 1,
        	editable: false,
        	renderer: function (v) {
        		return This.getProvenanceCreationHtml(v);
        	}
        }];

        var sm = editable ? Ext.create('Ext.selection.CheckboxModel', {
            checkOnly: true,
            }) : Ext.create('Ext.selection.RowModel');

        var editorPlugin = Ext.create('Ext.grid.plugin.FlexibleCellEditing', {
            clicksToEdit: 1,
            listeners: {
            	edit: function(editor, e, opts) {
            		// Set provenance if value changed
            		if(e.value != e.originalValue) {
            			var prov = {};
            			prov[This.ns[''] + "editedBy"] = USER_ID;
    					prov[This.ns[''] + "timestamp"] = (new Date()).getTime();
    					e.record.set('provenance', prov);
            		}
            	}
            }
        });

        var plugins = editable ? [editorPlugin] : [];
        var bodycls = editable ? '': 'inactive-grid';

        var gridStore = (i == 0 ? ipStore: opStore);
        var tbar = null;
        if (editable) {
            tbar = [{
                text: 'Add',
                iconCls: 'addIcon',
                roletype: i,
                handler: function() {
                    var i = this.roletype;
                    var panel = (i == 0 ? iDataGrid: oDataGrid);
                    var gridStore = panel.getStore();
                    var pos = gridStore.getCount();
                    var sm = panel.getSelectionModel();
                    var role = new DataRole();
                    panel.editorPlugin.cancelEdit();
                    gridStore.insert(pos, role);
                    panel.editorPlugin.startEditByPosition({
                    	row:pos,
                    	column:1
                    });
                }
            }, {
                iconCls: 'delIcon',
                text: 'Delete',
                roletype: i,
                handler: function() {
                    var i = this.roletype;
                    var panel = (i == 0 ? iDataGrid: oDataGrid);
                    var gridStore = panel.getStore();
                    panel.editorPlugin.cancelEdit();
                    var s = panel.getSelectionModel().getSelection();
                    for (var i = 0, r; r = s[i]; i++) {
                        gridStore.remove(r);
                    }
                    // mainPanel.doLayout();
                }
            }];
        }

        var gridPanel = new Ext.grid.GridPanel({
            columnLines: true,
            autoHeight: true,
            border: false,
            // forceFit: true,
            title: (i == 0 ? 'Input Data': 'Output Data'),
            iconCls: (i == 0 ? 'inputIcon': 'outputIcon'),
            type: !i ? 'input' : 'output',
            columns: columns,
            selModel: sm,
            selType: 'cellmodel',
            plugins: plugins,
            bodyCls: bodycls,
            store: gridStore,
            tbar: tbar,
			tools : [{
				type : 'help',
				tooltip : 'Get Help',
				handler : function(event, toolEl, panel) {
					This.showIOFAQ();
				}
			}]
        });
        gridPanel.editorPlugin = editorPlugin;

        if (i == 0)
            iDataGrid = gridPanel;
        if (i == 1)
            oDataGrid = gridPanel;
        
        gridStore.on('add', function() {
            tab.setTitle("*" + tab.title.replace(/^\*/, ''));
            savebtn.setDisabled(false);
        });
        gridStore.on('remove', function() {
            tab.setTitle("*" + tab.title.replace(/^\*/, ''));
            savebtn.setDisabled(false);
        });
        gridStore.on('update', function() {
            tab.setTitle("*" + tab.title.replace(/^\*/, ''));
            savebtn.setDisabled(false);
        });
    }

    mainPanel.add(iDataGrid);
    mainPanel.add(oDataGrid);

    return mainPanel;
};


SoftwareViewer.prototype.getAssumptionsEditor = function(c, store, sninfo, tab, savebtn, editable) {
    var This = this;

    var mainPanel = new Ext.tab.Panel({
        region: 'center',
        layout: 'border',
        title: 'Assumptions',
        border: false
    });

    var activeTabId = 0;
    var tabId = 0;
    var catEditor = {};
    var assEditor = {};
    
    for(var repoid in sninfo.repos) {
    	if(repoid == "CSDMS")
    		activeTabId = tabId;
    	tabId++;
    	
    	var repourl = sninfo.repos[repoid];
    	var repons = repourl + "#";
    	
    	// Get assumptions for this repository id
    	var assumptions = [];
    	var categories = [];
    	
    	for(var i=0; i<sninfo.categories.length; i++) {
    		var cat = sninfo.categories[i];
    		if(cat.id.indexOf(repourl) == 0)
	    		categories.push(cat);
    	}
    	for(var i=0; i<sninfo.assumptions.length; i++) {
    		var assumption = sninfo.assumptions[i];
    		if(assumption.id.indexOf(repourl) == 0)
    			assumptions.push(assumption);
    	}
    	
    	var myassumptions = [];
    	for(var i=0; i<store.assumptions.length; i++) {
    		var assumption = store.assumptions[i];
    		if(assumption.id.indexOf(repourl) == 0)
    			myassumptions.push(assumption);
    	}
    	
    	// Create tab for this repo
    	var repotab = new Ext.Panel({
            region: 'center',
    		title: repoid,
            border: false,
            defaults: {
                border: false,
                padding: 0
            },
            autoScroll: true
    	});
    	
        // Register store models
        if (!Ext.ModelManager.isRegistered('Assumption'))
            Ext.define('Assumption', {
	            extend: 'Ext.data.Model',
	            fields: ['id', 'label', 'categoryId', 'provenance']
            });
        if (!Ext.ModelManager.isRegistered('Category'))
            Ext.define('Category', {
	            extend: 'Ext.data.Model',
	            fields: ['id', 'label']
            });

        var gridStore = new Ext.data.Store({
        	model: 'Assumption',
            data: myassumptions
        });
        
    	// Create category editor
        catEditor[repons] = new Ext.form.ComboBox({
            store: {
            	model: 'Category',
            	data: categories,
            	sorters: ['label']
            },
            displayField: 'label',
            valueField: 'id',
            queryMode: 'local',
            typeAhead: true,
            emptyText: 'None',
            //forceSelection: true,
            allowBlank: false
        });
        // Create assumption editor for this repo
        assEditor[repons] = new Ext.form.ComboBox({
            store: {
            	model: 'Assumption',
            	data: assumptions,
            	sorters: ['label']
            },
            displayField: 'label',
            valueField: 'id',
            queryMode: 'local',
            typeAhead: true,
            //forceSelection: true,
            emptyText: 'None',
            allowBlank: false
        });

        var columns = [{
	            dataIndex: 'categoryId',
	            header: 'Category',
	            flex: 1,
	            editor: catEditor[repons],
	            renderer: function (v) {
	            	return This.getLabel(v);
	            },
	            menuDisabled: true
	        },
	        {
	            dataIndex: 'id',
	            header: 'Assumption',
	            flex: 1,
	            editor: assEditor[repons],
	            renderer: function (v, b, rec) {
	            	return This.getLabel(v);
	            },
	            menuDisabled: true
	        },
	        {
	        	dataIndex: 'provenance',
	        	header: 'Creation Provenance',
	        	flex: 1,
	        	editable: false,
	        	renderer: function (v) {
	        		return This.getProvenanceCreationHtml(v);
	        	}
	        }];

        var sm = editable ? Ext.create('Ext.selection.CheckboxModel', {
            checkOnly: true,
            }) : Ext.create('Ext.selection.RowModel');

        var editorPlugin = Ext.create('Ext.grid.plugin.FlexibleCellEditing', {
            clicksToEdit: 1,
            listeners: {
            	beforeedit: function(editor, e, opts) {
            		if(e.field == "id") {
            			var catid = e.record.get("categoryId");
        				var astore = assEditor[e.grid.repons].getStore();
        				astore.clearFilter(true);
        				
            			if(catid) {
            				astore.filter({
            					filterFn: function(item) {
            						// Only show items of this category
	            					if(item.data.categoryId == catid)
	            						return true;
	            					return false;
            						// *removed* Also remove assumptions already used
            						// e.grid.getStore().getById(item.data.id)
            					}
            				});
            			}
            		}
            	},
            	edit: function(editor, e, opts) {
            		if(e.field == "categoryId") {
            			if(e.value.indexOf("http://") != 0) {
            				// If this is a new category
	    	        		var catlabel = e.value;
	    	        		var catid = e.grid.repons + camelCase(catlabel);
	    	        		var category = {id: catid, label: catlabel, added: true};
	    	        		This.idmap[catid] = category;
	    	        		e.record.set('categoryId', catid);
	    	        		catEditor[e.grid.repons].getStore().add(new Category(category));
	    	        		This.setDefaultProvenance(e.record);
            			}
            			var id = e.record.get("id");
            			if(!id) 
            				return;
            			var assumption = This.idmap[id];
            			if(assumption && assumption.categoryId != e.value) {
            				e.record.set("id", null);
            			}
            		}
            		else if (e.field == "id") {
    	        		var catid = e.record.get("categoryId");
    	        		var asid = e.value;
            			if(asid.indexOf("http://") != 0) {
            				// If this is a new assumption
	    	        		var aslabel = e.value.replace(/\s/g,'_');
	    	        		asid = e.grid.repons + camelCase(aslabel);
	    	        		var assumption = {id: asid, label: aslabel, categoryId: catid, added:true};
	    	        		if(This.idmap[asid]) {
	    	        			e.value = "";
	    	        			return false;
	    	        		}
	    	        		This.idmap[asid] = assumption;
	    	        		e.record.set('id', asid);
	    	        		e.record.set('label', aslabel);
	    	        		assEditor[e.grid.repons].getStore().add(new Assumption(assumption));
	    	        		This.setDefaultProvenance(e.record);
            			}
            			var catid = e.record.get("categoryId");
            			if(!catid) {
            				var assumption = This.idmap[asid];
            				if(assumption)
            					e.record.set("categoryId", assumption.categoryId);
            			}
            		}
            	}
            }
        });

        var plugins = editable ? [editorPlugin] : [];
        var bodycls = editable ? '': 'inactive-grid';

        var tbar = null;
        if (editable) {
            tbar = [{
                text: 'Add Assumption',
                iconCls: 'addIcon',
                handler: function() {
                	var panel = this.up('panel');
                    var gstore = panel.getStore();
                    var pos = gstore.getCount();
                    var sm = panel.getSelectionModel();
                    panel.editorPlugin.cancelEdit();
                    gstore.insert(pos, new Assumption());
                    panel.editorPlugin.startEditByPosition({
                    	row:pos,
                    	column:1
                    });
                }
            }, {
                iconCls: 'delIcon',
                text: 'Delete',
                roletype: i,
                handler: function() {
                	var panel = this.up('panel');
                    var gstore = panel.getStore();
                    panel.editorPlugin.cancelEdit();
                    var s = panel.getSelectionModel().getSelection();
                    for (var i = 0, r; r = s[i]; i++) {
                    	gstore.remove(r);
                    }
                }
            }];
        }

        var gridPanel = new Ext.grid.GridPanel({
            type: 'assumptions',
            repons: repons,
            
            columnLines: true,
            autoHeight: true,
            border: false,
            editorPlugin: editorPlugin,
            // forceFit: true,
            title: 'Assumptions',
            columns: columns,
            selModel: sm,
            selType: 'cellmodel',
            plugins: plugins,
            bodyCls: bodycls,
            store: gridStore,
            tbar: tbar
			/*tools : [{
				type : 'help',
				tooltip : 'Get Help',
				handler : function(event, toolEl, panel) {
					This.showAssumptionsFAQ();
				}
			}]*/
        });
        
        gridStore.on('add', function() {
            tab.setTitle("*" + tab.title.replace(/^\*/, ''));
            savebtn.setDisabled(false);
        });
        gridStore.on('remove', function() {
            tab.setTitle("*" + tab.title.replace(/^\*/, ''));
            savebtn.setDisabled(false);
        });
        gridStore.on('update', function() {
            tab.setTitle("*" + tab.title.replace(/^\*/, ''));
            savebtn.setDisabled(false);
        });
        
        repotab.add(gridPanel);
        mainPanel.add(repotab);
    }
    mainPanel.setActiveTab(activeTabId);

    return mainPanel;
};

SoftwareViewer.prototype.setDefaultProvenance = function(rec) {
	var prov = {};
	prov[this.ns[''] + "editedBy"] = USER_ID;
	prov[this.ns[''] + "timestamp"] = (new Date()).getTime();
	rec.set('provenance', prov);	
};

SoftwareViewer.prototype.getStandardNamesEditor = function(c, store, sninfo, tab, savebtn, editable) {
    var This = this;

    var mainPanel = new Ext.tab.Panel({
        region: 'center',
        layout: 'border',
        title: 'Standard Names',
        border: false
    });

    var activeTabId = 0;
    var tabId = 0;
    
    var objectEditor = {};
    var quantityEditor = {};
    var operatorEditor = {};
    
    for(var repoid in sninfo.repos) {
    	if(repoid == "CSDMS")
    		activeTabId = tabId;
    	tabId++;
    	
    	var repourl = sninfo.repos[repoid];
    	var repons = repourl + "#";
    	
    	// Get assumptions for this repository id
    	var objects = [];
    	var operators = [];
    	var quantities = [];
    	
    	// These are stored with the grid below
    	var objqs = {};
    	var objqops = {};
    	
    	for(var i=0; i<sninfo.objects.length; i++) {
    		var item = sninfo.objects[i];
    		if(item.id.indexOf(repourl) == 0)
	    		objects.push(item);
    	}
    	for(var i=0; i<sninfo.operators.length; i++) {
    		var item = sninfo.operators[i];
    		if(item.id.indexOf(repourl) == 0)
	    		operators.push(item);
    	}
    	for(var i=0; i<sninfo.quantities.length; i++) {
    		var item = sninfo.quantities[i];
    		if(item.id.indexOf(repourl) == 0)
	    		quantities.push(item);
    	}
    	for(var i=0; i<sninfo.standardnames.length; i++) {
    		var item = sninfo.standardnames[i];
    		if(item.id.indexOf(repourl) == 0) {
    			if(!objqs[item.objectId]) {
    				objqs[item.objectId] = {};
    				objqops[item.objectId] = {};
    			}
    			if(!objqs[item.objectId][item.quantityId]) {
        			objqs[item.objectId][item.quantityId] = 1;
    				objqops[item.objectId][item.quantityId] = {};
    			}
    			for(var j=0; j<item.operatorIds.length; j++)
    				objqops[item.objectId][item.quantityId][item.operatorIds[j]] = 1;
    		}
    	}
    	
    	var mystandardnames = [];
    	for(var i=0; i<store.standardnames.length; i++) {
    		var sname = store.standardnames[i];
    		if(sname.id.indexOf(repourl) == 0) {
    			mystandardnames.push(sname);
    		}
    	}
    	
    	// Create tab for this repo
    	var repotab = new Ext.Panel({
            region: 'center',
    		title: repoid,
            border: false,
            defaults: {
                border: false,
                padding: 0
            },
            autoScroll: true
    	});
    	
        // Register store models
        if (!Ext.ModelManager.isRegistered('IdLabel'))
            Ext.define('IdLabel', {
	            extend: 'Ext.data.Model',
	            fields: ['id', 'label']
            });
        
        if (!Ext.ModelManager.isRegistered('StandardName'))
            Ext.define('StandardName', {
	            extend: 'Ext.data.Model',
	            fields: ['id', 'label', 'objectId', 'quantityId', 'operatorIds', 'provenance']
            });

        var gridStore = new Ext.data.Store({
        	model: 'StandardName',
            data: mystandardnames
        });
        
    	// Create object editor
        objectEditor[repons] = new Ext.form.ComboBox({
            store: {
            	model: 'IdLabel',
            	data: objects,
            	sorters: ['label']
            },
            displayField: 'label',
            valueField: 'id',
            queryMode: 'local',
            typeAhead: true,
            //forceSelection: true,
            emptyText: 'None',
            allowBlank: false
        });
    	// Create quantity editor
        quantityEditor[repons] = new Ext.form.ComboBox({
            store: {
            	model: 'IdLabel',
            	data: quantities,
            	sorters: ['label']
            },
            displayField: 'label',
            valueField: 'id',
            queryMode: 'local',
            typeAhead: true,
            //forceSelection: true,
            emptyText: 'None',
            allowBlank: false
        });
    	// Create operator editor
        operatorEditor[repons] = new Ext.form.ComboBox({
            store: {
            	model: 'IdLabel',
            	data: operators,
            	sorters: ['label']
            },
            displayField: 'label',
            valueField: 'id',
            queryMode: 'local',
            typeAhead: true,
            multiSelect: true,
            //forceSelection: true,
            emptyText: 'None',
            allowBlank: true
        });
        
        var columns = [{
	            dataIndex: 'objectId',
	            header: 'Object',
	            flex: 1,
	            editor: objectEditor[repons],
	            renderer: function (v) {
	            	return This.getLabel(v);
	            },
	            menuDisabled: true
	        },
	        {
	        	dataIndex: 'quantityId',
	            header: 'Quantity',
	            flex: 1,
	            editor: quantityEditor[repons],
	            renderer: function (v) {
	            	return This.getLabel(v);
	            },
	            menuDisabled: true
	        },
	        {
	        	dataIndex: 'operatorIds',
	            header: 'Operators',
	            flex: 1,
	            editor: operatorEditor[repons],
	            renderer: function (v) {
	            	if(!v) return v;
	            	var nv = [];
	            	for(var j=0; j<v.length; j++) {
	            		var label = This.getLabel(v[j]);
	            		if(label) nv.push(label);
	            	}
	            	return nv;
	            },
	            menuDisabled: true
	        },
	        {
	        	dataIndex: 'provenance',
	        	header: 'Creation Provenance',
	        	flex: 1,
	        	editable: false,
	        	renderer: function (v) {
	        		return This.getProvenanceCreationHtml(v);
	        	}
	        }];

        var sm = editable ? Ext.create('Ext.selection.CheckboxModel', {
            checkOnly: true,
            }) : Ext.create('Ext.selection.RowModel');

        var editorPlugin = Ext.create('Ext.grid.plugin.FlexibleCellEditing', {
            clicksToEdit: 1,
            listeners: {
            	beforeedit: function(editor, e, opts) {
            		if(e.field == "quantityId") {
            			var objid = e.record.get("objectId");
        				var qstore = quantityEditor[e.grid.repons].getStore();
        				qstore.clearFilter(true);
            			if(objid) {
            				qstore.filter({
            					filterFn: function(item) {
	            					if(e.grid.objqs[objid] &&
	            							e.grid.objqs[objid][item.data.id])
	            						return true;
	            					return false;
            					}
            				});
            			}
            		}
            		else if(e.field == "operatorIds") {
            			var objid = e.record.get("objectId");
            			var qid = e.record.get("quantityId");
        				var opstore = operatorEditor[e.grid.repons].getStore();
        				opstore.clearFilter(true);
            			if(objid) {
            				opstore.filter({
            					filterFn: function(item) {
            						if(e.grid.objqops[objid][qid][item.data.id])
            							return true;
            						return false;
            					}
            				});
            			}
            		}
            	},
            	edit: function(editor, e, opts) {
            		if(e.field == "objectId") {
            			var objid = e.value;
            			if(!objid) return;
            			if(objid.indexOf("http://") != 0) {
            				// If this is a new object
	    	        		var objlabel = objid.replace(/\s/g,'_');
	    	        		var objid = e.grid.repons + camelCase(objlabel);
	    	        		var obj = {id: objid, label: objlabel, added: true};
	    	        		
	    	        		This.idmap[objid] = obj;
	    	        		e.record.set('objectId', objid);
	    	        		objectEditor[e.grid.repons].getStore().add(new IdLabel(obj));
	    	        		
	    	        		e.grid.objqops[objid] = {};
	    	        		This.setDefaultProvenance(e.record);
            			}
            			var qid = e.record.get("quantityId");
            			var opid = e.record.get("operatorId");
            			if(qid && !e.grid.objqs[objid][qid])
            				e.record.set("quantityId", null);
            			if(opid && !e.grid.objqops[objid][qid][opid])
            				e.record.set("operatorId", null);
            		}
            		if(e.field == "quantityId") {
            			var qid = e.value;
            			if(!qid) return;
            			if(qid.indexOf("http://") != 0) {
            				// If this is a new quantity
	    	        		var qlabel = qid.replace(/\s/g,'_');;
	    	        		var qid = e.grid.repons + camelCase(qlabel);
	    	        		var q = {id: qid, label: qlabel, added: true};
	    	        		
	    	        		This.idmap[qid] = q;
	    	        		e.record.set('quantityId', qid);
	    	        		quantityEditor[e.grid.repons].getStore().add(new IdLabel(q));
	    	        		
            				var objid = e.record.get("objectId");
            				e.grid.objqops[objid][qid] = {};
            				This.setDefaultProvenance(e.record);
            			}
            		}
            		else if(e.field == "operatorIds") {
            			var opids = e.value;
            			if(!opids) return;
            			if(!Array.isArray(opids))
            				opids = [opids];
            			var newop = false;
            			for(var j=0; j<opids.length; j++) {
            				var opid = opids[j];
            				if(!opid) continue;
	            			if(opid.indexOf("http://") != 0) {
	            				// If this is a new operator
		    	        		var oplabel = opid.replace(/\s/g,'_');;
		    	        		var opid = e.grid.repons + camelCase(oplabel);
		    	        		var op = {id: opid, label: oplabel, added: true};
		    	        		
		    	        		This.idmap[opid] = op;
		    	        		operatorEditor[e.grid.repons].getStore().add(new IdLabel(op));

		    	        		newop = true;
		    	        		opids[j] = opid;
		    	        		
	            				var objid = e.record.get("objectId");
	            				var qid = e.record.get("quantityId");
	            				e.grid.objqops[objid][qid][opid] = 1;
	            				This.setDefaultProvenance(e.record);
	            			}
            			}
            			if(newop) 
            				e.record.set('operatorIds', opids);
            		}
            	}
            }
        });

        var plugins = editable ? [editorPlugin] : [];
        var bodycls = editable ? '': 'inactive-grid';

        var tbar = null;
        if (editable) {
            tbar = [{
                text: 'Add Standard Name',
                iconCls: 'addIcon',
                handler: function() {
                	var panel = this.up('panel');
                    var gstore = panel.getStore();
                    var pos = gstore.getCount();
                    var sm = panel.getSelectionModel();
                    panel.editorPlugin.cancelEdit();
                    gstore.insert(pos, new StandardName());
                    panel.editorPlugin.startEditByPosition({
                    	row:pos,
                    	column:1
                    });
                }
            }, {
                iconCls: 'delIcon',
                text: 'Delete',
                roletype: i,
                handler: function() {
                	var panel = this.up('panel');
                    var gstore = panel.getStore();
                    panel.editorPlugin.cancelEdit();
                    var s = panel.getSelectionModel().getSelection();
                    for (var i = 0, r; r = s[i]; i++) {
                    	gstore.remove(r);
                    }
                }
            }];
        }

        var gridPanel = new Ext.grid.GridPanel({
            type: 'standardnames',
            repons: repons,
            objqs : objqs,
            objqops : objqops,
            
            columnLines: true,
            autoHeight: true,
            border: false,
            editorPlugin: editorPlugin,
            // forceFit: true,
            title: 'Standard Names',
            columns: columns,
            selModel: sm,
            selType: 'cellmodel',
            plugins: plugins,
            bodyCls: bodycls,
            store: gridStore,
            tbar: tbar
			/*tools : [{
				type : 'help',
				tooltip : 'Get Help',
				handler : function(event, toolEl, panel) {
					This.showStandardNamesFAQ();
				}
			}]*/
        });
        
        gridStore.on('add', function() {
            tab.setTitle("*" + tab.title.replace(/^\*/, ''));
            savebtn.setDisabled(false);
        });
        gridStore.on('remove', function() {
            tab.setTitle("*" + tab.title.replace(/^\*/, ''));
            savebtn.setDisabled(false);
        });
        gridStore.on('update', function() {
            tab.setTitle("*" + tab.title.replace(/^\*/, ''));
            savebtn.setDisabled(false);
        });
        
        repotab.add(gridPanel);
        mainPanel.add(repotab);
    }
    mainPanel.setActiveTab(activeTabId);

    return mainPanel;
};


SoftwareViewer.prototype.showSuggestionsFAQ = function() {
	var html = "<ul>"
		+"<li>What are suggestions? (1)</li>"
		+"<li>How does the system make suggestions? (2)</li>"
		+"<li>Can I see the rules that the sytem uses to make suggestions ? (3)</li>"
		+"</ul>"
		+"<ol>"
		+"<li>The system will make suggestions based on what you have specified " +
				"so far about the software you are describing. These suggestions are " +
				"meant to save you time </li>" 
		+"<li>You are interacting with an intelligent system that tries to understand " +
				"what you have specified so far about the software and reasons about it " +
				"to make suggestions.  The system has some basic knowledge about how " +
				"software works, some common concepts in geosciences, and how users " +
				"describe software.  It combines this basic knowledge together with " +
				"rules that result in the suggestions that you are shown .</li>"
		+"<li>The rules that the system uses are codified in mathematical logic. " +
				"Naturally, you have to learn logic in order to understand them. " +
				"But if you are really curious, " +
				"<a href='http://www.isi.edu/ikcap/geosoft/ontology/software.rules'>here is the " +
				"list of rules</a> </li>"
		+"</ol>";
	Ext.create('Ext.window.Window', {
		title: 'Help',
		layout: 'border',
        constrain: true,
        maximizable: true,
        autoScroll: true,
        width: 400,
        height: 350,
		html: html,
		padding: 5
	}).show();
};

SoftwareViewer.prototype.showIOFAQ = function() {
	var html = "<ul>"
		+"<li>What is an identifier ? (1)</li>"
		+"<li>Can I use the same identifier for two inputs or for one input " +
				"and one output of the component ? (2)</li>"
		+"<li>Are there special characters that should not be used in unique identifiers  ? (3)</li>"
		+"</ul>"
		+"<ol>"
		+"<li>Each input and output of a software component needs a unique way for " +
				"the system to refer to it.  For example, if you were to define " +
				"a  program for division, one of the inputs would have the unique " +
				"identifier 'divisor' and the other input would have an identifier " +
				"like 'dividend'.  Note that both inputs would have the same type (eg number) </li>" 
		+"<li>No. The system needs to see unique identifiers for each input and output  .</li>"
		+"<li>Please only use alphanumeric characters and underscores. Also " +
				"don't start identifier names with a numeral </li>"
		+"</ol>";
	Ext.create('Ext.window.Window', {
		title: 'Help',
		layout: 'border',
        constrain: true,
        maximizable: true,
        autoScroll: true,
        width: 400,
        height: 350,
		html: html,
		padding: 5
	}).show();
};

SoftwareViewer.prototype.openNewIconTab = function(tabname, iconCls) {
    var tab = new Ext.Panel({
        layout: 'fit',
        closable: true,
        iconCls: iconCls,
        title: tabname,
        items: []
        });
    this.tabPanel.add(tab);
    return tab;
};

SoftwareViewer.prototype.initSoftwareTreePanelEvents = function() {
    var This = this;
    This.treePanel.on("itemclick", function(view, rec, item, ind, event) {
        if(!rec.data.leaf)
        	return false;
        var id = rec.data.id;
        var path = getTreePath(rec, 'text');
        var tabName = getLocalName(id);

        // Check if tab is already open
        var items = This.tabPanel.items.items;
        for (var i = 0; i < items.length; i++) {
            var tab = items[i];
            if (tab && tab.title.replace(/^\**/, '') == tabName) {
                This.tabPanel.setActiveTab(tab);
                return null;
            }
        }

        // Fetch Store via Ajax
        var url = This.op_url + '/getSoftwareJSON?softwareid=' + escape(id);

        var tab = This.openNewIconTab(tabName, 'compIcon');
        Ext.apply(tab, {
            path: path,
            guifn: This.openSoftwareEditor,
            args: [tab, id, {}]
            });
        This.tabPanel.setActiveTab(tab);
        
        Ext.apply(tab, {
            loader: {
                loadMask: true,
                url: url,
                renderer: function(loader, response, req) {
                    var store = Ext.decode(response.responseText);
                    if (store) {
                        tab.removeAll();
                        tab.args[2] = store;
                        tab.guifn.call(This, tab.args);
                        // tab.doLayout(false,true);
                    }
                }
            }
        });
        tab.getLoader().load();
    });

    This.tabPanel.on('tabchange', function(tp, tab) {
        if (tab.path)
            This.treePanel.selectPath(tab.path, 'text');
        else
            This.treePanel.getSelectionModel().deselectAll();
    });

    return This.treePanel;
};

SoftwareViewer.prototype.initialize = function() {
    // Add the template tabPanel in the center
    var This = this;
    this.tabPanel = new Ext.TabPanel({
        region: 'center',
        margins: '5 5 5 0',
        enableTabScroll: true,
        activeTab: 0,
        resizeTabs: true,
        plain: true,
        // resizeTabs: true,
        // minTabWidth: 50,
        // tabWidth: 135,
        items: [{
            layout: 'fit',
            title: 'Describe Software',
            autoLoad: {
                url: this.op_url + '/intro'
            }
        }]
        });

    this.treePanel = this.getSoftwareListTree();
    
    var This = this;
    var delbtn = new Ext.Button({
        text: 'Delete',
        iconCls: 'delIcon',
        handler: function() {
            var nodes = This.treePanel.getSelectionModel().getSelection();
            if (!nodes || !nodes.length)
                return;
            var node = nodes[0];
            Ext.MessageBox.confirm("Confirm Delete", "Are you sure you want to Delete " + getLocalName(node.id), function(yesno) {
                if (yesno == "yes") {
                    var url = This.op_url + '/delSoftware';
                    Ext.get(This.treePanel.getId()).mask("Deleting..");
                    Ext.Ajax.request({
                        url: url,
                        params: {
                            softwareid: node.data.id
                        },
                        success: function(response) {
                            Ext.get(This.treePanel.getId()).unmask();
                            if (response.responseText == "OK") {
                                node.parentNode.removeChild(node);
                                This.tabPanel.remove(This.tabPanel.getActiveTab());
                            } else {
                                _console(response.responseText);
                            }
                        },
                        failure: function(response) {
                            Ext.get(This.treePanel.getId()).unmask();
                            _console(response.responseText);
                        }
                    });
                }
            });
        }
    });

    var tbar = null;
    if (this.advanced_user) {
        tbar = [{
        	text: 'Add',
    	    iconCls: 'addIcon',
    	    menu:[
    	    {
    	    	itemId: 'createbutton',
    	    	text: 'Add Software',
    	    	iconCls: 'addIcon',
    	    	handler: function() {
                    This.addSoftware();
                }
    	    },
    	    {
    	    	itemId: 'importbutton',
    	    	text: 'Import CSDMS Software',
    	    	iconCls: 'importIcon',
    	    	handler: function() {
                    This.importSoftware('CSDMS');
                }
    	    }]
        }];
        tbar.push(delbtn);
    }

    var leftPanel = new Ext.TabPanel({
        region: 'west',
        width: 250,
        split: true,
        plain: true,
        margins: '5 0 5 5',
        activeTab: 0,
        tbar: tbar
    });

    Ext.apply(this.treePanel, {
        title: 'Software'
    });

    //this.store.types.sort();
    this.initSoftwareTreePanelEvents();

    leftPanel.add(this.treePanel);
    leftPanel.setActiveTab(0);

    this.mainPanel = new Ext.Viewport({
        layout: {
            type: 'border'
        },
        items: [leftPanel, this.tabPanel]
    });
    this.mainPanel.add(getPortalHeader(CONTEXT_ROOT));
    return this.mainPanel;
};
