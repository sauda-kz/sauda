package com.sauda.repository;

import java.util.UUID;

public interface LotMatchCountView {

    UUID getLotId();

    long getMatchCount();
}
