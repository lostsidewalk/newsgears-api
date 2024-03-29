package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.request.DisplaySettingsUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.SettingsUpdateRequest;
import com.lostsidewalk.buffy.app.model.response.DisplaySettingsResponse;
import com.lostsidewalk.buffy.app.model.response.SettingsResponse;
import com.lostsidewalk.buffy.app.settings.SettingsService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.lostsidewalk.buffy.app.user.UserRoles.UNVERIFIED_ROLE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@Validated
public class SettingsController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    SettingsService settingsService;

    @Secured({UNVERIFIED_ROLE})
    @GetMapping("/settings/display")
    public ResponseEntity<DisplaySettingsResponse> getDisplaySettings(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        StopWatch stopWatch = StopWatch.createStarted();
        DisplaySettingsResponse displaySettingsResponse = settingsService.getDisplaySettings(username);
        stopWatch.stop();
        appLogService.logDisplaySettingsFetch(username, stopWatch);
        //
        return ok(displaySettingsResponse);
    }

    @Secured({UNVERIFIED_ROLE})
    @Transactional
    @PutMapping("/settings/display")
    public ResponseEntity<?> updateDisplaySettings(@Valid @RequestBody DisplaySettingsUpdateRequest displaySettingsUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        StopWatch stopWatch = StopWatch.createStarted();
        settingsService.updateDisplaySettings(username, displaySettingsUpdateRequest);
        stopWatch.stop();
        appLogService.logDisplaySettingsUpdate(username, stopWatch);
        return ok(EMPTY);
    }

    @Secured({UNVERIFIED_ROLE})
    @GetMapping("/settings")
    public ResponseEntity<SettingsResponse> getSettings(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        StopWatch stopWatch = StopWatch.createStarted();
        SettingsResponse settingsResponse = settingsService.getFrameworkConfig(username);
        stopWatch.stop();
        appLogService.logSettingsFetch(username, stopWatch);
        //
        return ok(settingsResponse);
    }

    @Secured({UNVERIFIED_ROLE})
    @Transactional
    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(@Valid @RequestBody SettingsUpdateRequest settingsUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException, DataConflictException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        StopWatch stopWatch = StopWatch.createStarted();
        settingsService.updateFrameworkConfig(username, settingsUpdateRequest);
        stopWatch.stop();
        appLogService.logSettingsUpdate(username, stopWatch, settingsUpdateRequest);
        return ok(EMPTY);
    }
}
