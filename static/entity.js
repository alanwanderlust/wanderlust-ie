var pattern_autocomplete = {};
var pattern_autocomplete_rev = {};

var show_all_patterns = false;
var show_all_entities = false;
var show_all_output   = false;

var entities = [];
var entities_active = [];
var entities_autocomplete = [];

var queryingInfo = false;
var queryAgain = false;

var active_entity = "";
var freebase_entities = [];

var active_entity_extractor = "";

var output = [];

$(function() {
  $.getJSON("entity/alltypes").done(function(data) {
    if (data != null && data.length > 0) {
      typeList = data;
      $( "#entity-searcher" ).autocomplete({
          source: function(request, response) {
            response(searchType(request.term));
          },
          minLength: 3,
          select: function(event, ui) { $("#entity-searcher").val(ui.item.label); searchEntity(); setTimeout(function() { $("#entity-searcher").val(""); }, 10);}
      });
    }
  });
  $.getJSON("extractor/all/subjects/objects/entities").done(function(data) {
    if (data != null && data.length > 0) {
      nameList = data;
      $("#save-typesname").autocomplete({
        source: function(request, response) {
          response(searchExName(request.term));
        }
      });

      $(".ui-autocomplete").css("z-index", "2147483647");
    }
  });
})

$(function () {
  // Initialize pattern autocomplete
  $( "#pattern-searcher" ).autocomplete({
    source: function(request, response) {
      $.getJSON("search/entity/pattern/"+encodePattern(encodeURI(request.term))).done(function(data) {
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
})

$(function () {
  // Load settings
  if (getParameterByName("patterns") !== "" &&
    getParameterByName("entities") !== "") {

    patterns_active = JSON.parse(decodePattern(getParameterByName("patterns")));

    var entity_param = JSON.parse(getParameterByName("entities"));
    if (entity_param.length === 1 && entity_param[0].indexOf("__") === 0) {
      $("#entity-entry").children().first().click();
      $("#entity-search").val("/m/"+entity_param[0].replace(/__/g, ""));
      searchFreebaseEntity(true);
    } else if (entity_param.length === 1 && entity_param[0].indexOf("@@") === 0) {
      $("#entity-extractor-entry").children().first().click();
      var extractorstring = entity_param[0].replace(/@@/g, "");
      activateTheThing(extractorstring);
    } else {
      entities_active = entity_param;
    }

    queryInfo();
  }
});

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
  $.getJSON("search/entity/pattern/"+encodePattern(encodeURI($('#pattern-searcher').val()))).done(function(data) {
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

  var good_entities = getGoodEntities();

  var pat = $(pattern).children().first()[0].textContent;
  pat = cleansed_patterns_rev[pat];
  $.getJSON("entity/sentence/"+encodePattern(encodeURI(pat))+"/"+good_entities+"/limit/5").done(function(data) {
    var html = "";
    html += '<div style="margin: 5px;">';
    html += "<p><b>Example sentences:</b></p>";
    html += "<ul>";
    for (var i = 0; i < data.length; i++) {
      var sentence = data[i][0];
      var ent = data[i][1].replace(/_/g, " ");
      sentence = sentence.replace(ent, '<span class="text-success">' + ent + '</span>');
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

function getGoodEntities() {
  if (!$("#type-body").hasClass("hidden") || (active_entity === "" && active_entity_extractor === ""))
    return encodeURI(JSON.stringify(entities_active));
  else if (!$("#entity-body").hasClass("hidden"))
    return encodeURI(JSON.stringify(["__"+active_entity.replace("/m/", "")+"__"]));
  else if (!$("#entity-extractor-body").hasClass("hidden"))
    return encodeURI(JSON.stringify([active_entity_extractor]))
  else
    return encodeURI(JSON.stringify([]));
}

function toggleType(object) {
  var value = $(object).text();

  if (!$("#type-body").hasClass("hidden")) {
    $("#type-body").addClass("hidden");
    $("#type-menu").prepend('<li id="type-entry"><a style="cursor: pointer;" onclick="toggleType(this);">Entity type</a></li>');
  } else if (!$("#entity-body").hasClass("hidden")) {
    $("#entity-body").addClass("hidden");
    $("#type-menu").prepend('<li id="entity-entry"><a style="cursor: pointer;" onclick="toggleType(this);">Entity</a></li>');
  } else if (!$("#entity-extractor-body").hasClass("hidden")) {
    $("#entity-extractor-body").addClass("hidden");
    $("#type-menu").prepend('<li id="entity-extractor-entry"><a style="cursor: pointer;" onclick="toggleType(this);">Extractor</a></li>');
  }

  switch(value) {
    case "Entity":
      $("#type-button")[0].innerHTML = 'Entity <span class="caret"></span>';
      $("#entity-body").removeClass("hidden");
      $("#entity-entry").remove();
      break;
    case "Entity type":
      $("#type-button")[0].innerHTML = 'Entity type <span class="caret"></span>';
      $("#type-body").removeClass("hidden");
      $("#type-entry").remove();
      break;
    case "Extractor":
      $("#type-button")[0].innerHTML = 'Extractor <span class="caret"></span>';
      $("#entity-extractor-body").removeClass("hidden");
      $("#entity-extractor-entry").remove();
      break;
  }
}

function searchEntity() {
  $('#entity-searcher').blur();
  if (typeExists($('#entity-searcher').val())) {
    if (entities_active.indexOf($('#entity-searcher').val()) != -1)
      return;
    entities_active.push($('#entity-searcher').val());
    updateView();
    queryInfo();
    $('#entity-searcher').val("");
    return;
  }
  var searchResults = searchType($('#entity-searcher').val());
  if (searchResults.length == 0) {
    display_message("Could not find entity types that match your search request");
  } else {
    entities = searchResults;
    updateView();
  }
}

function searchEntityExtractor() {
  var query = $('#entity-extractor-search').val();
  var resultdiv = $("#entity-extractor-results");
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

function activateTheThing(element) {
  muh = $(element);

  var btnText;
  var extractor;

  if (typeof element === "string") {
    var arrrr = element.split(".");
    btnText = arrrr[1];
    extractor = arrrr[0];
  } else {
    btnText = $(element).text().toLowerCase();
    extractor = $(element).parent().parent().parent().parent().children().first().children().first().text();
  }

  console.log(btnText);
  console.log(extractor);

  var extractorType = "";
  if (btnText === "subjects" || btnText === "objects") {
    extractorType = "Relation extractor";
  } else {
    extractorType = "Entity extractor";
  }
  console.log(extractorType);

  $("#selected-entity-extractor")[0].innerHTML = '<label style="color: #333;">Currently selected:</label><br><p class="text-center lead"><b>'+extractor+'</b>&nbsp;'+ btnText +'<p class="text-center" style="font-size: 11px; margin-top: -5px;">'+extractorType+'</p></p>';
  active_entity_extractor = "@@"+extractor+"."+btnText+"@@";
  queryInfo();
}

function queryInfo() {

  if (queryingInfo) {
    queryAgain = true;
    return;
  }

  if (patterns_active.length == 0 && entities_active.length == 0 && active_entity == "")
    return;

  $(".loader").removeClass("hidden");

  var good_patterns = encodeURI(JSON.stringify(patterns_active));

  var good_entities = getGoodEntities();

  queryingInfo = true;
  $.getJSON("entity/suggestion/"+encodePattern(good_patterns)+"/"+good_entities).done(function(data) {
    if (data[0].length != 0) {
      patterns = data[0];
    }
    entities = data[1];

    output = [];
    for (var i = 0; i < data[2].length; i++)
      output.push(JSON.parse(data[2][i]));

    //Delete redundant patterns
    var toDelete = [];
    for (var x = 0; x < output.length; x++) {
      for (var y = x+1; y < output.length; y++) {
        if (output[x].entity === output[y].entity) {
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

    while (entities.indexOf("") != -1)
      entities.splice(entities.indexOf(""), 1);

    for (var i = 0; i < entities_active.length; i++) {
      while (entities.indexOf(entities_active[i]) != -1)
        entities.splice(entities.indexOf(entities_active[i]), 1);
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

function toggleEntity(entity) {
  $(entity).toggleClass("active");
  var ent = $(entity)[0].textContent;
  if ($(entity).hasClass("active")) {
    entities.splice(entities.indexOf(ent), 1);
    entities_active.push(ent);
    queryInfo();
  } else {
    entities_active.splice(entities_active.indexOf(ent), 1);
    entities.unshift(ent);
    queryInfo();
  }
  updateView();
}

function newExtractor() {
  location.href = "/entity.html";
}

function saveExtractor() {
  var name = $("#save-name").val();
  var description = $("#save-description").val();
  var typesname = $("#save-typesname").val();
  var relation = $("#save-relation").val();

  $("#save-name").val("");
  $("#save-description").val("");
  $("#save-typesname").val("");
  $("#save-relation").val("");

  if (name === undefined || name.replace(/\s/g, "") === "") {
    display_message("Name of the extractor cannot be empty!", "danger");
    setTimeout(function() {$("#save-dialog").modal();}, 500);
    return;
  }

  if (description === undefined || description.replace(/\s/g, "") === "") {
    display_message("Description of the extractor cannot be empty!", "danger");
    setTimeout(function() {$("#save-dialog").modal();}, 500);
    return;
  }

  if (typesname === undefined || typesname.replace(/\s/g, "") === "") {
    display_message("Type name of the extractor cannot be empty!", "danger");
    setTimeout(function() {$("#save-dialog").modal();}, 500);
    return;
  }

  if (relation === undefined || relation.replace(/\s/g, "") === "") {
    display_message("Relation of the extractor cannot be empty!", "danger");
    setTimeout(function() {$("#save-dialog").modal();}, 500);
    return;
  }

  var good_patterns = encodeURI(JSON.stringify(patterns_active));

  var good_entities = getGoodEntities();

  if (patterns_active.length == 0 && entities_active.length == 0 && active_entity === "") {
    display_message("Cannot save empty extractor!", "danger");
    setTimeout(function() {$("#save-dialog").modal();}, 500);
    return;
  }

  $.getJSON("extractor/save/"+encodeURI(name)+"/"+encodeURI(typesname)+"/"+encodeURI(relation)+"/"+encodeURI(description)+"/"+encodePattern(good_patterns)+"/"+good_entities).done(function(response) {
    if (response === "success") {
      display_message("The extractor was saved successfully!", "success");
    } else if (response === "unique") {
      display_message("An extractor with the same name already exists!", "danger");
    } else {
      display_message("An unknown error occurred!");
    }
  });
}

function searchFreebaseEntity(selectZero) {
  selectZero = typeof selectZero !== 'undefined' ? selectZero : false;

  var query = $('#entity-search').val();
  console.log(query);
  $.getJSON("https://www.googleapis.com/freebase/v1/search?query=" + query + "&limit=5").done(function(data) {
    if (data.status === "200 OK") {
      console.log(data.result);
      var html = '<div class="list-group">';
      if (data.result.length == 0) {
        display_message("Could not find any freebase entities for query '"+query+"'");
        return;
      }
      freebase_entities = [];
      active_subject = "";
      for (var i = 0; i < data.result.length; i++) {
        if (data.result[i].name === undefined || data.result[i].name === "")
          continue;
        freebase_entities.push(data.result[i].mid);
        if (data.result[i].notable !== undefined)
          html += '<a id="entity'+i+'" class="list-group-item" style="height: 30px; padding-top: 7px; cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;" ondblclick="activateResult(this);">'+data.result[i].name+' ('+data.result[i].notable.name+')</a>';
        else
          html += '<a id="entity'+i+'" class="list-group-item" style="height: 30px; padding-top: 7px; cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;" ondblclick="activateResult(this);">'+data.result[i].name+'</a>';
      }
      html += "</div>";
      $("#entity-results")[0].innerHTML = html;
      if (selectZero)
        $("#entity0").dblclick();
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
  active_entity = freebase_entities[parseInt(obid.replace("entity", ""))];
  queryInfo();
}

function inspectOutput(i) {

  var entity = output[i].id;
  var elements = [];
  for (var x = 0; x < output.length; x++) {
    if (output[x].id === entity) {
      elements.push(output[x]);
    }
  }

  var html = "";

  html += '<p><b>Entity:</b> ';
  var displayed = [];
  for (var x = 0; x < elements.length; x++) {
    if (displayed.indexOf(elements[x].entity) === -1)
      displayed.push(elements[x].entity);
  }
  for (var x = 0; x < displayed.length; x++) {
    html += '<span style="font-variant: small-caps;">' + displayed[x].replace(/_/g, " ") + "</span>";
    if (x !== displayed.length-1)
      html += ' <span style="font-size: 9px;">or</span> ';
  }
  html += '</p>';
  html += '<p style="font-size: 10px; max-height: 60px; overflow-y: auto; border: 1px solid black; padding: 5px;"><b>Types:</b> ' + output[i].types.join(", ") + '</p>';

  html += '<p><b>Patterns:</b></p>';
  html += '<ul>'

  displayed = [];
  for (var x = 0; x < elements.length; x++) {
    var patternSplit = elements[x].patterns;
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

  $.getJSON("entity/sentence/"+encodeURI(encodePattern(output[i].patterns[0]))+"/"+encodeURI(JSON.stringify(["__"+output[i].id+"__"]))+"/limit/10").done(function(data) {
    var html2 = "";
    if (data.length == 0)
      html2 += '<p>'+output[i].sentence+'</p>';
    for (var z = 0; z < data.length; z++) {
      html2 += "<p>" + data[z][0] + '</p>';
    }
    $("#example-sentences")[0].innerHTML = html2;
  });
}

function permaLink() {
  var encodePats = encodeURI(JSON.stringify(patterns_active));

  var encodeEntities = getGoodEntities();

  window.location.href = "entity.html?patterns=" + encodePats + "&entities=" + encodeEntities;
}

function download_result() {
  var good_patterns = encodeURI(JSON.stringify(patterns_active));

  var good_entities = getGoodEntities();

  location.href = "/entity/output/"+encodePattern(good_patterns)+"/"+good_entities+"/results.txt";
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

  //===== ENTITIES ======
  html = '';
  if (entities_active.length > 0) {
    html += '<b>Active:</b><div class="list-group">';
    for (var i = 0; i < entities_active.length; i++) {
      html += '<a class="list-group-item active" style="height: 30px; padding-top: 7px; cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;" ondblclick="toggleEntity(this);">'+entities_active[i]+'</a>';
    }
    html += '</div>';
  }
  if (entities.length > 0) {
    html += '<div class="list-group">';
    for (var i = 0; i < entities.length; i++) {
      if ((!show_all_entities && i > MIN_DISPLAY) || i > MAX_DISPLAY)
        break;
      html += '<a class="list-group-item" style="height: 30px; padding-top: 7px; cursor: pointer; -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;" ondblclick="toggleEntity(this);">'+entities[i]+'</a>';
    }
    html += '</div>';
    if (show_all_entities)
      html += '<p style="margin-top: 10px; font-size: 11px;" class="text-center"><a style="cursor: pointer;" onclick="show_all_entities = false; updateView();">Less</a></p>';
    else
      html += '<p style="margin-top: 10px; font-size: 11px;" class="text-center"><a style="cursor: pointer;" onclick="show_all_entities = true; updateView();">More</a></p>';
  }
  $("#entities")[0].innerHTML = html;

  //===== OUTPUT =====
  html = '';
  if (output.length > 0) {
    var usedIDs = [];
    for (var i = 0; i < output.length; i++) {
      if ((!show_all_output && i > MIN_DISPLAY) || i > MAX_DISPLAY)
        break;
      if (usedIDs.indexOf(output[i].id) == -1) {
        html += "<tr>";
        html += '<td><a target="blank" href="http://freebase.com/m/'+output[i].id+'">'+output[i].entity.replace(/_/g, " ")+"</a></td>";
        html += '<td style="font-size: 10px;">'+output[i].sentence+"</td>";
        html += '<td><a style="cursor: pointer;" onclick="inspectOutput('+i+');"><span class="glyphicon glyphicon-search"></span></a></td>';
        html += "</tr>";
        usedIDs.push(output[i].id);
      }
    }
    if (show_all_output)
      html += '<tr><td colspan="3" class="text-center"><a style="cursor: pointer;" onclick="show_all_output = false; updateView();">Less</a></td></tr>';
    else
      html += '<tr><td colspan="3" class="text-center"><a style="cursor: pointer;" onclick="show_all_output = true; updateView();">More</a></td></tr>';
  }
  $("#results")[0].innerHTML = html;

}