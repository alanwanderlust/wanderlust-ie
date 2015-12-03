var subjects = [];
var subjects_active = [];
var freebase_subjects = [];
var freebase_objects = [];
var active_subject = "";
var active_object = "";
var active_subject_extractor = "";
var active_object_extractor = "";

var objects = [];
var objects_active = [];

var output = [];

var show_all_subjects = false;
var show_all_objects  = false;
var show_all_patterns = false;
var show_all_output   = false;

var pattern_autocomplete = {};
var pattern_autocomplete_rev = {};

var queryingInfo = false;
var queryAgain = false;


$(function() {
	$.getJSON("pattern/alltypes").done(function(data) {
		if (data != null && data.length > 0) {
			typeList = data;
			$( "#subject-searcher" ).autocomplete({
		      source: function(request, response) {
		      	response(searchType(request.term));
		      },
		    	minLength: 3,
		    	select: function(event, ui) { $("#subject-searcher").val(ui.item.label); searchSubject(); setTimeout(function() { $("#subject-searcher").val(""); }, 10);}
			});

			$( "#object-searcher" ).autocomplete({
		      source: function(request, response) {
		      	response(searchType(request.term));
		      },
		    	minLength: 3,
		    	select: function(event, ui) { $("#object-searcher").val(ui.item.label); searchObject(); setTimeout(function() { $("#object-searcher").val(""); }, 10);}
			});
		}
	});
	$.getJSON("extractor/all/subjects/objects/entities").done(function(data) {
		if (data != null && data.length > 0) {
			nameList = data;
			$("#save-xname").autocomplete({
				source: function(request, response) {
					response(searchExName(request.term));
				}
			});

			$("#save-yname").autocomplete({
				source: function(request, response) {
					response(searchExName(request.term));
				}
			});

			$(".ui-autocomplete").css("z-index", "2147483647");
		}
	});
});

$(function() {

	// Initialize autocomplete
	$( "#pattern-searcher" ).autocomplete({
      	source: function(request, response) {
      		$.getJSON("search/pattern/"+encodePattern(encodeURI(request.term))).done(function(data) {
				if (data != null && data.length > 0) {
					var cleansed_data = [];
					for (var i = 0; i < data.length; i++) {
						var cleansed = data[i].replace(/\.n/g, "").replace(/\.v/g, "").replace(/\.j/g, "");
						pattern_autocomplete[data[i]] = cleansed;
						pattern_autocomplete_rev[cleansed] = data[i];
						cleansed_data.push(cleansed);
					}
					response(cleansed_data);
				}
    		});
    	},
    	minLength: 3,
    	select: function(event, ui) { $("#pattern-searcher").val(ui.item.label); searchPattern(); setTimeout(function() { $("#pattern-searcher").val(""); }, 10); }
	});

	// Load settings
	if (getParameterByName("patterns") !== "" &&
		getParameterByName("subjects") !== "" &&
		getParameterByName("objects") !== "") {

		patterns_active = JSON.parse(decodePattern(getParameterByName("patterns")));

		var subject_param = JSON.parse(getParameterByName("subjects"));
		if (subject_param.length === 1 && subject_param[0].indexOf("__") === 0) {
			$("#subject-entry").children().first().click();
			$("#subject-search").val("/m/"+subject_param[0].replace(/__/g, ""));
			searchFreebaseSubject(true);
		} else if (subject_param.length === 1 && subject_param[0].indexOf("@@") === 0) {
			$("#subject-extractor-entry").children().first().click();
			var extractorstring = subject_param[0].replace(/@@/g, "") + ".subjects";
			activateTheThing(extractorstring);
		} else {
			subjects_active = subject_param;
		}

		var object_param = JSON.parse(getParameterByName("objects"));
		if (object_param.length === 1 && object_param[0].indexOf("__") === 0) {
			$("#object-entry").children().first().click();
			$("#object-search").val("/m/"+object_param[0].replace(/__/g, ""));
			searchFreebaseObject(true);
		} else if (object_param.length === 1 && object_param[0].indexOf("@@") === 0) {
			$("#object-extractor-entry").children().first().click();
			var extractorstring = object_param[0].replace(/@@/g, "") + ".objects";
			activateTheThing(extractorstring);
		} else {
			objects_active = object_param;
		}
		queryInfo();
	}
});

