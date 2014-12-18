// HEJ--
var Utils = {};
	
Utils.hideme = function( id ) {
	$( "#" + id ).hide( "slow" );
}

Utils.startupload = function( nodeid, append ) {
	$.getJSON('/upload?nodeid=' + nodeid, function(data) {
		if( data.ratio < 1.0 ) {
			$("#status"+append).width( Math.floor( data.ratio * 100 ) + "%" );
			setTimeout( function() { Utils.startupload( nodeid, append ); }, 1000 );
		} else {
			$("#status"+append).width( "100%" );
			$("#nodeid"+append).val( nodeid );
		}
	});
}

Utils.getProgress = function( resourceId, element, p ) {
    $('#' + element).hide();
    $('#' + p).show();
    Utils.updateProgress( resourceId, p );
}

Utils.updateProgress = function( resourceId, element ) {
    $.get( '/resource/' + resourceId + '/progress', function( data ) {
            $('#' + element).width( ( data * 100 ) + '%' );

            if( data < 1.0 ) {
                setTimeout( function() {
                    Utils.updateProgress( resourceId, element );
                }, 500)
            } else if( data == 1.0 ) {
                $('#' + element).html( "Upload done" );
                $('#' + element).css( 'background-color', '#ffffff' );
            }
        }
    )
}

Utils.onChangeYear = function(element) {
    if(element.value.length  > 0) {
        var pattern = new RegExp("(19|20)\\d{2}");
        if( pattern.test(element.value) ) {
            return true;
        } else {
            alert("Illegal year, reverting to 2000.");
            element.value = 2000;
            return false;
        }
    } else {
        return true;
    }
}

Utils.disableOnChecked = function(element, inputNames) {
    var disable = false;
    if( $(element).is(":checked") ) {
        disable = true;
    }
    for(i in inputNames) {
        var inputName = inputNames[i];
        $("form [name="+inputName+"]").prop('disabled', disable);
    }
}

Utils.popupselect = function( myurl, height ) {
		var w1 = screen.width * 0.9;
		var w2 = screen.width * 0.05;
		var h1 = screen.height * 0.45;
		
		a = window.open( myurl, '', "width=" + w1+ ",height=" + height + ",status=no,toolbar=no,menubar=no,dependent=yes,directories=no,hotkeys=no,scrollbars=yes,location=no,resizable=no,screenX=" + w2 + ",screenY=" + h1 );
}

Utils.addJsonElement = function( form ) {
	var jsonData = {};
    var d = new Dispatch();
    d.dispatch(form, jsonData);
	alert( "JSON: " + JSON.stringify( jsonData ) );
	var element   = document.createElement( 'input' );
	element.name  = "json";
	element.type  = "hidden";
	element.value = JSON.stringify( jsonData );
	form.appendChild( element );
}

Utils.getJsonElement = function( form ) {
	var jsonData = {};
    var d = new Dispatch();
    d.dispatch(form, jsonData);
	alert( "JSON: " + JSON.stringify( jsonData ) );
	return jsonData;
}

Utils.injectCredentials = function(json) {
	var session = Utils.getCookie('session');
	json['session'] = session;
}

Utils.getCookie = function(cname) {
    var name = cname + "=";
    var ca = document.cookie.split(';');
    for(var i=0; i<ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1);
        if (c.indexOf(name) != -1) return c.substring(name.length,c.length);
    }
    return "";
} 

Utils.injectJson = function(form, json) {
	var element   = document.createElement( 'input' );
	element.name  = "json";
	element.type  = "hidden";
	element.value = JSON.stringify( jsonData );
	form.appendChild( element );
}

Utils.getAttribute = function( e, a ) {
    if(e.attributes == undefined ) {
        return "";
    } else {
        for( i = 0 ; i < e.attributes.length ; i++ ) {
            if(e.attributes.item(i).name == a ) {
                return e.attributes.item(i).nodeValue;
            }
        }

        return e.title;
    }
}

