package com.lostsidewalk.buffy.app.settings;

import com.lostsidewalk.buffy.*;
import com.lostsidewalk.buffy.app.model.response.SettingsResponse;
import com.lostsidewalk.buffy.app.model.request.SettingsUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@Slf4j
public class SettingsService {

    @Autowired
    UserDao userDao;

    @Autowired
    private FrameworkConfigDao frameworkConfigDao;

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

        log.info("Framework configuration updated for username={}", username);
    }
}
