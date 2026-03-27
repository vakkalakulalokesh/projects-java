package com.lokesh.jobscheduler.service;

import com.lokesh.jobscheduler.common.enums.ExecutionStatus;
import com.lokesh.jobscheduler.common.enums.JobStatus;
import com.lokesh.jobscheduler.common.enums.JobType;
import com.lokesh.jobscheduler.common.enums.Priority;
import com.lokesh.jobscheduler.common.enums.TriggerType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SchedulerServiceApplicationTests {

    @Test
    void sharedEnumsAndModelsAreConsistent() {
        assertEquals(10, Priority.CRITICAL.getValue());
        assertNotNull(JobType.HTTP_CALL);
        assertNotNull(JobStatus.ACTIVE);
        assertNotNull(ExecutionStatus.PENDING);
        assertNotNull(TriggerType.MANUAL);
    }
}
