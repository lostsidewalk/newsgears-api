package com.lostsidewalk.buffy.app.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.io.Serial;
import java.util.Collection;

import static org.springframework.security.core.SpringSecurityCoreVersion.SERIAL_VERSION_UID;

public final class WebAuthenticationToken extends AbstractAuthenticationToken {

		public WebAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
			super(authorities);
			this.principal = principal;
			this.credentials = credentials;
			super.setAuthenticated(true); // must use super, as we override
		}

		@Serial
		private static final long serialVersionUID = SERIAL_VERSION_UID;

		private final Object principal;

		private Object credentials;

		@Override
		public Object getCredentials() {
			return this.credentials;
		}

		@Override
		public Object getPrincipal() {
			return this.principal;
		}

		@Override
		public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
			Assert.isTrue(!isAuthenticated,
					"Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
			super.setAuthenticated(false);
		}

		@Override
		public void eraseCredentials() {
			super.eraseCredentials();
			this.credentials = null;
		}
	}

