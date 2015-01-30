function SoftwareViewer(guid, store, op_url, upload_url, ontns, liburl, editable) {
    this.guid = guid;
    this.store = store;
    this.op_url = op_url;
    this.upload_url = upload_url;
    this.liburl = liburl;
    this.libname = liburl.replace(/.+\//, '').replace(/\.owl$/, '');
    this.editable = editable;
    
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
	        fields: ['text', 'software', 'mine']
        });

    var treeStore = Ext.create('Ext.data.TreeStore', {
        model: 'compTreeRecord',
        root: root,
        sorters: ['text']
    });
    
    var tbar = [];
    if (this.editable) {
    	var renitem = This.getRenameMenuItem();
        var delitem = This.getDeleteMenuItem();
        renitem.iconCls = "icon-edit fa fa-browngrey";
        delitem.iconCls = "icon-del fa fa-red";
        // Edit toolbar
        tbar.push({
        	xtype: 'toolbar',
        	dock: 'top',
        	items: [{
        		text: 'Add',
        		iconCls: 'icon-add fa fa-green',
        		menu:[This.getAddSoftwareMenuItem(), This.getAddSoftwareTypeMenuItem(),
    	          '-', This.getImportMenuItem()]
        	},
        	renitem, delitem,
        	{
        		xtype: 'button',
        		itemId: 'taskbtn',
        		enableToggle: true,
        		iconCls: 'icon-users fa fa-green',
        		text: 'All',
                handler: function () {
                	var tree = this.up('treepanel');
                	if(this.pressed) {
             			tree.filter(true, 'mine');
                		this.setText('My edits');
                		this.setIconCls('icon-user fa fa-green');
                	} else {
                		tree.clearFilter();
                		tree.filter(tree.down('trigger').value);
                		this.setText('All');
                		this.setIconCls('icon-users fa fa-green');
                	}
                }
        	}]
        });
    }
    // Filter toolbar
    tbar.push({
    	xtype: 'toolbar',
    	dock: 'bottom',
    	items: {
    		fieldLabel: 'Filter',
    		emptyText: 'Enter text to filter by..',
         	margin: 2,
         	labelWidth: 30,
         	xtype: 'trigger',
         	width: '100%',
         	triggerCls: 'x-form-clear-trigger',
         	onTriggerClick: function () {
         		this.reset();
         		this.focus();
         	},
         	listeners: {
         		change: function (field, newVal) {
         			var tree = field.up('treepanel');
             		var taskbtn = tree.down('#taskbtn');
             		if(!newVal) {
                 		tree.clearFilter();
             			if(taskbtn.pressed)
             				tree.filter(true, 'mine');
             		}
             		else {
             			tree.filter(newVal);
             		}
         		},
         		buffer: 250
         	}
    	}
    });
    
    var treePanel = new Ext.tree.TreePanel({
        width: '100%',
        border: false,
        autoScroll: true,
        hideHeaders: true,
        rootVisible: false,
        useArrows: true,
        iconCls: iconCls,
        //bodyCls: 'x-docked-noborder-top',
        title: title,
        store: treeStore,
        url: This.op_url,
        viewConfig: {
            plugins: {
                ptype: 'treeviewdragdrop',
                enableDrag: true,
                appendOnly: true
            },
            stripeRows: true
        },
        plugins: {
            ptype: 'treefilter',
            allowParentFolders: true
        }, 
        dockedItems: tbar,
        listeners: {
            itemcontextmenu: {
                fn: This.onSoftwareItemContextMenu,
                scope: this
            },
        	beforeitemmove: function(node, oldParent, newParent, index, eOpts ) {
        		var url = This.op_url + '/moveSoftware';
        		if(!node.data.leaf) url += "Type";
        		if(This.ignoremove == node) { 
        			This.ignoremove = null;
        			return true;
        		}
                Ext.Ajax.request({
                    url: url,
                    params: {
                        id: node.data.id,
                        parentid: newParent.data.id
                    },
                    success: function(response) {
                    	var cTree = node.getOwnerTree();
                        Ext.get(cTree.getId()).unmask();
                        if (response.responseText == "OK") {
                            This.ignoremove = node;
                        	var pNode = cTree.getStore().getNodeById(newParent.data.id);
                        	pNode.appendChild(node);
                            cTree.getStore().sort('text', 'ASC');
                        } else {
                            _console(response.responseText);
                        }
                    },
                    failure: function(response) {
                        Ext.get(cTree.getId()).unmask();
                        _console(response.responseText);
                    }
                });
                return false;
        	}
        }
    });
    return treePanel;
};

SoftwareViewer.prototype.getSoftwareTree = function(list) {	
	var root = null;
	var nodes = {};
	var parent = {};

	var queue = [this.store.software_types];
	while(queue.length) {
		var type = queue.pop();
		var typeid = type.id;
	    type.readonly = (getNamespace(type.id) == this.ns['']);
		nodes[typeid] = {
			id: typeid,
			text: getLocalName(typeid),
			leaf: false,
			readonly: type.readonly,
			iconCls: 'icon-folder fa fa-' + (
					type.readonly ? 'grey' : 'yellow'),
			expIconCls: 'icon-folder-open fa fa-' + (
					type.readonly ? 'grey' : 'yellow'),					
			expanded: true,
			children: []
		};
		if(!root)
			root = nodes[typeid];
		
		for(var i=0; i<type.subtypes.length; i++) {
			var stype = type.subtypes[i];
			parent[stype.id] = nodes[typeid];
			queue.push(stype);
		}
		if(parent[typeid])
			parent[typeid].children.push(nodes[typeid]);
	}
	
    for (var i = 0; i < list.length; i++) {
    	var software = list[i];
    	var node = nodes[software.classId];
    	if(!node)
    		continue;
	    var mine = Ext.Array.contains(this.store.my_softwares, software.id);
    	node.children.push({
    		id: software.id,
    		text: getLocalName(software.id),
			mine: mine,
    		leaf: true,
    		iconCls: 'icon-component fa-tree fa-orange'
    	});
    }
    return root;
};

SoftwareViewer.prototype.getSoftwareListTree = function(enableDrag) {
    var tmp = this.getSoftwareTree(this.store.softwares);
    return this.getSoftwareTreePanel(tmp, 'Softwares', 
    		'icon-component fa-title fa-browngrey', enableDrag);
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
                    		iconCls: 'icon-component fa-tree fa-orange'
                    	});
                    	pNode.expand();
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

