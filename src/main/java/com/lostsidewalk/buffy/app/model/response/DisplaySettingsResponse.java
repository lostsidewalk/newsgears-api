package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.ThemeConfig;
import lombok.Data;

import java.util.Map;

@Data
public class DisplaySettingsResponse {

    final Map<String, String> displayConfig;

    final ThemeConfig themeConfig;

    DisplaySettingsResponse(Map<String, String> displayConfig, ThemeConfig themeConfig) {
        this.displayConfig = displayConfig;
        this.themeConfig = themeConfig;
    }

    public static DisplaySettingsResponse from(Map<String, String> displayConfig, ThemeConfig themeConfig) {
        return new DisplaySettingsResponse(displayConfig, themeConfig);
    }
}
