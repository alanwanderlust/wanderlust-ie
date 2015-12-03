var tourStep = 0;

function takeStep() {

  if (tourStep == 0 || tourStep == 10) $("#tour-buttons").addClass("hidden");
  else $("#tour-buttons").removeClass("hidden");

  $("#tour-texts").children().addClass("hidden");
  $("#tour-text-"+tourStep).removeClass("hidden");

  $(".col-md-3").css("z-index", '');
  $(".col-md-6").css("z-index", '');
  $(".panel-primary").last().css("z-index", '');


  switch (tourStep) {
    case 1:
      objects_active = [];
      subjects_active = []
      patterns_active = [];
      updateView();
      break;
    case 2:
      //The x-type and y-type boxes
      $(".col-md-3").first().css("z-index", "9998");
      $(".col-md-3").last().css("z-index", "9998");
      break;
    case 3:
      subjects_active = [];
      $(".col-md-3").first().css("z-index", "9998");
      $(".col-md-3").last().css("z-index", "9998");
      $("#subject-searcher").val("software");
      searchSubject();
      break;
    case 4:
      subjects_active = [];
      objects_active = [];
      $(".col-md-3").first().css("z-index", "9998");
      $(".col-md-3").last().css("z-index", "9998");
      $("#subject-searcher").val("software");
      searchSubject();
      $("#subjects div a").first().dblclick();
      break;
    case 5:
      objects_active = [];
      $(".col-md-3").first().css("z-index", "9998");
      $(".col-md-3").last().css("z-index", "9998");
      $("#object-searcher").val("file");
      searchObject();
      $("#objects div a").first().dblclick();
      break;
    case 6:
      $(".panel-primary").first().css("z-index", "9998");
      break;
    case 7:
      $(".col-md-6").first().css("z-index", "9998");
      break;
    case 8:
      $(".col-md-6").first().css("z-index", "9998");
      $("#patterns .panel-group").children().first().children().first().click();
      break;
    case 9:
      $(".col-md-6").first().css("z-index", "9998");
      $(".panel-primary").first().css("z-index", "9998");
      $("#patterns .panel-group").children().first().children().first().dblclick();
      break;
    case 11:
      $('#overlay').addClass('hidden');
      tourStep = 0;
      break;
  }
}

function setCookie(c_name,value,exdays) {
  var exdate=new Date();
  exdate.setDate(exdate.getDate() + exdays);
  var c_value=escape(value) + ((exdays===null) ? "" : "; expires="+exdate.toUTCString());
  document.cookie=c_name + "=" + c_value;
}

function getCookie(c_name) {
  var c_value = document.cookie;
  var c_start = c_value.indexOf(" " + c_name + "=");
  if (c_start == -1) {
    c_start = c_value.indexOf(c_name + "=");
  }
  if (c_start == -1) {
    c_value = null;
  } else {
    c_start = c_value.indexOf("=", c_start) + 1;
    var c_end = c_value.indexOf(";", c_start);
    if (c_end == -1) {
      c_end = c_value.length;
    }
    c_value = unescape(c_value.substring(c_start,c_end));
  }
  return c_value;
}

$(function() {
  if (getCookie("ereTour") !== "true") {
    $("#overlay").removeClass("hidden");
    setCookie("ereTour", "true");
  }
})