SoftwareViewer.prototype.addSoftwareType = function() {
    var This = this;
    var cTree = this.treePanel;
    
    var nodes = cTree.getSelectionModel().getSelection();
    if (!nodes || !nodes.length || nodes[0].data.leaf) {
    	Ext.MessageBox.show({
            icon: Ext.MessageBox.ERROR,
            buttons: Ext.MessageBox.OK,
            msg: "Select an existing Software Type to add this under"
        });
        return;
    }
    var pNode = nodes[0];
    
    Ext.Msg.prompt("Add Software Type", "Enter name for the new Software Type:", function(btn, text) {
        if (btn == 'ok' && text) {
            text = getRDFID(text);
            var typeid = This.ns['lib'] + text;
            var enode = cTree.getStore().getNodeById(typeid);
            if (enode) {
                showError(text + ' already exists');
                return;
            }
            var url = This.op_url + '/addSoftwareType';
            Ext.get(cTree.getId()).mask("Creating..");
            Ext.Ajax.request({
                url: url,
                params: {
                    typeid: typeid,
                    parentid: pNode.data.id
                },
                success: function(response) {
                    Ext.get(cTree.getId()).unmask();
                    if (response.responseText == "OK") {
                    	pNode.appendChild({
                    		id: typeid,
                    		text: getLocalName(typeid),
                    		leaf: false,
                    		iconCls: 'icon-folder fa fa-yellow',
                    		expIconCls: 'icon-folder-open fa fa-yellow',
                    		children: []
                    	});
                        cTree.getStore().sort('text', 'ASC');
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

SoftwareViewer.prototype.confirmAndDelete = function(node) {
	var This = this;
    var opurl = This.op_url + '/delSoftware';
    var params = {};
    if (!node.data.leaf) {
        opurl += "Type";
        params['typeid'] = node.data.id;
    }
    else {
    	params['softwareid'] = node.data.id;
    }
    Ext.MessageBox.confirm("Confirm Delete", "Are you sure you want to Delete " + getLocalName(node.id), function(yesno) {
        if (yesno == "yes") {
            Ext.get(This.treePanel.getId()).mask("Deleting..");
            Ext.Ajax.request({
                url: opurl,
                params: params,
                success: function(response) {
                    Ext.get(This.treePanel.getId()).unmask();
                    if (response.responseText == "OK") {
                        node.parentNode.removeChild(node);
                        //FIXME: Remove the tab with the same title
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
};

SoftwareViewer.prototype.confirmAndRename = function(node) {
	var This = this;
    var opurl = This.op_url + '/renameSoftware';
    var params = {};
    if (!node.data.leaf) {
        opurl += "Type";
        params['typeid'] = node.data.id;
    }
    else {
    	params['softwareid'] = node.data.id;
    }
    
    var name = getLocalName(node.data.id);
    var ns = getNamespace(node.data.id);
    
    Ext.Msg.prompt("Rename " + name, "Enter new name:", function(btn, text) {
        if (btn == 'ok' && text) {
            var newName = getRDFID(text);
            var newid = ns + newName;
            var enode = This.treePanel.getStore().getNodeById(newid);
            if (enode) {
                showError(getRDFID(text) + ' already exists ! Choose a different name.');
                This.confirmAndRename(node);
                return;
            }
            Ext.get(This.treePanel.getId()).mask("Renaming..");
            
            params['newid'] = newid;
            Ext.Ajax.request({
                url: opurl,
                params: params,
                success: function(response) {
                    Ext.get(This.treePanel.getId()).unmask();
                    if (response.responseText == "OK") {
                    	node.set('text', newName);
                    	node.set('id', newid);
                    	node.commit();
                    	This.treePanel.getStore().sort('text', 'ASC');
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
    }, this, false, name);	
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
                    		iconCls: 'icon-component fa-tree fa-orange'
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
        text: 'Save',
        iconCls: 'icon-save fa fa-browngrey',
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
    
	var checkbtn = new Ext.Button({
		text: 'Check code',
		iconCls: 'icon-run fa fa-brown',
		handler: function() {
			var form = tab.down('form');
			Ext.get(This.tabPanel.getId()).mask("Checking Code...");
			Ext.Ajax.request({
				url : This.op_url+'/checkCode',
				params: {
					softwareid: id
					//codelocation: 
				},
				success: function(response) {
					Ext.get(This.tabPanel.getId()).unmask();
					if(response.responseText) {
						var checkResults = Ext.decode(response.responseText);
						This.checkCodeResults(checkResults, form);
						// TODO: show response
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
        iconCls: 'icon-run fa fa-brown',
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

    var tbar = null;
    if(this.editable) {
    	tbar = [ inferbtn, checkbtn, savebtn ];
    	tbar.push({xtype: 'tbfill'});
    	tbar.push('-');
    	tbar.push({
    		iconCls : 'icon-reload fa fa-green',
    		text : 'Reload',
    		handler : function() {
    			tab.getLoader().load();
    			savebtn.setDisabled(true);
    			tab.setTitle(tab.title.replace(/^\*/, ''));
    		}
    	});
    }
	
    tab.softwareEditor = This.getSoftwareEditor(id, compStore, This.store.properties, tab, savebtn, 
    		This.editable);
    
    var mainPanelItems = [ tab.softwareEditor ];
    
    var mainPanel = new Ext.Panel({
        region: 'center',
        border: false,
        tbar: tbar,
        layout: 'fit',
        bodyStyle: This.editable ? '' : 'background-color:#ddd',
        items: mainPanelItems
    });
    tab.add(mainPanel);
};

SoftwareViewer.prototype.openSoftwareTypeEditor = function(args) {
    var tab = args[0];
    var id = args[1];
    var typeStore = args[2];
    var mainPanel;
    var This = this;
    
    var savebtn = new Ext.Button({
        text: 'Save',
        iconCls: 'icon-save fa fa-browngrey',
        disabled: true,
        handler: function() {
        	var form = tab.down('form');
        	var annotation = form.getForm().findField('annotation').getValue();
        	var stype = {id: id, annotation: annotation, subtypes: []};
        	Ext.get(This.tabPanel.getId()).mask("Saving..");
        	Ext.Ajax.request({
                url: This.op_url + '/saveSoftwareTypeJSON',
                params: {
                    typeid: id,
                    json: Ext.encode(stype)
                },
                success: function(response) {
                    Ext.get(This.tabPanel.getId()).unmask();
                    if (response.responseText == "OK") {
						// Reset dirty bit
						form.getForm().getFields().each(function(field) {
							field.resetOriginalValue();
						});
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
    
	tab.form = {
		xtype : 'form',
		layout: 'fit',
		frame : true,
		bodyStyle : 'padding:5px',
		margin : 5,
		autoScroll : true,
		items : [ {
			xtype : 'textarea',
			name : 'annotation',
			fieldLabel : 'Description',
			value : typeStore.annotation,
			flex: 1,
			anchor: '100%',
			listeners: {
				dirtychange: function(item, dirty, opts) {
					if(dirty) {
						savebtn.setDisabled(false);
						tab.setTitle("*" + tab.title.replace(/^\*/, ''));
					}
				}
			}
		} ]
	};

    var mainPanelItems = [ tab.form ];
    
    var mainPanel = new Ext.Panel({
        region: 'center',
        border: false,
        tbar: This.editable ? [ savebtn ] : null,
        layout: 'fit',
        //tbar: tbar,
        bodyStyle: This.editable ? '' : 'background-color:#ddd',
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
	if(inputGrid) {
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
	}
    
	var outputGrid = form.down('grid[type=output]');
	if(outputGrid) {
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
	}
    
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
	            rec.data.id = snGrid.repons + snlabel;
	        	software.standardnames.push(This.prepareRoleRecord(rec.data));
	        }
	    });
	}
		
	var fileGrids = form.query('grid[type=filegrid]');
	for(var j=0; j<fileGrids.length; j++) {
		var fileGrid = fileGrids[j];
	    fileGrid.getStore().each(function(rec) {
	    	var item = This.prepareRoleRecord(rec.data);
	    	item.type = fileGrid.range;
	    	var prov = item.provenance;
	    	var provarr = [];
	    	for(var ppropid in prov) {
				if(prov[ppropid])
					provarr.push({propertyId: ppropid, value: prov[ppropid]});
			}
	    	delete item.provenance;
	    	software.propertyValues.push({
	    		propertyId: fileGrid.propid, 
	    		value: item,
	    		provenance: provarr
	    	});
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
    	if(prop.parentIds.length == 0)
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
    for(var i=0; i<newsoftware.assumptions.length; i++) {
    	var newass = newsoftware.assumptions[i];
		if(!newass.id) continue;
    	var newone = true;
    	for(var j=0; j<software.assumptions.length; j++) {
    		var ass = software.assumptions[j];
    		if(newass.id == ass.id) {
    			newone = false;
    			break;
    		}
    	}
    	if(newone) {
    		changes.push({
    			propid : "_#Assumption",
    			oldval : '',
    			newval : newass.id,
    			newdata : newass
    		});
    	}
	}
    for(var i=0; i<newsoftware.standardnames.length; i++) {
    	var newsn = newsoftware.standardnames[i];
		if(!newsn.label) continue;
    	var newone = true;
    	for(var j=0; j<software.standardnames.length; j++) {
    		var sn = software.standardnames[j];
    		if(newsn.label == sn.label) {
    			newone = false;
    			break;
    		}
    	}
    	if(newone) {
    		changes.push({
    			propid : "_#StandardName",
    			oldval : '',
    			newval : newsn.id,
    			newdata : newsn
    		});
    	}
	}
    for(var i=0; i<newsoftware.inputs.length; i++) {
    	var newrole = newsoftware.inputs[i];
    	var newone = true;
    	for(var j=0; j<software.inputs.length; j++) {
    		var role = software.inputs[j];
    		if(newrole.id == role.id) {
    			newone = false;
    			break;
    		}
    	}
    	if(newone) {
    		changes.push({
    			propid : "_#Input",
    			oldval : '',
    			newval : newrole.id,
    			newdata : newrole
    		});
    	}
	}
    for(var i=0; i<newsoftware.outputs.length; i++) {
    	var newrole = newsoftware.outputs[i];
    	var newone = true;
    	for(var j=0; j<software.outputs.length; j++) {
    		var role = software.outputs[j];
    		if(newrole.id == role.id) {
    			newone = false;
    			break;
    		}
    	}
    	if(newone) {
    		changes.push({
    			propid : "_#Output",
    			oldval : '',
    			newval : newrole.id,
    			newdata : newrole
    		});
    	}
	}
	return changes;
};

SoftwareViewer.prototype.showSuggestions = function(software, newsoftware, form) {
	var changes = this.compareSoftwares(software, newsoftware);
	if(!changes.length) {
		showInfo("No Suggestions !");
		return;
	}
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
		    	renderer: function(v, b, rec) {
		    		if(v == "") return "-";
		    		if(rec.data.propid.substring(0,2) == "_#")
		    			return rec.get('newdata').label;
		    		return v;
		    	}
		    }
		],
		store: {
			fields: ['propid', 'oldval', 'newval', 'newdata'],
			data: changes
		},
        tbar: [{
            text: 'Accept Suggestions',
            iconCls: 'icon-select-alt fa fa-green',
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
                    if(propid == "_#Assumption") {
                    	var assGrids = form.query('grid[type=assumptions]');
                    	var ass = rec.get('newdata');
                    	for(var j=0; j<assGrids.length; j++) {
                    		var assGrid = assGrids[j];
                    		if(assGrid.repons == getNamespace(newval))
                    			assGrid.getStore().add(ass);
                    	}
                	}
                    else if(propid == "_#StandardName") {
                    	var snGrids = form.query('grid[type=standardnames]');
                    	var sn = rec.get('newdata');
                    	for(var j=0; j<snGrids.length; j++) {
                    		var snGrid = snGrids[j];
                    		if(snGrid.repons == getNamespace(newval))
                    			snGrid.getStore().add(sn);
                    	}
                	}
                    else if(propid == "_#Input") {
                    	var rolegrid = form.down('grid[type=input]');
                    	var role = rec.get('newdata');
                    	rolegrid.getStore().add(role);
                	}
                    else if(propid == "_#Output") {
                    	var rolegrid = form.down('grid[type=output]');
                    	var role = rec.get('newdata');
                    	rolegrid.getStore().add(role);
                	}
                    else {
	                    var field = form.getForm().findField(propid);
	                    field.setValue(newval);
	                    field.provenance = {};
	                    field.provenance[This.ns[''] + "isInferred"] = true;
	                    field.provenance[This.ns[''] + "timestamp"] = (new Date()).getTime();
	                    field.setFieldStyle(This.getProvenanceStyle(field.provenance, editable));
						var infolabel = field.nextSibling('label');
						infolabel.setText(This.getProvenanceHtml(field.provenance, editable)
								, false);
                    }
					
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
				showHelp("suggestions");
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

SoftwareViewer.prototype.checkCodeResults = function (results, form) { 
	var This = this;
	var vals=[];
	var res=results.split("|");
	for (i=0; i<res.length-1; i++) {
		var entries=res[i].split(",");
		vals.push({
			check : entries[0],
			linenum : entries[1],
			instance : entries[2],
			suggest : entries[3]
		});
	}
	Ext.create('Ext.window.Window', {
		title: 'Code Check',
		height: 400,
		width: 900,
		layout: 'fit',
		autoScroll: true,

		items: {
			xtype: 'grid',
			border: false,
			columns: [
			          { text: 'Warning', dataIndex: 'check', width: 100, flex: 2},
			          { text: 'Line Number', dataIndex: 'linenum', width: 100, flex: 1},
			          { text: 'Instance', dataIndex: 'instance', flex: 3},
			          { text: 'Suggestion', dataIndex: 'suggest', flex:4}
			          ],
			          store: {
			        	  fields: ['check', 'linenum', 'instance', 'suggest'],
			        	  data: vals
			          },
		}
	}).show();
};

SoftwareViewer.prototype.getSuperClasses = function(clsid, clsnode, supers) {
	supers.push(clsnode.id);
	if(clsnode.id == clsid) 
		return supers;
	for(var i=0; i<clsnode.subtypes.length; i++) {
		var tmp = supers.concat();
		var search = this.getSuperClasses(clsid, clsnode.subtypes[i], tmp);
		if(search != null)
			return search;
	}
	return null;
};

SoftwareViewer.prototype.getSoftwareComboBox = function(mstore) {
	var This = this;
    
	if(!this.version_store) {
		this.version_store = Ext.create('Ext.data.Store', {
			fields : [ 'id', 'classId', 
			{name:'groupDisplay',
				convert: function (val, model) {
					return getLocalName(model.get('classId'));
				}
			}, 
			{name:'softwareDisplay',
				convert: function (val, model) {
					return getLocalName(model.get('id'));
				}
			}],
			groupers : 'classId',
			sorters : ['groupDisplay', 'softwareDisplay'],
			data: This.store.softwares
		});
	}

	var tpl = new Ext.XTemplate(
			'<tpl for=".">',
			'<tpl if="this.classId != values.classId">',
			'<tpl exec="this.classId = values.classId"></tpl>',
			'<div class="x-panel-header-default x-panel-header-text-container ' 
			+ 'x-panel-header-text x-panel-header-text-default">'
			+ '{groupDisplay}</div>',
			'</tpl>',
			'<div class="x-boundlist-item">{softwareDisplay}</div>',
			'</tpl>'
	);

	return {
		xtype: 'combo',
		store: This.version_store,
		queryMode: 'local',
		multiSelect: true,
		displayField: 'softwareDisplay',
		groupField: 'classId',
		valueField: 'id',
		tpl: tpl
	};
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
        margins: '5 0 0 0',
		activetab : 0,
		items : []
	};
	editorPanel.items.push(editorTabPanel);
	
	var auditResults = store.auditResults;
	var propContainer = {};
	var sectionById = {};
	var propLabels = {};
	
	var propValues = {};
	var propProv = {};
	var prop = [];
	for(var i=0; i<store.propertyValues.length; i++) {
		var pv = store.propertyValues[i];
		var prop = pv.propertyId;
		
		// Store value provenance
		var provObject = {};
		var prov = pv.provenance;
		for(var j=0; j<prov.length; j++) {
			var ppv = prov[j];
			var pprop = ppv.propertyId;
			provObject[pprop] = ppv.value;
		}
		if(!propProv[prop])
			propProv[prop] = provObject;
		pv.value.provenance = provObject;
		
		// Store value
		var curval = propValues[prop];
		if(curval) {
			if(Array.isArray(curval)) 
				propValues[prop].push(pv.value);
			else
				propValues[prop] = [curval, pv.value];
		}
		else
			propValues[prop] = pv.value;
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
	
	// Get superclasses
	var clsids = this.getSuperClasses(store.classId, this.store.software_types, []);

	// Get tab groups for this class
	var tabGroupPropIds = [];
	for(var i=0; i<props.length; i++) {
		var prop = props[i];
		var pcom = This.propcomments[prop.id];
		var isTabGroupProp = false;
		// If this is a tab group property
		if(pcom && pcom.ui && pcom.ui.tabs) {
			// Check each item in pcom.ui.includes
			if(pcom.ui.includes) 
				for(var j=0; j<pcom.ui.includes.length; j++)
					if(clsids.indexOf(this.ns[''] + pcom.ui.includes[j]) >=0)
						isTabGroupProp = true;
			// Check each item in pcom.ui.excludes
			if(pcom.ui.excludes) 
				for(var j=0; j<pcom.ui.excludes.length; j++)
					if(clsids.indexOf(this.ns[''] + pcom.ui.excludes[j]) >=0)
						isTabGroupProp = false;
		}
		if(isTabGroupProp)
			tabGroupPropIds.push(prop.id);
	}
	
	// Create tabs
	for(var i=0; i<props.length; i++) {
		var prop = props[i];
		for(var j=0; j<prop.parentIds.length; j++) {
			var parentId = prop.parentIds[j];
			if(tabGroupPropIds.indexOf(parentId) >= 0) {
				propLabels[prop.id] = prop.label;
				var tab = propContainer[prop.label];
				if(!tab) {
					tab = {
						xtype: 'panel',
						title: prop.label,
				        frame:true,
				        layout: (prop.label == "Description" ||
				        		prop.label == "Usability"
				        			? 'fit': 'form'),
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
	}
	
    // Add IO, Assumptions, Standard Names editors
	var ioEditor = propContainer['I/O'];
	var asEditor = propContainer['Assumptions'];
	var snEditor = propContainer['Standard Names'];
	var dpEditor = propContainer['Dependencies'];
	if(ioEditor)
		this.getIOListEditor(id, store, this.store.data_types, 
				ioEditor, maintab, savebtn, editable);
	if(asEditor)
		this.getAssumptionsEditor(id, store, this.store.sninfo, 
				asEditor, maintab, savebtn, editable);
	if(snEditor)
		this.getStandardNamesEditor(id, store, this.store.sninfo, 
				snEditor, maintab, savebtn, editable);
	
	if(dpEditor) {
		Ext.apply(dpEditor, {
			iconCls: 'icon-dropbox fa-title fa-browngrey'
		});
	}
	
	// Create sections (if needed)
	for(var i=0; i<props.length; i++) {
		var prop = props[i];
		for(var j=0; j<prop.parentIds.length; j++) {
			var parentId = prop.parentIds[j];
			var catlabel = propLabels[parentId];
			if(!catlabel) continue;
			var tab = propContainer[catlabel];
			if(!tab) continue;
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
		for(var j=0; j<prop.parentIds.length; j++) {
			var parentId = prop.parentIds[j];
			var catlabel = propLabels[parentId];
			if(!catlabel) continue;
			var pcui = This.propcomments[prop.id].ui;
			var comp = null;
			if(pcui && pcui.section)
				comp = propContainer[catlabel + ":" + pcui.section];
			if(!comp)
				comp = propContainer[catlabel];
			if(!comp) continue;
			var provenance = propProv[prop.id];
			
			if(pcui && pcui.isfile) {
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
				// Create a grid
				var filegrid = {
					xtype: 'grid',
					type: 'filegrid',
					propid: prop.id,
					title: prop.label,
					range: prop.range,
			        columns: [{
			            dataIndex: 'hasFileLocation',
			            header: 'File Location',
			            menuDisabled: true,
			            flex: 1,
			            editor: editable,
						// TODO: Editor should be the uploader ?
			            /*editor: new Ext.form.field.Trigger({
		                    onTriggerClick: function() {
								// Open up upload window
		                    }
			            })*/
			        }],
			        plugins: [editorPlugin],
			        selModel: Ext.create('Ext.selection.CheckboxModel'),
		            selType: 'cellmodel',
		            bodyCls: "multi-line-grid",
		            columnLines: true,
		            autoHeight: true
				};
				
				var fields = ['hasFileLocation'];
				
				// Columns of the grid (all with simple text editors for now): 
				// - file location (populated with propValues[id].hasFileLocation)
				// - for each prop in extra
				//   - extraprop.label (populated with propValues[id].[extraProp]
				if(pcui.extra) {
					for(var j=0; j<pcui.extra.length; j++) {
						var extraprop = This.properties[this.ns[''] + pcui.extra[j]];
						var extrapcom = This.propcomments[extraprop.id];
						filegrid.columns.push({
							dataIndex: getLocalName(extraprop.id),
							header: extraprop.label ? 
									extraprop.label : getLocalName(extraprop.id),
							menuDisabled: true,
							//flex: 1,
							editor: true
						});
						fields.push(getLocalName(extraprop.id));
					}
				}
				fields.push('provenance');
				filegrid.columns.push({
		        	dataIndex: 'provenance',
		        	header: 'Provenance',
		        	flex: 1,
		        	editable: editable,
		        	renderer: function (v) {
		        		return This.getProvenanceCreationHtml(v, editable);
		        	}
		        });
				
				var proprole = 'FileRole'+getLocalName(prop.id);
			    if (!Ext.ModelManager.isRegistered(proprole))
			        Ext.define(proprole, {
				        extend: 'Ext.data.Model',
				        fields: fields
			        });

			    filegrid.store = {
			    	xtype: 'store',
			        model: proprole,
			        data: propValues[prop.id],
			        listeners: {
			        	update: function() {
							maintab.setTitle("*" + maintab.title.replace(/^\*/, ''));
			        		savebtn.setDisabled(false);
			        	}, 
			        	remove: function(store,records) {
							maintab.setTitle("*" + maintab.title.replace(/^\*/, ''));
			        		savebtn.setDisabled(false);
			        	}
			        }
			    };
			    filegrid.pcui = pcui;
			    filegrid.proprole = proprole;
			    
				// Create a grid toolbar and have an "Add"/"Delete" button
			    if(editable) {
					filegrid.tbar = [{
		                text: 'Add',
		                iconCls: 'icon-add fa fa-green',
		                handler: function() {
		                	var fgrid = this.up('grid');
		                    var gridStore = fgrid.getStore();
		                    var pos = gridStore.getCount();
		                    if(pos == 1 && !fgrid.pcui.multiple)
		                    	return showError('Can only add 1 file here');
	
		                    var sm = fgrid.getSelectionModel();
		                    var role = eval("new "+fgrid.proprole+"();");
		                    editorPlugin.cancelEdit();
		                    gridStore.insert(pos, role);
		                    editorPlugin.startEditByPosition({
		                    	row:pos,
		                    	column:1
		                    });
		                }
		            }, {
		                iconCls: 'icon-del fa fa-red',
		                text: 'Delete',
		                roletype: i,
		                handler: function() {
		                	var fgrid = this.up('grid');
		                    var gridStore = fgrid.getStore();
		                    editorPlugin.cancelEdit();
		                    var s = fgrid.getSelectionModel().getSelection();
		                    for (var i = 0, r; r = s[i]; i++) {
		                        gridStore.remove(r);
		                    }
		                }
		            }];
			    }
				if(prop.label == "Code" && editable) {
					filegrid.tbar.push({xtype: 'tbfill'}, '-');
					if(auditResults) {
						var lines = auditResults.split("\n");
						var resStore = {};
						var headers = [];
						for(var x=0; x<lines.length; x++) {
							var cols = lines[x].split(",");
							for(var y=0; y<cols.length; y++) {
								if(x==0)
									headers[y] = cols[y];
								else 
									resStore[headers[y]] = cols[y];
							}
						}
						
						filegrid.tbar.push({
							iconCls: 'icon-runAlt fa fa-browngrey',
							text: 'Get Audit Results',
							handler: function() {
			                	Ext.create('Ext.window.Window', {
			                		title: 'DRAT results',
			                		layout : 'border',
		                            constrain : true,
		                            maximizable : true,
		                            frame : false,
		                            border : false,
		                            autoScroll : true,
		                            width : 250,
		                            height : 250,
		                            items: {
		                                xtype: 'propertygrid',
		                                region: 'center',
		                                source: resStore,
		                                listeners: {
		                                    'beforeedit': {
		                                        fn: function () {
		                                            return false;
		                                        }
		                                    }
		                                }
			                		}
			                	}).show();
							}
						})
					}
					else {
						filegrid.tbar.push({
							iconCls: 'icon-runAlt fa fa-browngrey',
							text: 'Run Audit Tool (DRAT)',
							handler: function() {
								Ext.get(maintab.getId()).mask("Submitting Audit job..");
								Ext.Ajax.request({
									url: This.op_url+'/runAuditTool',
									params: {
										softwareid: id
									},
									success: function(response) {
										Ext.get(maintab.getId()).unmask();
										if (response.responseText == "OK") {
											showInfo('Audit job submitted.');
										} else {
											showError('Cannot run DRAT right now.' 
													+ 'Try later');
											_console(response.responseText);
										}
									},
									failure: function(response) {
										Ext.get(maintab.getId()).unmask();
										_console(response.responseText);
									}
								});
							}
						});
					}
				}
				
				comp.items.push(filegrid);
			}
			else {
				var item = {
					name: prop.id,
					fieldLabel: prop.label ? prop.label : getLocalName(prop.id),
					value: propValues[prop.id],
					flex: 1,
					anchor: '100%',
					disabled: !editable,
					provenance: provenance,
					fieldStyle: this.getProvenanceStyle(provenance, editable),
					listeners: {
						change: function (item, newv, oldv, opts) {
							item.provenance = {};
							item.provenance[This.ns[''] + "editedBy"] = USER_ID;
							item.provenance[This.ns[''] + "timestamp"] = (new Date()).getTime();
							item.setFieldStyle(This.getProvenanceStyle(item.provenance, editable));
							
							var infolabel = item.nextSibling('label');
							infolabel.setText(
									This.getProvenanceHtml(item.provenance, editable), 
									false);
						}
					}
				};
				var align = '';
				if(prop.isObjectProperty) {
					if(prop.id == This.ns[''] + 'requiresSoftware') {
						Ext.apply(item, This.getSoftwareComboBox());
					}
					else {
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
				}
				else if(prop.range == this.ns['xsd'] + "int") {
					item.xtype = 'numberfield';
					item.allowDecimals = false;
				}
				else if(prop.range == this.ns['xsd'] + "integer") {
					item.xtype = 'numberfield';
					item.allowDecimals = false;
				}
				else if(prop.range == this.ns['xsd'] + "float") {
					item.xtype = 'numberfield';
					item.allowDecimals = true;
				}
				else if(prop.range == this.ns['xsd'] + "boolean") {
					item.xtype = 'checkbox';
					item.checked = Boolean(item.value);
				}
				else if(prop.range == this.ns['xsd'] + "date")
					item.xtype = 'datefield';
				else if(prop.label && prop.label.match(/descri|comment/i)) {
					item.xtype = 'textareafield';
					align = 'stretch';
					item.flex = 1;
					item.rows = 8;
				}
				else 
					item.xtype = 'textfield';
				
				var info = this.getProvenanceHtml(provenance, editable);
				var infoitem = {
					xtype : 'label',
					cls : 'info',
					html : info,
					width: 150,
					style: 'padding-left:10px; font-size: 10px; font-style:italic'
				};
				
				comp.items.push({
					xtype: 'panel',
					//layout: 'hbox',
					layout: {
						type: 'hbox',
						align: align
					},
					padding: 3,
					border: false,
					bodyStyle: 'background:transparent',
					items: [item, infoitem]
				});
			}
		}
	}
	return editorPanel;
};

SoftwareViewer.prototype.getProvenanceHtml = function(provenance, editable) {
	var info = "";
	var x = !editable ? 'opacity:0.5': '';
	if(provenance) {
		var xns = this.ns[''];
		var eby = provenance[xns+'editedBy'];
		var inf = provenance[xns+'isInferred'];
		var imp = provenance[xns+'importedFrom'];
		var ts = provenance[xns+'timestamp'];
		if(eby)
			info += "<b style='color:blue;"+x+"'>Edited by "+eby+"</b>";
		else if(inf)
			info += "<b style='color:brown;"+x+"'>Turbosoft Suggestion"+"</b>";
		else if(imp)
			info += "<b style='color:green;"+x+"'>Imported from "+imp+"</b>";
		if(ts)
			info += "<div style='color:grey;"+x+"'>(" + Ext.Date.format(new Date(ts), 
					'F j Y, g:ia') + ")</div>";
	}
	else {
		info = "<span style='color:grey;"+x+"'>No value</span>";
	}
	return info;
};

SoftwareViewer.prototype.getProvenanceCreationHtml = function(provenance, editable) {
	var info = "";
	var x = !editable ? 'opacity:0.5': '';
	if(provenance) {
		var xns = this.ns[''];
		var eby = provenance[xns+'editedBy'];
		var inf = provenance[xns+'isInferred'];
		var ts = provenance[xns+'timestamp'];
		if(eby)
			info += "By <b>"+eby+"</b>";
		else if(inf)
			info += "<b style='color:brown"+x+"'>Turbosoft Suggestion"+"</b>";
		if(ts)
			info += " on " + Ext.Date.format(new Date(ts), 'F j Y, g:ia');
	}
	if(!info)
		info = "<i style='color:#999"+x+"'>From original ontology</i>";
	return info;
};

SoftwareViewer.prototype.getProvenanceStyle = function(provenance, editable) {
	var style = "color:grey";
	if(provenance) {
		var xns = this.ns[''];
		if(provenance[xns+'editedBy'])
			style = "color:blue";
		if(provenance[xns+'isInferred'])
			style = "color:brown";
		if(provenance[xns+'importedFrom'])
			style = "color:green";
	}
	if(!editable)
		style += ";opacity:0.5";
	return style;
};

SoftwareViewer.prototype.getIOListEditor = function(c, iostore, data_types, 
		mainPanel, tab, savebtn, editable) {
    var This = this;

    Ext.apply(mainPanel, {
    	xtype: 'tabpanel',
        region: 'center',
        layout: 'border',
        iconCls: 'icon-param fa-title fa-browngrey',
        bodyStyle:'padding:0px',
        layout: 'auto',
        border: false,
        padding:0,
        defaults: {
            border: false,
            padding: 0
        },
        tabBar : {
			items : [ {
				xtype : 'tbfill'
			}, {
				xtype : 'tool',
				type : 'help',
				tooltip : 'Get Help',
				handler : function(btn, e) {
					showHelp('IO');
				}
			} ]
		}
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
        		return This.getProvenanceCreationHtml(v, editable);
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
                iconCls: 'icon-add fa fa-green',
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
                iconCls: 'icon-del fa fa-red',
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
            title: (i == 0 ? 'Inputs': 'Outputs'),
            iconCls: (i == 0 ? 'icon-input fa-title fa-blue': 
            	'icon-output fa-title fa-brown'),
            type: !i ? 'input' : 'output',
            columns: columns,
            selModel: sm,
            selType: 'cellmodel',
            plugins: plugins,
            bodyCls: bodycls,
            store: gridStore,
            tbar: tbar
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

    mainPanel.items.push(iDataGrid);
    mainPanel.items.push(oDataGrid);

    return mainPanel;
};


SoftwareViewer.prototype.getAssumptionsEditor = function(c, store, sninfo, 
		mainPanel, tab, savebtn, editable) {
    var This = this;

    Ext.apply(mainPanel, {
    	xtype: 'tabpanel',
        region: 'center',
        layout: 'border',
        bodyStyle:'padding:0px',
        iconCls: 'icon-tasks fa-title fa-browngrey',
        border: false,
        layout: 'auto',
        padding: 0,
        tabBar : {
			items : [ {
				xtype : 'tbfill'
			}, {
				xtype : 'tool',
				type : 'help',
				tooltip : 'Get Help',
				handler : function(btn, e) {
					showHelp('assumptions');
				}
			} ]
		}
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
    		layout: 'fit',
            border: false,
            defaults: {
                border: false,
                padding: 0
            },
            //autoScroll: true
    	});
    	
        // Register store models
        if (!Ext.ModelManager.isRegistered('Assumption'))
            Ext.define('Assumption', {
	            extend: 'Ext.data.Model',
	            fields: ['id', 'label', 'categoryId', 'provenance', 'note']
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
	        	dataIndex: 'note',
	        	header: 'Note',
	        	flex: 1,
	        	editor: true
	        },
	        {
	        	dataIndex: 'provenance',
	        	header: 'Creation Provenance',
	        	flex: 1,
	        	editable: false,
	        	renderer: function (v) {
	        		return This.getProvenanceCreationHtml(v, editable);
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
	    	        		asid = e.grid.repons + aslabel;
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
        bodycls += " multi-line-grid";
        
        var tbar = null;
        if (editable) {
            tbar = [{
                text: 'Add Assumption',
                iconCls: 'icon-add fa fa-green',
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
                iconCls: 'icon-del fa fa-red',
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
            // title: 'Assumptions',
            columns: columns,
            selModel: sm,
            selType: 'cellmodel',
            plugins: plugins,
            bodyCls: bodycls,
            store: gridStore,
            tbar: tbar
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
        mainPanel.items.push(repotab);
    }
    mainPanel.activeTab = activeTabId;

    return mainPanel;
};

SoftwareViewer.prototype.setDefaultProvenance = function(rec) {
	var prov = {};
	prov[this.ns[''] + "editedBy"] = USER_ID;
	prov[this.ns[''] + "timestamp"] = (new Date()).getTime();
	rec.set('provenance', prov);	
};

SoftwareViewer.prototype.importStandardNamesFromCSV = function(csv, grid) {
	var This = this;
	var store = grid.getStore();
	var lines = csv.split("\n");
	var not_imported = [];
	var ns = grid.repons;
	for(var i=0; i<lines.length; i++) {
		if(!lines[i]) continue;
		var vals = lines[i].split(",");
		var objlabel = vals[0].trim();
		var qtylabel = vals[1].trim();
		
		var oplabels = [];
		var opstr = vals[2].trim();
		if(opstr != '-' && opstr != '_') {
			var opstrs = opstr.split(';');
			for(var j=0; j<opstrs.length; j++) {
				oplabels.push(opstrs[j].trim().replace(/_of$/, ''));
			}	
		}

		var internalvar = vals[3].trim();
		var units = null;
		var io = null;
		if(vals.length > 4)
			units = vals[4].trim();
		if(vals.length > 5)
			io = vals[5].trim();
		
		var label = objlabel+"__";
		for(var j=0; j<oplabels.length; j++) {
			if(oplabels[j])
				label += oplabels[j] + "_of_";
		}
		label += qtylabel;
		
		var sname = This.idmap[ns + label];
		if(sname) {
			sname.internalVariable = internalvar;
			sname.note = '';
			if(units && io)
				sname.note = units+", "+io;
			if(!store.getById(sname.id))
				store.add(sname);
		}
		else {
			not_imported[label] = "name "+label;
			if(!This.idmap[ns + "object_" + objlabel]) 
				not_imported[label] = "object " + objlabel;
			else if(!This.idmap[ns + "quantity_" + qtylabel])
				not_imported[label] = "quantity " +qtylabel;
			else {
				for(var j=0; j<oplabels.length; j++)
					if(!This.idmap[ns + "operator_" + oplabels[j]])
						not_imported[label] = "operator " + oplabels[j];
			}
		}
	}
	var errorstr = "";
	for(var label in not_imported)
		errorstr += "<li>" + label + " ("+not_imported[label]+" not found)</li>";
	if(errorstr) 
		errorstr = "Could not import the following standard names:<br>" + errorstr;
	return errorstr;
};

SoftwareViewer.prototype.getStandardNamesEditor = function(c, store, sninfo, 
		mainPanel, tab, savebtn, editable) {
    var This = this;

    Ext.apply(mainPanel, {
    	xtype: 'tabpanel',
        region: 'center',
        layout: 'border',
        bodyStyle:'padding:0px',
        iconCls: 'icon-table fa-title fa-browngrey',
        border: false,
        //layout: 'auto',
        padding: 0,
        tabBar : {
			items : [ {
				xtype : 'tbfill'
			}, {
				xtype : 'tool',
				type : 'help',
				tooltip : 'Get Help',
				handler : function(btn, e) {
					showHelp('standard_names');
				}
			} ]
		}
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
    		layout: 'fit',
            border: false,
            defaults: {
                border: false,
                padding: 0
            },
            //autoScroll: true
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
	            fields: ['id', 'label', 'objectId', 'quantityId', 'operatorIds', 
	                     'provenance', 'internalVariable', 'note']
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
	            //flex: 1,
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
	        	dataIndex: 'internalVariable',
	        	header: 'Internal Variable',
	        	//flex: 1,
	        	editor: true
	        },
	        {
	        	dataIndex: 'note',
	        	header: 'Note',
	        	//flex: 1,
	        	editor: true
	        },
	        {
	        	dataIndex: 'provenance',
	        	header: 'Creation Provenance',
	        	flex: 1,
	        	editable: false,
	        	renderer: function (v) {
	        		return This.getProvenanceCreationHtml(v, editable);
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
	    	        		var objid = e.grid.repons + objlabel;
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
	    	        		var qid = e.grid.repons + qlabel;
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
		    	        		var opid = e.grid.repons + oplabel;
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
        bodycls += " multi-line-grid";

        var tbar = null;
        if (editable) {
            tbar = [{
                text: 'Add Standard Name',
                iconCls: 'icon-add fa fa-green',
                handler: function() {
                	var panel = this.up('grid');
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
                iconCls: 'icon-del fa fa-red',
                text: 'Delete',
                roletype: i,
                handler: function() {
                	var panel = this.up('grid');
                    var gstore = panel.getStore();
                    panel.editorPlugin.cancelEdit();
                    var s = panel.getSelectionModel().getSelection();
                    for (var i = 0, r; r = s[i]; i++) {
                    	gstore.remove(r);
                    }
                }
            }, '-', {
            	xtype: 'tbfill'
            }, {
                iconCls: 'icon-download-cloud fa fa-browngrey',
                text: 'Import from CSV',
                roletype: i,
                handler: function() {
                	var grid = this.up('grid');
                	Ext.create('Ext.window.Window', {
                		title: 'Paste CSV data',
                		layout: 'border',
                        constrain: true,
                        maximizable: true,
                        autoScroll: true,
                        width: '80%',
                        height: '80%',
                        defaults: {
                        	margin: 5
                        },
                		items: [{
            				xtype : 'button',
            				region : 'south',
            				text : 'Submit',
            				handler : function() {
            					var csv = this.up('window').down('textarea').getValue();
            					var errorstr = This.importStandardNamesFromCSV(csv, grid);
            					if(errorstr) {
            						showError(errorstr);
            					}
            					this.up('window').close();
            				}
            			}, {
                			xtype : 'form',
                			region : 'center',
                			layout : 'fit',
                			frame : true,
                			autoScroll : true,
                			items : {
                				xtype : 'textarea',
                				flex: 1,
                				anchor: '100%'
                			}
                		}]
                	}).show();
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
            // title: 'Standard Names',
            columns: columns,
            selModel: sm,
            selType: 'cellmodel',
            plugins: plugins,
            bodyCls: bodycls,
            store: gridStore,
            tbar: tbar
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
        mainPanel.items.push(repotab);
    }
    mainPanel.activeTab = activeTabId;

    return mainPanel;
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
        /*if(!rec.data.leaf)
        	return false;*/
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
        var guifn = This.openSoftwareEditor;
        var icon = 'icon-component fa-title fa-orange';
        
        if(!rec.data.leaf) {
        	url = This.op_url + '/getSoftwareTypeJSON?typeid=' + escape(id);
        	guifn = This.openSoftwareTypeEditor;
        	icon = 'icon-folder-open fa-title fa-yellow';
        }

        var tab = This.openNewIconTab(tabName, icon);
        Ext.apply(tab, {
            path: path,
            guifn: guifn,
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
        if (tab.path) {
            This.treePanel.selectPath(tab.path, 'text');
            window.top.location.hash = "#" + tab.title;
        }
        else {
            This.treePanel.getSelectionModel().deselectAll();
            window.top.location.hash = "";
        }
    });

    return This.treePanel;
};

SoftwareViewer.prototype.onSoftwareItemContextMenu = function(
		sview, node, item, index, e, eOpts) {
    var This = this;
    e.stopEvent();
    if (!this.menu) {
        this.menu = Ext.create('Ext.menu.Menu', {
            items: [This.getAddSoftwareMenuItem(), This.getAddSoftwareTypeMenuItem(), '-', 
                    This.getImportMenuItem(), '-',
                    This.getRenameMenuItem(), This.getDeleteMenuItem()]
            });
        this.swmenu = Ext.create('Ext.menu.Menu', {
            items: [This.getRenameMenuItem(), This.getDeleteMenuItem()]
            });
        this.sysmenu = Ext.create('Ext.menu.Menu', {
            items: [This.getAddSoftwareMenuItem(), This.getAddSoftwareTypeMenuItem(), '-', 
                    This.getImportMenuItem()]
            });
    }
    if (node.raw.readonly)
        this.sysmenu.showAt(e.getXY());
    else if(node.data.leaf)
    	this.swmenu.showAt(e.getXY());
    else
        this.menu.showAt(e.getXY());
};


SoftwareViewer.prototype.getAddSoftwareMenuItem = function() {
	var This = this;
	return {
		itemId: 'createbutton',
		text: 'Add Software',
		iconCls: 'icon-component fa-menu fa-orange',
		handler: function() {
	        This.addSoftware();
	    }
	};
};

SoftwareViewer.prototype.getAddSoftwareTypeMenuItem = function() {
	var This = this;
	return {
		itemId: 'createtypebutton',
		text: 'Add Software Type',
		iconCls: 'icon-folder-open fa-menu fa-yellow',
		handler: function() {
	        This.addSoftwareType();
	    }
	};
};

SoftwareViewer.prototype.getImportMenuItem = function() {
	var This = this;
	return {
		itemId: 'importbutton',
		text: 'Import CSDMS Software',
		iconCls: 'icon-download-cloud fa-menu fa-browngrey',
		handler: function() {
	        This.importSoftware('CSDMS');
	    }
	};
};

SoftwareViewer.prototype.getDeleteMenuItem = function() {
    var This = this;
    return {
        text: 'Delete',
        iconCls: 'icon-del fa-menu fa-red',
        handler: function() {
            var nodes = This.treePanel.getSelectionModel().getSelection();
            if (!nodes || !nodes.length || !nodes[0].parentNode)
                return;
            var node = nodes[0];
            if(node.raw.readonly) {
            	showError('Cannot delete a system software type');
            	return;
            }
            This.confirmAndDelete(node);
        }
    };
};

SoftwareViewer.prototype.getRenameMenuItem = function() {
    var This = this;
    return {
        text: 'Rename',
        iconCls: 'icon-edit fa-menu fa-browngrey',
        //FIXME: Get an "Edit" icon for this
        handler: function() {
            var nodes = This.treePanel.getSelectionModel().getSelection();
            if (!nodes || !nodes.length || !nodes[0].parentNode)
                return;
            var node = nodes[0];
            if(node.raw.readonly) {
            	showError('Cannot rename a system software type');
            	return;
            }
            This.confirmAndRename(node);
        }
    };
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
    Ext.apply(this.treePanel, {
        title: 'Browse'
    });
    this.initSoftwareTreePanelEvents();
    
    var This = this;
    var leftPanel = new Ext.TabPanel({
        region: 'west',
        width: 300,
        split: true,
        plain: true,
        margins: '5 0 5 5',
        activeTab: 0,
        items: [
        This.treePanel, 
        {
            xtype : 'component',
            title: 'Search',
            iconCls: 'icon-zoomOut fa-title fa-browngrey',
            autoEl : {
                tag : 'iframe',
                src : this.op_url + '/../jsp/search.jsp'
            },
            border: false
        }]
    });

    //this.store.types.sort();

    this.mainPanel = new Ext.Viewport({
        layout: {
            type: 'border'
        },
        items: [leftPanel, this.tabPanel],
        listeners: {
        	afterrender: function() {
        		if(window.top.location.hash) {
        			var comp = window.top.location.hash.replace("#", "");
        			var softwareid = This.ns['lib'] + comp;
        			var tnode = This.treePanel.getStore().getNodeById(softwareid);
        			if(tnode) {
        				This.treePanel.fireEvent("itemclick", This.treePanel, tnode);
        			}
        		}
        	}
        }
    });
    this.mainPanel.add(getPortalHeader(CONTEXT_ROOT));
    return this.mainPanel;
};
