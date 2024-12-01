package com.github.bruce_mig.spring_batch;

import com.github.bruce_mig.spring_batch.config.BatchConfig;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBatchTest
@SpringJUnitConfig({SpringBatchApplication.class, BatchConfig.class})
@SpringBootTest
@ActiveProfiles("test")
public class DataSourceTest extends AbstractContainerProviderConfig{

    @Autowired
    private DataSource dataSource;

    @Test
    void testDataSourceInitialization() throws SQLException {
        assertNotNull(dataSource);
        System.out.println("Database URL: " + dataSource.getConnection().getMetaData().getURL());
    }
}
