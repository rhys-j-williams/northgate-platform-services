package com.meridian.platform.common.jpa;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/** DB2 dialect for H2 in MODE=DB2. Same reasoning as {@link LocalOracleDialect}; audit-trail only. */
public class LocalDb2Dialect extends DB2Dialect {

    @Override
    public SequenceInformationExtractor getSequenceInformationExtractor() {
        return SequenceInformationExtractorNoOpImpl.INSTANCE;
    }
}