function openSave() {
	var sub0 = JSON.parse(decodeURIComponent(getGoodSubjects()));
	if (sub0.length !== 0 && sub0[0].indexOf("@@") === 0) {
		sub0 = sub0[0];
		sub0 = sub0.replace(/@@/g, "");
		if (sub0.indexOf(".entities") !== -1) {
			sub0 = sub0.replace(".entities", "");
			$.getJSON("extractor/entity/name/"+encodeURI(sub0)).done(function(result) {
				$("#save-xname").val(result.typesname);
				$("#save-xname").prop("disabled", true);
				$("#save-dialog").modal();
			});
		} else {
			var type = "";
			if (sub0.indexOf(".subjects") !== -1) {
				sub0 = sub0.replace(".subjects", "");
				type = "xname";
			} else {
				sub0 = sub0.replace(".objects", "");
				type = "yname";
			}

			$.getJSON("extractor/relation/name/"+encodeURI(sub0)).done(function(result) {
				$("#save-xname").val(result[type]);
				$("#save-xname").prop("disabled", true);
				$("#save-dialog").modal();
			});

		}
	} else {
		$("#save-xname").val("")
		$("#save-xname").prop("disabled", false);
	}

	var obj0 = JSON.parse(decodeURIComponent(getGoodObjects()));
	if (obj0.length !== 0 && obj0[0].indexOf("@@") === 0) {
		obj0 = obj0[0];
		obj0 = obj0.replace(/@@/g, "");
		if (obj0.indexOf(".entities") !== -1) {
			obj0 = obj0.replace(".entities", "");
			$.getJSON("extractor/entity/name/"+encodeURI(obj0)).done(function(result) {
				$("#save-yname").val(result.typesname);
				$("#save-yname").prop("disabled", true);
			});
		} else {
			var type = "";
			if (obj0.indexOf(".subjects") !== -1) {
				obj0 = obj0.replace(".subjects", "");
				type = "xname";
			} else {
				obj0 = obj0.replace(".objects", "");
				type = "yname";
			}

			$.getJSON("extractor/relation/name/"+encodeURI(obj0)).done(function(result) {
				$("#save-yname").val(result[type]);
				$("#save-yname").prop("disabled", true);
			});

		}
	} else {
		$("#save-yname").val("")
		$("#save-yname").prop("disabled", false);
	}

	$("#save-dialog").modal();
}

function togglePattern(pattern) {
	$(pattern).toggleClass("active");
	var pat = $(pattern)[0].textContent;
	pat = cleansed_patterns_rev[pat];
	if ($(pattern).hasClass("active")) {
		current_pattern_toggle = true;
		patterns.splice(patterns.indexOf(pat), 1);
		patterns_active.push(pat);
		updateView();
		queryInfo();
	} else {
		patterns_active.splice(patterns_active.indexOf(pat), 1);
		patterns.unshift(pat);
		updateView();
		queryInfo();
	}

}

function toggleSubject(subject) {
	$(subject).toggleClass("active");
	var subj = $(subject)[0].textContent;
	if ($(subject).hasClass("active")) {
		subjects.splice(subjects.indexOf(subj), 1);
		subjects_active.push(subj);
		queryInfo();
	} else {
		subjects_active.splice(subjects_active.indexOf(subj), 1);
		subjects.unshift(subj);
		queryInfo();
	}
	updateView();
}


function toggleObject(object) {
	$(object).toggleClass("active");
	var obj = $(object)[0].textContent;
	if ($(object).hasClass("active")) {
		objects.splice(objects.indexOf(obj), 1);
		objects_active.push(obj);
		queryInfo();
	} else {
		objects_active.splice(objects_active.indexOf(obj), 1);
		objects.unshift(obj);
		queryInfo();
	}
	updateView();
}

function showInfoDelayed(pattern) {
	setTimeout(function() {
		if (current_pattern_toggle) {
			current_pattern_toggle = false;
			return;
		}
		showInfo(pattern);
	}, 100);
}

function showInfo(pattern) {

	if ($(pattern).parent().children()[1].textContent !== "") {
		$($(pattern).parent().children()[1]).collapse("toggle");
		return;
	}

	var good_subjects = getGoodSubjects();

	var good_objects = getGoodObjects();

	var pat = $(pattern).children().first()[0].textContent;
	pat = cleansed_patterns_rev[pat];
	$.getJSON("pattern/sentence/"+encodePattern(encodeURI(pat))+"/"+good_subjects+"/"+good_objects+"/limit/5").done(function(data) {
		var html = "";
		html += '<div style="margin: 5px;">';
		html += "<p><b>Example sentences:</b></p>";
		html += "<ul>";
		for (var i = 0; i < data.length; i++) {
			var sentence = data[i][0];
			var subj = data[i][1].replace(/_/g, " ");
			var obj = data[i][2].replace(/_/g, " ");
			sentence = sentence.replace(subj, '<span class="text-success">' + subj + '</span>');
			sentence = sentence.replace(obj, '<span class="text-danger">' + obj + '</span>');
			html += '<li><small>'+sentence+'</small></li>';
		}
		if (data.length == 0)
			html += "No example sentences were found!";
		html += "</ul>"
		html += "</div>";

		$(pattern).parent().children()[1].innerHTML = html;
		$($(pattern).parent().children()[1]).collapse();
	});
}

