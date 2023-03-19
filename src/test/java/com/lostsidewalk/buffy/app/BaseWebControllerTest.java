package com.lostsidewalk.buffy.app;


import com.lostsidewalk.buffy.*;
import com.lostsidewalk.buffy.app.auth.AuthService;
import com.lostsidewalk.buffy.app.discovery.FeedDiscoveryService;
import com.lostsidewalk.buffy.app.feed.FeedDefinitionService;
import com.lostsidewalk.buffy.app.mail.MailService;
import com.lostsidewalk.buffy.app.opml.OpmlService;
import com.lostsidewalk.buffy.app.order.*;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.query.QueryDefinitionService;
import com.lostsidewalk.buffy.app.settings.SettingsService;
import com.lostsidewalk.buffy.app.thumbnail.ThumbnailService;
import com.lostsidewalk.buffy.app.token.TokenService;
import com.lostsidewalk.buffy.app.token.TokenService.JwtUtil;
import com.lostsidewalk.buffy.app.user.LocalUserService;
import com.lostsidewalk.buffy.app.user.UserRoles;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfoDao;
import com.lostsidewalk.buffy.feed.FeedDefinitionDao;
import com.lostsidewalk.buffy.json.JSONPublisher;
import com.lostsidewalk.buffy.post.PostImporter;
import com.lostsidewalk.buffy.post.PostPurger;
import com.lostsidewalk.buffy.post.StagingPostDao;
import com.lostsidewalk.buffy.query.QueryDefinitionDao;
import com.lostsidewalk.buffy.query.QueryMetricsDao;
import com.lostsidewalk.buffy.rss.RSSPublisher;
import com.lostsidewalk.buffy.rss.RssImporter;
import com.lostsidewalk.buffy.thumbnail.ThumbnailDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static java.time.temporal.ChronoUnit.DAYS;

class BaseWebControllerTest {

    @Autowired
    protected MockMvc mockMvc;
    //
    // transaction manager
    //
    @MockBean
    PlatformTransactionManager platformTransactionManager;
    //
    // service layer
    //
//    @MockBean
//    AuditService auditService;

    @MockBean
    FeedDefinitionService feedDefinitionService;

    @MockBean
    FeedDiscoveryService feedDiscoveryService;

    @MockBean
    QueryDefinitionService queryDefinitionService;

    @MockBean
    StripeOrderService stripeOrderService;

    @MockBean
    SettingsService settingsService;

    @MockBean
    StagingPostService stagingPostService;

    @MockBean
    AuthService authService;

    @MockBean
    TokenService tokenService;

    @MockBean
    LocalUserService userService;

    @MockBean
    MailService mailService;

    @MockBean
    OpmlService opmlService;

    @MockBean
    ThumbnailService thumbnailService;
    //
    // stripe callback queue processors
    //
    @MockBean
    StripeCallbackQueueProcessor stripeCallbackQueueProcessor;

    @MockBean
    CustomerEventQueueProcessor customerEventQueueProcessor;

    @MockBean
    StripeCustomerHandler stripeCustomerHandler;

    @MockBean
    StripeInvoiceHandler stripeInvoiceHandler;
    //
    // post publisher/importer
    //
    @MockBean
    PostPublisher postPublisher;

    @MockBean
    RSSPublisher rssPublisher;

    @MockBean
    JSONPublisher jsonPublisher;

    @MockBean
    PostImporter postImporter;

    @MockBean
    PostPurger postPurger;
    //
    // persistence layer
    //
    @MockBean
    FeatureDao featureDao;

    @MockBean
    FeedDefinitionDao feedDefinitionDao;

    @MockBean
    FeedDiscoveryInfoDao feedDiscoveryInfoDao;

    @MockBean
    FrameworkConfigDao frameworkConfigDao;

    @MockBean
    StagingPostDao stagingPostDao;

    @MockBean
    QueryDefinitionDao queryDefinitionDao;

    @MockBean
    QueryMetricsDao queryMetricsDao;

    @MockBean
    ThumbnailDao thumbnailDao;

    @MockBean
    RoleDao roleDao;

    @MockBean
    UserDao userDao;

    @MockBean
    RssImporter rssImporter;

    protected static final JwtUtil TEST_JWT_UTIL = new JwtUtil() {
        @Override
        public String extractUsername() {
            return "me";
        }

        @Override
        public Date extractExpiration() {
            Instant nowPlus30Days = Instant.now().plus(30L, DAYS);
            return Date.from(nowPlus30Days);
        }

        @Override
        public String extractValidationClaim() {
            return "b4223bd3427db93956acaadf9e425dd259bfb11dac44234604c819dbbf75e180";
        }

        @Override
        public Boolean isTokenValid() {
            return true;
        }

        @Override
        public Boolean isTokenExpired() {
            return false;
        }

        @Override
        public void requireNonExpired() {}

        @Override
        public void validateToken() {
            // pass
        }
    };

    protected static final Collection<GrantedAuthority> TEST_AUTHORITIES = new ArrayList<>();
    static {
        TEST_AUTHORITIES.add(UserRoles.UNVERIFIED_AUTHORITY);
        TEST_AUTHORITIES.add(UserRoles.VERIFIED_AUTHORITY);
        TEST_AUTHORITIES.add(UserRoles.SUBSCRIBER_AUTHORITY);
        TEST_AUTHORITIES.add(UserRoles.DEV_AUTHORITY);
    }

    protected static final UserDetails TEST_USER_DETAILS = new UserDetails() {
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return TEST_AUTHORITIES;
        }

        @Override
        public String getPassword() {
            return "testPassword";
        }

        @Override
        public String getUsername() {
            return "me";
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
