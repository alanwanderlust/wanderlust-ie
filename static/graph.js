var sys;
var nodes = {};
var currentselected = {data: {selected: true}};

(function($){

  var Renderer = function(canvas){
    var canvas = $(canvas).get(0);
    var ctx = canvas.getContext("2d");
    var particleSystem;

    var that = {
      init:function(system){
        //
        // the particle system will call the init function once, right before the
        // first frame is to be drawn. it's a good place to set up the canvas and
        // to pass the canvas size to the particle system
        //
        // save a reference to the particle system for use in the .redraw() loop
        particleSystem = system

        // inform the system of the screen dimensions so it can map coords for us.
        // if the canvas is ever resized, screenSize should be called again with
        // the new dimensions
        particleSystem.screenSize(canvas.width, canvas.height)
        particleSystem.screenPadding(80) // leave an extra 80px of whitespace per side

        // set up some event handlers to allow for node-dragging
        that.initMouseHandling()
      },

      redraw:function(){
        //
        // redraw will be called repeatedly during the run whenever the node positions
        // change. the new positions for the nodes can be accessed by looking at the
        // .p attribute of a given node. however the p.x & p.y values are in the coordinates
        // of the particle system rather than the screen. you can either map them to
        // the screen yourself, or use the convenience iterators .eachNode (and .eachEdge)
        // which allow you to step through the actual node objects but also pass an
        // x,y point in the screen's coordinate system
        //
        ctx.fillStyle = "#fafafa";
        ctx.fillRect(0,0, canvas.width, canvas.height)

        particleSystem.eachEdge(function(edge, pt1, pt2){
          // edge: {source:Node, target:Node, length:#, data:{}}
          // pt1:  {x:#, y:#}  source position in screen coords
          // pt2:  {x:#, y:#}  target position in screen coords

          // draw a line from pt1 to pt2
          if (edge.source.data.selected || edge.target.data.selected)
            ctx.strokeStyle = "rgba(255,0,0,.666)";
          else
            ctx.strokeStyle = "rgba(0,0,0, .333)";
          ctx.lineWidth = 1;
          ctx.beginPath();
          ctx.moveTo(pt1.x, pt1.y);
          ctx.lineTo(pt2.x, pt2.y);
          ctx.stroke();

          ctx.fillStyle = "#000";
          var text = edge.data.relation.relation;
          drawText(text, (pt2.x+pt1.x)/2, (pt2.y+pt1.y)/2, ctx);
        })

        particleSystem.eachNode(function(node, pt){
          // node: {mass:#, p:{x,y}, name:"", data:{}}
          // pt:   {x:#, y:#}  node position in screen coords

          // draw a rectangle centered at pt
          var w = 15;
          if (node.data.selected) {
            w = 30;
          }

          ctx.beginPath();
          ctx.arc(pt.x, pt.y, w, 0, 2*Math.PI, false);
          ctx.fillStyle = "#428bca";
          for (var i = 0; i < nodes[node.name].length; i++) {
            if (nodes[node.name][i].objtype === "entity") {
              ctx.fillStyle = "#468847";
              break;
            }
          }
          ctx.fill();
          ctx.lineWidth = node.data.selected ? 2 : 1;
          ctx.strokeStyle = "#000";
          ctx.stroke();

          var txtfnt = node.data.selected ? "12pt Arial" : "10pt Arial";
          drawText(node.name, pt.x, pt.y + w + 15, ctx, txtfnt);
        })
      },

      initMouseHandling:function(){
        // no-nonsense drag and drop (thanks springy.js)
        var dragged = null;

        // set up a handler object that will initially listen for mousedowns then
        // for moves and mouseups while dragging
        var handler = {
          clicked:function(e){
            var pos = $(canvas).offset();
            _mouseP = arbor.Point(e.pageX-pos.left, e.pageY-pos.top)
            dragged = particleSystem.nearest(_mouseP);

            tmpposx = dragged.point.x;
            tmpposy = dragged.point.y;

            if (dragged && dragged.node !== null){
              // while we're dragging, don't let physics move the node
              dragged.node.fixed = true
            }

            $(canvas).bind('mousemove', handler.dragged)
            $(window).bind('mouseup', handler.dropped)

            return false
          },
          dragged:function(e){
            var pos = $(canvas).offset();
            var s = arbor.Point(e.pageX-pos.left, e.pageY-pos.top)

            if (dragged && dragged.node !== null){
              var p = particleSystem.fromScreen(s)
              dragged.node.p = p
            }

            return false
          },

          dropped:function(e){
            if (dragged===null || dragged.node===undefined) return
            if (dragged.node !== null) dragged.node.fixed = false
            dragged.node.tempMass = 1000;

          // console.log(Math.abs(tmpposx - dragged.point.x));
          // console.log(Math.abs(tmpposy - dragged.point.y));

            if (Math.abs(tmpposx - dragged.point.x) < 0.1 && Math.abs(tmpposy - dragged.point.y) < 0.1) {
                //if (dragged.node.data.np !== true)
                currentselected.data.selected = false;
                currentselected.data.mass = 0.5;

                dragged.node.data.selected = true;
                dragged.node.data.mass = 5;

                currentselected = dragged.node;
                displayInfo(dragged.node.name);
            }

            dragged = null
            $(canvas).unbind('mousemove', handler.dragged)
            $(window).unbind('mouseup', handler.dropped)
            _mouseP = null
            return false
          }
        }

        // start listening
        $(canvas).mousedown(handler.clicked);

      },

    }
    return that
  }

  $(document).ready(function(){
    sys = arbor.ParticleSystem(1000, 600, 0.5); // create the system with sensible repulsion/stiffness/friction
    sys.parameters({gravity:true}); // use center-gravity to make the graph settle nicely (ymmv)
    sys.renderer = Renderer("#viewport"); // our newly created renderer will have its .init() method called shortly by sys...

    $.getJSON("extractor/relation/all").done(function(result) {
      if (result.length == 0) return;

      for (var i = 0; i < result.length; i++) {
        result[i].objtype = "relation";
        if (nodes[result[i].xname] === undefined) {
          sys.addNode(result[i].xname, {mass:.5, relation: result[i]});
          nodes[result[i].xname] = [result[i]];
        } else {
          nodes[result[i].xname].push(result[i]);
        }

        if (nodes[result[i].yname] === undefined) {
          sys.addNode(result[i].yname, {mass:.5, relation: result[i]});
          nodes[result[i].yname] = [result[i]];
        } else {
          nodes[result[i].yname].push(result[i]);
        }

        sys.addEdge(result[i].xname, result[i].yname, {relation: result[i], type: "relation"});
      }
    });

    $.getJSON("extractor/entity/all").done(function(result) {
      if (result.length == 0) return;

      for (var i = 0; i < result.length; i++) {
        result[i].objtype = "entity";
        if (nodes[result[i].typesname] === undefined) {
          sys.addNode(result[i].typesname, {mass:.5, relation: result[i]});
          nodes[result[i].typesname] = [result[i]];
        } else {
          nodes[result[i].typesname].push(result[i]);
        }
      }
    });

    // add some nodes to the graph and watch it go...
    // sys.addNode('thing1', {mass:.25, schmuh: "short"})
    // sys.addNode('thing2', {mass:.25, schmuh: ""})
    // sys.addEdge('thing1', 'thing2');

    // or, equivalently:
    //
    // sys.graft({
    //   nodes:{
    //     f:{alone:true, mass:.25}
    //   },
    //   edges:{
    //     a:{ b:{},
    //         c:{},
    //         d:{},
    //         e:{}
    //     }
    //   }
    // })

  });

})(this.jQuery);

