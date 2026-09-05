package com.meridian.platform.common.jpa;

import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/**
 * Oracle dialect for H2 running in MODE=Oracle. Everything Hibernate emits is Oracle SQL (so the
 * queries we run locally are the ones that run on Exadata) except the sequence metadata lookup,
 * which Oracle does from ALL_SEQUENCES. H2 2.x renamed the INFORMATION_SCHEMA.SEQUENCES columns so
 * Hibernate 5.6 cannot read them either; the lookup is a no-op here. Use in local and test
 * profiles only; prod uses the stock Oracle12cDialect. PLAT-0967.
 */
public class LocalOracleDialect extends Oracle12cDialect {

    @Override
    public SequenceInformationExtractor getSequenceInformationExtractor() {
        return SequenceInformationExtractorNoOpImpl.INSTANCE;
    }
}