function activateTheThing(element) {
	muh = $(element);

	var btnText;
	var extractor;
	var type;

	if (typeof element === "string") {
		var arrrr = element.split(".");
		btnText = arrrr[1];
		extractor = arrrr[0];
		type = arrrr[2];
	} else {
		btnText = $(element).text().toLowerCase();
		extractor = $(element).parent().parent().parent().parent().children().first().children().first().text();
		type = $(element).parent().parent().parent().parent().parent().attr("id") === "subject-extractor-results" ? "subjects" : "objects";
	}

	console.log(btnText);
	console.log(extractor);
	console.log(type);

	var extractorType = "";
	if (btnText === "subjects" || btnText === "objects") {
		extractorType = "Relation extractor";
	} else {
		extractorType = "Entity extractor";
	}
	console.log(extractorType);

	if (type === "subjects") {
		$("#selected-subject-extractor")[0].innerHTML = '<label style="color: #333;">Currently selected:</label><br><p class="text-center lead"><b>'+extractor+'</b>&nbsp;'+ btnText +'<p class="text-center" style="font-size: 11px; margin-top: -5px;">'+extractorType+'</p></p>';
		active_subject_extractor = "@@"+extractor+"."+btnText+"@@";
		queryInfo();
	} else {
		$("#selected-object-extractor")[0].innerHTML = '<label style="color: #333;">Currently selected:</label><br><p class="text-center lead"><b>'+extractor+'</b>&nbsp;'+ btnText +'<p class="text-center" style="font-size: 11px; margin-top: -5px;">'+extractorType+'</p></p>';
		active_object_extractor = "@@"+extractor+"."+btnText+"@@";
		queryInfo();
	}
}

function newExtractor() {
location.href = "relation.html";
}

function permaLink() {
	var encodePats = encodeURI(JSON.stringify(patterns_active));

	var encodeSubj = getGoodSubjects();
	var encodeObj = getGoodObjects();

	window.location.href = "relation.html?patterns=" + encodePats + "&subjects=" + encodeSubj + "&objects=" + encodeObj;
}

function download_result() {
	var good_patterns = encodeURI(JSON.stringify(patterns_active));

	var good_subjects = getGoodSubjects();

	var good_objects = getGoodObjects();

	location.href = "/pattern/output/"+encodePattern(good_patterns)+"/"+good_subjects+"/"+good_objects+"/results.txt";
}

function queryInfo() {

	if (queryingInfo) {
		queryAgain = true;
		return;
	}

	if (patterns_active.length == 0 && subjects_active.length == 0 && objects_active.length == 0 && active_subject == "" && active_object == "")
		return;

	$(".loader").removeClass("hidden");

	var good_patterns = encodeURI(JSON.stringify(patterns_active));

	var good_subjects = getGoodSubjects();

	var good_objects = getGoodObjects();

	queryingInfo = true;
	$.getJSON("pattern/suggestion/"+encodePattern(good_patterns)+"/"+good_subjects+"/"+good_objects).done(function(data) {
		if (data[0].length != 0) {
			patterns = data[0];
		}
		subjects = data[1];
		objects = data[2];

		output = [];
		for (var i = 0; i < data[3].length; i++)
			output.push(JSON.parse(data[3][i]));
		var toDelete = [];
		for (var x = 0; x < output.length; x++) {
			for (var y = x+1; y < output.length; y++) {
				if (output[x].subject === output[y].subject && output[x].object === output[y].object) {
					toDelete.push (output[y]);
				}
			}
		}

		for (var i = 0; i < toDelete.length; i++) {
			if (output.indexOf(toDelete[i]) != -1) {
				output.splice(output.indexOf(toDelete[i]), 1);
			}
		}

		//cleanse
		while (patterns.indexOf("") != -1)
			patterns.splice(patterns.indexOf(""), 1);

		for (var i = 0; i < patterns_active.length; i++) {
			while (patterns.indexOf(patterns_active[i]) != -1)
				patterns.splice(patterns.indexOf(patterns_active[i]), 1);
		}

		while (subjects.indexOf("") != -1)
			subjects.splice(subjects.indexOf(""), 1);

		for (var i = 0; i < subjects_active.length; i++) {
			while (subjects.indexOf(subjects_active[i]) != -1)
				subjects.splice(subjects.indexOf(subjects_active[i]), 1);
		}

		while (objects.indexOf("") != -1)
			objects.splice(objects.indexOf(""), 1);

		for (var i = 0; i < objects_active.length; i++) {
			while (objects.indexOf(objects_active[i]) != -1)
				objects.splice(objects.indexOf(objects_active[i]), 1);
		}

		cleansePatterns();

		updateView();
		$(".loader").addClass("hidden");
		queryingInfo = false;
		if (queryAgain) {
			queryAgain = false;
			queryInfo();
		}
	});
}

