package com.lostsidewalk.buffy.app.settings;

import com.google.gson.Gson;
import com.lostsidewalk.buffy.*;
import com.lostsidewalk.buffy.app.model.request.DisplaySettingsUpdateRequest;
import com.lostsidewalk.buffy.app.model.response.DisplaySettingsResponse;
import com.lostsidewalk.buffy.app.model.response.SettingsResponse;
import com.lostsidewalk.buffy.app.model.request.SettingsUpdateRequest;
import com.lostsidewalk.buffy.auth.User;
import com.lostsidewalk.buffy.auth.UserDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@Slf4j
public class SettingsService {

    @Autowired
    UserDao userDao;

    @Autowired
    private FrameworkConfigDao frameworkConfigDao;

    @Autowired
    private ThemeConfigDao themeConfigDao;

    public DisplaySettingsResponse getDisplaySettings(String username) throws DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        FrameworkConfig frameworkConfig = frameworkConfigDao.findByUserId(user.getId());
        Map<String, String> displayConfig = frameworkConfig.getDisplay();
        ThemeConfig themeConfig = themeConfigDao.findByUserId(user.getId());
        return DisplaySettingsResponse.from(displayConfig, themeConfig);
    }

    public void updateDisplaySettings(String username, DisplaySettingsUpdateRequest updateRequest) throws DataAccessException, DataUpdateException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        ThemeConfig themeConfig = updateRequest.getThemeConfig();
        if (themeConfig != null) {
            themeConfigDao.upsertThemeConfig(user.getId(),
                    serializeTheme(themeConfig.getLightTheme()),
                    serializeTheme(themeConfig.getDarkTheme())
            );
        }

        log.debug("Theme configuration updated for userId={}", user.getId());
    }

    public SettingsResponse getFrameworkConfig(String username) throws DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return SettingsResponse.from(
                user.getUsername(),
                user.getEmailAddress(),
                user.getAuthProvider(),
                user.getAuthProviderProfileImgUrl(),
                user.getAuthProviderUsername(),
                frameworkConfigDao.findByUserId(user.getId())
        );
    }

    public void updateFrameworkConfig(String username, SettingsUpdateRequest updateRequest) throws DataAccessException, DataUpdateException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        String emailAddress = updateRequest.getEmailAddress();
        if (isNotBlank(emailAddress) && !StringUtils.equals(user.getEmailAddress(), emailAddress)) {
            userDao.setVerified(username, false);
            user.setEmailAddress(emailAddress);
            userDao.updateEmailAddress(user);
        }

        FrameworkConfig frameworkConfig = updateRequest.getFrameworkConfig();
        if (frameworkConfig != null) {
            frameworkConfig.setUserId(user.getId());
            log.debug("Updating framework configuration for userId={}", user.getId());
            frameworkConfigDao.save(frameworkConfig);
        }

        log.debug("Framework configuration updated for userId={}", user.getId());
    }

    private static final Gson GSON = new Gson();

    private Serializable serializeTheme(Map<String, String> theme) {
        return theme == null ? null : GSON.toJson(theme);
    }
}
