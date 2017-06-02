define(
	[ 
	 	'jquery', 
	 	'underscore', 
	 	'backbone', 
	 	'marionette',
	 	'js/views/login/loginHeader',
	 	'js/views/login/loginview',
	 	'js/views/login/loginFooter'
	], 
	function(
		$,
		_,
		Backbone, 
		Marionette,
		LoginHeaderView,
		LoginLayout,
		LoginFooterView
	) {
		return Backbone.Marionette.LayoutView.extend({
			el: 'body',
			template: false,
			regions: {
				main: "#login-main"
			},
			onRender: function() {
				console.log("Wavity Login application layout onRender called");
				this.main.show(new LoginLayout());
			},			
			onShow: function() {
				console.log("Wavity application layout onShow called");
			}
		});
});

// EOF
