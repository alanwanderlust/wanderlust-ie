var patterns = [];
var patterns_active = [];
var cleansed_patterns = {};
var cleansed_patterns_rev = {};

var current_pattern_toggle = false;

var MIN_DISPLAY = 10;
var MAX_DISPLAY = 100;

var typeList = [];
var nameList = [];

// Second parameter is optional
// Type can be one of "warning" (default), "danger", "success", "info"
function display_message(text, type) {
  type = typeof type !== 'undefined' ? type : "warning";

  var box = $("#info-box").children().first();
  box.removeClass();
  box.addClass("alert");
  box.addClass("alert-"+type);

  $("#info-content")[0].innerHTML = text;
  $("#info-box").fadeIn("fast");
  setTimeout(function() {
    $("#info-box").fadeOut("fast");
  }, 2000)
}

// Encodes a pattern so that it can be send to the server
function encodePattern(pat) {
  if (pat === undefined)
    return "";
  return pat.replace(/\#/g, ":octothorp:").replace(/\?/g, ":qmark:");
}

// Decodes a pattern (only necessary when parsing url parameters)
function decodePattern(pat) {
  if (pat === undefined)
    return "";
  return pat.replace(/:octothorp:/g, "#").replace(/:qmark:/g, "?");
}

function getParameterByName(name) {
  name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
  var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
  results = regex.exec(location.search);
  return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

function cleansePatterns() {

  cleansed_patterns = {};
  cleansed_patterns_rev = {};

  for (var i = 0; i < patterns.length; i++) {
    var cleansed = patterns[i].replace(/\.n/g, "").replace(/\.v/g, "").replace(/\.j/g, "");
    if (cleansed_patterns_rev[cleansed] !== undefined && cleansed_patterns_rev[cleansed] !== patterns[i]) {
      cleansed_patterns[cleansed_patterns_rev[cleansed]] = cleansed_patterns_rev[cleansed];
      cleansed_patterns_rev[cleansed_patterns_rev[cleansed]] = cleansed_patterns_rev[cleansed];
      cleansed = patterns[i];
    }
    cleansed_patterns[patterns[i]] = cleansed;
    cleansed_patterns_rev[cleansed] = patterns[i];
  }
  for (var i = 0; i < patterns_active.length; i++) {
    var cleansed = patterns_active[i].replace(/\.n/g, "").replace(/\.v/g, "").replace(/\.j/g, "");
    if (cleansed_patterns_rev[cleansed] !== undefined && cleansed_patterns_rev[cleansed] !== patterns_active[i]) {
      cleansed_patterns[cleansed_patterns_rev[cleansed]] = cleansed_patterns_rev[cleansed];
      cleansed_patterns_rev[cleansed_patterns_rev[cleansed]] = cleansed_patterns_rev[cleansed];
      cleansed = patterns_active[i];
    }
    cleansed_patterns[patterns_active[i]] = cleansed;
    cleansed_patterns_rev[cleansed] = patterns_active[i];
  }
}

function searchType(term) {
  var selected_types = [];
  for (var i = 0; i < typeList.length; i++) {
    if (typeList[i].name.toLowerCase().indexOf(term.toLowerCase()) != -1) {
      selected_types.push(typeList[i]);
    }
  }
  selected_types.sort(function(a,b) {
    if (a.count > b.count)
      return -1;
    if (a.count < b.count)
      return 1;
    return 0;
  });
  var resp = [];
  for (var i = 0; i < selected_types.length; i++) {
    resp.push(selected_types[i].name);
  }
  return resp;
}

function searchExName(term) {
  var selected_names = [];
  for (var i = 0; i < nameList.length; i++) {
    if (nameList[i].toLowerCase().indexOf(term.toLowerCase()) != -1) {
      selected_names.push(nameList[i]);
    }
  }
  var resp = [];
  for (var i = 0; i < selected_names.length; i++) {
    if (selected_names[i].toLowerCase().indexOf(term.toLowerCase()) === 0) {
      resp.unshift(selected_names[i]);
    } else {
      resp.push(selected_names[i]);
    }
  }
  return resp;
}


function typeExists(type) {
  for (var i = 0; i < typeList.length; i++) {
    if (typeList[i].name === type) {
      return true;
    }
  }
  return false;
}

function showRelationExtractorInfo(element) {
  if ($($(element).parent().children()[1]).text() !== "") {
    $($(element).parent().children()[1]).collapse("toggle");
    return;
  }

  var extractorName = $(element).children().first().text();
  $.getJSON("extractor/description/relation/"+encodeURI(extractorName)).done(function(result) {
    if (result["status"] === "success") {
      $($(element).parent().children()[1]).empty();
      $($(element).parent().children()[1]).append('<div style="margin: 5px;"><b>Description:</b><br>'+result["description"]+'<br><br><b>Choose which result of the extractor you want to use:</b><br><div class="btn-group"><button type="button" class="btn btn-primary" onclick="activateTheThing(this);">Subjects</button><button type="button" class="btn btn-primary" onclick="activateTheThing(this);">Objects</button></div></div>');
      $($(element).parent().children()[1]).collapse();
    }
  });
}

function showEntityExtractorInfo(element) {
  if ($($(element).parent().children()[1]).text() !== "") {
    $($(element).parent().children()[1]).collapse("toggle");
    return;
  }

  var extractorName = $(element).children().first().text();
  $.getJSON("extractor/description/entity/"+encodeURI(extractorName)).done(function(result) {
    if (result["status"] === "success") {
      $($(element).parent().children()[1]).empty();
      $($(element).parent().children()[1]).append('<div style="margin: 5px;"><b>Description:</b><br>'+result["description"]+'<br><br><b>Click here to use this extractor:</b><br><div class="btn-group"><button type="button" class="btn btn-primary" onclick="activateTheThing(this);">Entities</button></div></div>');
      $($(element).parent().children()[1]).collapse();
    }
  });
}