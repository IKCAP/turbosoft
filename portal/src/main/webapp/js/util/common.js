function _console(msg) {
	if (window.console) {
		window.console.log(msg);
		window.console.trace();
	}	
}

function getRDFID(id) {
	id = id.replace(/[^a-zA-Z0-9_\-\.]/g, '_');
	if (id.match(/^([0-9]|\.|\-)/))
		id = '_' + id;
	return id;
}

function getLocalName(url) {
	if (!url)
		return url;	
	if (url.indexOf('urn:') == 0)
		return url.replace(/^.*:/, '');
	return url.replace(/^.*#/, '');
}

function getNamespace(url) {
	if (!url)
		return url;
	if (url.indexOf('urn:') == 0)
		return url.replace(/:.*$/, ':');
	return url.replace(/#.*$/, '#');
}

function getPrefixedUrl(url, nsmap, default_ns) {
	if (!url)
		return url;
	// If what's passed in isn't a string (i.e. no replace function), just
	// return it as it is
	if (typeof url.replace != "function")
		return url;

	var nurl = url;
	for ( var pfx in nsmap) {
		var nsurl = nsmap[pfx];
		nurl = nurl.replace(nsurl, pfx + ":");
	}
	if (default_ns) {
		nurl = nurl.replace(default_ns, "");
	} else if (nurl == url) {
		nurl = getLocalName(nurl);
	}
	return nurl;
}

function xsdDateTime(date) {
	function pad(n) {
		var s = n.toString();
		return s.length < 2 ? '0' + s : s;
	}
	var yyyy = date.getFullYear();
	var mm1 = pad(date.getMonth() + 1);
	var dd = pad(date.getDate());
	var hh = pad(date.getHours());
	var mm2 = pad(date.getMinutes());
	var ss = pad(date.getSeconds());

	return yyyy + '-' + mm1 + '-' + dd + 'T' + hh + ':' + mm2 + ':' + ss;
}

function camelCase(str) {
	str = str.replace(/[\-_]/g, ' ');
	str = str.replace(/(\w)(\w*)/g,
	        function(g0,g1,g2){return g1.toUpperCase() + g2.toLowerCase();});
	return str.replace(/\s/g,'');
}

function getTreePath(node, field, separator) {
	field = field || node.idProperty;
	separator = separator || '/';

	var path = [ node.get(field) ];

	var parent = node.parentNode;
	while (parent) {
		path.unshift(parent.get(field));
		parent = parent.parentNode;
	}
	return separator + path.join(separator);
}

function showError(msg) {
	Ext.Msg.show({
		icon : Ext.MessageBox.ERROR,
		buttons : Ext.MessageBox.OK,
		msg : msg
	});
}

function showInfo(msg) {
	Ext.Msg.show({
		icon : Ext.MessageBox.INFO,
		buttons : Ext.MessageBox.OK,
		msg : msg
	});
}

function showWarning(msg) {
	Ext.Msg.show({
		icon : Ext.MessageBox.WARNING,
		buttons : Ext.MessageBox.OK,
		msg : msg
	});
}

function getPortalHeader(path) {
	return new Ext.Container(
			{
				id : "app-north",
				region : 'north',
				height : 60,
				layout : {
					type : "vbox",
					align : "stretch"
				},
				items : [
						{
							id : "app-header",
							bodyStyle : "background-color: transparent",
							border : false,
							region : 'north',
							height : 36,
							layout : 'fit',
							items : [ {
								border : false,
								xtype : "component",
								id : "app-header-title",
								html : "<a href=\""+path+"\">Turbosoft Portal</a>",
							} ]
						},
						{
							id : "app-menu",
							border : false,
							xtype : "component",
							height : 24,
							html : "<div class=\"menu\">"
									+ "<ul>"
									+ "<li class=\"first active\"><a href=\""+path+"\">Home</a></li>"
									+ "<li><a href=\""+path+"/software\">Software</a></li>\n"
									+ "<li><a href=\""+path+"/data\">File Types</a></li>\n"
									+ "<li><a href=\""+path+"/community\">Community</a></li>\n"
									+ (USER_ID != 'null' ? "<li style='float:right'><a href=\""
									+ path
									+ "/jsp/logout.jsp\">Logout <span class='user'>"+USER_ID+"</span></a></li>" : '')
									+ "</ul>"
									+ "</div>\n"
						}]
			});
}

function getPortalFooter(path) {
	/*var html = '<div class="home_footer">'
		+ '<div style="float:left;padding-right:5px">'
		+ '<img src="' + path + '/images/nsf-logo.gif" ' 
		+ 'title="NSF" style="float:left" />'
		+ '<img src="' + path + '/images/earthcube-logo.png" '
		+ 'title="Earthcube" style="float:left" />'
		+ '</div>'	
		+ '<div style="height:36px;padding-top:8px">'
		+ 'The GeoSoft project is funded by the National Science Foundation under ' 
		+ 'the EarthCube Initiative through grants ICER-1343800 and ICER-1440323'
		+ '</div>'
		+ '</div>';
	return {
		xtype : 'component',
		id : "app-south",
		region : 'south',
		height : 36,
		html : html,
		margins : '0 5 2 5'
	};*/
}

function setURLComboListOptions(copts, data, selection, emptyText, editable,
		multi) {
	copts.emptyText = emptyText;
	var xdata = [];
	if (data) {
		for ( var x = 0; x < data.length; x++) {
			xdata[x] = {
				"uri" : data[x],
				"name" : getLocalName(data[x])
			};
		}
	}
	copts.store = {
		xtype : 'store',
		fields : [ 'uri', 'name' ],
		data : xdata,
		sorters : [ 'name' ]
	};

	if (selection)
		copts.value = selection;
	if (!editable)
		copts.forceSelection = true;
	copts.sorters = [ 'name' ];
	copts.editable = editable;
	copts.queryMode = 'local';
	copts.displayField = 'name';
	copts.valueField = 'uri';
	copts.triggerAction = 'all';
	copts.multiSelect = multi;
	return copts;
}

function showHelp(id) {
	var url = CONTEXT_ROOT + "/docs/" + id + ".html";
	Ext.create('Ext.window.Window', {
		title: 'Help',
		layout: 'border',
		constrain: true,
		maximizable: true,
		autoScroll: true,
		width: 600,
		height: 350,
		padding: 5,
		autoLoad: {
			url: url,
			text: 'Loading..',
			timeout: 10000,
			//scripts: false 
		}
	}).show();
};

Ext.onReady(function () {
	/* TreeFilter 
	 * @author: Seth Lemmons
	 * @site: http://sethlemmons.com/blog
	 */
    Ext.define('TreeFilter', {
        extend: 'Ext.AbstractPlugin', 
        alias: 'plugin.treefilter',
        collapseOnClear: false,
        allowParentFolders: false,        
        	init: function (tree) {
        		var me = this;
        		me.tree = tree;
        		tree.filter = Ext.Function.bind(me.filter, me);
        		tree.clearFilter = Ext.Function.bind(me.clearFilter, me);
        	}, 

        	filter: function (value, property, re) {
                var me = this, 
                    tree = me.tree,
                    matches = [],
                    root = tree.getRootNode(),
                    property = property || 'text',
                    re = re || new RegExp(value, "ig"),
                    visibleNodes = [],
                    viewNode;

                if (Ext.isEmpty(value)) {
                    me.clearFilter();
                    return;
                }

                tree.expandAll();

                root.cascadeBy(function (node) {
                    if (node.get(property).match(re)) {
                        matches.push(node);
                    }
                });

                if (me.allowParentFolders === false) {
                    Ext.each(matches, function (match) {
                        if (!match.isLeaf()) {
                            Ext.Array.remove(matches, match);
                        }
                    });
                }

                Ext.each(matches, function (item, i, arr) {
                    root.cascadeBy(function (node) {
                        if (node.contains(item) == true) {
                            visibleNodes.push(node);
                        }
                    });
                    if (me.allowParentFolders === true && !item.isLeaf()) {
                        item.cascadeBy(function (node) {
                            visibleNodes.push(node);
                        });
                    }
                    visibleNodes.push(item);
                });

                root.cascadeBy(function (node) {
                    viewNode = Ext.fly(tree.getView().getNode(node));
                    if (viewNode) {
                        viewNode.setVisibilityMode(Ext.Element.DISPLAY);
                        viewNode.setVisible(Ext.Array.contains(visibleNodes, node))
                    }
                });
            },

            clearFilter: function () {
                var me = this,
                	tree = this.tree,
                	root = tree.getRootNode();

                if (me.collapseOnClear) {
                    tree.collapseAll();
                }
                root.cascadeBy(function (node) {
                    viewNode = Ext.fly(tree.getView().getNode(node));
                    if (viewNode) {
                        viewNode.show();
                    }
                });
            }
    });

	/*
	 * Uncached Cell Editing Allows full flexibility of editors based on record data
	 * *WARNING* -- will create a new cell editor for each row !! *WARNING* -- don't
	 * use for grids with a large number of cells and records
	 */
	Ext.define('Ext.grid.plugin.FlexibleCellEditing', {
		extend : Ext.grid.plugin.CellEditing,
		getEditor : function(record, column) {
			var me = this;
			// var editors = me.editors;
			var editorId = column.getItemId();
			// editorId += "-"+getLocalName(record.get('subject'));
			// editorId += "-"+getLocalName(record.get('predicate'));
			/*
			 * for(var key in record.getData()) { editorId += record.get(key)+"|"; }
			 */
			// var editor = editors.getByKey(editorId);
			var editorOwner = me.grid.ownerLockable || me.grid;
			// editor = null;
			// if(!editor) {
			var coleditor = column.getEditor(record);
			if (!coleditor)
				return false;
			editor = new Ext.grid.CellEditor({
				floating : true,
				editorId : editorId,
				field : coleditor
			});
			editorOwner.add(editor);
			editor.on({
				scope : me,
				specialkey : me.onSpecialKey,
				complete : me.onEditComplete,
				canceledit : me.cancelEdit
			});
			column.on('removed', me.cancelActiveEdit, me);
			// editors.add(editor);
			// }
			editor.grid = me.grid;
			editor.editingPlugin = me;
			return editor;
		}
	});
});