package com.lostsidewalk.buffy.app.user;

import com.lostsidewalk.buffy.*;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.audit.ErrorLogService;
import com.lostsidewalk.buffy.app.audit.RegistrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.google.common.collect.Sets.union;
import static com.lostsidewalk.buffy.app.user.UserRoles.*;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@Slf4j
public class LocalUserService implements UserDetailsService {

    @Autowired
    AppLogService appLogService;

    @Autowired
    ErrorLogService errorLogService;

    @Autowired
    UserDao userDao;

    @Autowired
    RoleDao roleDao;

    @Autowired
    FeatureDao featureDao;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("${newsgears.development:false}")
    boolean isDevelopment;
    //
    // user loading
    //
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if ("NONE_PROVIDED".equals(username)) {
            throw new UsernameNotFoundException(username);
        }

        User user;
        try {
            user = userDao.findByName(username);
            if (user == null) {
                throw new UsernameNotFoundException(username);
            }
            Set<SimpleGrantedAuthority> grantedAuthorities = gatherGrantedAuthorities(user);
            Set<SimpleGrantedAuthority> implicitAuthorities = gatherImplicitAuthorities(user);
            return toUserDetails(user, union(grantedAuthorities, implicitAuthorities).immutableCopy(), this.passwordEncoder);
        } catch (DataAccessException e) {
            log.error("Unable to load user due to data access exception");
            errorLogService.logDataAccessException(username, new Date(), e);
            return null;
        }
    }
    //
    // role/feature assignation
    //
    private Set<SimpleGrantedAuthority> gatherGrantedAuthorities(User user) throws DataAccessException {
        Set<String> grantedFeatures = new HashSet<>();
        List<Role> roles = roleDao.findByUsername(user.getUsername());
        if (isNotEmpty(roles)) {
            for (Role r : roles) {
                grantedFeatures.addAll(featureDao.findByRolename(r.getName()));
            }
        }

        return grantedFeatures.stream().map(SimpleGrantedAuthority::new).collect(toSet());
    }

    private Set<SimpleGrantedAuthority> gatherImplicitAuthorities(User user) {
        Set<SimpleGrantedAuthority> implicitFeatures = new HashSet<>();

        // all users get the 'unverified' role
        implicitFeatures.add(UNVERIFIED_AUTHORITY);

        // verified users get the 'verified' role
        if (user.isVerified()) {
            implicitFeatures.add(VERIFIED_AUTHORITY);
        }

        // all users get the 'development' role when that property is enabled
        if (isDevelopment) {
            implicitFeatures.add(DEV_AUTHORITY);
        }

        // subscribed users get the 'subscriber' role (regardless of subscription status)
        if (isNotBlank(user.getSubscriptionStatus())) {
            implicitFeatures.add(SUBSCRIBER_AUTHORITY);
        }

        return implicitFeatures;
    }
    //
    // registration
    //
    public void registerUser(String username, String email, String password) throws RegistrationException, DataAccessException, DataUpdateException {

        List<String> errors = new ArrayList<>();
        errors.addAll(validateUsername(username));
        errors.addAll(validateEmailAddress(email));
        errors.addAll(validatePassword(password));
        errors.addAll(validateUser(username, email));

        List<String> results = errors.stream()
                .filter(Objects::nonNull)
                .collect(toList());

        boolean isValid = results.isEmpty();

        if (isValid) {
            User newUser = new User(username, password, email);
            userDao.add(newUser);
            log.info("Registered user, username={}, email={}", username, email);
        } else {
            throw new RegistrationException(join("; ", results));
        }
    }

    private List<String> validateUsername(String username) {
        if (isBlank(username)) {
            return singletonList("Username must not be blank");
        }

        return emptyList();
    }

    private List<String> validateEmailAddress(String email) {
        if (isBlank(email)) {
            return singletonList("Email address must not be blank");
        }

        return emptyList();
    }

    private List<String> validatePassword(String password) {
        if (isBlank(password)) {
            return singletonList("Password must not be blank");
        }
        if (password.length() < 6) {
            return singletonList("Password must be greater than 6 characters");
        }

        return emptyList();
    }

    private List<String> validateUser(String username, String email) throws DataAccessException {
        boolean alreadyExists;
        alreadyExists = userDao.checkExists(username, email);
        if (alreadyExists) {
            return singletonList("Username and email address must both be unique.");
        }

        return emptyList();
    }

    public void deregisterUser(String username) throws DataAccessException, DataUpdateException {
        userDao.deleteByName(username);
    }
    //
    // verification
    //
    public void markAsVerified(String username) throws DataAccessException, DataUpdateException {
        userDao.setVerified(username, true);
    }
    //
    //
    //
    public AuthProvider getAuthProvider(String username) throws DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return user.getAuthProvider();
    }

    public String getAuthProviderProfileImgUrl(String username) throws DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return user.getAuthProviderProfileImgUrl();
    }
    //
    // utility methods
    //
    private static UserDetails toUserDetails(User user, Set<SimpleGrantedAuthority> grantedAuthorities, PasswordEncoder passwordEncoder) {
        return new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return grantedAuthorities;
            }

            @Override
            public String getPassword() {
                return passwordEncoder.encode(user.getPassword());
            }

            @Override
            public String getUsername() {
                return user.getUsername();
            }

            @Override
            public boolean isAccountNonExpired() {
                return true;
            }

            @Override
            public boolean isAccountNonLocked() {
                return true;
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return true;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
    }
}
