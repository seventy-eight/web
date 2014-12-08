
function Conversations(resourceId_, container_) {
	var THIS = this;
	this.resourceId = resourceId_;
	this.container = container_ !== undefined ? container_ : this.resourceId + "-conversations";
}

Conversations.prototype.get = function() {
	//alert("Fetching " + this.resourceId);
	var THIS = this;
    $.ajax({
        type: "GET",
        url: "/resource/" + this.resourceId + "/conversations/getAll",
        success: function(data, textStatus, jqxhr){THIS.insertConversations(JSON.parse(data))},
        error: function(ajax, text, error) {alert(error)}
    });
}

Conversations.prototype.insertConversations = function(json) {
	var THIS = this;
	for(i in json) {
		//$("#" + this.container).append(json[i].document.view + "<br>");
		//this.insertConversation(json[i].document);
	    $.ajax({
	        type: "GET",
	        url: "/resource/" + this.resourceId + "/conversations/get/" + json[i].document._id + "/getView?view=view",
	        //success: function(data, textStatus, jqxhr){alert(data);THIS.insertConversation(JSON.parse(data))},
	        success: function(data, textStatus, jqxhr){THIS.insertConversation(data)},
	        error: function(ajax, text, error) {alert(error)}
	    });
	}
}

Conversations.prototype.insertConversation = function(view) {
	$("#" + this.container).append(view + "<br>");
}

Conversations.insertConversation = function(d) {
//	debugger;
    var c = eval("(" + d + ")");
	$("#" + c.document.parent + "-conversations").append(c.document.view + "<br>");
}

function Conversation(cid_, parent_, number_) {
	this.cid = cid_;
	this.parent = parent_;
	this.number = number_;
	this.offset = 0;
}

Conversation.addComment = function(d) {
    var json = eval("(" + d + ")");
    $(json.view + "<br>").appendTo("#" + json.parent + "-conversation").hide().fadeIn(2000);
}

Conversation.prototype.getComments = function(rid, reversed) {
	var THIS = this;
	//debugger;
	if(this.parent) {
		var url = "/resource/" + this.parent + "/conversations/get/" + this.cid + "/getComments?offset=" + THIS.offset + "&number=" + THIS.number + (reversed ? "&reversed=1" : "");
	} else {
		var url = "/resource/" + this.cid + "/getComments?offset=" + THIS.offset + "&number=" + THIS.number + (reversed ? "&reversed=1" : "");
	}
	$.ajax({
        type: "GET",
        url: url, 
        success: function(data, textStatus, jqxhr){
        	THIS.insertComments(JSON.parse(data), rid, reversed);
        	THIS.offset += THIS.number;
        	if(reversed) {
        		THIS.hasOlder(rid, THIS.offset);
        	} else {
        		THIS.hasMore(rid, THIS.offset);
        	}
        },
        error: function(ajax, text, error) {alert(error)}
    });
}

Conversation.prototype.hasOlder = function(id, offset) {
	var THIS = this;
	$.ajax({
        type: "GET",
        url: "/resource/" + THIS.cid + "/getNumberOfFirstLevelComments",
        success: function(data, textStatus, jqxhr){
        	if(data > offset) {
        		var more = THIS.getMoreDialog(id, "Older", "(" + (data - offset) + ")");
        		$("#" + id + "-conversation").prepend(more);
        	} else {
        		// No more
        		//$("#" + id + "-conversation").append("NO MORE" + data + "<br>");
        	}
        },
        error: function(ajax, text, error) {alert(error)}
    });
}

Conversation.prototype.hasMore = function(id, offset) {
	var THIS = this;
	$.ajax({
        type: "GET",
        url: "/resource/" + THIS.cid + "/getNumberOfFirstLevelComments",
        success: function(data, textStatus, jqxhr){
        	if(data > offset) {
        		var more = THIS.getMoreDialog(id);
        		$("#" + id + "-conversation").append(more);
        	} else {
        		// No more
        		//$("#" + id + "-conversation").append("NO MORE" + data + "<br>");
        	}
        },
        error: function(ajax, text, error) {alert(error)}
    });
}

