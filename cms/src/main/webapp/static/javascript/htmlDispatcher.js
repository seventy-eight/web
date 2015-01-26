function Dispatch() {
}

Dispatch.prototype.start = function(element) {
    var data = {};
    this.dispatch(element, data);
    alert("JSON: " + JSON.stringify(data));
}

Dispatch.prototype.dispatch = function(element, data) {
    // Do not process elements that is disabled
    if($(element).hasClass("disabledTarget")) {
        return;
    }

    var tag = element.tagName;

    if(tag) {
        switch(tag) {
            case "DIV":
                this.visitDiv(element, data);
                break;

            case "FORM":
                this.visitForm(element, data);
                break;

            case "INPUT":
                this.visitInput(element, data);
                break;

            case "LI":
                this.visitLi(element, data);
                break;
                
            case "SELECT":
                this.visitSelect(element, data);
                break;

            case "TABLE":
                this.visitTable(element, data);
                break;

            case "TBODY":
                this.visitTBody(element, data);
                break;

            case "TD":
                this.visitTd(element, data);
                break;

            case "TEXTAREA":
                this.visitTextarea(element, data);
                break;

            case "TR":
                this.visitTr(element, data);
                break;

            case "UL":
                this.visitUl(element, data);
                break;
        }
    }
}

Dispatch.prototype.visitSelect = function(element, data) {
    var name = $(element).attr('name')
    console.log("Visiting select, " + name);

    if(name && name.length > 0) {
        if( element.type == "select-one" ) {
            data[element.name] = element.value;
        } else {
            data[element.name] = {};
            for(var i = 0 ; i < element.length ; i++) {
                var t = data[element.name];
                if(element[i].selected) {
                    t[i] = element[i].value;
                }
            }
        }
    }
}

Dispatch.prototype.visitTextarea = function(element, data) {
    var name = $(element).attr('name')
    console.log("Visiting textarea, " + name);

    if(name && name.length > 0) {
        data[element.name] = element.value;
    }
}

Dispatch.prototype.visitInput = function(element, data) {
    var name = $(element).attr('name')
    console.log("Visiting input, " + name + " / " + element.id);

    if(name && name.length > 0) {
        switch(element.type) {
            case "radio": // Special radio button case
                if(element.checked ) {
                    data[element.name] = element.value;
                }
                break;

            case "checkbox": // Special check-box case
                if( element.checked ) {
                    data[element.name] = element.value;
                } else {
                    data[element.name] = null;
                }
                break;

            default:
                data[element.name] = element.value;
        }
    }
}

Dispatch.prototype.visitDiv = function(element, data) {
    var name = $(element).attr('name')
    console.log("Visiting div, " + name + " / " + element.id);

    if(name && name.length > 0) {
        if($(element).hasClass("targetValue")) {
            if( data[name] == undefined ) {
                data[name] = [];
            }
            data[name].push( element.innerHTML );
        } else if($(element).hasClass("targetObject")) {
            if( data[name] == undefined ) {
                data[name] = {};
            }
            this.dispatchChilds(element, data[name]);
        } else {
            if( data[name] == undefined ) {
                data[name] = [];
            }
            var l = data[name].push( {} );
            this.dispatchChilds(element, data[name][l-1]);
        }
    } else {
        this.dispatchChilds(element, data);
    }
}

Dispatch.prototype.visitForm = function(element, data) {
    console.log("Visiting form");
    this.dispatchChilds(element, data);
}

Dispatch.prototype.visitLi = function(element, data) {
    console.log("Visiting list item");
    this.dispatchChilds(element, data);
}

Dispatch.prototype.visitTable = function(element, data) {
    console.log("Visiting table");
    this.dispatchChilds(element, data);
}

Dispatch.prototype.visitTBody = function(element, data) {
    console.log("Visiting table body");
    this.dispatchChilds(element, data);
}

Dispatch.prototype.visitTr = function(element, data) {
    console.log("Visiting table row");
    this.dispatchChilds(element, data);
}

Dispatch.prototype.visitTd = function(element, data) {
    console.log("Visiting table cell");
    this.dispatchChilds(element, data);
}

Dispatch.prototype.visitUl = function(element, data) {
    console.log("Visiting unordered list");
    this.dispatchChilds(element, data);
}

Dispatch.prototype.dispatchChilds = function(element, data) {
    var childs = element.childNodes;

    for( var i = 0 ; i < childs.length ; i++ ) {
        this.dispatch(childs[i], data);
    }
}