// @deprecated
Utils.getJsonFromForm = function( e, jsonData ) {
    var childs = e.childNodes;

    for( var i = 0 ; i < childs.length ; i++ ) {
        //alert( "Child: " + childs[i] + ", TAG:" + childs[i].tagName + ", nodename:" + childs[i].name );
        var name = Utils.getAttribute(childs[i], "name");
        var tag = childs[i].tagName;
        var id = childs[i].id;

        if( childs[i].tagName != undefined && name.length > 0 ) {

            // Check class disabled
            if($(childs[i]).hasClass("disabledTarget")) {
                continue;
            }

            //alert( "NAME=" + name + ", " + childs[i].tagName );
            switch( childs[i].tagName ) {
                case "INPUT":
                    //alert("INPUT: " + childs[i].name + "=" + childs[i].value + ", type: " + childs[i].type);
                    if( childs[i].type == "radio" ) { // Special radio button case
                        if( childs[i].checked ) {
                            jsonData[childs[i].name] = childs[i].value;
                        }
                    } else if( childs[i].type == "checkbox" ) {
                        if( childs[i].checked ) {
                            jsonData[childs[i].name] = childs[i].value;
                        } else {
                            jsonData[childs[i].name] = null;
                        }
                    } else {
                        jsonData[childs[i].name] = childs[i].value;
                    }
                    break;

                case "TEXTAREA":
                    //alert("TEXT AREA: " + name)
                    jsonData[childs[i].name] = childs[i].value;
                    break;

                case "SELECT":
                    //alert( "SELECT: " + childs[i].name + "=" + childs[i].value + ", type: " + childs[i].type );
                    if( childs[i].type == "select-one" ) {
                        jsonData[childs[i].name] = childs[i].value;
                    } else {
                        //alert( "Debug: " + debug( childs[i] ) );
                        jsonData[childs[i].name] = {};
                        for( var j = 0 ; j < childs[i].length ; j++ ) {
                            //alert(j+ "= "+ childs[i][j].value );
                            var t = jsonData[childs[i].name];
                            if( childs[i][j].selected ) {
                                t[j] = childs[i][j].value;
                            }
                        }
                    }

                    break;
                case "DIV":
                    //alert("DIV: " + name + "=" + $(childs[i]).html() );
                    //if( $(childs[i]).is( ':visible' ) || $(childs[i]).hasClass("rootConfiguration") ) {
                    if($(childs[i]).hasClass("arrayConfiguration") ) {
                        //alert("Root: " + name);
                        if($(childs[i]).hasClass("objects")) {
                            if( jsonData[name] == undefined ) {
                                jsonData[name] = {};
                            }
                            Utils.getJsonFromForm( childs[i], jsonData[name] );
                        } else {
                            if( jsonData[name] == undefined ) {
                                jsonData[name] = [];
                            }
                            var j = jsonData[name].push( {} );
                            Utils.getJsonFromForm( childs[i], jsonData[name][j-1] );
                        }
                        //alert( JSON.stringify( jsonData[name][j-1] ) );
                    }

                    //if($(childs[i]).hasClass("targetValue")) {
                    if($(childs[i]).hasClass("targetValue")) {
                        //jsonData[childs[i].name] = childs[i].value;
                        //alert("TARGET: " + name + ", " + childs[i].innerHTML);
                        //Utils.getJsonFromForm( childs[i], jsonData );
                        //jsonData[name].push( childs[i].innerHTML );
                        if( jsonData[name] == undefined ) {
                            jsonData[name] = [];
                        }
                        jsonData[name].push( childs[i].innerHTML );
                    } else {
                    //if( $(childs[i]).is( ':visible' ) && !$(childs[i]).hasClass("targetValue") ) {
                        //jsonData[childs[i].name] = childs[i].value;
                       // alert("DEFAULT: " + name + ", " + childs[i].innerHTML);
                        //Utils.getJsonFromForm( childs[i], jsonData );
                        //jsonData[name].push( childs[i].innerHTML );
                        if( jsonData[name] == undefined ) {
                            jsonData[name] = {};
                        }
                        Utils.getJsonFromForm( childs[i], jsonData[name] );
                    }

                    break;
            }
        }

            //alert( "Testing= " + childs[i].tagName );
        if( childs[i].tagName == 'TR' || childs[i].tagName == 'TD' || childs[i].tagName == 'TABLE' || childs[i].tagName == 'TBODY' ) {
            //alert( "IN HERE= " + $(childs[i]).is( ':visible' ) );
            Utils.getJsonFromForm( childs[i], jsonData );
        }

        if( childs[i].tagName == 'DIV' && !$(childs[i]).hasClass("disabledTarget")) {
            //if( $(childs[i]).is( ':visible' ) || $(childs[i]).hasClass("rootConfiguration") ) {
                //alert("Div parent: " + e.name + ", " + e.tagName);
                if( name.length == 0 ) {
                    Utils.getJsonFromForm( childs[i], jsonData );
                }
            //}
        }
    }

    return jsonData;
}

