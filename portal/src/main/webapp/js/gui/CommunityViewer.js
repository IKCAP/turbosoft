function CommunityViewer(guid, store, op_url, ontns, is_admin) {
	this.guid = guid;
	this.store = store;
	this.op_url = op_url;
	this.ontns = ontns;
	this.is_admin = is_admin;

	this.leftPanel = null;
}

CommunityViewer.prototype.createStoreModels = function() {
	if (!Ext.ModelManager.isRegistered('UserRecord'))
		Ext.define('UserRecord', {
			extend : 'Ext.data.Model',
			fields : [ 'text' ]
		});
	if (!Ext.ModelManager.isRegistered('UserContribution'))
		Ext.define('UserContribution', {
			extend : 'Ext.data.Model',
			fields : [ 'softwareId', 'propertyId', 'value', 'timestamp' ]
		});
};

CommunityViewer.prototype.getProfileHTML = function(userStore) {
    var default_pic = "http://ieee-sb.ca/sites/default/files/default_user.jpg";
    var html = "<div class='profile'>";
    html += "<div class='profile_image'><img src='"+
    	(userStore.picture ? userStore.picture : default_pic ) + "' " +
    	"onerror='this.style.display = \"none\"'/></div>";
    html += "<h1>"+(userStore.username ? userStore.username : getLocalName(id))+"</h1>";
    html += "<div class='profile_detail'>Site: <a href='"+userStore.site+"'>"+userStore.site+"</a></div>";
    html += "<div class='profile_detail'>Affiliation: "+userStore.affiliation+"</div>";
    html += "<div class='profile_detail'>Expertise: "+userStore.expertise+"</div>";
    html += "<div style='clear:both'></div>";
    html += "</div>";
    return html;
};

