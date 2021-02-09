/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.id;

import java.util.Date;
import java.util.List;

import com.oracle.nosql.spring.data.repository.NosqlRepository;

public interface RepoSensorIdDate
    extends NosqlRepository<SensorIdDate, Date> {

    List<SensorIdDate> findByTimeBetween(Date fromDate, Date toDate);
}
