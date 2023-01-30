package com.lostsidewalk.buffy.app.catalog;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.RenderedCatalogDao;
import com.lostsidewalk.buffy.discovery.ThumbnailedFeedDiscovery;
import com.lostsidewalk.buffy.model.RenderedFeedDiscoveryInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class CatalogService {

    @Autowired
    RenderedCatalogDao renderedCatalogDao;

    public List<ThumbnailedFeedDiscovery> getCatalog() throws DataAccessException {
        return renderedCatalogDao.getCatalog().stream()
                .map(RenderedFeedDiscoveryInfo::getFeedDiscoveryInfo)
                .collect(toList());
    }
}
