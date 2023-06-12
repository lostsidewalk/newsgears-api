package com.lostsidewalk.buffy.app.querymetrics;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.subscription.SubscriptionMetrics;
import com.lostsidewalk.buffy.subscription.SubscriptionMetricsDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
public class SubscriptionMetricsService {

    @Autowired
    SubscriptionMetricsDao subscriptionMetricsDao;

    public List<SubscriptionMetrics> findByUsername(String username) throws DataAccessException {
        return subscriptionMetricsDao.findByUsername(username);
    }

    public Map<Long, Date> findLatestByUsername(String username) throws DataAccessException {
        return subscriptionMetricsDao.findLatestByUsername(username).entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> toDate(e.getValue())));
    }

    private Date toDate(Timestamp t) {
        return new Date(t.getTime());
    }
}
