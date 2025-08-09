package com.chatapp.synk.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class PhoneNumberAuthenticationToken extends AbstractAuthenticationToken {

    private Object principal; //using this field to store userdetails or phoneNumberOrEmail in below constructors.
    private Object credentials; // Will store password

    public PhoneNumberAuthenticationToken() {
        super(null);
    }

    //calls at time of authentication from AuthController
    public PhoneNumberAuthenticationToken(String phoneNumberOrEmail, String password) {
        super(null);
        this.principal = phoneNumberOrEmail;
        this.credentials = password;
        setAuthenticated(false);
    }

    //calls from PhnoneNumberAuthenticationProvider
    public PhoneNumberAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities); // this sets the internal authorities list for AbstractAuthenticationToken
        this.principal = principal; // This will be the actual User object
        this.credentials = credentials; //this is password
        super.setAuthenticated(true);
    }


    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
