package com.lostsidewalk.buffy.app.rule;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.rule.RuleSet;
import com.lostsidewalk.buffy.rule.RuleSetDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.lostsidewalk.buffy.rule.RuleSetDao.RuleSetMappingType.QUEUE_IMPORT;
import static com.lostsidewalk.buffy.rule.RuleSetDao.RuleSetMappingType.SUBSCRIPTION_IMPORT;

@Slf4j
@Service
public class RuleSetService {

    @Autowired
    RuleSetDao ruleSetDao;

    public Map<Long, RuleSet> findQueueImportRulesSetsByUsername(String username) throws DataAccessException {
        return ruleSetDao.findRuleSetByUsername(username, QUEUE_IMPORT);
    }

    public Map<Long, RuleSet> findSubscriptionImportRuleSetsByUsername(String username) throws DataAccessException {
        return ruleSetDao.findRuleSetByUsername(username, SUBSCRIPTION_IMPORT);
    }
}