function saveExtractor() {
	var name = $("#save-name").val();
	var xname = $("#save-xname").val();
	var yname = $("#save-yname").val();
	var relation = $("#save-relation").val();
	var description = $("#save-description").val();

	$("#save-name").val("");
	$("#save-description").val("");

	$("#save-xname").val("");
	$("#save-yname").val("");
	$("#save-relation").val("");

	if (name === undefined || name.replace(/\s/g, "") === "") {
		display_message("Name of the extractor cannot be empty!", "danger");
		setTimeout(function() {$("#save-dialog").modal();}, 500);
		return;
	}

	if (xname === undefined || xname.replace(/\s/g, "") === "") {
		display_message("Name for the x-entities cannot be empty!", "danger");
		setTimeout(function() {$("#save-dialog").modal();}, 500);
		return;
	}

	if (yname === undefined || yname.replace(/\s/g, "") === "") {
		display_message("Name for the y-entities cannot be empty!", "danger");
		setTimeout(function() {$("#save-dialog").modal();}, 500);
		return;
	}

	if (relation === undefined || relation.replace(/\s/g, "") === "") {
		display_message("Name for the relation cannot be empty!", "danger");
		setTimeout(function() {$("#save-dialog").modal();}, 500);
	}

	if (description === undefined || description.replace(/\s/g, "") === "") {
		display_message("Description of the extractor cannot be empty!", "danger");
		setTimeout(function() {$("#save-dialog").modal();}, 500);
		return;
	}

	var good_patterns = encodeURI(JSON.stringify(patterns_active));

	var good_subjects = getGoodSubjects();

	var good_objects = getGoodObjects();

	if (patterns_active.length == 0 && subjects_active.length == 0 && active_subject === "" && objects_active.length == 0 && active_object === "") {
		display_message("Cannot save empty extractor!", "danger");
		setTimeout(function() {$("#save-dialog").modal();}, 500);
		return;
	}

	$.getJSON("extractor/save/"+encodeURI(name)+"/"+encodeURI(xname)+"/"+encodeURI(yname)+"/"+encodeURI(relation)+"/"+encodeURI(description)+"/"+encodePattern(good_patterns)+"/"+good_subjects+"/"+good_objects).done(function(response) {
		// console.log(response);
		if (response === "success")
			display_message("The extractor was saved successfully!", "success");
		else if (response === "unique")
			display_message("An extractor with the same name already exists!", "danger");
		else
			display_message("An unknown error occurred!");
	});
}

function getGoodSubjects() {
	if (!$("#xtype-body").hasClass("hidden") || (active_subject === "" && active_subject_extractor === ""))
		return encodeURI(JSON.stringify(subjects_active));
	else if (!$("#subject-body").hasClass("hidden"))
		return encodeURI(JSON.stringify(["__"+active_subject.replace("/m/", "")+"__"]));
	else if (!$("#subject-extractor-body").hasClass("hidden"))
		return encodeURI(JSON.stringify([active_subject_extractor]))
	else
		return encodeURI(JSON.stringify([]));
}

function getGoodObjects() {
	if (!$("#ytype-body").hasClass("hidden") || (active_object === "" && active_object_extractor === ""))
		return encodeURI(JSON.stringify(objects_active));
	else if (!$("#object-body").hasClass("hidden"))
		return encodeURI(JSON.stringify(["__"+active_object.replace("/m/", "")+"__"]));
	else if (!$("#object-extractor-body").hasClass("hidden"))
		return encodeURI(JSON.stringify([active_object_extractor]))
	else
		return encodeURI(JSON.stringify([]));
}

