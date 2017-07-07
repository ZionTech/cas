define(
	[
		'jquery', 
		'underscore', 
		'backbone', 
		'marionette',
		'bootstrap',
		'js/login',
		'js/views/login/loginviewhelper', 
		'text!templates/login/login.html'
	], 
	function(
		$, 
		_, 
		Backbone, 
		Marionette, 
		Bootstrap,
		Login,
		LoginHelper,
		LoginViewTemplate
	)  {
	return Marionette.ItemView.extend({
		template: LoginViewTemplate,
		templateHelpers: LoginHelper,
		initialize: function() {
			console.log("Login View Initialize()");
		},
		events: {
			"focus #username": "usernameFocus",
			"blur #username": "usernameBlur",
			"focus #password": "passwordFocus",
			"change #password": "checkPasswordFormInput",
			"blur #password": "passwordBlur",
			"submit #loginForm" : "login"
		},
		onShow: function(){
			$('#loginForm input[name=lt]').val($('input[name=loginTicket]').val());
			$('#loginForm input[name=execution]').val($('input[name=flowExecutionKey]').val());
			$('#loginForm').attr('action', $('#tempForm').attr('action'));

			$('#loginForm input[name=prevAddress]').val($('input[name=prevAddressContainer]').val());

			$('#loginColumns .socialNetWorks a#facebook').attr("href", $('#list-providers li#Facebook a').attr("href"));
			$('#loginColumns .socialNetWorks a#twitter').attr("href", $('#list-providers li#Twitter a').attr("href"));
			$('#loginColumns .socialNetWorks a#google').attr("href", $('#list-providers li#Google2 a').attr("href"));
			$('#loginColumns .socialNetWorks a#linkedin').attr("href", $('#list-providers li#LinkedIn2 a').attr("href"));

			$('input[name=appLogo]').remove();
			$('input[name=tenantLogo]').remove();
			$('input[name=loginTicket]').remove();
			$('input[name=flowExecutionKey]').remove();
			$('#tempForm').remove();
			$('input[name=prevAddressContainer]').remove();
			$('#list-providers').remove();
			this.updateTenantBranding();
			this.addForgetPasswordLink();
			this.checkLoginErrorMessage();
			this.focusUserNameInput();
			this.checkPasswordFormInput();
		},
		focusUserNameInput: function(event){
			setTimeout(function(){
				$('#username').focus();
			}, 500);
		},
		checkPasswordFormInput: function(){
			var password = $('#password').val();
			if(password != ""){
				$('.passwordLabel').show();
			}
		},
		usernameFocus: function(event){
			$('.userNameLabel').show();
			$('#username').attr("placeholder", "");
		},
		usernameBlur: function(event){
			if($('#username').val().trim() == ""){
				$('.userNameLabel').hide();
			}
			$('#username').attr("placeholder", "Enter Email");
		},
		passwordFocus: function(event){
			$('.passwordLabel').show();
			$('#password').attr("placeholder", "");
		},
		passwordBlur: function(event){
			if($('#password').val().trim() == ""){
				$('.passwordLabel').hide();
			}
			$('#password').attr("placeholder", "Password");
		},
		updateTenantBranding: function(){
			var serviceUrl = this.getParam("service");
			var decodedUrl = decodeURIComponent(serviceUrl);
			var tenantBrandingImageUrl = this.createLocation(decodedUrl) + '/scim/v2/TenantImage/jpegPhoto/appBranding';
			var tenantBrandingOnErrorUrl = this.createLocation(decodedUrl) + '/scim/v2/TenantImage/jpegPhoto/primary';
			$("#tenantBranding").attr("src", tenantBrandingImageUrl);
			$("#tenantBranding").on("error", function(){
		        $(this).unbind("error").attr('src', tenantBrandingOnErrorUrl);
		    });			
		},
		checkLoginErrorMessage: function(){
			var errors = $("input[name='loginErrorMsg']");
			if(errors.length > 0){
				for(var i = 0; i < errors.length; i++){
					var msg = $(errors[i]).val();
					this.showErrorMessage(msg);
				}
			}
		},
		showErrorMessage: function(msg){
			$('#errorMessage').text(msg).show();
		},
		hideErrorMessage: function(){
			$('#errorMessage').text("").hide();
		},
		errormsgclose: function() {
			$('.notification_inner').hide(500);
			$('.ot_username, .ot_password').removeClass('has-error');
			$('.ot_username .help-inline, .ot_password .help-inline').removeClass('oneteam-error-msg').text('');
		},
		addForgetPasswordLink: function(){
			var serviceUrl = this.getParam("service");
			var decodedUrl = decodeURIComponent(serviceUrl);
			var serviceHost = this.createLocation(decodedUrl);
			var forgetPasswordUrl = serviceHost + "/ics/passwordReset.html";
			$('#forgetPasswordLink').attr("href",forgetPasswordUrl);
		},
		getParam: function(sParam) {
			var sPageURL = window.location.search.substring(1);
			var sURLVariables = sPageURL.split('&');
			for(var i = 0; i < sURLVariables.length; i++) {
				var sParameterName = sURLVariables[i].split('=');
				if (sParameterName[0] == sParam) {
					return sParameterName[1];
				}
			}
		},
		createLocation: function(href){
			var location = document.createElement("a");
			location.href = href;
			//Fix for Internet Explorer
			if (!location.origin) {
				return location.protocol 
				+ "//" + location.hostname 
				+ (location.port ? ':' + location.port: '');
			} else {
				return location.origin;
			}
		},
		login: function(event){
			this.hideErrorMessage();
			var allValid = true;
			//Validate Username
			var username = $('#username');
			if(username.val().trim() == ""){
				$('.userNameInputErrorMessage').text("Please enter your email");
				$('.userNameLabel').addClass("color-red");
				$('#username').addClass('errorBackgroundLine');
				allValid = false;
				event.preventDefault();
			} else {
				$('.userNameInputErrorMessage').text("");
				$('.userNameLabel').removeClass("color-red");
				$('#username').removeClass('errorBackgroundLine');
			}
			
			//Validate Password
			var password = $('#password');
			if(password.val().trim() == ""){
				$('.passwordInputErrorMessage').text("Please enter your password");
				$('.passwordLabel').addClass("color-red");
				$('#password').addClass('errorBackgroundLine');
				allValid = false;
				event.preventDefault();
			}else {
				$('.passwordInputErrorMessage').text("");
				$('.passwordLabel').removeClass("color-red");
				$('#password').removeClass('errorBackgroundLine');
			}
		}
	});
});


/* EOF */