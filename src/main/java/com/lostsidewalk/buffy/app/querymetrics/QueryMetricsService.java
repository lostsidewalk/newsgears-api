package com.lostsidewalk.buffy.app.querymetrics;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.query.QueryMetrics;
import com.lostsidewalk.buffy.query.QueryMetricsDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class QueryMetricsService {

    @Autowired
    QueryMetricsDao queryMetricsDao;

    public List<QueryMetrics> findByUsername(String username) throws DataAccessException {
        return queryMetricsDao.findByUsername(username);
    }

    public List<QueryMetrics> findByFeedId(String username, Long feedId) throws DataAccessException {
        return queryMetricsDao.findByFeedId(username, feedId);
    }
}