function inspectOutput(i) {

	var idPair = output[i].idPair;
	var elements = [];
	for (var x = 0; x < output.length; x++) {
		if (output[x].idPair === idPair) {
			elements.push(output[x]);
		}
	}

	var html = "";

	html += '<p><b>Subjects:</b> ';
	var displayed = [];
	for (var x = 0; x < elements.length; x++) {
		if (displayed.indexOf(elements[x].subject) === -1)
			displayed.push(elements[x].subject);
	}
	for (var x = 0; x < displayed.length; x++) {
		html += '<span style="font-variant: small-caps;">' + displayed[x].replace(/_/g, " ") + "</span>";
		if (x !== displayed.length-1)
			html += ' <span style="font-size: 9px;">or</span> ';
	}
	html += '</p>';
	html += '<p style="font-size: 10px; max-height: 60px; overflow-y: auto; border: 1px solid black; padding: 5px;"><b>Types:</b> ' + output[i].subjectTypes.join(", ") + '</p>';

	html += '<p><b>Objects:</b> ';
	displayed = [];
	for (var x = 0; x < elements.length; x++) {
		if (displayed.indexOf(elements[x].object) === -1)
			displayed.push(elements[x].object);
	}
	for (var x = 0; x < displayed.length; x++) {
		html += '<span style="font-variant: small-caps;">' + displayed[x].replace(/_/g, " ") + "</span>";
		if (x !== displayed.length-1)
			html += ' <span style="font-size: 9px;">or</span> ';
	}
	html += '</p>';
	html += '<p style="font-size: 10px; max-height: 60px; overflow-y: auto; border: 1px solid black; padding: 5px;"><b>Types:</b> ' + output[i].objectTypes.join(", ") + '</p>';

	html += '<p><b>Patterns:</b></p>';
	html += '<ul>'

	displayed = [];
	for (var x = 0; x < elements.length; x++) {
		var patternSplit = elements[x].pattern.split(" ");
		for (var z = 0; z < patternSplit.length; z++) {
			if (displayed.indexOf(patternSplit[z]) === -1) {
				displayed.push(patternSplit[z]);
			}
		}
	}
	for (var z = 0; z < displayed.length; z++) {
		html += '<li>' + displayed[z].replace(/_/g, " ") + '</li>';
	}
	html += '</ul>'


	html += '<p><b>Example Sentences:</b></p><div id="example-sentences"><img src="loader.gif"></div>';

	$("#output-info-body")[0].innerHTML = html;
	$("#output-info").modal("show");

	$.getJSON("pattern/sentence/"+encodeURI(encodePattern(output[i].pattern.split(" ")[0]))+"/"+encodeURI(JSON.stringify(["__"+output[i].idPair.split(" + ")[0].replace("/m/", "")+"__"]))+"/"+encodeURI(JSON.stringify(["__"+output[i].idPair.split(" + ")[1].replace("/m/", "")+"__"]))+"/limit/10").done(function(data) {
		var html2 = "";
		if (data.length == 0)
			html2 += '<p>'+output[i].sentence+'</p>';
		for (var z = 0; z < data.length; z++) {
			html2 += "<p>" + data[z][0] + '</p>';
		}
		$("#example-sentences")[0].innerHTML = html2;
	});
}

function searchPattern() {
	//console.log(pattern_autocomplete_rev[($('#pattern-searcher').val())])
	$('#pattern-searcher').blur();
	if (pattern_autocomplete_rev[($('#pattern-searcher').val())] !== undefined) {
		if (patterns_active.indexOf(pattern_autocomplete_rev[$('#pattern-searcher').val()]) !== -1)
			return;
		patterns_active.push(pattern_autocomplete_rev[$('#pattern-searcher').val()]);
		cleansePatterns();
		updateView();
		queryInfo();
		$('#pattern-searcher').val("");
		return;
	}
	$.getJSON("search/pattern/"+encodePattern(encodeURI($('#pattern-searcher').val()))).done(function(data) {
		$('#pattern-searcher').val("");
		if (data.length != 0) {
			patterns = data;
			for (var i = 0; i < patterns_active.length; i++) {
				while (patterns.indexOf(patterns_active[i]) != -1)
					patterns.splice(patterns.indexOf(patterns_active[i]), 1);
			}
			cleansePatterns();
			updateView();
			$("#pattern-searcher").val("");
		} else {
			display_message("Could not find patterns that match your search request");
		}
	});
}

function searchFreebaseSubject(selectZero) {
	selectZero = typeof selectZero !== 'undefined' ? selectZero : false;

	var query = $('#subject-search').val();
	console.log(query);
	$.getJSON("https://www.googleapis.com/freebase/v1/search?query=" + query + "&limit=5").done(function(data) {
		if (data.status === "200 OK") {
			console.log(data.result);
			var html = '<div class="list-group">';
			if (data.result.length == 0) {
				display_message("Could not find any freebase entities for query '"+query+"'");
				return;
			}
			freebase_subjects = [];
			active_subject = "";
			for (var i = 0; i < data.result.length; i++) {
				if (data.result[i].name === undefined || data.result[i].name === "")
					continue;
				freebase_subjects.push(data.result[i].mid);
				if (data.result[i].notable !== undefined)
					html += '<a id="subject'+i+'" class="list-group-item" style="height: 30px; padding-top: 7px; cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;" ondblclick="activateResult(this);">'+data.result[i].name+' ('+data.result[i].notable.name+')</a>';
				else
					html += '<a id="subject'+i+'" class="list-group-item" style="height: 30px; padding-top: 7px; cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;" ondblclick="activateResult(this);">'+data.result[i].name+'</a>';
			}
			html += "</div>";
			$("#subject-results")[0].innerHTML = html;
			if (selectZero)
				$("#subject0").dblclick();
		}
	});

}

