package com.lostsidewalk.buffy.app.security;

import com.lostsidewalk.buffy.app.auth.AuthTokenFilter;
import com.lostsidewalk.buffy.app.auth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.lostsidewalk.buffy.app.auth.OAuth2AuthenticationFailureHandler;
import com.lostsidewalk.buffy.app.auth.OAuth2AuthenticationSuccessHandler;
import com.lostsidewalk.buffy.app.user.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static java.util.Collections.singletonList;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
		securedEnabled = true,
		jsr250Enabled = true)
class WebSecurityConfig {

	@Autowired
	AuthTokenFilter authTokenFilter;

	@Bean
	public AuthenticationManager authenticationManager() {
		return authentication -> new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials());
	}

	@Bean
	public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
		return new HttpCookieOAuth2AuthorizationRequestRepository();
	}

	@Autowired
	private CustomOAuth2UserService customOAuth2UserService;

	@Autowired
	private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

	@Autowired
	private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

	private AuthenticationEntryPoint currentUserEntryPoint() {
		return (request, response, authException) -> response.setStatus(SC_UNAUTHORIZED);
	}

//	private static final String CONTENT_SECURITY_POLICY_DIRECTIVES =
//			"default-src 'self';" +
//			"connect-src 'self' http://localhost:8080 ws://192.168.86.180:3000/ws;img-src 'self' data: https://* http://*;" +
//			"style-src 'unsafe-inline' https://fonts.googleapis.com;base-uri 'self';" +
//			"form-action 'self';" +
//			"font-src https://fonts.gstatic.com http://localhost:3000";

	@Value("${newsgears.originUrl}")
	String feedGearsOriginUrl;

	@Bean
	protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
				.logout().disable()
				.csrf().disable()
				.formLogin().disable()
				.httpBasic().disable()
//				.headers(httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer.contentSecurityPolicy(CONTENT_SECURITY_POLICY_DIRECTIVES))
				.cors().configurationSource(request -> {
					CorsConfiguration configuration = new CorsConfiguration();
					configuration.setAllowedOriginPatterns(singletonList(this.feedGearsOriginUrl));
					configuration.setAllowedMethods(singletonList("*"));
					configuration.setAllowedHeaders(singletonList("*"));
					configuration.setAllowCredentials(true);
					return configuration;
				})
				.and().authorizeHttpRequests()
					// permit pre-auth
					.requestMatchers("/").permitAll() // index
					.requestMatchers("/authenticate").permitAll() // user authentication (login)
					.requestMatchers("/oauth2/**").authenticated() // oauth2
					.requestMatchers("/pw_reset").permitAll() // password reset (init)
					.requestMatchers("/pw_reset/**").permitAll() // password reset w/token (get-back)
					.requestMatchers("/deauthenticate").permitAll() // user de-authentication (logout)
					.requestMatchers("/register").permitAll() // user registration (init)
					.requestMatchers("/verify/**").permitAll() // verification (i.e., user registration get-back)
					.requestMatchers("/stripe").permitAll() // stripe callback (payment)
					// permit actuators
					.requestMatchers("/actuator").permitAll()
					.requestMatchers("/actuator/**").permitAll()
					// permit options calls
					.requestMatchers(OPTIONS, "/**").permitAll() // OPTIONS calls are validated downstream by checking for the presence of required headers
					// permit image proxy calls
					.requestMatchers("/proxy/unsecured/**").permitAll()
					// (all others require authentication)
					.anyRequest().authenticated()
				.and().sessionManagement().sessionCreationPolicy(STATELESS)
				.and().exceptionHandling().defaultAuthenticationEntryPointFor(currentUserEntryPoint(), new AntPathRequestMatcher("/**"))
				.and().oauth2Login()
					.authorizationEndpoint().baseUri("/oauth2/authorize")
					.authorizationRequestRepository(cookieAuthorizationRequestRepository())
				.and().redirectionEndpoint().baseUri("/oauth2/callback/*")
				.and().userInfoEndpoint().userService(customOAuth2UserService)
				.and().successHandler(oAuth2AuthenticationSuccessHandler).failureHandler(oAuth2AuthenticationFailureHandler);
		http.addFilterBefore(this.authTokenFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
