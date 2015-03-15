function Modal(container, options) {
    this.container = container;
    this.options = options;
}

Modal.prototype.initialize() {
    $(this.container).hide();
}
