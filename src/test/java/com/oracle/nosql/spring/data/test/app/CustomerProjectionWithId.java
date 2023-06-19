/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.app;

import java.util.Objects;

import com.oracle.nosql.spring.data.core.mapping.NosqlId;

public class CustomerProjectionWithId {

    @NosqlId
    long customerId;
    String firstName;
    String lastName;

    public long getCustomerId() {
        return customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomerProjectionWithId)) {
            return false;
        }
        CustomerProjectionWithId that = (CustomerProjectionWithId) o;
        return customerId == that.customerId &&
            Objects.equals(firstName, that.firstName) &&
            Objects.equals(lastName, that.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, firstName, lastName);
    }
}