Utils.selectElements = function(searchUrl, subGroup, containerId, inputId) {
    $( '#' + inputId ).autocomplete({
        source: searchUrl,
        select: function( event, ui ) {
            this.value = "";
            $('#' + containerId).append('<div class="targetNode" id="' + containerId + ui.item._id + '"><div name="' + subGroup + '" class="targetValue" style="display: none">' + ui.item._id + '</div>' + ui.item.title + '</div>');
            //$('#' + containerId + ui.item._id).click(function() {
            //    $('#' + containerId + ui.item._id).hide();
            //});
            return false;
        }
    }).data( "ui-autocomplete" )._renderItem = function( ul, item ) {
        return $( "<li>" )
            .append( "<a>" + item.title + ", " + item.type + "</a>" )
            .appendTo( ul );
    };
}

Utils.selectElement = function(searchUrl, subGroup, containerId, inputId) {
    $( '#' + inputId ).autocomplete({
        source: searchUrl,
        select: function( event, ui ) {
            this.value = "";
            $('#' + containerId).html('<div class="targetNode" id="' + containerId + ui.item._id + '"><div name="' + subGroup + '" class="targetValue" style="display: none">' + ui.item._id + '</div>' + ui.item.title + '</div>');
            return false;
        }
    }).data( "ui-autocomplete" )._renderItem = function( ul, item ) {
        return $( "<li>" )
            .append( "<a>" + item.title + ", " + item.type + "</a>" )
            .appendTo( ul );
    };
}

Utils.selectElementsTest = function(searchUrl) {
    $( '#moderateInput' ).autocomplete({
        source: searchUrl,
        select: function( event, ui ) {
            this.value = "";
            $('#moderateContainer').append('<div class="targetNode" id="moderateContainer' + ui.item._id + '"><div name="moderate" class="targetValue" style="display: none">' + ui.item._id + '</div>' + ui.item.title + '</div>');
            //$('#moderateContainer' + ui.item._id).click(function() {
            //    $('#moderateContainer' + ui.item._id).hide();
            //});
            return false;
        }
    }).data( "ui-autocomplete" )._renderItem = function( ul, item ) {
        return $( "<li>" )
            .append( "<a>" + item.title + ", " + item.type + "</a>" )
            .appendTo( ul );
    };
}

Utils.toggleFormElement = function( id ) {

    var element2 = $( '#' + Utils.jqEscape( id ) );

    if( element2.is( ':visible') ) { // Disable
        element2.hide( 500 );
        element2.attr( 'data-enabled', 0 );
    } else { // Enable
        element2.show( 500 );
        element2.attr( 'data-enabled', 1 );
    }
}