Conversation.prototype.getMoreDialog = function(id, reversed, extra) {
	var text = reversed ? "Older" : "newer";
	var div = document.createElement("div");
	div.innerHTML = text + extra;
	div.style.border = "2px solid";
	div.style.textAlign = "center";
	div.style.backgroundColor = "#998877";
	div.style.marginBottom = "10px";
	div.style.marginTop = "10px";
	
	var THIS = this;
	$(div).on('click', function(){
		THIS.getComments(id, reversed);
		$(this).remove();
	});
	
	return div;
}

Conversation.prototype.insertComments = function(json, id, reversed) {
	var comments = json[id];
	if(comments !== undefined) {
        for( var i = 0 ; i < comments.length ; i++ ) {
        	var e = "#" + comments[i].document.parent + "-conversation";
        	var h = comments[i].document.view + "<br>";
        	if(reversed) {
        		$(e).prepend(h);
        	} else {
        		$(e).append(h);
        	}
        	var child = comments[i].document._id;
          	this.insertComments(json, child);
        }
    }
}

Conversations.prototype.addComment = function(json) {
    $(json.view + "<br>").appendTo("#" + json.parent + "-container").hide().fadeIn(2000);
}

// Handlers

$(document).on("click", '.replyable', function(event) { 
    var p = $(this).parent().parent().attr('id');
    var pp = '#' + p + ' > .reply';
   
    $(pp).toggle();
    
    //var ppp = pp + ' > textarea[name="comment"]';
    //var ppp = '#' + p + ' > textarea[name="comment"]';
    var ppp = '#' + p + ' > textarea';
    //alert(pp);
    //$(pp).children('input[name="comment"]').trigger("focus");
    //$(ppp).trigger("focus");
    //alert($(ppp));
    //$(ppp).focus();
    //var o = $(pp).find("textarea");
    $(pp).find("textarea").focus();
    //debugger;
    //$(ppp).trigger("focus");
    
	event.preventDefault();
    return false;
});

// Toggle conversations
$(document).on("click", ".conversation", function(){
	$(this).parent().children(".container").toggle();
});

// Submit a new conversation
//$( "#conversationSubmit" ).click(function(event) {
$(document).on("click", '#conversationSubmit', function(event) {
    event.preventDefault();
    var form = $('#conversationForm');

    // Remove previously added json inputs
    $(form).children("input[name='json']").remove();

    // Add new json input
    Utils.addJsonElement( form[0] );
    $.ajax({
        type: "POST",
        url: "conversations/add",
        data: form.serialize(),
        //success: function(data, textStatus, jqxhr){$(this).parent()[0].reset();addPost(data)},
        success: function(data, textStatus, jqxhr){
        	Conversations.insertConversation(data)
        },
        error: function(ajax, text, error) {alert(error)}
    });
});


$(document).on("click", '.commentSubmit', function(event) {
	//alert("HEY");
    event.preventDefault();
    //Utils.addJsonElement( document.getElementById('commentForm') );
    var form = $(this).parent();
    //alert("adawd"+form.attr('id'));
    
    // Remove previously added json inputs
    $(form).children("input[name='json']").remove();

    // Add new json input
    Utils.addJsonElement( form[0] );
    
    //
    var c = $(form).children("input[name='conversation']").val();
    $.ajax({
        type: "POST",
        url: "/resource/" + c + "/",
        data: form.serialize(),
        //success: function(data, textStatus, jqxhr){$(this).parent()[0].reset();addPost(data)},
        success: function(data, textStatus, jqxhr){$(form).parent().hide();form[0].reset();Conversation.addComment(data)},
        error: function(ajax, text, error) {alert(error)}
    });
});
