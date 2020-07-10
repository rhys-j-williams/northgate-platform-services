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

    /** DB2 says "values nextval for"; H2's DB2 mode only knows the SQL standard spelling. */
    @Override
    public String getSequenceNextValString(String sequenceName) {
        return "select next value for " + sequenceName;
    }

    @Override
    public String getSelectSequenceNextValString(String sequenceName) {
        return "next value for " + sequenceName;
    }
}