Utils.jqEscape = function( str ) {
    return str.replace(/([;&,\.\+\*\~':"\!\^#$%@\[\]\(\)=>\|])/g, '\\$1');
}

Utils.fetchResourceViewAppend = function(id, container, view) {
    Utils.fetchResourceView(id, view, function(e) {
        $(container).append('<span style="padding-right:5px">' + e + '</span>');
        //alert(e);
    }, function(e){});
}

Utils.fetchResourceViewAppendLink = function(id, container, view) {
    Utils.fetchResourceView(id, view, function(e) {
        $(container).append('<a href="/resource/' + id + '/" style="padding-right:5px">' + e + '</a>');
        //alert(e);
    });
}

Utils.fetchResourceView = function(id, view, f, e) {
    $.ajax({
        type: "GET",
        url: "/resource/" + id + "/getView" + (view !== undefined ? '?view=' + view : ''),
        success: function(data, textStatus, jqxhr){
            f(data);
        },
        error: function(ajax, text, error) {e(error)}
    });
}

Utils.removeId = function(id, removeUrl) {
    $.ajax({
        type: "DELETE",
        //url: removeUrl + id + '/delete',
        url: removeUrl + id + '/',
        data: {"resource": id},
        success: function(data, textStatus, jqxhr){
            $('#top_' + id).remove()
        },
        error: function(ajax, text, error) {alert(error);}
    });
}


Utils.resourceListHandler = function(container, autoCompleteInput, inputSource, addUrl, callbackClass, ids, inputCallback) {
	if(typeof(inputCallback) === "undefined") {
		inputCallback = function(id){ return "<div style=\";float:left\" id=\"top_" + id + "\"><div class=\"newResource\" id=\"" + id + "\" style=\"\">Waiting for " + id + "</div><div name=\"ids\" class=\"targetValue\" style=\"display:none\">" + id + "</div><div onclick=\"Utils.removeId('" + id + "', 'get/')\">REMOVE</div></div>" };
	}

    $( '#' + autoCompleteInput ).autocomplete({
        source: inputSource,
        select: function( event, ui ) {
            this.value = "";
            //$('#' + container).append('<table><tr><td id="' + ui.item._id + '">Waiting ' + ui.item._id + '</td></tr></table>');
            //$('#' + container).append('<div class="" id="' + ui.item._id + '">Waiting ' + ui.item._id + '</td></tr></table>');
            $('#' + container).append(inputCallback(ui.item._id));
            if(addUrl !== undefined) {
            	add(ui.item._id);
            } else {
            	getView(ui.item._id);
            }
            return false;
        }
    }).data( "ui-autocomplete" )._renderItem = function( ul, item ) {
        return $( "<li>" )
            .append( "<a>" + item.title + ", " + item.type + "</a>" )
            .appendTo( ul );
    };

    $(function() {
        for(id in ids) {
        	$('#' + container).append(inputCallback(ids[id]));
            getView(ids[id]);
        }
    });

    function add(id) {
        $.ajax({
            type: "POST",
            url: addUrl,
            data: {"resource": id},
            success: function(data, textStatus, jqxhr){
                getView(id);
            },
            error: function(ajax, text, error) {alert(error); $('#' + id + '.newResource').remove()}
        });
    }

    function getView(id) {
        $.ajax({
            type: "GET",
            url: "/resource/" + id + "/getView?view=wide",
            success: function(data, textStatus, jqxhr){
                //$('#' + id).html(data);
            	$('#' + id).html(data);
            },
            error: function(ajax, text, error) {alert(error)}
        });
    }

}

/*
Utils.getConversation = function(id) {
	function insertComments(data) {
		alert(id + ", here " + data);
	    var json = JSON.parse(data);
		var firstLevel = json[id];
		if(firstLevel !== undefined) {
	        for( i = 0 ; i < firstLevel.length ; i++ ) {
	          	$("#" + firstLevel[i].document.parent + "-conversation").append(firstLevel[i].document.view + "<br>");
	          	//addSubComments(json, firstLevel[i].document._id);
	        }
	    }
	}
	
	$.ajax({
        type: "GET",
        url: "/resource/" + id + "/getComments",
        success: function(data, textStatus, jqxhr){insertComments(data)},
        error: function(ajax, text, error) {alert(error)}
    });
}
*/

// Just for conversations
/*
$(document).on("click", '.replyable', function(event) { 
    var p = $(this).parent().parent().attr('id');
    var pp = '#' + p + ' > .reply';
   
    $(pp).toggle();
	event.preventDefault();
    return false;
});


// Submit a new conversation
$( "#conversationSubmit" ).click(function(event) {
    event.preventDefault();
    var form = $('#conversationForm');
    //alert("adawd"+form.attr('id'));
    Utils.addJsonElement( form[0] );
    $.ajax({
        type: "POST",
        url: "addConversation",
        data: form.serialize(),
        //success: function(data, textStatus, jqxhr){$(this).parent()[0].reset();addPost(data)},
        success: function(data, textStatus, jqxhr){addConversation(data)},
        error: function(ajax, text, error) {alert(error)}
    });
});


$(document).on("click", '.commentSubmit', function(event) {
	alert("HEY");
    event.preventDefault();
    //Utils.addJsonElement( document.getElementById('commentForm') );
    var form = $(this).parent();
    //alert("adawd"+form.attr('id'));
    Utils.addJsonElement( form[0] );
    $.ajax({
        type: "POST",
        url: "addComment",
        data: form.serialize(),
        //success: function(data, textStatus, jqxhr){$(this).parent()[0].reset();addPost(data)},
        success: function(data, textStatus, jqxhr){Utils.addComment(data)},
        error: function(ajax, text, error) {alert(error)}
    });
});
*/
/*
Utils.addComment = function(d) {
	//alert("--->" + d);
    var json = eval("(" + d + ")");
    //$("#commentsContainer").append(json.view + "<br>").children(':last').hide().fadeIn(2000);
    //$(json.view + "<br>").appendTo("#commentsContainer").hide().fadeIn(2000);
    $(json.view + "<br>").appendTo("#" + json.parent + "-container").hide().fadeIn(2000);
}
*/