/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import java.util.Objects;

public class IpAddress {
    private final String ip;

    public IpAddress(String ip) {
        this.ip = ip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IpAddress)) {
            return false;
        }
        IpAddress ipAddress = (IpAddress) o;
        return Objects.equals(ip, ipAddress.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip);
    }
}