function searchFreebaseObject(selectZero) {
	selectZero = typeof selectZero !== 'undefined' ? selectZero : false;

	var query = $('#object-search').val();
	console.log(query);
	$.getJSON("https://www.googleapis.com/freebase/v1/search?query=" + query + "&limit=5").done(function(data) {
		if (data.status === "200 OK") {
			console.log(data.result);
			var html = '<div class="list-group">';
			if (data.result.length == 0) {
				display_message("Could not find any freebase entities for query '"+query+"'");
				return;
			}
			freebase_objects = [];
			active_object = "";
			for (var i = 0; i < data.result.length; i++) {
				if (data.result[i].name === undefined || data.result[i].name === "")
					continue;
				freebase_objects.push(data.result[i].mid);
				if (data.result[i].notable !== undefined)
					html += '<a id="object'+i+'" class="list-group-item" style="height: 30px; padding-top: 7px; cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;" ondblclick="activateResult(this);">'+data.result[i].name+' ('+data.result[i].notable.name+')</a>';
				else
					html += '<a id="object'+i+'" class="list-group-item" style="height: 30px; padding-top: 7px; cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;" ondblclick="activateResult(this);">'+data.result[i].name+'</a>';
			}
			html += "</div>";
			$("#object-results")[0].innerHTML = html;
			if (selectZero)
				$("#object0").dblclick();
		}
	});

}

function activateResult(obj) {
	objects = $(obj).parent().children();
	for (var i = 0; i < objects.length; i++) {
		$(objects[i]).removeClass('active');
	}
	$(obj).addClass('active');
	var obid = $(obj)[0].id;
	if (obid.indexOf("subject") != -1) {
		active_subject = freebase_subjects[parseInt(obid.replace("subject", ""))];
	} else {
		active_object = freebase_objects[parseInt(obid.replace("object", ""))];
	}
	queryInfo();
}

function searchSubject() {
	$('#subject-searcher').blur();
	if (typeExists($('#subject-searcher').val())) {
		if (subjects_active.indexOf($('#subject-searcher').val()) != -1)
			return;
		subjects_active.push($('#subject-searcher').val());
		updateView();
		queryInfo();
		$('#subject-searcher').val("");
		return;
	}
	var searchResults = searchType($('#subject-searcher').val());
	if (searchResults.length == 0) {
		display_message("Could not find X-types that match your search request");
	} else {
		subjects = searchResults;
		updateView();
	}
}

function searchSubjectExtractor() {
	var query = $('#subject-extractor-search').val();
	var resultdiv = $("#subject-extractor-results");
	resultdiv.empty();
	$.getJSON("search/extractor/relation/"+encodeURI(query)).done(function(result){
		// console.log(result);
		if (result.length == 0) {
			display_message("No relation results found!");
			return;
		}

		for (var i = 0; i < result.length; i++) {
			resultdiv.append('<div class="panel panel-default" style="margin-top: 1px;"><div class="panel-heading" style="cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; height: 25px; padding-top: 3px;" onclick="showRelationExtractorInfo(this);"><h4 class="panel-title"><a>'+result[i]+'</a></h4><div style="float:right; margin-top: -18px;"><small style="color: #CCC;">relation</small></div></div><div class="panel-collapse collapse"><div class="panel-body"></div></div></div>');
		}
	});

	$.getJSON("search/extractor/entity/"+encodeURI(query)).done(function(result){
		// console.log(result);
		if (result.length == 0) {
			display_message("No entity results found!");
			return;
		}

		for (var i = 0; i < result.length; i++) {
			resultdiv.append('<div class="panel panel-default" style="margin-top: 1px;"><div class="panel-heading" style="cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; height: 25px; padding-top: 3px;" onclick="showEntityExtractorInfo(this);"><h4 class="panel-title"><a>'+result[i]+'</a></h4><div style="float:right; margin-top: -18px;"><small style="color: #CCC;">entity</small></div></div><div class="panel-collapse collapse"><div class="panel-body"></div></div></div>');
		}
	});
}

function searchObjectExtractor() {
	var query = $('#object-extractor-search').val();
	var resultdiv = $("#object-extractor-results");
	resultdiv.empty();

	$.getJSON("search/extractor/relation/"+encodeURI(query)).done(function(result){
		console.log(result);
		if (result.length == 0) {
			display_message("No relation results found!");
			return;
		}

		for (var i = 0; i < result.length; i++) {
			resultdiv.append('<div class="panel panel-default" style="margin-top: 1px;"><div class="panel-heading" style="cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; height: 25px; padding-top: 3px;" onclick="showRelationExtractorInfo(this);"><h4 class="panel-title"><a>'+result[i]+'</a></h4><div style="float:right; margin-top: -18px;"><small style="color: #CCC;">relation</small></div></div><div class="panel-collapse collapse"><div class="panel-body"></div></div></div>');
		}
	});

	$.getJSON("search/extractor/entity/"+encodeURI(query)).done(function(result){
		// console.log(result);
		if (result.length == 0) {
			display_message("No entity results found!");
			return;
		}

		for (var i = 0; i < result.length; i++) {
			resultdiv.append('<div class="panel panel-default" style="margin-top: 1px;"><div class="panel-heading" style="cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; height: 25px; padding-top: 3px;" onclick="showEntityExtractorInfo(this);"><h4 class="panel-title"><a>'+result[i]+'</a></h4><div style="float:right; margin-top: -18px;"><small style="color: #CCC;">entity</small></div></div><div class="panel-collapse collapse"><div class="panel-body"></div></div></div>');
		}
	});
}

