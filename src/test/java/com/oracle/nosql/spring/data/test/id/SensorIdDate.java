/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.id;

import java.util.Date;
import java.util.Objects;

import org.springframework.data.annotation.Id;

public class SensorIdDate {
    String name;
    int temp;

    @Id
    Date time;

    @Override
    public String toString() {
        return "SensorIdDate{" +
            "name='" + name + '\'' +
            ", temp=" + temp +
            ", time=" + time +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SensorIdDate)) {
            return false;
        }
        SensorIdDate that = (SensorIdDate) o;
        return temp == that.temp &&
            Objects.equals(name, that.name) &&
            Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, temp, time);
    }
}
