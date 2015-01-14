

/**
 * The fetch function must provide functionality to fetch a number of resource from a given offset.
 * The append argument is a function, that takes the returned json object as input. 
 */

function ResourceBrowser(options /* fetchFunction, number, urlFunction, overlay */) {
	this.options = {
			fetchFunction: undefined,
			number: 10,
			urlFunction: undefined,
			overlay: undefined,
			onClickFunction: undefined, // 
			container: '#container',
			view: 'avatar',
			moreId: '#more'
	};
	
	// Overwrite default values
	for(s in options){this.options[s] = options[s]}
	
	/*
	this.fetchFunction = fetchFunction;
	this.number = number;
	this.urlFunction = urlFunction;
	this.overlay = overlay;
	*/ 
	
	this.offset = 0;
	/*
	this.container = '#container';
	this.view = 'avatar';
	this.moreId = '#more';
	*/
}

ResourceBrowser.prototype.fetch = function() {
	this.options.fetchFunction(this);
}

ResourceBrowser.prototype.append = function(resources) {
	var l = resources.length;
	for(var i = 0 ; i < l ; i++) {
		var r = resources[i].document;
		var e = this.createElement(this.offset + i, r[this.options.view]);
		console.log(e);
		$(e).hide().appendTo(this.options.container).fadeIn(600);
	}
	
	this.offset += this.options.number;
	
    if( l < 10 ) {
        
    } else {
    	$(this.options.moreId).show();
    	this.addMore();
    }
}

ResourceBrowser.prototype.createElement = function(idx, view) {
	/*
	var e = '<a href=';
	if(this.options.urlFunction !== undefined) {
		e += '"' + this.options.urlFunction(idx) + '"';
	} else {
		e += '"javascript:void()"';
	}
	*/

	var e = "";
	//debugger;
	var ctx = this.options.onClickFunction;
	$(this.options.container).on('click', '#node' + idx, function() {
		ctx(idx);
		$('#collectionModal').trigger('openModal');
	});
	
	
	
	/*
	if(this.options.onClickFunction !== undefined) {
		e += ' onClick="' + this.options.onClickFunction + '(' + idx + ')"';
	}
	*/
	
	e += '<div class="result overlayContainer" id="node' + idx + '">';
	e += '<div class="overlayContent">' + view + '</div>';
	if(this.options.overlay !== undefined) {
		e += '<div class="overlay">' + this.options.overlay(data.document, false) + '</div>';
		e += '<div class="" style="height:100%">' + this.options.overlay(data.document, true) + '</div>';
	}
	e += '</div>';
	
	return e;
}

ResourceBrowser.prototype.addMore = function() {
	var THIS = this;
	$('<div class="result" style="text-align:center;margin:40px 40px 40px 40px" id="more">More</div>').hide().appendTo(this.options.container).fadeIn(600);
	$(this.options.moreId).on('click', function(event) {
		$(THIS.options.moreId).remove();
		THIS.fetch();
	});
}