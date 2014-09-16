<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    
	<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
	<link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/themes/smoothness/jquery-ui.css" />
	<script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/jquery-ui.min.js"></script>

	<style>
		.ui-tabs-anchor, 
		input.ui-button
		{
			font-size: 11px !important;
			padding: .25em 0.5em;
		}
		.ui-tabs .ui-tabs-panel {
			padding: 0.5em 0.25em;
		}
		form {
			margin-bottom: 0.5em;
		}
		.ui-accordion .ui-accordion-header {
			padding-top: .1em;
			padding-bottom: .1em;
		}
	</style>
	<script>
	$(function() {

		// General UI Setup
		$( "#search" ).tabs({
			activate: wiperesults
		});
		$( "input[type=submit], input[type=reset]" ).button();
		$( "#results" ).accordion({
			collapsible: true,
			heightStyle: "content",
			active: false,
			disabled: true
		});
		function wiperesults () {
			$( ".result_item" ).remove();
			$( "#results" ).accordion("option", "active", false);
			$( "#resultshead" ).text("Results");
		}


		// Basic Search Query Builder
		$( "#basictext" ).change(function() {
			var terms = $( "#basictext" ).val().match(/("[^"]*?")|(\b\S+?\b)/g);
			if (null == terms) {
				$( "#basicquery" ).val("");
				return;
			}
			var q = "";
			for (i = 0; i < terms.length; ++i) {
				if ("\"\"" != terms[i]) q += " AND catchall:" + terms[i];
			}
			q = q.substring(5);
			$( "#basicquery" ).val(q);
			wiperesults();
		});
		$( "#basicform" ).submit(function( event ) {
			event.preventDefault();
			var q = $( "#basicquery" ).val();
			if (q.length > 0) {
				$( "#basicq" ).val(q);
				// yay
				submit_query( $( this ).serialize() );
			}
			// nay
		});


		// Advanced Search UI Setup
		$( "#exp_add_input" ).click(function() {
			$( "<span class=\"advinput_line\"><input class=\"advinput emptyprompt\" type=\"text\" value=\"mimetype\" /> <a href=\"#\">&#x2715;</a></span>" ).insertBefore( "#exp_add_input" );
		});
		$( "#exp_add_output" ).click(function() {
			$( "<span class=\"advoutput_line\"><input class=\"advoutput emptyprompt\" type=\"text\" value=\"mimetype\" /> <a href=\"#\">&#x2715;</a></span>" ).insertBefore( "#exp_add_output" );
		});
		$( "#exp_add_standardname" ).click(function() {
			$( "<span class=\"advstandardname_line\"><input class=\"advstandardname_obj emptyprompt\" type=\"text\" value=\"object\" /> <input class=\"advstandardname_qty emptyprompt\" type=\"text\" value=\"quantity\" /> <a href=\"#\">&#x2715;</a></span>" ).insertBefore( "#exp_add_standardname" );
		});
		$(document).on("focus", ".advinput, .advoutput, .advstandardname_obj, .advstandardname_qty", function(event) {
			if ($(event.target).hasClass( "emptyprompt" )) {
				$(event.target).val("");
				$(event.target).removeClass( "emptyprompt" );
			}
		});
        $(document).on("blur", ".advinput, .advoutput", function(event) {
            if ($(event.target).val().trim().length == 0) {
                $(event.target).val("mimetype");
                $(event.target).addClass( "emptyprompt" );
            }
        });
        $(document).on("blur", ".advstandardname_obj", function(event) {
            if ($(event.target).val().trim().length == 0) {
                $(event.target).val("object");
                $(event.target).addClass( "emptyprompt" );
            }
        });
        $(document).on("blur", ".advstandardname_qty", function(event) {
            if ($(event.target).val().trim().length == 0) {
                $(event.target).val("quantity");
                $(event.target).addClass( "emptyprompt" );
            }
        });
		$(document).on("click", ".advinput_line a", function( event ) {
			event.target.parentNode.remove();
			update_advinput(null);
		});
		$(document).on("click", ".advoutput_line a", function( event ) {
			event.target.parentNode.remove();
			update_advoutput(null);
		});
		$(document).on("click", ".advstandardname_line a", function( event ) {
			event.target.parentNode.remove();
			update_advstandardname(null);
		});
		$( "#advreset" ).click(function() {
			$( ".advinput_line" ).each(function(index, element) {
				$(element).remove();
			});
			$( ".advoutput_line" ).each(function(index, element) {
				$(element).remove();
			});
			$( ".advstandardname_line" ).each(function(index, element) {
				$(element).remove();
			});
			update_advinput(null);
			update_advoutput(null);
			update_advstandardname(null);
		});


		// Advanced Search Query Builder
		function update_advq() {
			var fields = [
				$( "#advcomponenttype_q" ).val(),
				$( "#advdescription_q" ).val(),
				$( "#advinput_q" ).val(),
				$( "#advoutput_q" ).val(),
				$( "#advstandardname_q" ).val(),
				$( "#advdependencies_q" ).val(),
				$( "#advproglang_q" ).val(),
				$( "#advlicense_q" ).val()
			];
			var q = "";
			for (i = 0; i < fields.length; ++i) {
				if (fields[i].length > 0) q += " AND " + fields[i];
			}
			if (q.length > 0) q = q.substring(5);
			$( "#advquery" ).val(q);
			wiperesults();
		}
		$( "#advcomponenttype" ).change(function() {
			var q = $( "#advcomponenttype" ).val();
			if (q.length > 0) q = "componenttype:\"" + q + "\"";
			$( "#advcomponenttype_q" ).val(q);
			update_advq();
		});
		$( "#advdescription" ).change(function() {
			var terms = $( "#advdescription" ).val().match(/("[^"]*?")|(\b\S+?\b)/g);
			if (null == terms) {
				$( "#advdescription_q" ).val("");
				update_advq();
				return;
			}
			var q = "";
			for (i = 0; i < terms.length; ++i) {
				if ("\"\"" != terms[i]) q += " AND description:" + terms[i];
			}
			q = q.substring(5);
			$( "#advdescription_q" ).val(q); 
			update_advq();
		});
		function update_advinput(event) {
			var q = "";
			$( ".advinput" ).each(function(index, element) {
				var qq = $(element).val().trim();
				if ((qq.length > 0) && ("mimetype" != qq)) q += " AND io_input:" + qq;
			});
			if (q.length > 0) q = q.substring(5);
			$( "#advinput_q" ).val(q); 
			update_advq();
		}
		$(document).on("change", ".advinput", function(event) { update_advinput(event); });
		function update_advoutput(event) {
			var q = "";
			$( ".advoutput" ).each(function(index, element) {
				var qq = $(element).val().trim();
				if ((qq.length > 0) && ("mimetype" != qq)) q += " AND io_output:" + qq;
			});
			if (q.length > 0) q = q.substring(5);
			$( "#advoutput_q" ).val(q); 
			update_advq();
		}
		$(document).on("change", ".advoutput", function(event) { update_advoutput(event); });
		function update_advstandardname(event) {
			var q = "";
			$( ".advstandardname_line" ).each(function(index, element) {
				var qq_obj = $(element).children( ".advstandardname_obj" ).val().trim();
				var qq_qty = $(element).children( ".advstandardname_qty" ).val().trim();
				var b_obj = (qq_obj.length > 0) && ("object" != qq_obj);
				var b_qty = (qq_qty.length > 0) && ("quantity" != qq_qty);
				if ( b_obj && b_qty) q += " AND standardnames:\"" + qq_obj + " " + qq_qty + "\"";
				else if (b_obj) q += " AND standardnames:\"" + qq_obj + "\"";
				else if (b_qty) q += " AND standardnames:\"" + qq_qty + "\"";
			});
			if (q.length > 0) q = q.substring(5);
			$( "#advstandardname_q" ).val(q); 
			update_advq();
		}
		$(document).on("change", ".advstandardname_obj, .advstandardname_qty", function(event) { update_advstandardname(event); });
		$( "#advdependencies" ).change(function() {
			var terms = $( "#advdependencies" ).val().match(/("[^"]*?")|(\b\S+?\b)/g);
			if (null == terms) {
				$( "#advdependencies_q" ).val("");
				update_advq();
				return;
			}
			var q = "";
			for (i = 0; i < terms.length; ++i) {
				if ("\"\"" != terms[i]) q += " AND tech_dependencies:" + terms[i];
			}
			q = q.substring(5);
			$( "#advdependencies_q" ).val(q); 
			update_advq();
		});
		$( "#advproglang" ).change(function() {
			var q = $( "#advproglang" ).val();
			if (q.length > 0) q = "tech_pl:\"" + q + "\"";
			$( "#advproglang_q" ).val(q);
			update_advq();
		});
		$( "#advlicense" ).change(function() {
			var q = $( "#advlicense" ).val();
			if (q.length > 0) {
				if ("foss" == q) q = "(legal_license:\"sw:ALv2\" OR legal_license:\"sw:BSDv2\" OR legal_license:\"sw:BSDv3\" OR legal_license:\"sw:GPLv2\" OR legal_license:\"sw:GPLv3\" OR legal_license:\"sw:LGPL\" OR legal_license:\"sw:MIT\")";
				else q = "legal_license:\"" + q + "\"";
			}
			$( "#advlicense_q" ).val(q);
			update_advq();
		});
		$( "#advancedform" ).submit(function( event ) {
			event.preventDefault();
			var q = $( "#advquery" ).val();
			if (q.length > 0) {
				$( "#advq" ).val(q);
				// yay
				submit_query( $( this ).serialize() );
			}
			// nay
		});


		// Raw Query
		$( "#rawq" ).change( wiperesults );
		$( "#rawform" ).submit(function( event ) {
			event.preventDefault();
			var q = $( "#rawq" ).val();
			if (q.length > 0) {
				submit_query( $( this ).serialize() );
			}
			// nay
		});


		// AJAX
		var entityMap = {
			"&": "&amp;",
			"<": "&lt;",
			">": "&gt;",
			'"': '&quot;',
			"'": '&#39;',
			"/": '&#x2F;',
			"\n":'<br/>\n'
		};
		function escapeHtml(string) {
			return String(string).replace(/[&<>"'\/]|\n/g, function (s) {
				return entityMap[s];
			});
		}
		function openSoftwareTab(sname) {
			var viewer = parent.compViewer_1;
			var softwareid = viewer.ns['lib']+sname;
			var tnode = viewer.treePanel.getStore().getNodeById(softwareid);
			if(tnode) {
				viewer.treePanel.fireEvent("itemclick", viewer.treePanel, tnode);
			}
			else {
				alert('Cannot find software '+sname);
			}
		}
		function submit_query(queryparams) {
			$.ajax({
				"url": "http://seagull.isi.edu:8983/solr/turbosoft/select?"+queryparams,
				"success": function( result ) {
					var docs = result.response.docs;
					$( "#resultshead" ).text("Results (" + docs.length + ")");
					$( "#resultsquery" ).text(result.responseHeader.params.q);
					var doclisting = [];
					for (i=0; i < docs.length; i++) {
						var docelemstr = "<li class=\"result_item\">";
						docelemstr += "<h4><a href=\"" + docs[i].uri + "\" target=\"_blank\">" + docs[i].id + "</a></h4>";
						docelemstr += "<span class=\"result_uri\">" + docs[i].uri + "</span><br/>";
						docelemstr += "<span class=\"result_type\">(Type: " + docs[i].componenttype + ")</span><br/>";
						if ("description" in docs[i]) docelemstr += "<p><b>Description</b></br>" + escapeHtml(docs[i].description) + "</p>";
						if ("io_input" in docs[i]) {
							docelemstr += "<p><b>Inputs</b></br>";
							for (j=0; j < docs[i].io_input.length; j++) {
								docelemstr += docs[i].io_input[j] + "<br/>";
							}
							docelemstr += "</p>";
						}
						if ("io_output" in docs[i]) {
							docelemstr += "<p><b>Outputs</b></br>";
							for (j=0; j < docs[i].io_output.length; j++) {
								docelemstr += docs[i].io_output[j] + "<br/>";
							}
							docelemstr += "</p>";
						}
						if ("standardnames" in docs[i]) {
							docelemstr += "<p><b>Standard Names:</b></br>";
							for (j=0; j < docs[i].standardnames.length; j++) {
								docelemstr += docs[i].standardnames[j] + "<br/>";
							}
							docelemstr += "</p>";
						}
						if ("tech_dependencies" in docs[i]) docelemstr += "<p><b>Dependencies</b></br>" + escapeHtml(docs[i].tech_dependencies) + "</p>";
						if ("tech_pl" in docs[i]) docelemstr += "<b>Programming Language: </b>" + docs[i].tech_pl + "<br/>";
						if ("legal_license" in docs[i]) docelemstr += "<b>License: </b>" + docs[i].legal_license + "<br/>";
						docelemstr += "<a class=\"result_toplink\" href=\"#\">Back to top</a><hr/></li>";
						doclisting.push( docelemstr );
					}
					$( "#resultlist" ).append( doclisting );
					$( "#resultlist a" ).bind("click", function() {
						var surl = $(this).attr('href');
						openSoftwareTab(surl.replace(/.+#/,''));
						return false;
					});
					$( "#results" ).accordion("option", "active", 0);
				},
				"dataType": "jsonp",
				"jsonp": "json.wrf"
			});
		}

	});
	</script>


	<style>
		body {
			margin:0; padding:0; text-align: center; height:100%; background-color:#ccc;
			font-family: National, "Helvetica Neue", Helvetica, Arial, Helmet, Freesans, "DejaVu Sans", Calibri, sans-serif;
			}

		#pagecenter {margin:auto; padding:10px; text-align: left; min-height: 100%; background-color:#eee;}
		*html #pagecenter {height:100%}

		#top {margin:-10px -10px 10px -10px; padding:10px 20px 5px 20px; background:linear-gradient(rgb(108,17,28), rgb(51, 51, 51));}
		h1 {display:inline; margin-left:10px;color:rgb(204, 29, 51); font-size:150%; }

		.ui-widget {font-size:75%}

		input {display:inline-block; vertical-align:middle;}
		textarea[readonly] {background-color:#eee;color:#888;}
		.emptyprompt {color:#aaa;}
		.hidden {display:none; !important}

		.advfield {margin: 0 0 15px 0;}
		.expander {margin:0; padding:0;}
		.expander span {display:block;}
		.expander a {font-size:75%;}
		.expander .exp_add {display:block;}

		#results h3 {font-size:150%;font-weight:bold;}
		ul#resultlist {padding:0;}
		li.result_item {display:block; margin:0 0 25px 0;}
		li.result_item h4 {margin:0; font-size:133%;}
		li.result_item h4 a {color:#44C;}
		li.result_item .result_type {font-size:80%; color:#888;}
		li.result_item .result_uri {font-size:80%; color:#66C;}
		li.result_item .result_toplink {display:block;text-align:right;font-size:80%; color:#C88;}

		.debug {display:none; !important}
	</style>

</head>
<body><div id="pagecenter">

	<!-- div id="top"><img src="http://seagull.isi.edu:8080/turbosoft-portal/images/logo.png" /><h1>Turbosoft :: Component Search</h1></div--> 

	<div id="search">

		<ul>
		<li><a href="#basicsearch">Basic Search</a></li>
		<li><a href="#advancedsearch">Advanced Search</a></li>
		<li class="debug"><a href="#rawquery">Raw Query</a></li>
		</ul>

		<div id="basicsearch">
		<form id="basicform" action="http://seagull.isi.edu:8983/solr/turbosoft/select" method="get">
			<input id="basicq" name="q" type="text" class="hidden" value="" />
			<input id="basicwt" name="wt" type="text" class="hidden" value="json" />
			<input id="basicindent" name="indent" type="text" class="hidden" value="true" />

			<input id="basictext" type="text" style="width:100%;" />
			<input id="basicsubmit" type="submit" style="width:50%;" />

			<div class="debug">
				<hr/>
				Query Preview:<br/>
				<textarea readonly id="basicquery" style="width:100%; height:75px; resize:vertical;"></textarea>
			</div>
		</form>
		</div>

		<div id="advancedsearch">
		<form id="advancedform" action="http://seagull.isi.edu:8983/solr/turbosoft/select" method="get">
			<input id="advq" name="q" type="text" class="hidden" value="" />
			<input id="advwt" name="wt" type="text" class="hidden" value="json" />
			<input id="advindent" name="indent" type="text" class="hidden" value="true" />

			<div class="advfield">Component Type:<br/>
			<input id="advcomponenttype_q" type="text" class="hidden" value="" />
			<select id="advcomponenttype">
			<option value="" selected>Any</option>
			<option value="sw:ModelComponent">Model Component</option>
			<option value="sw:DataProcessingComponent">Data Processing Component</option>
			<option value="sw:VisualizationComponent">Visualization Component</option>
			</select>
			</div>

			<div class="advfield">Description:<br/>
			<input id="advdescription_q" type="text" class="hidden" value="" />
			<input id="advdescription" type="text" style="width:100%;" />
			</div>

			<div class="advfield">Inputs:<br/>
			<input id="advinput_q" type="text" class="hidden" value="" />
			<div id="exp_input" class="expander">
				<a href="#" class="exp_add" id="exp_add_input">Add...</a>
			</div>
			</div>

			<div class="advfield">Outputs:<br/>
			<input id="advoutput_q" type="text" class="hidden" value="" />
			<div id="exp_output" class="expander">
				<a href="#" class="exp_add" id="exp_add_output">Add...</a>
			</div>
			</div>

			<div class="advfield">Standard Names:<br/>
			<input id="advstandardname_q" type="text" class="hidden" value="" />
			<div id="exp_output" class="expander">
				<a href="#" class="exp_add" id="exp_add_standardname">Add...</a>
			</div>
			</div>

			<div class="advfield">Dependencies:<br/>
			<input id="advdependencies_q" type="text" class="hidden" value="" />
			<input id="advdependencies" type="text" style="width:100%;" />
			</div>

			<div class="advfield">Programming Language:<br/>
			<input id="advproglang_q" type="text" class="hidden" value="" />
			<select id="advproglang">
			<option value="" selected>Any</option>
			<option value="sw:C">C</option>
			<option value="sw:C++">C++</option>
			<option value="sw:Fortran77">Fortran77</option>
			<option value="sw:Fortran90">Fortran90</option>
			<option value="sw:IDL">IDL</option>
			<option value="sw:Java">Java</option>
			<option value="sw:Matlab">Matlab</option>
			<option value="sw:Python">Python</option>
			</select>
			</div>

			<div class="advfield">License:<br/>
			<input id="advlicense_q" type="text" class="hidden" value="" />
			<select id="advlicense">
			<option value="" selected>Any</option>
			<option value="foss">Open Source Only</option>
			<option value="sw:proprietary">Proprietary</option>
			<option value="sw:ALv2">ALv2</option>
			<option value="sw:BSDv2">BSDv2</option>
			<option value="sw:BSDv3">BSDv3</option>
			<option value="sw:GPLv2">GPLv2</option>
			<option value="sw:GPLv3">GPLv3</option>
			<option value="sw:LGPL">LGPL</option>
			<option value="sw:MIT">MIT</option>
			</select>
			</div>

			<input id="advsubmit" type="submit" /> <input id="advreset" type="reset" />

		<div class="debug">
			<hr/>
			Query Preview:<br/>
			<textarea readonly id="advquery" style="width:100%; height:75px; resize:vertical;"></textarea>
		</div>

		</form>
		</div>

		<div id="rawquery" class="debug">
		<form id="rawform" action="http://seagull.isi.edu:8983/solr/turbosoft/select" method="get">
			<textarea id="rawq" name="q" style="width:100%; height:75px; resize:vertical;"></textarea>
			<input id="rawwt" name="wt" type="text" class="hidden" value="json" />
			<input id="rawindent" name="indent" type="text" class="hidden" value="true" />
			<input id="rawsubmit" type="submit" style="width:19%" />
		</form>
		</div>

	</div>

	<p/>

	<div id="results">
		<h3><span id="resultshead">Results</span></h3>
		<div>
		<p><b>Query: </b><span id="resultsquery"></span></p>
		<hr/>
		<ul id="resultlist"></ul>
		</div>
	</div>


</div></body>