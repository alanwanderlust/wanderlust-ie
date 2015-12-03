var mode = "SUBJ";
var currentText = "";

function evaluateText(text) {
	currentText = text.split(" ");
	for (var i = 0; i < currentText.length; i++) {
		$('#select-subj')[0].innerHTML += '<span id="word-'+i+'"style="cursor:pointer; display: inline-block;" onclick="changeState(this);">'+currentText[i]+'<div class="hidden" style="font-size: 10px; float:left; position: relative; left: 10%; width: 80%; border-top: 1px dotted;">Subject</div><div class="hidden" style="font-size: 10px; float:left; position: relative; left:10%; width: 80%; border-top: 1px dotted;">Object</div></span> ';
	}
	$('#input-mask').fadeOut('fast', function() {
		$('#sub-select').fadeIn('fast');
	});
}

function next() {
	if (mode == "SUBJ") {
		$('#sub-ob')[0].innerHTML = "object";
		mode = "OBJ";
		$('#next-btn').removeClass("btn-default").addClass("btn-primary");
	} else {
		var subject = "";
		for (var i = 0; i < currentText.length; i++) {
			if ($('#word-'+i).hasClass("text-success"))
				subject += currentText[i] + " ";
		}
		subject = $.trim(subject);

		var object = "";
		for (var i = 0; i < currentText.length; i++) {
			if ($('#word-'+i).hasClass("text-danger"))
				object += currentText[i] + " ";
		}
		object = $.trim(object);

		if (!subject || !object) {
			$('#error').fadeIn("normal", function() {
				setTimeout(function() {
					$('#error').fadeOut("normal");
				}, 2000);
			});
			return;
		}

		console.log(currentText.join(" "));
		console.log(subject);
		console.log(object);
		window.location.href="/pattern.html?sentence="+encodeURI(currentText.join(" "))+"&subject="+encodeURI(subject)+"&object="+encodeURI(object);
	}

}

function changeState(element) {
	if (mode == "SUBJ") {
		if (!$(element).hasClass("text-danger")) {
			$(element).toggleClass("text-success");
			$($(element).children().get(0)).toggleClass("hidden");
		}
	} else if (!$(element).hasClass("text-success")) {
		$(element).toggleClass("text-danger");
		$($(element).children().get(1)).toggleClass("hidden")
	}
}

function reset() {
	if (mode == "SUBJ") {
		$('#sub-select').fadeOut('fast', function() {
			$('#select-subj')[0].innerHTML = "";
			$('#input-mask').fadeIn('fast', function() {
				$('#sentence-input').focus();
			});
		});
	} else {
		mode = 'SUBJ';
		$('#sub-ob')[0].innerHTML = "subject";
		$('#next-btn').removeClass("btn-primary").addClass("btn-default");
	}
}