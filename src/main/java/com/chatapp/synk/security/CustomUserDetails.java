package com.chatapp.synk.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

//adding  JsonIgnoreProperties for now as Caching to redis causing json searlization issue for these fields.
@JsonIgnoreProperties({"enabled", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "authorities"})
public class CustomUserDetails implements UserDetails {
    private String username;//setting phone no in this field
    private String password;
    private String name;
    private String email;
    private String userRoles;
    private String profilePictureUrl;
    private String id;
    @JsonIgnore
    private Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails() {
    }

    /**
     * Constructor for CustomUserDetails.
     * calling in loadsUserByUsername method of CustomUserDetailsService
     *
     * @param username          the username (phone number)
     * @param name              the name of the user
     * @param password          the password of the user
     * @param authorities       the authorities granted to the user
     * @param email             the email of the user
     * @param profilePictureUrl the URL of the user's profile picture
     * @param id                the unique identifier of the user
     */
    public CustomUserDetails(String username, String name, String password, Collection<? extends GrantedAuthority> authorities, String email, String profilePictureUrl, String id) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.name = name;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        //give me uthorities as comma seprated
        this.userRoles = authorities.stream().map(GrantedAuthority::getAuthority).reduce((a, b) -> a + ";" + b).orElse("");
    }

    //this is using in test
    public CustomUserDetails(String username, String name, String email, String userRole, String profilePictureUrl) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.userRoles = userRole;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    //custom attributes

    public String getName() {
        return name;
    }

    public String getUserRoles() {
        return userRoles;
    }

    public String getEmail() {
        return email;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public String getId() {
        return id;
    }
}
