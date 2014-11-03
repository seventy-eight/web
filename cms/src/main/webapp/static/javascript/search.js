
function Search(formId, queryInputId) {
	this.query = "";
	this.offset = 0;
	
	this.container = "#container";
	this.moreId = '#more';
	this.formId = formId;
	
	var THIS = this;
	
	$(formId).submit(function(event) {
		//debugger;
		THIS.setQuery($(queryInputId).val());
		
		THIS.offset = 0;
		$(THIS.container).empty();
		
		THIS.fetchNext(10);
		return false;
	});
}

Search.prototype.setQuery = function(query) {
	this.query = query;
}

Search.prototype.fetchNext = function(number) {
	var THIS = this;
    $.ajax({
        type: "GET",
        url: "search",
        data: { query: this.query, offset: this.offset, number: number },
        success: function(data, textStatus, jqxhr){/*alert(JSON.stringify(data)) */;THIS.populate(eval(data));},
        error: function(ajax, text, error) {alert(error)}
    });

    this.offset = this.offset + number;
}

Search.prototype.addMore = function() {
	var THIS = this;
	$('<div class="result" style="text-align:center;margin:40px 40px 40px 40px" id="more">More</div>').hide().appendTo(this.container).fadeIn(600);
	$(this.moreId).on('click', function(event) {
		$(THIS.moreId).remove();
		THIS.fetchNext(10);
	});
}

Search.prototype.populate = function(jsonResult) {
	//alert(jsonResult.length);
    if( jsonResult === undefined ) {
        $(this.container).hide();
    } else {
        //for( i = 0 ; i < jsonResult.length ; i++ ) {
    	for(var i in jsonResult) {
        	var data = jsonResult[i];
        	//alert(i + ":" + data);
            //$('<div class="result" id="node' + (i+this.offset) + '">' + data.document.badge + '</div>').hide().insertBefore(this.container).fadeIn(600);
        	$('<div class="result" id="node' + (i+this.offset) + '">' + data.document.badge + '</div>').hide().appendTo(this.container).fadeIn(600);
            //getNode(data[i].document._id, "node" + (i+offset));
        }

    	debugger;
        if( jsonResult.length === undefined || jsonResult.length < 10 ) {
            //$(this.moreId).hide();
        } else {
        	$(this.moreId).show();
        	this.addMore();
        }
    }
}