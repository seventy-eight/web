

/**
 * The fetch function must provide functionality to fetch a number of resource from a given offset.
 * The append argument is a function, that takes the returned json object as input. 
 */

function ResourceBrowser(fetchFunction, number, urlFunction, overlay) {
	this.fetchFunction = fetchFunction;
	this.number = number;
	this.urlFunction = urlFunction;
	this.overlay = overlay;
	this.offset = 0;
	
	this.container = '#container';
	this.view = 'avatar';
	this.moreId = '#more';
}

ResourceBrowser.prototype.fetch = function() {
	//debugger;
	this.fetchFunction(this);
}

ResourceBrowser.prototype.append = function(resources) {
	var l = resources.length;
	for(var i = 0 ; i < l ; i++) {
		var r = resources[i].document;
		var e = this.createElement(this.offset + i, r[this.view]);
		
		$(e).hide().appendTo(this.container).fadeIn(600);
	}
	
	this.offset += this.number;
	
    if( l < 10 ) {
        
    } else {
    	$(this.moreId).show();
    	this.addMore();
    }
}

ResourceBrowser.prototype.createElement = function(idx, view) {
	var e = '<a href="' + this.urlFunction(idx) + '"><div class="result overlayContainer" id="node' + idx + '">';
	e += '<div class="overlayContent">' + view + '</div>';
	if(this.overlay !== undefined) {
		e += '<div class="overlay">' + this.overlay(data.document, false) + '</div>';
		e += '<div class="" style="height:100%">' + this.overlay(data.document, true) + '</div>';
	}
	e += '</div></a>';
	
	return e;
}

ResourceBrowser.prototype.addMore = function() {
	var THIS = this;
	$('<div class="result" style="text-align:center;margin:40px 40px 40px 40px" id="more">More</div>').hide().appendTo(this.container).fadeIn(600);
	$(this.moreId).on('click', function(event) {
		$(THIS.moreId).remove();
		THIS.fetch();
	});
}