CommunityViewer.prototype.openUserEditor = function(args) {
    var tab = args[0];
    var id = args[1];
    var allStore = args[2];
    var mainPanel;
    var This = this;
    
    var userStore = allStore.user;
    
    var editable = (USER_ID==getLocalName(id));
    
    var editbtn = new Ext.Button({
    	text: 'Edit',
    	iconCls: 'docsIcon',
    	handler: function(btn) {
    		btn.up('panel').up('panel').getLayout().setActiveItem(1);
    	}
    });
    var viewerpanel = {
    	xtype: 'panel',
		frame : true,
		margin : 5,
		layout: 'border',
		items: [{
			xtype: 'panel',
			region: 'north',
			html: This.getProfileHTML(userStore),
			margin: '5 0 5 0'
		}, {
			xtype: 'grid',
			region: 'center',
			title: 'Current Contributions',
			store: {
				data: allStore.user_contributions,
				model: 'UserContribution',
				sorters: [{
					property: 'timestamp',
					direction: 'DESC'
				}]
			},
			columns: [{
				dataIndex: 'softwareId',
	            header: 'Software',
	            flex: 1,
	            renderer: function(item) {
	                return getLocalName(item);
	            },
	            menuDisabled: true
			},{
				dataIndex: 'propertyId',
	            header: 'Property',
	            flex: 1,
	            renderer: function(item) {
	                return getLocalName(item);
	            },
	            menuDisabled: true
			},{
				dataIndex: 'timestamp',
	            header: 'Timestamp',
	            flex: 1,
	            renderer: function(item) {
	            	return Ext.Date.format(new Date(item),
	    					'F j Y, g:ia');
	            },
	            menuDisabled: true
			}]
		}
		],
    	tbar: editable ? [ editbtn ] : null
    };
    
    var savebtn = new Ext.Button({
        text: 'Save',
        iconCls: 'saveIcon',
        disabled: true,
        handler: function(btn) {
        	var form = tab.down('form');
        	var fields = form.getForm().getFields();
        	var user = {id: id};
        	fields.each(function(field) {
        		user[field.getName()] = field.getValue();
        	});
        	Ext.get(This.tabPanel.getId()).mask("Saving..");
        	Ext.Ajax.request({
                url: This.op_url + '/saveUserJSON',
                params: {
                    userid: id,
                    json: Ext.encode(user)
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
                        // Update HTML and return
                        var hpanel = btn.up('panel').up('panel').items.items[0];
                        btn.up('panel').up('panel').getLayout().setActiveItem(0);
                        hpanel.items.items[0].body.update(This.getProfileHTML(user));
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
    
    var backbtn = new Ext.Button({
    	text: 'Back',
    	//iconCls: 'docsIcon',
    	handler: function(btn) {
    		btn.up('panel').up('panel').getLayout().setActiveItem(0);
    	}
    });
    
	var form = {
		xtype : 'form',
		frame : true,
		bodyStyle : 'padding:5px',
		margin : 5,
		autoScroll : true,
		defaults: {
			xtype: 'textfield',
			flex: 1,
			anchor: '100%'
		},
		items : [ {
			name : 'username',
			fieldLabel : 'Name',
			value: userStore.username
		},{
			name : 'picture',
			fieldLabel : 'Picture URL',
			value: userStore.picture
		},{
			name : 'site',
			fieldLabel : 'Website',
			value: userStore.site
		},{
			name : 'expertise',
			fieldLabel : 'Expertise (keywords)',
			value: userStore.expertise
		},{
			name : 'affiliation',
			fieldLabel : 'Affiliation',
			value: userStore.affiliation
		}
		],
		listeners: {
			dirtychange: function(item, dirty, opts) {
				if(dirty) {
					savebtn.setDisabled(false);
					tab.setTitle("*" + tab.title.replace(/^\*/, ''));
				}
			}
		},
		tbar: editable ? [ backbtn, savebtn ] : null
	};

    var mainPanel = {
    	xtype: 'panel',
        region: 'center',
        layout: 'card',
        border: false,
        bodyStyle: editable ? '' : 'background-color:#ddd',
        items: [ viewerpanel, form ]
    };
    tab.add(mainPanel);
};


CommunityViewer.prototype.createLeftPanel = function() {
	var This = this;

	this.leftPanel = {
		xtype : 'treepanel',
		width : '20%',
		region : 'west',
		split : true,
		margins : '5 0 5 5',
		cmargins : '5 5 5 5',
        hideHeaders: true,
        rootVisible: false,
        bodyCls: 'x-docked-noborder-top',
		border : true,
		autoScroll : true,
		title : 'Users',
		//iconCls: 'userIcon',
		url : This.op_url,
		store: Ext.create('Ext.data.TreeStore', {
	        model: 'UserRecord',
	        root: This.getTree(This.store.users),
	        sorters: ['text']
	    }),
		listeners : {
			itemclick : function(view, rec, item, ind, event) {
				var id = rec.data.id;
		        var path = getTreePath(rec, 'text');
		        var tabName = getLocalName(id);

		        var tabPanel = this.up('viewport').down('tabpanel');
		        
		        // Check if tab is already open
		        var items = tabPanel.items.items;
		        for (var i = 0; i < items.length; i++) {
		            var tab = items[i];
		            if (tab && tab.title.replace(/^\**/, '') == tabName) {
		                This.tabPanel.setActiveTab(tab);
		                return null;
		            }
		        }

		        // Fetch Store via Ajax
		        var url = This.op_url + '/getUserJSON?userid=' + escape(id);
		        var guifn = This.openUserEditor;
		        var icon = 'userIcon';

		        var tab = This.openNewIconTab(tabName, icon);
		        Ext.apply(tab, {
		            path: path,
		            guifn: guifn,
		            args: [tab, id, {}]
		        });
		        tabPanel.setActiveTab(tab);
		        
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
		                    }
		                }
		            }
		        });
		        tab.getLoader().load();
		    },
		    
		    tabchange: function(tp, tab) {
		        if (tab.path)
		            this.up('treepanel').selectPath(tab.path, 'text');
		        else
		            this.up('treepanel').getSelectionModel().deselectAll();
		    }
		}
	};
};

CommunityViewer.prototype.openNewIconTab = function(tabname, iconCls) {
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

CommunityViewer.prototype.getTree = function(users) {
	var root = {
	        text: "Users",
	        id: "_users",
	        expanded: true,
	        children: []
	    };
	for(var i=0; i<users.length; i++) {
		var userid = users[i];
	    root.children.push({
	            text: getLocalName(userid),
	            id: userid,
	            iconCls: 'userIcon',
	            leaf: true
	        });
	}
	return root;
};

CommunityViewer.prototype.createTabPanel = function() {
	var This = this;
	this.tabPanel = new Ext.TabPanel({
		region : 'center',
		margins : '5 5 5 0',
		enableTabScroll : true,
		activeTab : 0,
		plain : true,
		resizeTabs : true,
		// minTabWidth: 135,
		// tabWidth: 135,
		items : [ {
			layout : 'fit',
			title : 'User Community',
			autoLoad : {
				url : this.op_url + '/intro'
			}
		} ]
	});
	this.tabPanel.on('tabchange', function(tp, tab) {
		var treePanel = this.up('viewport').down('treepanel');
		if (tab.path)
			treePanel.selectPath(tab.path, 'text');
		else
			treePanel.getSelectionModel().deselectAll();
	});
};

CommunityViewer.prototype.createMainPanel = function() {
	this.mainPanel = Ext.create('Ext.Viewport',
			{
				layout : {
					type : 'border'
				},
				items : [ this.leftPanel, this.tabPanel,
						getPortalHeader(CONTEXT_ROOT) ]
			});
};

CommunityViewer.prototype.initialize = function() {
	this.createStoreModels();
	this.createLeftPanel();
	this.createTabPanel();
	this.createMainPanel();
};