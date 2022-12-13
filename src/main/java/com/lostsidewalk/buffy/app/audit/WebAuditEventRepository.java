package com.lostsidewalk.buffy.app.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class WebAuditEventRepository extends InMemoryAuditEventRepository {
}