function searchObject() {
	$('#object-searcher').blur();
	if (typeExists($('#object-searcher').val())) {
		if (objects_active.indexOf($('#object-searcher').val()) != -1)
			return;
		objects_active.push($('#object-searcher').val());
		updateView();
		queryInfo();
		$('#object-searcher').val("");
		return;
	}
	var searchResults = searchType($('#object-searcher').val());
	if (searchResults.length == 0) {
		display_message("Could not find Y-types that match your search request");
	} else {
		objects = searchResults;
		updateView();
	}
}

function toggleXType(object) {
	var value = $(object).text();

	if (!$("#xtype-body").hasClass("hidden")) {
		$("#xtype-body").addClass("hidden");
		$("#xtype-menu").prepend('<li id="xtype-entry"><a style="cursor: pointer;" onclick="toggleXType(this);">X-Type</a></li>');
	} else if (!$("#subject-body").hasClass("hidden")) {
		$("#subject-body").addClass("hidden");
		$("#xtype-menu").prepend('<li id="subject-entry"><a style="cursor: pointer;" onclick="toggleXType(this);">Subject</a></li>');
	} else if (!$("#subject-extractor-body").hasClass("hidden")) {
		$("#subject-extractor-body").addClass("hidden");
		$("#xtype-menu").prepend('<li id="subject-extractor-entry"><a style="cursor: pointer;" onclick="toggleXType(this);">Extractor</a></li>');
	}

	switch(value) {
		case "Subject":
			$("#xtype-button")[0].innerHTML = 'Subject <span class="caret"></span>';
			$("#subject-body").removeClass("hidden");
			$("#subject-entry").remove();
			break;
		case "X-Type":
			$("#xtype-button")[0].innerHTML = 'X-Type <span class="caret"></span>';
			$("#xtype-body").removeClass("hidden");
			$("#xtype-entry").remove();
			break;
		case "Extractor":
			$("#xtype-button")[0].innerHTML = 'Extractor <span class="caret"></span>';
			$("#subject-extractor-body").removeClass("hidden");
			$("#subject-extractor-entry").remove();
			break;
	}
}

function toggleYType(object) {
	var value = $(object).text();

	if (!$("#ytype-body").hasClass("hidden")) {
		$("#ytype-body").addClass("hidden");
		$("#ytype-menu").prepend('<li id="ytype-entry"><a style="cursor: pointer;" onclick="toggleYType(this);">Y-Type</a></li>');
	} else if (!$("#object-body").hasClass("hidden")) {
		$("#object-body").addClass("hidden");
		$("#ytype-menu").prepend('<li id="object-entry"><a style="cursor: pointer;" onclick="toggleYType(this);">Object</a></li>');
	} else if (!$("#object-extractor-body").hasClass("hidden")) {
		$("#object-extractor-body").addClass("hidden");
		$("#ytype-menu").prepend('<li id="object-extractor-entry"><a style="cursor: pointer;" onclick="toggleYType(this);">Extractor</a></li>');
	}

	switch(value) {
		case "Object":
			$("#ytype-button")[0].innerHTML = 'Object <span class="caret"></span>';
			$("#object-body").removeClass("hidden");
			$("#object-entry").remove();
			break;
		case "Y-Type":
			$("#ytype-button")[0].innerHTML = 'Y-Type <span class="caret"></span>';
			$("#ytype-body").removeClass("hidden");
			$("#ytype-entry").remove();
			break;
		case "Extractor":
			$("#ytype-button")[0].innerHTML = 'Extractor <span class="caret"></span>';
			$("#object-extractor-body").removeClass("hidden");
			$("#object-extractor-entry").remove();
			break;
	}
}

