/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.sequence.MariaDBSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorMariaDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.StandardBasicTypes;

/**
 * A {@linkplain Dialect SQL dialect} for MariaDB
 *
 * @author Vlad Mihalcea
 * @author Gavin King
 */
public class MariaDBDialect extends MySQLDialect {
	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 10, 3 );
	private static final DatabaseVersion MYSQL57 = DatabaseVersion.make( 5, 7 );

	public MariaDBDialect() {
		this( MINIMUM_VERSION );
	}

	public MariaDBDialect(DatabaseVersion version) {
		super(version);
	}

	public MariaDBDialect(DialectResolutionInfo info) {
		super(info);
	}

	@Override
	public DatabaseVersion getMySQLVersion() {
		return MYSQL57;
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry(queryEngine);

		CommonFunctionFactory commonFunctionFactory = new CommonFunctionFactory( queryEngine );
		commonFunctionFactory.windowFunctions();
		commonFunctionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
		queryEngine.getSqmFunctionRegistry().registerNamed(
				"json_valid",
				queryEngine.getTypeConfiguration()
						.getBasicTypeRegistry()
						.resolve( StandardBasicTypes.BOOLEAN )
		);
		commonFunctionFactory.inverseDistributionOrderedSetAggregates_windowEmulation();
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new MariaDBSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public boolean supportsColumnCheck() {
		return true;
	}

	@Override
	protected MySQLStorageEngine getDefaultMySQLStorageEngine() {
		return InnoDBStorageEngine.INSTANCE;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return getVersion().isSameOrAfter( 10, 5 );
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return MariaDBSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return getSequenceSupport().supportsSequences()
				? "select table_name from information_schema.TABLES where table_schema=database() and table_type='SEQUENCE'"
				: super.getQuerySequencesString(); //fancy way to write "null"
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getSequenceSupport().supportsSequences()
				? SequenceInformationExtractorMariaDBDatabaseImpl.INSTANCE
				: super.getSequenceInformationExtractor();
	}

	@Override
	public boolean supportsSkipLocked() {
		//only supported on MySQL and as of 10.6
		return getVersion().isSameOrAfter( 10, 6 );
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}

	@Override
	public boolean supportsWait() {
		return true;
	}

	@Override
	boolean supportsForShare() {
		//only supported on MySQL
		return false;
	}

	@Override
	boolean supportsAliasLocks() {
		//only supported on MySQL
		return false;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {

		// some MariaDB drivers does not return case strategy info
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );

		return super.buildIdentifierHelper( builder, dbMetaData );
	}
}