function drawText(text, xpos, ypos, ctx, font) {
  font = typeof font !== 'undefined' ? font : "10pt Arial";
  ctx.font = font;
  ctx.fillStyle = "rgba(255,255,255,0.2)";
  ctx.fillText(text, xpos - (text.length*3.9) -2, ypos-2);
  ctx.fillText(text, xpos - (text.length*3.9) +2, ypos+2);
  ctx.fillStyle = "rgba(255,255,255,0.5)";
  ctx.fillText(text, xpos - (text.length*3.9) -1, ypos-1);
  ctx.fillText(text, xpos - (text.length*3.9) +1, ypos+1);
  ctx.fillStyle = "#000";
  ctx.fillText(text, xpos - (text.length*3.9), ypos);
}

function displayInfo(entity) {

  var extractors = nodes[entity];

  $("#selected-name").text(entity)

  $("#selected-content").empty();

  var thing = "text-primary";
  for (var i = 0; i < extractors.length; i++) {
    if (extractors[i].objtype === "entity") {
      thing = "text-success";
      break;
    }
  }
  $("#selected-content").append($('<h2 class="' + thing +'" style="text-align: center; margin-bottom: 30px;">'+entity+'</h2>'));

  var xtrname = "";
  if (extractors[0].objtype === "relation") {
    if (extractors[0].xname === entity) {
      xtrname = "@@" + extractors[0].name + ".subjects@@";
    } else {
      xtrname = "@@" + extractors[0].name + ".objects@@";
    }
  } else {
    xtrname = "@@" + extractors[0].name + ".entities@@";
  }
  xtrname = [xtrname];

  var html = '<label>Create a new extractor based on "' +entity+ '":</label><div style="text-align: center; margin-bottom: 30px;">';
  html += '<a href="relation.html?patterns=%5B%5D&subjects='+encodeURI(JSON.stringify(xtrname))+'&objects=%5B%5D" class="btn btn-success" style="margin-left: 5px; margin-bottom: 5px;">Use '+entity+' as subject</a>';
  html += '<a href="relation.html?patterns=%5B%5D&objects='+encodeURI(JSON.stringify(xtrname))+'&subjects=%5B%5D" class="btn btn-danger" style="margin-left: 5px; margin-bottom: 5px;">Use '+entity+' as object</a>';
  html += '<a href="entity.html?patterns=%5B%5D&entities='+encodeURI(JSON.stringify(xtrname))+'" class="btn btn-primary" style="margin-left: 5px;  margin-bottom: 5px;">Use '+entity+' as entity</a>';
  html += '</div>';

  $("#selected-content").append($(html));

  $("#selected-content").append($("<label>Used in the following extractors: </label>"));
  html = '<div class="panel-group" id="selected-extractors">';
  for (var i = 0; i < extractors.length; i++) {
    if (extractors[i].objtype === "relation") {
      html += '<div class="panel panel-primary"><div class="panel-heading">';
      html += '<div style="float: right;"><a style="color: #fff;" href="relation.html?patterns='+encodeURI(extractors[i].patterns)+'&subjects='+encodeURI(extractors[i].xtypes)+'&objects='+encodeURI(extractors[i].ytypes)+'"><span class="glyphicon glyphicon-link"></span></a></div>';
      html += '<h4 class="panel-title"><a data-toggle="collapse" data-parent="#selected-extractors" href="#'+extractors[i].name.replace(/ /g, "")+'">';
      html += extractors[i].name;
      html += '</a></h4></div><div id="'+extractors[i].name.replace(/ /g, "")+'" class="panel-collapse collapse"><div class="panel-body">';

      html += '<label style="color: #aaa; font-weight: normal;">Extractor name:</label><h4>' + extractors[i].name + '</h4>';
      html += '<br>';
      html += '<label style="color: #aaa; font-weight: normal;">Description:</label><p>' + extractors[i].description + '</p>';

      html += "<br>"

      if (extractors[i].xtypes.indexOf("@@") !== -1) {
        var xtractor = JSON.parse(extractors[i].xtypes)[0].replace(/@@/g, "").split(".");
        html += '<label style="color: #aaa; font-weight: normal;">Subjects:</label><h4>' + extractors[i].xname + '</h4><p>' + xtractor[1] + ' of extractor <a data-toggle="collapse" data-parent="#selected-extractors" href="#'+xtractor[0].replace(/ /g, "")+'">' + xtractor[0] + '</a></p>';
      } else if (extractors[i].xtypes.indexOf("__") !== -1) {
        var xtractor = JSON.parse(extractors[i].xtypes)[0].replace(/__/g, "");
        html += '<label style="color: #aaa; font-weight: normal;">Entities:</label><h4>' + extractors[i].xname + '</h4><p>Freebase entity <a href="http://freebase.com/m/'+xtractor+'">'+xtractor+'</a></p>';
      } else {
        html += '<label style="color: #aaa; font-weight: normal;">Subjects:</label><h4>' + extractors[i].xname + '</h4><p>' + JSON.parse(extractors[i].xtypes).join(", ") + '</p>';
      }

      if (extractors[i].ytypes.indexOf("@@") !== -1) {
        var xtractor = JSON.parse(extractors[i].ytypes)[0].replace(/@@/g, "").split(".");
        html += '<label style="color: #aaa; font-weight: normal;">Objects:</label><h4>' + extractors[i].yname + '</h4><p>' + xtractor[1] + ' of extractor <a data-toggle="collapse" data-parent="#selected-extractors" href="#'+xtractor[0].replace(/ /g, "")+'">' + xtractor[0] + '</a></p>';
      } else if (extractors[i].ytypes.indexOf("__") !== -1) {
        var xtractor = JSON.parse(extractors[i].ytypes)[0].replace(/__/g, "");
        html += '<label style="color: #aaa; font-weight: normal;">Entities:</label><h4>' + extractors[i].yname + '</h4><p>Freebase entity <a href="http://freebase.com/m/'+xtractor+'">'+xtractor+'</a></p>';
      } else {
        html += '<label style="color: #aaa; font-weight: normal;">Objects:</label><h4>' + extractors[i].yname + '</h4><p>' + JSON.parse(extractors[i].ytypes).join(", ") + '</p>';
      }

      html += '<label style="color: #aaa; font-weight: normal;">Patterns:</label><ul><li>';
      html += JSON.parse(extractors[i].patterns).join("</li><li>")
      html += "</li></ul>";

      html += '</div></div></div>';
    } else {
      html += '<div class="panel panel-success"><div class="panel-heading">';
      html += '<div style="float: right;"><a style="color: #468847;" href="entity.html?patterns='+encodeURI(extractors[i].patterns)+'&entities='+encodeURI(extractors[i].types)+'"><span class="glyphicon glyphicon-link"></span></a></div>';
      html += '<h4 class="panel-title"><a data-toggle="collapse" data-parent="#selected-extractors" href="#'+extractors[i].name.replace(/ /g, "")+'">';
      html += extractors[i].name;
      html += '</a></h4></div><div id="'+extractors[i].name.replace(/ /g, "")+'" class="panel-collapse collapse"><div class="panel-body">';

      html += '<label style="color: #aaa; font-weight: normal;">Extractor name:</label><h4>' + extractors[i].name + '</h4>';
      html += '<br>';
      html += '<label style="color: #aaa; font-weight: normal;">Description:</label><p>' + extractors[i].description + '</p>';

      html += "<br>"

      if (extractors[i].types.indexOf("@@") !== -1) {
        var xtractor = JSON.parse(extractors[i].types)[0].replace(/@@/g, "").split(".");
        html += '<label style="color: #aaa; font-weight: normal;">Entities:</label><h4>' + extractors[i].typesname + '</h4><p>' + xtractor[1] + ' of extractor <a data-toggle="collapse" data-parent="#selected-extractors" href="#'+xtractor[0].replace(/ /g, "")+'">' + xtractor[0] + '</a></p>';
      } else if (extractors[i].types.indexOf("__") !== -1) {
        var xtractor = JSON.parse(extractors[i].types)[0].replace(/__/g, "");
        html += '<label style="color: #aaa; font-weight: normal;">Entities:</label><h4>' + extractors[i].typesname + '</h4><p>Freebase entity <a href="http://freebase.com/m/'+xtractor+'">'+xtractor+'</a></p>';
      } else {
        html += '<label style="color: #aaa; font-weight: normal;">Entities:</label><h4>' + extractors[i].typesname + '</h4><p>' + JSON.parse(extractors[i].types).join(", ") + '</p>';
      }

      html += '<label style="color: #aaa; font-weight: normal;">Patterns:</label><ul><li>';
      html += JSON.parse(extractors[i].patterns).join("</li><li>")
      html += "</li></ul>";

      html += '</div></div></div>';
    }
  }
  html += "</div>";
  $("#selected-content").append($(html));

}