function updateView() {

	//===== PATTERNS ======
	var html = '';
	if (patterns_active.length > 0) {
		html += '<b>Active:</b><div class="panel-group">';
		for (var i = 0; i < patterns_active.length; i++) {
			html += '<div class="panel panel-primary" style="margin-top: 1px;"><div class="panel-heading active" style="cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; height: 25px; padding-top: 3px;" onclick="showInfoDelayed(this);" ondblclick="togglePattern(this);"><h4 class="panel-title"><a>'+cleansed_patterns[patterns_active[i]]+'</a></h4></div>';
			html += '<div class="panel-collapse collapse"><div class="panel-body"></div></div></div>';
		}
		html += '</div><div style="height: 10px;"></div>';
	}
	html += '<div class="panel-group" id="accordion">';
	for (var i = 0; i < patterns.length; i++) {
		if ((!show_all_patterns && i > MIN_DISPLAY) || i > MAX_DISPLAY)
			break;
		html += '<div class="panel panel-default" style="margin-top: 1px;"><div class="panel-heading" style="cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; height: 25px; padding-top: 3px;" onclick="showInfoDelayed(this);" ondblclick="togglePattern(this);"><h4 class="panel-title"><a>'+cleansed_patterns[patterns[i]]+'</a></h4></div>';
		html += '<div class="panel-collapse collapse"><div class="panel-body"></div></div></div>';
	}
	html += '</div>';
	if (patterns.length > 0) {
		if (show_all_patterns)
			html += '<p style="margin-top: 10px; font-size: 11px;" class="text-center"><a style="cursor: pointer;" onclick="show_all_patterns = false; updateView();">Less</a></p>';
		else
			html += '<p style="margin-top: 10px; font-size: 11px;" class="text-center"><a style="cursor: pointer;" onclick="show_all_patterns = true; updateView();">More</a></p>';
	}
	$("#patterns")[0].innerHTML = html;

	//===== SUBJECTS ======
	html = '';
	if (subjects_active.length > 0) {
		html += '<b>Active:</b><div class="list-group">';
		for (var i = 0; i < subjects_active.length; i++) {
			html += '<a class="list-group-item active" style="height: 30px; padding-top: 7px; cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;" ondblclick="toggleSubject(this);">'+subjects_active[i]+'</a>';
		}
		html += '</div>';
	}
	if (subjects.length > 0) {
		html += '<div class="list-group">';
		for (var i = 0; i < subjects.length; i++) {
			if ((!show_all_subjects && i > MIN_DISPLAY) || i > MAX_DISPLAY)
				break;
			html += '<a class="list-group-item" style="height: 30px; padding-top: 7px; cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;" ondblclick="toggleSubject(this);">'+subjects[i]+'</a>';
		}
		html += '</div>';
		if (show_all_subjects)
			html += '<p style="margin-top: 10px; font-size: 11px;" class="text-center"><a style="cursor: pointer;" onclick="show_all_subjects = false; updateView();">Less</a></p>';
		else
			html += '<p style="margin-top: 10px; font-size: 11px;" class="text-center"><a style="cursor: pointer;" onclick="show_all_subjects = true; updateView();">More</a></p>';
	}
	$("#subjects")[0].innerHTML = html;

	//===== OBJECTS ======
	html = '';
	if (objects_active.length > 0) {
		html += '<b>Active:</b><div class="list-group">';
		for (var i = 0; i < objects_active.length; i++) {
			html += '<a class="list-group-item active" style="height: 30px; padding-top: 7px; cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;" ondblclick="toggleObject(this);">'+objects_active[i]+'</a>';
		}
		html += '</div>';
	}
	if (objects.length > 0) {
		html += '<div class="list-group">';
		for (var i = 0; i < objects.length; i++) {
			if ((!show_all_objects && i > MIN_DISPLAY) || i > MAX_DISPLAY)
				break;
			html += '<a class="list-group-item" style="height: 30px; padding-top: 7px; cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;" ondblclick="toggleObject(this);">'+objects[i]+'</a>';
		}
		html += '</div>';
		if (show_all_objects)
			html += '<p style="margin-top: 10px; font-size: 11px;" class="text-center"><a style="cursor: pointer;" onclick="show_all_objects = false; updateView();">Less</a></p>';
		else
			html += '<p style="margin-top: 10px; font-size: 11px;" class="text-center"><a style="cursor: pointer;" onclick="show_all_objects = true; updateView();">More</a></p>';
	}
	$("#objects")[0].innerHTML = html;

	//===== OUTPUT =====
	html = '';
	if (output.length > 0) {
		var usedIDs = [];
		for (var i = 0; i < output.length; i++) {
			if ((!show_all_output && i > MIN_DISPLAY) || i > MAX_DISPLAY)
				break;
			if (usedIDs.indexOf(output[i].idPair) == -1) {
				html += "<tr>";
				html += '<td><a target="blank" href="http://freebase.com'+output[i].idPair.split(" + ")[0]+'">'+output[i].subject.replace(/_/g, " ")+"</a></td>";
				html += '<td><a target="blank" href="http://freebase.com'+output[i].idPair.split(" + ")[1]+'">'+output[i].object.replace(/_/g, " ")+"</a></td>";
				html += '<td style="font-size: 10px;">'+output[i].sentence+"</td>";
				html += '<td><a style="cursor: pointer;" onclick="inspectOutput('+i+');"><span class="glyphicon glyphicon-search"></span></a></td>';
				html += "</tr>";
				usedIDs.push(output[i].idPair);
			}
		}
		if (show_all_output)
			html += '<tr><td colspan="4" class="text-center"><a style="cursor: pointer;" onclick="show_all_output = false; updateView();">Less</a></td></tr>';
		else
			html += '<tr><td colspan="4" class="text-center"><a style="cursor: pointer;" onclick="show_all_output = true; updateView();">More</a></td></tr>';
	}
	$("#results")[0].innerHTML = html;

}